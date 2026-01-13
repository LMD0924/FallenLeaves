<template>
  <div class="grading-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="fixed inset-0 bg-white bg-opacity-70 flex items-center justify-center z-50">
      <a-spin size="large" tip="加载中..." />
    </div>

    <!-- 头部信息 -->
    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div>
          <h3 class="text-lg font-semibold text-gray-800">学生信息</h3>
          <p class="text-gray-600 mt-2">姓名：{{ studentInfo.username || '-' }}</p>
          <p class="text-gray-600">学号：{{ studentInfo.account || '-' }}</p>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-gray-800">考试信息</h3>
          <p class="text-gray-600 mt-2">试卷：{{ examInfo.title || '-' }}</p>
          <p class="text-gray-600">总分：{{ examInfo.total_score || 0 }}</p>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-gray-800">当前得分</h3>
          <p class="text-3xl font-bold text-green-600 mt-2">{{ currentScore }}</p>
          <p class="text-gray-600">满分：{{ examInfo.total_score || 0 }}</p>
        </div>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="space-y-4">
      <div v-for="(question, index) in questions" :key="question.id"
           class="bg-white rounded-lg shadow p-6 transition-all duration-300 hover:shadow-md">
        <div class="flex items-start justify-between mb-4">
          <div>
            <h4 class="text-lg font-semibold text-gray-800">
              第 {{ index + 1 }} 题：{{ question.text }}
            </h4>
            <div class="flex items-center space-x-3 mt-2">
              <span class="px-3 py-1 bg-blue-100 text-blue-600 rounded-full text-sm">
                分值：{{ question.score }} 分
              </span>
              <span class="px-3 py-1 bg-purple-100 text-purple-600 rounded-full text-sm">
                {{ getQuestionTypeName(question.type) }}
              </span>
            </div>
          </div>

          <div class="text-right">
            <div v-if="question.graded" class="text-green-600 font-semibold">
              得分：{{ question.obtained_score || 0 }} / {{ question.score }}
            </div>
            <div v-else class="text-orange-600 font-semibold">
              待批改
            </div>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="space-y-4 mt-6">
          <!-- 显示选项（如果是选择题） -->
          <div v-if="(question.type === 'radio' || question.type === 'checkbox') && question.options" class="bg-gray-50 p-4 rounded-lg">
            <p class="text-sm font-medium text-gray-700 mb-2">选项：</p>
            <div class="space-y-2">
              <div v-for="(option, optIndex) in question.options" :key="optIndex" class="flex items-center">
                <span class="inline-block w-6 h-6 flex-shrink-0 mr-2 text-gray-700">{{ getOptionLabel(optIndex) }}</span>
                <span class="text-gray-800">{{ option }}</span>
              </div>
            </div>
          </div>

          <!-- 显示正确答案（如果可用） -->
          <div v-if="question.correct_answer" class="bg-green-50 p-4 rounded-lg">
            <p class="text-sm font-medium text-green-700 mb-1">正确答案：</p>
            <p class="text-green-800">{{ formatAnswer(question.correct_answer, question.type) }}</p>
          </div>

          <!-- 显示学生答案 -->
          <div class="bg-gray-50 p-4 rounded-lg">
            <p class="text-sm font-medium text-gray-700 mb-1">学生答案：</p>
            <div v-if="question.student_answer">
              <template v-if="question.type === 'checkbox'">
                <a-tag v-for="opt in question.student_answer.split(',')"
                       :key="opt"
                       class="mr-2 mb-2 bg-blue-100 text-blue-800">
                  {{ opt }}
                </a-tag>
              </template>
              <template v-else>
                <p class="text-gray-800">{{ question.student_answer }}</p>
              </template>
            </div>
            <p v-else class="text-gray-500 italic">学生未回答此题</p>
          </div>

          <!-- 批改区域 -->
          <div class="border-t pt-4 mt-4">
            <div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div class="flex-1 min-w-[150px]">
                <label class="block text-sm font-medium text-gray-700 mb-2">给分</label>
                <div class="flex items-center">
                  <a-input-number
                    v-model:value="question.obtained_score"
                    :min="0"
                    :max="question.score"
                    :step="0.5"
                    style="width: 120px"
                    @change="() => { markAsGraded(question); updateTotalScore() }"
                  />
                  <span class="text-gray-500 ml-2">/ {{ question.score }}</span>
                </div>
              </div>

              <div class="flex-1 min-w-[200px]">
                <label class="block text-sm font-medium text-gray-700 mb-2">评语</label>
                <a-textarea
                  v-model:value="question.comment"
                  placeholder="输入评语（可选）"
                  :rows="2"
                  @change="markAsGraded(question)"
                />
              </div>

              <div class="ml-0 md:ml-4 mt-2 md:mt-0">
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
    </div>

    <!-- 底部操作栏 -->
    <div class="sticky bottom-0 bg-white border-t mt-8 p-6 rounded-lg shadow-lg">
      <div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <p class="text-lg font-semibold text-gray-800">
            总分：<span class="text-green-600">{{ currentScore }}</span> / {{ examInfo.total_score || 0 }}
          </p>
          <p class="text-sm text-gray-500 mt-1">
            已批改 {{ gradedCount }} / {{ questions.length }} 题
            <span v-if="gradedCount < questions.length" class="text-orange-500">(还有 {{ questions.length - gradedCount }} 题待批改)</span>
          </p>
        </div>

        <div class="flex items-center space-x-4">
          <a-button @click="saveAllScores" :loading="savingAll" class="min-w-[120px]">
            保存全部
          </a-button>
          <a-button type="primary" @click="completeGrading" :loading="completing" class="min-w-[120px]">
            完成批改
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { get, post } from '@/net/index.js'
import { message, spin } from 'ant-design-vue'

