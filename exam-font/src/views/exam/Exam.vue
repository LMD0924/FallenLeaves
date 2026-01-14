<script setup>
import { onMounted, ref, onBeforeUnmount, computed, watch } from 'vue'
import { get, post } from '@/net/index.js'
import { message, Modal } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router';

const showBackgroundSettings = ref(false)
const [messageApi, contextHolder] = message.useMessage();
const User = ref({}) // 个人信息
const questions = ref([]) // 题目
const paper = ref({}) // 试卷信息
const answerSheet = ref({}) // 答题卡（使用对象存储，key为题目id）
const countdown = ref(0) // 剩余秒
const submitting = ref(false)
const examLoading = ref(false) // 考试加载状态
const currentPage = ref(1) // 当前页码
const pageSize = 1 // 每页1题
let timer = null
let autosaveTimer = null

// 路由
const route = useRoute();
const router = useRouter();

/* ---------- 处理题目数据转换 ---------- */
const processQuestions = (rawQuestions) => {
  return rawQuestions.map(q => {
    // 处理选项，将optionA, optionB等转换为标准选项格式
    const options = []
    for (let i = 'A'.charCodeAt(0); i <= 'G'.charCodeAt(0); i++) {
      const optionKey = String.fromCharCode(i)
      const optionValue = q[`option${optionKey}`]
      if (optionValue && optionValue.trim() !== '') {
        options.push({
          key: optionKey,
          label: optionValue
        })
      }
    }

    return {
      ...q,
      id: q.id || q.question_id || Math.random(), // 确保每个题目有唯一ID
      // 确保题目类型正确映射
      type: q.questionType === '单选题' ? 'radio' :
        q.questionType === '多选题' ? 'checkbox' :
          q.questionType === '填空题' ? 'blank' :
            q.questionType === '简答题' ? 'essay' : 'radio',
      options: options,
      // 确保分数有默认值
      question_score: q.question_score || 0
    }
  })
}

/* ---------- 加载试卷和题目 ---------- */
const loadExamData = async () => {
  // 获取路由参数中的examId
  const examId = route.query.examId

  if (!examId) {
    message.error('考试ID不存在')
    return
  }

  examLoading.value = true

  try {
    // 获取用户信息
    await getUserInfo()

    // 获取试卷信息
    await new Promise((resolve, reject) => {
      get('/api/exam/SelectExamById', { id: examId },
        (message, data) => {
          paper.value = data
          resolve(data)
        },
        (error) => {
          message.error('获取试卷信息失败')
          reject(error)
        }
      )
    })

    // 获取题目列表
    await new Promise((resolve, reject) => {
      get('/api/exam/getExamQuestionsByExamId', { exam_id: examId },
        (message, data) => {
          // 处理题目数据，转换为前端需要的格式
          questions.value = processQuestions(data)
          console.log('处理后的题目数据:', questions.value)
          // 初始化答题卡为对象
          answerSheet.value = {}
          resolve(data)
        },
        (error) => {
          message.error('获取题目失败')
          reject(error)
        }
      )
    })

    // 创建或获取考试记录
    await createOrGetExamRecord(examId)

    // 设置倒计时
    countdown.value = (paper.value.duration || paper.value.exam_duration || 120) * 60

    // 启动计时器
    if (timer) {
      clearInterval(timer)
    }
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        message.warning('时间到，系统自动交卷')
        submitPaper()
      }
    }, 1000)

  } catch (error) {
    console.error('加载考试数据失败:', error)
  } finally {
    examLoading.value = false
  }
}

/* ---------- 组件挂载时加载数据 ---------- */
onMounted(() => {
  loadExamData()
})

/* ---------- 监听当前题目变化，保存答案 ---------- */
watch(currentPage, (newPage) => {
  // 自动保存当前题目的答案到本地存储
  saveCurrentAnswer()
})

