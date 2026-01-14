<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 顶部标题和导航 -->
    <div class="bg-white shadow-sm border-b">
      <div class="container mx-auto px-6 py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800">考试批阅系统</h1>
            <p class="text-gray-600 mt-1">快速高效地批改学生考试答案</p>
          </div>
          
          <a-button type="primary" @click="goToRecordList">
            返回考试记录列表
          </a-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="container mx-auto px-6 py-8">
      <!-- 考试信息卡片 -->
      <div class="bg-white rounded-xl shadow p-6 mb-8">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div>
            <p class="text-sm font-medium text-gray-500">试卷名称</p>
            <p class="text-lg font-semibold text-gray-800">{{ examInfo.title || '-' }}</p>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-500">学生姓名</p>
            <p class="text-lg font-semibold text-gray-800">{{ studentInfo.username || '-' }}</p>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-500">学生学号</p>
            <p class="text-lg font-semibold text-gray-800">{{ studentInfo.account || '-' }}</p>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-500">考试状态</p>
            <a-tag :color="getStatusColor(examRecord.status)">
              {{ getStatusText(examRecord.status) }}
            </a-tag>
          </div>
        </div>
      </div>

      <!-- 批改面板 -->
      <div class="bg-white rounded-xl shadow overflow-hidden">
        <div class="p-6 border-b">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">试卷批改</h2>
            <div class="flex items-center space-x-4">
              <span class="text-lg font-bold text-green-600">
                当前总分: {{ currentScore }}
              </span>
              <span class="text-gray-500">/ {{ examInfo.total_score || 100 }}</span>
            </div>
          </div>
        </div>

        <!-- 评分进度 -->
        <div class="p-6 border-b">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-600">批改进度</span>
            <span class="text-sm font-medium text-gray-800">
              {{ gradedCount }} / {{ questions.length }} 题
            </span>
          </div>
          <a-progress 
            :percent="progressPercentage" 
            :format="progressFormat" 
            :status="progressStatus" 
          />
        </div>

        <!-- 题目列表 -->
        <div class="p-6">
          <div class="space-y-8">
            <div v-for="(question, index) in questions" :key="question.id" 
                 class="border rounded-lg p-6 transition-all duration-300 hover:shadow-md">
              <div class="flex flex-col md:flex-row md:items-start justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center space-x-3">
                    <span class="text-lg font-bold text-indigo-600">第 {{ index + 1 }} 题</span>
                    <a-tag :color="getQuestionTypeColor(question.type)">
                      {{ getQuestionTypeName(question.type) }}
                    </a-tag>
                    <span class="text-sm text-gray-500">分值: {{ question.score }} 分</span>
                  </div>
                  <p class="mt-3 text-gray-800">{{ question.text }}</p>

                  <!-- 选项（选择题） -->
                  <div v-if="(question.type === 'radio' || question.type === 'checkbox') && question.options" 
                       class="mt-4 bg-gray-50 p-4 rounded-lg">
                    <p class="text-sm font-medium text-gray-700 mb-2">选项：</p>
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
                      <div v-for="(option, optIndex) in question.options" :key="optIndex" 
                           class="flex items-center p-2 border rounded">
                        <span class="w-6 h-6 flex items-center justify-center mr-2 text-gray-700 font-medium">{{ getOptionLabel(optIndex) }}</span>
                        <span class="text-gray-800">{{ option }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 得分显示 -->
                <div class="flex items-center justify-center w-24 h-24 bg-indigo-50 rounded-full">
                  <span class="text-2xl font-bold" :class="{ 'text-green-600': question.graded, 'text-gray-400': !question.graded }">
                    {{ question.obtained_score || 0 }}
                  </span>
                </div>
              </div>

              <!-- 答案区域 -->
              <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- 正确答案 -->
                <div class="bg-green-50 p-4 rounded-lg">
                  <p class="text-sm font-medium text-green-700 mb-2">正确答案：</p>
                  <p class="text-green-800">{{ formatAnswer(question.correct_answer, question.type) }}</p>
                </div>

                <!-- 学生答案 -->
                <div class="bg-gray-50 p-4 rounded-lg">
                  <p class="text-sm font-medium text-gray-700 mb-2">学生答案：</p>
                  <div v-if="question.student_answer">
                    <template v-if="question.type === 'checkbox'">
                      <a-tag v-for="opt in question.student_answer.split(',')" 
                             :key="opt" 
                             class="mr-2 mb-2 bg-blue-100 text-blue-800">
                        {{ opt }}
                      </a-tag>
                    </template>
                    <template v-else-if="question.type === 'essay'">
                      <div class="min-h-[100px] p-2 border border-gray-200 rounded bg-white">
                        {{ question.student_answer }}
                      </div>
                    </template>
                    <template v-else>
                      <p class="text-gray-800">{{ question.student_answer }}</p>
                    </template>
                  </div>
                  <p v-else class="text-gray-500 italic">学生未回答</p>
                </div>
              </div>

              <!-- 批改区域 -->
              <div class="mt-6 border-t pt-4">
                <div class="flex flex-col md:flex-row gap-4">
                  <div class="flex-1">
                    <label class="block text-sm font-medium text-gray-700 mb-2">给分</label>
                    <div class="flex items-center">
                      <a-input-number 
                        v-model:value="question.obtained_score" 
                        :min="0" 
                        :max="question.score" 
                        :step="0.5"
                        style="width: 120px"
                        @change="() => { markAsGraded(question); updateProgress() }"
                      />
                      <span class="text-gray-500 ml-2">/ {{ question.score }}</span>
                    </div>
                  </div>

                  <div class="flex-1">
                    <label class="block text-sm font-medium text-gray-700 mb-2">评语</label>
                    <a-textarea 
                      v-model:value="question.comment" 
                      placeholder="输入评语（可选）"
                      :rows="3"
                      @change="markAsGraded(question)"
                    />
                  </div>

                  <div class="flex items-end">
                    <a-button 
                      type="primary" 
                      @click="saveQuestionScore(question)"
                      :loading="question.saving"
                      class="min-w-[100px]"
                    >
                      保存
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="mt-8 flex justify-end space-x-4">
            <a-button @click="saveAllScores" :loading="savingAll" class="min-w-[120px]">
              保存全部
            </a-button>
            <a-button 
              type="primary" 
              @click="completeGrading"
              :loading="completing"
              class="min-w-[120px]"
              :disabled="gradedCount === 0"
            >
              完成批改
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { get, post } from '@/net/index.js'
import { message, progress, tag, button, input, textarea, inputNumber } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

// 路由和导航
const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(false)
const savingAll = ref(false)
const completing = ref(false)

// 数据
const studentInfo = reactive({})
const examInfo = reactive({})
const examRecord = reactive({})
const questions = ref([])

// 从路由参数获取ID
const examRecordId = computed(() => route.query.recordId ? parseInt(route.query.recordId) : null)
const studentId = computed(() => route.query.studentId ? parseInt(route.query.studentId) : null)
const examId = computed(() => route.query.examId ? parseInt(route.query.examId) : null)

// 计算属性
const currentScore = computed(() => {
  return questions.value.reduce((sum, q) => sum + (q.obtained_score || 0), 0)
})

const gradedCount = computed(() => {
  return questions.value.filter(q => q.graded).length
})

const progressPercentage = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((gradedCount.value / questions.value.length) * 100)
})