const props = defineProps({
  examRecordId: {
    type: Number,
    required: true
  },
  studentId: {
    type: Number,
    required: true
  },
  examId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['grading-complete'])

// 状态
const loading = ref(false)
const savingAll = ref(false)
const completing = ref(false)
const studentInfo = reactive({})
const examInfo = reactive({})
const questions = ref([])

// 计算属性
const currentScore = computed(() => {
  return questions.value.reduce((sum, q) => sum + (q.obtained_score || 0), 0)
})

const gradedCount = computed(() => {
  return questions.value.filter(q => q.graded).length
})

// 获取题目类型名称
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

// 获取选项标签
const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index) // A, B, C, D...
}

// 格式化答案显示
const formatAnswer = (answer, type) => {
  if (type === 'checkbox' && answer.includes(',')) {
    return answer.split(',').join('、')
  }
  return answer
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 加载学生信息
    await new Promise((resolve, reject) => {
      get('/api/exam/current', { id: props.studentId },
        (msg, data) => {
          Object.assign(studentInfo, data)
          resolve(data)
        },
        reject
      )
    })

    // 加载考试信息
    await new Promise((resolve, reject) => {
      get('/api/exam/SelectExamById', { id: props.examId },
        (msg, data) => {
          Object.assign(examInfo, data)
          resolve(data)
        },
        reject
      )
    })

    // 加载题目和答案
    await new Promise((resolve, reject) => {
      get('/api/exam/getExamQuestionsByExamId', { exam_id: props.examId },
        (msg, questionData) => {
          // 加载学生答案
          get('/api/exam/answer/record', { recordId: props.examRecordId },
            (msg, answerData) => {
              // 合并题目和答案
              const combinedQuestions = questionData.map(question => {
                const studentAnswer = answerData.find(a => a.questionId === question.id)
                return {
                  ...question,
                  student_answer: studentAnswer?.answer || '',
                  obtained_score: studentAnswer?.score || 0,
                  comment: studentAnswer?.comment || '',
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
  } catch (error) {
    message.error('加载数据失败: ' + error.message)
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
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
      // 查找对应的答案对象
      const answer = { id: question.id, score: question.obtained_score || 0, comment: question.comment || '' }
      post('/api/exam/answer/update-score', answer,
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
      post('/api/exam/answer/batch-update-score', scores,
        (msg) => {
          message.success('全部保存成功')
          // 标记所有题目为已批改
          questions.value.forEach(q => {
            q.graded = true
          })
          resolve()
        },
        reject
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

      post('/api/exam/answer/batch-update-score', scores,
        (msg) => {
          // 更新考试记录总分和状态
          post('/api/exam/record/complete-grading', {
              recordId: props.examRecordId,
              totalScore: currentScore.value
            },
            (msg) => {
              message.success('批改完成')
              emit('grading-complete')
              resolve()
            },
            reject
          )
        },
        reject
    })
  } catch (error) {
    message.error('完成批改失败: ' + error.message)
    console.error('完成批改失败:', error)
  } finally {
    completing.value = false
  }
}

// 更新总分
const updateTotalScore = () => {
  // 总分会通过计算属性自动更新
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.grading-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

@media (max-width: 768px) {
  .grading-container {
    padding: 10px;
  }
}
</style>