/* ---------- 保存当前题目的答案 ---------- */
const saveCurrentAnswer = () => {
  // 确保 currentQuestion 已经初始化
  const question = currentQuestion.value
  if (!question) {
    console.log('currentQuestion 还未初始化')
    return
  }

  if (question) {
    const answer = answerSheet.value[question.id]
    if (answer !== undefined && answer !== null) {
      localStorage.setItem(`exam_${route.query.examId}_answer_${question.id}`, JSON.stringify(answer))
    }
  }
}

/* ---------- 加载已保存的答案 ---------- */
const loadSavedAnswers = () => {
  const examId = route.query.examId
  if (!examId) return

  questions.value.forEach(question => {
    const saved = localStorage.getItem(`exam_${examId}_answer_${question.id}`)
    if (saved) {
      try {
        answerSheet.value[question.id] = JSON.parse(saved)
      } catch (e) {
        console.error('加载保存的答案失败:', e)
      }
    }
  })
}

// 在获取题目后加载保存的答案
watch(questions, () => {
  if (questions.value.length > 0) {
    loadSavedAnswers()
  }
})

// 监听答案变化，实现自动保存
watch(() => JSON.stringify(answerSheet.value), () => {
  // 延迟保存，避免频繁保存
  clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(() => {
    Object.keys(answerSheet.value).forEach(key => {
      localStorage.setItem(`exam_${route.query.examId}_answer_${key}`, JSON.stringify(answerSheet.value[key]))
    })
  }, 1000)
}, { deep: true })

onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(autosaveTimer)
  // 组件卸载前保存所有答案
  saveCurrentAnswer()
})

/* ---------- 计算属性 ---------- */
// 当前显示的题目
const currentQuestion = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize
  return questions.value[startIndex]
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(questions.value.length / pageSize)
})

// 计算已答题数量
const answeredCount = computed(() => {
  return Object.keys(answerSheet.value).filter(key => {
    const answer = answerSheet.value[key]
    return answer !== undefined && answer !== null && answer !== '' &&
      (!Array.isArray(answer) || answer.length > 0)
  }).length
})

// 计算试卷总分
const totalScore = computed(() => {
  return questions.value.reduce((sum, q) => sum + (q.question_score || 0), 0)
})

// 当前题目的答案
const currentAnswer = computed({
  get() {
    const question = currentQuestion.value
    return question ? answerSheet.value[question.id] : undefined
  },
  set(value) {
    const question = currentQuestion.value
    if (question) {
      answerSheet.value[question.id] = value
      saveCurrentAnswer() // 保存到本地存储
    }
  }
})

// 是否已答当前题目
const isCurrentAnswered = computed(() => {
  const answer = currentAnswer.value
  if (answer === undefined || answer === null || answer === '') return false
  if (Array.isArray(answer)) return answer.length > 0
  return true
})