const progressStatus = computed(() => {
  if (gradedCount.value === 0) return 'normal'
  if (gradedCount.value < questions.value.length) return 'active'
  return 'success'
})

// 加载数据
const loadData = async () => {
  if (!examRecordId.value || !studentId.value || !examId.value) {
    message.error('缺少必要的考试信息参数')
    return
  }

  loading.value = true
  try {
    // 1. 加载学生信息
    await loadStudentInfo()
    // 2. 加载考试信息
    await loadExamInfo()
    // 3. 加载考试记录
    await loadExamRecord()
    // 4. 加载题目和答案
    await loadQuestionsAndAnswers()
  } catch (error) {
    message.error('加载数据失败: ' + error.message)
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载学生信息
const loadStudentInfo = () => {
  return new Promise((resolve, reject) => {
    // 使用用户接口按ID获取学生信息
    get('/api/user/getUserById', { id: studentId.value },
      (msg, data) => {
        Object.assign(studentInfo, data)
        resolve(data)
      },
      reject
    )
  })
}

// 加载考试信息
const loadExamInfo = () => {
  return new Promise((resolve, reject) => {
    get('/api/exam/SelectExamById', { id: examId.value },
      (msg, data) => {
        Object.assign(examInfo, data)
        resolve(data)
      },
      reject
    )
  })
}

// 加载考试记录
const loadExamRecord = () => {
  return new Promise((resolve, reject) => {
    get('/api/exam/record/detail', { id: examRecordId.value },
      (msg, data) => {
        Object.assign(examRecord, data)
        resolve(data)
      },
      reject
    )
  })
}

// 加载题目和答案
const loadQuestionsAndAnswers = () => {
  return new Promise((resolve, reject) => {
    // 加载题目
    get('/api/exam/getExamQuestionsByExamId', { exam_id: examId.value },
      (msg, questionData) => {
        // 加载学生答案（根据考试记录ID）
        get('/api/exam/answer/record', { examRecordId: examRecordId.value },
          (msg, answerData) => {
            // 合并题目和答案
            const combinedQuestions = questionData.map(question => {
              const studentAnswer = answerData.find(a => a.questionId === question.id)
              return {
                ...question,
                student_answer: studentAnswer?.studentAnswer || '',
                obtained_score: studentAnswer?.score || 0,
                comment: studentAnswer?.teacherComment || '',
                graded: !!studentAnswer?.score,
                saving: false
              }
            })
            questions.value = combinedQuestions
            resolve()
          },
          reject
        )
      },
      reject
    )
  })
}

// 工具函数
const getQuestionTypeName = (type) => {
  const typeMap = {
    'radio': '单选题',
    'checkbox': '多选题',
    'blank': '填空题',
    'essay': '简答题',
    'judge': '判断题'
  }
  return typeMap[type] || '未知类型'
}

const getQuestionTypeColor = (type) => {
  const colorMap = {
    'radio': 'blue',
    'checkbox': 'purple',
    'blank': 'orange',
    'essay': 'green',
    'judge': 'red'
  }
  return colorMap[type] || 'default'
}

const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index) // A, B, C, D...
}

const formatAnswer = (answer, type) => {
  if (type === 'checkbox' && answer.includes(',')) {
    return answer.split(',').join('、')
  }
  return answer
}

const getStatusText = (status) => {
  const statusMap = {
    'not_started': '未开始',
    'in_progress': '考试中',
    'submitted': '已提交',
    'graded': '已批改'
  }
  return statusMap[status] || '未知状态'
}

const getStatusColor = (status) => {
  const colorMap = {
    'not_started': 'default',
    'in_progress': 'blue',
    'submitted': 'orange',
    'graded': 'green'
  }
  return colorMap[status] || 'default'
}

const progressFormat = (percent) => {
  return `${percent}%`
}

// 更新进度显示
const updateProgress = () => {
  // 进度通过计算属性自动更新
}

// 标记为已批改
const markAsGraded = (question) => {
  if (question.obtained_score !== undefined) {
    question.graded = true
  }
}

// 保存单个题目分数
const saveQuestionScore = async (question) => {
  question.saving = true
  try {
    await new Promise((resolve, reject) => {
      const answer = { 
        examRecordId: examRecordId.value,
        questionId: question.id, 
        score: question.obtained_score || 0, 
        comment: question.comment || '' 
      }
      post('/api/exam/answer/save-score', answer,
        (msg) => {
          message.success('保存成功')
          question.graded = true
          resolve()
        },
        reject
      )
    })
  } catch (error) {
    message.error('保存失败: ' + error.message)
    console.error('保存单个题目分数失败:', error)
  } finally {
    question.saving = false
  }
}

// 保存所有题目分数
const saveAllScores = async () => {
  savingAll.value = true
  try {
    const scores = questions.value.map(q => ({
      id: q.id,
      score: q.obtained_score || 0,
      comment: q.comment || ''
    }))

    await new Promise((resolve, reject) => {
      post('/api/exam/answer/save-all-scores', {
          examRecordId: examRecordId.value,
          scores: scores
        },
        (msg) => {
          message.success('全部保存成功')
          // 标记所有题目为已批改
          questions.value.forEach(q => {
            q.graded = true
          })
          resolve()
        },
        reject
      )
    })
  } catch (error) {
    message.error('保存失败: ' + error.message)
    console.error('保存所有题目分数失败:', error)
  } finally {
    savingAll.value = false
  }
}

// 完成批改
const completeGrading = async () => {
  // 检查是否所有题目都已批改
  if (gradedCount.value < questions.value.length) {
    message.warning('还有未批改的题目，是否继续完成批改？', 3, () => {
      doCompleteGrading()
    })
    return
  }
  
  // 如果所有题目都已批改，直接完成
  await doCompleteGrading()
}

const doCompleteGrading = async () => {
  completing.value = true
  try {
    await new Promise((resolve, reject) => {
      // 先保存所有分数
      const scores = questions.value.map(q => ({
        id: q.id,
        score: q.obtained_score || 0,
        comment: q.comment || ''
      }))

      post('/api/exam/answer/save-all-scores', {
          examRecordId: examRecordId.value,
          scores: scores
        },
        (msg) => {
          // 更新考试记录总分和状态
          post('/api/exam/record/update-status-score', {
              id: examRecordId.value,
              status: 'graded',
              totalScore: currentScore.value
            },
            (msg) => {
              message.success('批改完成')
              // 跳转到考试记录列表
              router.push('/ExamRecord')
              resolve()
            },
            reject
          )
        },
        reject
      )
    })
  } catch (error) {
    message.error('完成批改失败: ' + error.message)
    console.error('完成批改失败:', error)
  } finally {
    completing.value = false
  }
}

// 返回记录列表
const goToRecordList = () => {
  router.push('/ExamRecord')
}

// 监听路由参数变化
watch([examRecordId, studentId, examId], () => {
  if (examRecordId.value && studentId.value && examId.value) {
    loadData()
  }
})

// 组件挂载
onMounted(() => {
  if (examRecordId.value && studentId.value && examId.value) {
    loadData()
  } else {
    message.error('缺少必要的参数')
    goToRecordList()
  }
})
</script>

<style scoped>
/* 自定义样式 */
</style>