/* ---------- 工具函数 ---------- */
const formatTime = (sec) => {
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60

  if (h > 0) {
    return `${h}时${String(m).padStart(2, '0')}分${String(s).padStart(2, '0')}秒`
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const scrollToQuestion = (idx) => {
  currentPage.value = idx + 1
}

// 获取个人信息
const getUserInfo = () => {
  return new Promise((resolve) => {
    get('api/user/current', {},
      (message, data) => {
        User.value = data
        console.log('用户信息:', User.value)
        resolve(data)
      },
      (error) => {
        console.error('获取用户信息失败:', error)
        resolve(null)
      })
  })
}

// 创建或获取考试记录
const createOrGetExamRecord = (examId) => {
  return new Promise((resolve, reject) => {
    // 先尝试获取现有记录
    get('/api/exam/record/find', { examId: examId },
      (message, data) => {
        if (data && data.id) {
          console.log('找到现有考试记录:', data)
          resolve(data)
        } else {
          // 如果没有记录，创建新记录
          createExamRecord(examId).then(resolve).catch(reject)
        }
      },
      (error) => {
        // 如果获取失败，尝试创建新记录
        console.log('获取考试记录失败，尝试创建新记录:', error)
        createExamRecord(examId).then(resolve).catch(reject)
      }
    )
  })
}

// 创建考试记录
const createExamRecord = (examId) => {
  return new Promise((resolve, reject) => {
    const recordData = {
      examId: examId,
      studentId: User.value.id,
      startTime:new Date().toISOString(),
      // 使用统一的英文状态编码，便于前后端统一处理
      status: 'in_progress',
      attemptCount: 1
    }

    post('/api/exam/record/insert', recordData,
      (message, data) => {
        console.log('创建考试记录成功:', data)
        resolve(data)
      },
      (error) => {
        console.error('创建考试记录失败:', error)
        // 即使创建失败也继续，可能记录已存在
        resolve(null)
      }
    )
  })
}

/* ---------- 导航功能 ---------- */
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

/* ---------- 标记题目功能 ---------- */
const markedQuestions = ref(new Set()) // 标记的题目索引

const toggleMark = (idx) => {
  if (markedQuestions.value.has(idx)) {
    markedQuestions.value.delete(idx)
  } else {
    markedQuestions.value.add(idx)
  }
}

const isMarked = (idx) => {
  return markedQuestions.value.has(idx)
}

/* ---------- 交卷功能 ---------- */
const submitPaper = () => {
  if (submitting.value || examLoading.value) return

  // 确认提交
  Modal.confirm({
    title: '确认交卷',
    content: '确定要提交试卷吗？提交后将无法修改答案。',
    okText: '确认提交',
    cancelText: '取消',
    onOk() {
      submitting.value = true

      // 构建提交数据 - 修复：使用examId而不是paperId
      const submitData = {
        paperId: parseInt(route.query.examId), // 后端期望的是examId
        answers: Object.keys(answerSheet.value).map(key => {
          const answer = answerSheet.value[key]
          // 确保questionId是整数
          const questionId = typeof key === 'string' ? parseInt(key) : key
          return {
            questionId: questionId,
            answer: answer
          }
        })
      }

      console.log('提交数据:', submitData)

      // 实际提交接口
      post('/api/exam/record/submit-paper', submitData,
        (msg) => {
          message.success(msg || '提交成功！')
          // 清除计时器
          if (timer) {
            clearInterval(timer)
          }

          // 清除本地存储的答案
          Object.keys(answerSheet.value).forEach(key => {
            localStorage.removeItem(`exam_${route.query.examId}_answer_${key}`)
          })

          // 跳转到“我的考试/试卷列表”页面
          setTimeout(() => {
            router.push('/CreateExam')
          }, 1500)
        },
        (error) => {
          message.error('提交失败: ' + (error.message || '网络错误'))
          submitting.value = false
        }
      )
    }
  })
}

/* ---------- 页面离开前提示 ---------- */
window.addEventListener('beforeunload', (e) => {
  if (questions.value.length > 0 && countdown.value > 0 && !submitting.value) {
    e.preventDefault()
    e.returnValue = '您的考试尚未提交，确定要离开吗？'
    return e.returnValue
  }
})
</script>

<template>
  <contextHolder/>
  <div class="bg-black min-h-screen">
    <!-- 顶部 -->
    <header class="flex items-center justify-between p-6 bg-white/5 border-b border-white/10">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
          <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
        </svg>
        <h1 class="text-2xl font-bold text-white">{{ paper.title || paper.exam_title || '考试进行中' }}</h1>
      </div>

      <!-- 倒计时 & 进度信息 -->
      <div class="flex items-center space-x-6">
        <div class="text-lg font-mono text-red-400 flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/>
          </svg>
          {{ formatTime(countdown) }}
        </div>

        <div class="text-sm text-white/80 flex items-center">
          <span>{{ answeredCount }}/{{ questions.length }}题</span>
          <span class="mx-2">|</span>
          <span>第 {{ currentPage }}/{{ totalPages }} 页</span>
        </div>

        <button @click="showBackgroundSettings=true" class="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-white" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- 主体 -->
    <main class="flex-1 p-8 overflow-auto">
      <!-- 加载状态 -->
      <div v-if="examLoading" class="flex justify-center items-center h-full">
        <div class="text-center">
          <svg class="animate-spin h-16 w-16 text-indigo-400 mx-auto" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-4 text-lg text-white">加载考试中...</p>
        </div>
      </div>

      <!-- 考试内容 -->
      <div v-else class="max-w-6xl mx-auto">
        <div class="grid grid-cols-4 gap-8">
          <!-- 左侧信息栏 -->
          <aside class="col-span-1 space-y-6">
            <!-- 个人信息 -->
            <div class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-6 border border-white/10">
              <h3 class="text-lg font-medium text-white mb-3">个人信息</h3>
              <div class="space-y-2 text-sm">
                <p><span class="text-white/60">姓名：</span><span class="text-white font-bold">{{ User.account || User.username || '-' }}</span></p>
                <p><span class="text-white/60">身份：</span><span class="text-white">{{ User.role || '-' }}</span></p>
                <p><span class="text-white/60">学号：</span><span class="text-white">{{ User.id || '-' }}</span></p>
              </div>
            </div>

            <!-- 试卷信息 -->
            <div class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-6 border border-white/10">
              <h3 class="text-lg font-medium text-white mb-3">试卷信息</h3>
              <div class="space-y-2 text-sm">
                <p><span class="text-white/60">总分：</span><span class="text-white font-bold">{{ totalScore }}</span></p>
                <p><span class="text-white/60">题目数：</span><span class="text-white">{{ questions.length }}</span></p>
                <p><span class="text-white/60">限时：</span><span class="text-white">{{ paper.duration || paper.exam_duration || 120 }} min</span></p>
                <p v-if="paper.exam_pass_score"><span class="text-white/60">及格分：</span><span class="text-white">{{ paper.exam_pass_score }} 分</span></p>
              </div>
            </div>

            <!-- 答题卡 -->
            <div class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-6 border border-white/10">
              <h3 class="text-lg font-medium text-white mb-3">答题卡</h3>
              <div class="grid grid-cols-5 gap-2 mb-4">
                <span v-for="(_, idx) in questions" :key="idx"
                      :class="{
                        'bg-indigo-600 text-white ring-2 ring-indigo-400': answerSheet[questions[idx].id] !== undefined,
                        'bg-amber-600 text-white ring-2 ring-amber-400': isMarked(idx),
                        'bg-white/10 text-white/50': answerSheet[questions[idx].id] === undefined && !isMarked(idx)
                      }"
                      class="text-center rounded py-2 text-sm cursor-pointer transition-all hover:bg-white/20"
                      @click="scrollToQuestion(idx)">
                  {{ idx + 1 }}
                </span>
              </div>
              <div class="flex items-center justify-center space-x-4 text-xs text-white/70">
                <div class="flex items-center">
                  <div class="w-3 h-3 bg-indigo-600 rounded mr-1"></div>
                  <span>已答</span>
                </div>
                <div class="flex items-center">
                  <div class="w-3 h-3 bg-amber-600 rounded mr-1"></div>
                  <span>标记</span>
                </div>
                <div class="flex items-center">
                  <div class="w-3 h-3 bg-white/10 rounded mr-1"></div>
                  <span>未答</span>
                </div>
              </div>
            </div>

            <!-- 导航按钮 -->
            <div class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-6 border border-white/10">
              <h3 class="text-lg font-medium text-white mb-4">题目导航</h3>
              <div class="grid grid-cols-2 gap-3">
                <button @click="prevPage"
                        :disabled="currentPage <= 1"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                  上一题
                </button>
                <button @click="nextPage"
                        :disabled="currentPage >= totalPages"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                  下一题
                </button>
              </div>
              <div class="mt-4">
                <button @click="toggleMark(currentPage - 1)"
                        class="w-full px-4 py-2 bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 rounded-lg transition-colors">
                  {{ isMarked(currentPage - 1) ? '取消标记' : '标记本题' }}
                </button>
              </div>
            </div>
          </aside>

          <!-- 右侧题目区 -->
          <section class="col-span-3">
            <!-- 当前题目显示区 -->
            <div v-if="currentQuestion"
                 class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-8 border border-white/10 hover:border-indigo-400/30 transition-colors">
              <!-- 题目头部 -->
              <div class="flex items-start justify-between mb-6">
                <div>
                  <h4 class="text-white font-semibold text-xl">
                    <span class="text-indigo-400">第 {{ currentPage }} 题</span>
                    <span class="mx-2">·</span>
                    <span>{{ currentQuestion.text }}</span>
                  </h4>
                  <div class="flex items-center mt-2 space-x-4">
                    <span class="px-3 py-1 bg-indigo-900/30 rounded text-sm text-indigo-300">
                      分值：{{ currentQuestion.question_score || 0 }} 分
                    </span>
                    <span v-if="currentQuestion.difficulty" class="px-3 py-1 bg-gray-700/50 rounded text-sm text-white/80">
                      难度：{{ currentQuestion.difficulty }}
                    </span>
                    <span class="px-3 py-1 bg-purple-900/30 rounded text-sm text-purple-300">
                      {{ currentQuestion.questionType }}
                    </span>
                  </div>
                </div>
                <div class="flex items-center space-x-2">
                  <span :class="{
                    'px-3 py-1 rounded text-sm': true,
                    'bg-green-900/30 text-green-300': isCurrentAnswered,
                    'bg-red-900/30 text-red-300': !isCurrentAnswered
                  }">
                    {{ isCurrentAnswered ? '已作答' : '未作答' }}
                  </span>
                  <button @click="toggleMark(currentPage - 1)"
                          :class="{
                            'p-2 rounded-full transition-colors': true,
                            'bg-amber-600/20 hover:bg-amber-600/30 text-amber-300': isMarked(currentPage - 1),
                            'bg-white/5 hover:bg-white/10 text-white/50': !isMarked(currentPage - 1)
                          }">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 题目内容 -->
              <div class="mb-8">
                <!-- 单选 -->
                <a-radio-group v-if="currentQuestion.type === 'radio'"
                               v-model:value="currentAnswer"
                               class="mt-3 space-y-4 block">
                  <a-radio v-for="opt in currentQuestion.options" :key="opt.key"
                           :value="opt.key"
                           class="text-white/90 text-lg">
                    <div class="flex items-center p-3 hover:bg-white/5 rounded-lg transition-colors">
                      <span class="w-8 text-center font-bold text-xl text-indigo-400">{{ opt.key }}.</span>
                      <span class="ml-3">{{ opt.label }}</span>
                    </div>
                  </a-radio>
                </a-radio-group>

                <!-- 多选 -->
                <a-checkbox-group v-else-if="currentQuestion.type === 'checkbox'"
                                  v-model:value="currentAnswer"
                                  class="mt-3 space-y-4 block">
                  <a-checkbox v-for="opt in currentQuestion.options" :key="opt.key"
                              :value="opt.key"
                              class="text-white/90 text-lg">
                    <div class="flex items-center p-3 hover:bg-white/5 rounded-lg transition-colors">
                      <span class="w-8 text-center font-bold text-xl text-indigo-400">{{ opt.key }}.</span>
                      <span class="ml-3">{{ opt.label }}</span>
                    </div>
                  </a-checkbox>
                </a-checkbox-group>

                <!-- 填空 -->
                <div v-else-if="currentQuestion.type === 'blank'" class="mt-6">
                  <label class="block text-white/80 mb-3 text-lg">请填写答案：</label>
                  <a-input v-model:value="currentAnswer"
                           placeholder="在此输入答案..."
                           class="bg-white/5 border-white/10 text-white placeholder-white/40 text-lg py-3"/>
                </div>

                <!-- 简答题 -->
                <div v-else-if="currentQuestion.type === 'essay'" class="mt-6">
                  <label class="block text-white/80 mb-3 text-lg">请回答：</label>
                  <a-textarea v-model:value="currentAnswer"
                              :rows="6"
                              placeholder="在此输入详细答案..."
                              class="bg-white/5 border-white/10 text-white placeholder-white/40 text-lg"/>
                  <p class="text-white/60 text-sm mt-2">提示：简答题请尽量详细作答，可使用多行文字描述。</p>
                </div>
              </div>

              <!-- 知识点和导航 -->
              <div class="border-t border-white/10 pt-6">
                <div class="flex items-center justify-between">
                  <div v-if="currentQuestion.knowledge && currentQuestion.knowledge !== '暂无'"
                       class="flex items-center">
                    <span class="text-sm text-white/60 mr-2">相关知识点：</span>
                    <span class="text-sm px-3 py-1 bg-green-900/20 text-green-300 rounded-full">
                      {{ currentQuestion.knowledge }}
                    </span>
                  </div>
                  <div class="flex items-center space-x-3">
                    <span class="text-white/60">第 {{ currentPage }}/{{ totalPages }} 题</span>
                    <div class="flex space-x-2">
                      <button @click="prevPage"
                              :disabled="currentPage <= 1"
                              class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                        上一题
                      </button>
                      <button @click="nextPage"
                              :disabled="currentPage >= totalPages"
                              class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
                        下一题
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 页码导航 -->
            <div class="mt-6 flex items-center justify-between">
              <div class="flex items-center space-x-4">
                <button @click="goToPage(1)"
                        :disabled="currentPage <= 1"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50">
                  首页
                </button>
                <button @click="prevPage"
                        :disabled="currentPage <= 1"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>

              <div class="flex items-center space-x-2">
                <span v-for="page in Math.min(5, totalPages)" :key="page"
                      @click="goToPage(page)"
                      :class="{
                        'w-10 h-10 flex items-center justify-center rounded-lg cursor-pointer transition-colors': true,
                        'bg-indigo-600 text-white': currentPage === page,
                        'bg-white/10 hover:bg-white/20 text-white': currentPage !== page
                      }">
                  {{ page }}
                </span>
                <span v-if="totalPages > 5" class="text-white/50 px-2">...</span>
              </div>

              <div class="flex items-center space-x-4">
                <button @click="nextPage"
                        :disabled="currentPage >= totalPages"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button @click="goToPage(totalPages)"
                        :disabled="currentPage >= totalPages"
                        class="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors disabled:opacity-50">
                  末页
                </button>
              </div>
            </div>

            <!-- 交卷按钮 -->
            <div class="mt-8 text-center sticky bottom-8 py-6 bg-gray-900/80 backdrop-blur-lg rounded-xl border border-white/10">
              <div class="mb-4 text-white/80 text-lg">
                已完成 {{ answeredCount }}/{{ questions.length }} 题
                <span v-if="questions.length > 0" class="ml-4 text-indigo-300">
                  {{ Math.round((answeredCount / questions.length) * 100) }}%
                </span>
              </div>
              <button @click="submitPaper"
                      :disabled="submitting || questions.length === 0"
                      class="px-16 py-4 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 rounded-xl text-white font-bold text-lg transition-all disabled:opacity-50 disabled:from-gray-600 disabled:to-gray-600 disabled:hover:from-gray-600 disabled:hover:to-gray-600">
                {{ submitting ? '提交中...' : '立即交卷' }}
              </button>
              <p class="mt-3 text-white/60 text-sm">提交后无法修改答案，请确认已完成所有题目</p>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 单选/多选项样式优化 */
:deep(.ant-radio-wrapper) {
  align-items: flex-start !important;
}

:deep(.ant-radio) {
  margin-top: 0.5rem !important;
}

:deep(.ant-checkbox-wrapper) {
  align-items: flex-start !important;
}

:deep(.ant-checkbox) {
  margin-top: 0.5rem !important;
}

/* 输入框和文本区域样式优化 */
:deep(.ant-input),
:deep(.ant-textarea) {
  background: rgba(255, 255, 255, 0.05) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: white !important;
}

:deep(.ant-input):focus,
:deep(.ant-textarea):focus {
  border-color: #818cf8 !important;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2) !important;
}

:deep(.ant-input)::placeholder,
:deep(.ant-textarea)::placeholder {
  color: rgba(255, 255, 255, 0.4) !important;
}
</style>
