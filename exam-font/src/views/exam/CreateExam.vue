<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { get, post } from "@/net/index.js";
import { message, Modal } from "ant-design-vue"
import { useRouter } from 'vue-router';
import { isDark, currentTheme } from '@/stores/theme.js'

const [messageApi, contextHolder] = message.useMessage()

// 当前激活的标签页
const activeTab = ref('all')

// 搜索查询
const searchQuery = ref('')

// 对话框状态
const showCreateDialog = ref(false)
const showQuestionSelectDialog = ref(false)
const editingExam = ref(null)

// 路由
const router = useRouter();

// 表单数据
const examForm = ref({
  id: '',
  title: '',
  description: '',
  course_id: '',
  teacher_id: '',
  total_score: 100,
  duration: 120,
  pass_score: 60,
  attempts_allowed: 1,
  is_public: true,
  status: 'draft',
  questions: []
})

// 试卷数据
const exams = ref([])

// 科目选项
const subjects = ref([])
const questions = ref([])
const filteredQuestions = ref([])
const questionSearch = ref('')

// 获取个人信息
const User = ref({})
const getUser = () => {
  return new Promise((resolve, reject) => {
    get('api/user/current', {},
      (message, data) => {
        User.value = data
        resolve(data)
      }, (error) => {
        reject(error)
      })
  })
}

// 获取科目
const getCourse = async () => {
  try {
    await getUser();
    await new Promise((resolve, reject) => {
      get('api/exam/SelectCourseByTeacherId', {
        teacher_id: User.value.id
      }, (message, data) => {
        if(User.value.role === '管理员'){
          get('api/exam/AllCourse',{},(message,data)=>{
            subjects.value = data.filter(course => course.status === '审核通过')
            resolve(data)
          })
        } else {
          const approvedSubjects = data.filter(subject => subject.status === "审核通过");
          if (approvedSubjects.length === 0) {
            subjects.value = [{ id: -1, name: "暂无科目或科目未审核通过" }];
          } else {
            subjects.value = approvedSubjects;
          }
          resolve(data);
        }
      }, (error) => {
        reject(error);
      });
    });
  } catch (error) {
    console.log(error);
  }
}

// 获取题目列表
const fetchQuestions = async () => {
  try {
    await getUser()
    await new Promise((resolve, reject) => {
      get('api/exam/SelectQuestionByTeacherId', {
        teacher_id: User.value.id
      }, (message, data) => {
        if(User.value.role === '管理员'){
          get('api/exam/AllQuestion',{},(message,data)=>{
            questions.value = data
            filteredQuestions.value = data
            resolve(data)
          })
        } else {
          questions.value = data
          filteredQuestions.value = data
          resolve(data)
        }
      }, (error) => {
        reject(error)
      })
    })
  } catch (error) {
    console.log(error);
  }
}

// 获取试卷列表
const fetchExams = async () => {
  try {
    await getUser()
    await new Promise((resolve, reject) => {
      // 管理员查看所有考试
      if(User.value.role === '管理员'){
        get('api/exam/AllExam',{},(message,data)=>{
          exams.value = data
          resolve(data)
        })
      }
      // 学生查看所有考试
      else if(User.value.role === '学生'){
        get('api/exam/AllExam',{},(message,data)=>{
          exams.value = data
          resolve(data)
        })
      }
      // 教师查看自己的考试
      else {
        get('api/exam/SelectExamByTeacherId', {
          teacher_id: User.value.id
        }, (message, data) => {
          exams.value = data
          resolve(data)
        }, (error) => {
          reject(error)
        })
      }
    })
  } catch (error) {
    console.log(error);
  }
}

// 过滤题目
watch(questionSearch, (value) => {
  if (!value) {
    filteredQuestions.value = questions.value
  } else {
    const query = value.toLowerCase()
    filteredQuestions.value = questions.value.filter(q =>
      q.text?.toLowerCase().includes(query) ||
      q.knowledge?.toLowerCase().includes(query) ||
      q.questionType?.toLowerCase().includes(query)
    )
  }
})

// 添加题目到试卷
const addQuestionToExam = (question) => {
  if (!examForm.value.questions.some(q => q.id === question.id)) {
    // 如果是编辑模式且有exam_id，则发送请求到后端
    if (examForm.value.id && editingExam.value) {
      post('api/exam/InsertExamQuestion', {
        exam_id: examForm.value.id,
        question_id: question.id
      }, (message, data) => {
        if (data) {
          examForm.value.questions.push({
            ...question,
            score: question.score || 5,
            question_order: examForm.value.questions.length + 1
          })
          updateTotalScore()
          messageApi.success('题目添加成功')
        } else {
          messageApi.error('添加失败')
        }
      })
    } else {
      // 如果是新建模式，直接添加到本地列表
      examForm.value.questions.push({
        ...question,
        score: question.score || 5,
        question_order: examForm.value.questions.length + 1
      })
      updateTotalScore()
      messageApi.success('题目添加成功')
    }
  } else {
    messageApi.warning('题目已存在')
  }
}
//从试卷中删除题目
const DeleteExamQuestion = (examId, questionId) => {
  // 验证参数
  if (!examId) {
    messageApi.error('试卷ID不能为空')
    return
  }

  if (!questionId) {
    messageApi.error('题目ID不能为空')
    return
  }

  Modal.confirm({
    title: '确认删除题目',
    content: '确定要从试卷中删除这道题目吗？删除后该题目的分数也将从总分中扣除。',
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    onOk() {
      // 先获取要删除的题目信息用于后续提示
      const questionToDelete = examForm.value.questions.find(q => q.id === questionId)
      const questionTitle = questionToDelete?.text?.substring(0, 30) + '...' || '该题目'

      post('api/exam/DeleteExamQuestion', {
        exam_id: examId,
        question_id: questionId
      }, (message, data) => {
        // 从本地questions数组中移除
        const index = examForm.value.questions.findIndex(q => q.id === questionId)
        if (index !== -1) {
          const deletedQuestion = examForm.value.questions.splice(index, 1)[0]
          // 更新题目序号
          examForm.value.questions.forEach((q, i) => {
            q.question_order = i + 1
          })
          updateTotalScore()

          messageApi.success(`${questionTitle} 已从试卷中删除`)
          console.log('删除成功:', deletedQuestion)
        }
      }, (error) => {
        messageApi.error('删除失败: ' + error)
        console.error('删除题目失败:', error)
      })
    },
    onCancel() {
      console.log('用户取消删除')
    }
  })
}
// 编辑试卷
const editExam = (exam) => {
  editingExam.value = exam
  // 转换后端数据为前端格式
  // 首先清空现有题目，避免重复
  examForm.value.questions = []
  examForm.value = {
    ...exam,
    // 转换整数为布尔值
    is_public: exam.is_public === 1 || exam.is_public === true,
    // 转换状态代码为字符串
    status: exam.status === 0 ? 'draft' :
      exam.status === 1 ? 'published' : 'closed',
    questions: []
  }

  // 根据试卷id查询题目
  get('/api/exam/getExamQuestionsByExamId', {
    exam_id: exam.id
  }, (message, data) => {
    if (data && data.length > 0) {
      // 确保题目数据格式正确
      examForm.value.questions = data.map(q => ({
        ...q,
        id: q.id || q.question_id, // 兼容可能的不同字段名
        score: q.score || 0
      }))
      updateTotalScore()
    }
  })

  showCreateDialog.value = true
}

// 更新题目分数
const updateQuestionScore = (index, score) => {
  examForm.value.questions[index].score = parseInt(score) || 0
  updateTotalScore()
}

// 更新总分
const updateTotalScore = () => {
  examForm.value.total_score = examForm.value.questions.reduce((total, q) => total + (parseInt(q.score) || 0), 0)
}

// 调整题目顺序
const moveQuestion = (index, direction) => {
  if (direction === 'up' && index > 0) {
    const temp = examForm.value.questions[index]
    examForm.value.questions[index] = examForm.value.questions[index - 1]
    examForm.value.questions[index - 1] = temp
    examForm.value.questions.forEach((q, i) => q.question_order = i + 1)
  } else if (direction === 'down' && index < examForm.value.questions.length - 1) {
    const temp = examForm.value.questions[index]
    examForm.value.questions[index] = examForm.value.questions[index + 1]
    examForm.value.questions[index + 1] = temp
    examForm.value.questions.forEach((q, i) => q.question_order = i + 1)
  }
}

// 验证表单
const validateForm = () => {
  if (!examForm.value.title) {
    messageApi.error('请输入试卷标题')
    return false
  }

  if (!examForm.value.course_id) {
    messageApi.error('请选择科目')
    return false
  }

  // if (examForm.value.questions.length === 0) {
  //   messageApi.error('请至少添加一道题目')
  //   return false
  // }

  return true
}

// 保存试卷
const saveExam = async () => {
  if (!validateForm()) {
    return
  }

  // 确保已获取用户信息
  if (!User.value.id) {
    await getUser()
  }

  const requestData = {
    ...examForm.value,
    teacher_id: User.value.id, // 使用当前登录用户的ID作为teacher_id
    questions: examForm.value.questions.map(q => ({
      question_id: q.id,
      score: q.score,
      question_order: q.question_order
    }))
  }

  const url = editingExam.value ? 'api/exam/UpdateExam' : 'api/exam/InsertExam'
  post(url, requestData, (message) => {
    messageApi.success(message)
    fetchExams()
    closeDialog()
  }, (error) => {
    messageApi.error('保存失败: ' + error)
  })
}

// 删除试卷
const deleteExam = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这份试卷吗？此操作不可恢复。',
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    onOk() {
      post('api/exam/DeleteExam', { id }, (message) => {
        messageApi.success(message)
        fetchExams()
      })
    }
  })
}

// 关闭对话框
const closeDialog = () => {
  showCreateDialog.value = false
  showQuestionSelectDialog.value = false
  editingExam.value = null
  examForm.value = {
    id: '',
    title: '',
    description: '',
    course_id: '',
    teacher_id: '',
    total_score: 100,
    duration: 120,
    pass_score: 60,
    attempts_allowed: 1,
    is_public: true,
    status: 'draft',
    questions: []
  }
}

// 初始化加载
onMounted(() => {
  getCourse()
  // 只有教师和管理员需要获取题目列表
  if(User.value.role !== '学生') {
    fetchQuestions()
  }
  fetchExams()
})

// 学生考试状态映射：examId -> record status（not_started/in_progress/submitted/graded）
const examRecordStatus = ref({})

// 为学生加载每场考试的个人考试记录状态
const loadStudentExamStatus = async () => {
  if (User.value.role !== '学生' || !exams.value.length) return

  const statusMap = {}
  const requests = exams.value.map(exam => {
    return new Promise((resolve) => {
      get('/api/exam/record/find', { examId: exam.id },
        (msg, data) => {
          if (data && data.status) {
            statusMap[exam.id] = data.status
          }
          resolve()
        },
        () => resolve() // 404 等情况直接忽略
      )
    })
  })

  await Promise.all(requests)
  examRecordStatus.value = statusMap
}

// 获取某场考试在当前学生下的显示状态
const getStudentExamStatusText = (exam) => {
  const status = examRecordStatus.value[exam.id]
  if (!status) return '开始考试'
  if (status === 'submitted') return '已提交'
  if (status === 'graded') return '已批改'
  if (status === 'in_progress') return '继续考试'
  return '开始考试'
}

const isExamButtonDisabled = (exam) => {
  const status = examRecordStatus.value[exam.id]
  return status === 'submitted' || status === 'graded'
}

// 开始 / 继续考试
const startExam = (exam) => {
  if (exam.status !== 'published') {
    messageApi.warning('该考试未发布，无法开始')
    return
  }

  if (isExamButtonDisabled(exam)) {
    messageApi.info('该考试已提交或已批改')
    return
  }

  router.push({
    path: '/Exam',
    query: {
      examId: exam.id
    }
  })
}
</script>

<template>
  <contextHolder />
  <div
    :class="[
      'min-h-screen transition-all duration-500',
      isDark ? 'bg-black' : 'bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50'
    ]"
  >
    <!-- 顶部导航 -->
    <header
      :class="[
        'flex items-center justify-between p-6 border-b transition-all duration-300',
        isDark ? 'bg-black/80 backdrop-blur-xl border-white/10' : 'bg-white/80 backdrop-blur-xl border-gray-200/50'
      ]"
    >
      <div class="flex items-center space-x-4">
        <div
          :class="[
            'w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-300',
            isDark ? 'bg-gradient-to-br from-indigo-500 to-purple-600' : 'bg-gradient-to-br from-blue-500 to-indigo-600'
          ]"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-7 h-7 text-white">
            <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
          </svg>
        </div>
        <div>
          <h1
            :class="[
              'text-2xl font-bold transition-all duration-300',
              isDark ? 'text-white' : 'text-gray-900'
            ]"
          >
            {{ User.role === '学生' ? '我的考试' : '试卷管理' }}
          </h1>
          <p
            :class="[
              'text-sm transition-all duration-300',
              isDark ? 'text-white/60' : 'text-gray-600'
            ]"
          >
            管理您的考试和试卷
          </p>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="min-h-screen flex-1 p-8 overflow-auto">
      <!-- 统计卡片 - 只有教师和管理员显示 -->
      <div v-if="User.role !== '学生'" class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div
          :class="[
            'group relative overflow-hidden rounded-3xl p-6 transition-all duration-500 hover:scale-105 hover:-translate-y-2',
            isDark
              ? 'bg-gradient-to-br from-blue-500/10 to-indigo-600/10 backdrop-blur-xl border border-blue-500/20 hover:border-blue-400/50'
              : 'bg-white/80 backdrop-blur-xl border border-blue-200/50 shadow-xl hover:shadow-2xl hover:border-blue-400/50'
          ]"
        >
          <div class="flex items-center justify-between">
            <div>
              <p
                :class="[
                  'text-sm font-semibold transition-all duration-300',
                  isDark ? 'text-blue-400' : 'text-blue-600'
                ]"
              >
                总试卷数
              </p>
              <p
                :class="[
                  'text-3xl font-bold transition-all duration-300',
                  isDark ? 'text-white' : 'text-gray-900'
                ]"
              >
                {{ exams.length }}
              </p>
            </div>
            <div
              :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110',
                isDark ? 'bg-blue-500/20' : 'bg-blue-100'
              ]"
            >
              <svg xmlns="http://www.w3.org/2000/svg"
                :class="[
                  'h-7 w-7 transition-all duration-300',
                  isDark ? 'text-blue-400' : 'text-blue-600'
                ]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
          </div>
          <!-- 悬停效果 -->
          <div
            :class="[
              'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300',
              isDark ? 'bg-gradient-to-br from-blue-500/5 to-indigo-500/5' : 'bg-gradient-to-br from-blue-500/5 to-indigo-500/5'
            ]"
          ></div>
        </div>

        <div
          :class="[
            'group relative overflow-hidden rounded-3xl p-6 transition-all duration-500 hover:scale-105 hover:-translate-y-2',
            isDark
              ? 'bg-gradient-to-br from-emerald-500/10 to-green-600/10 backdrop-blur-xl border border-emerald-500/20 hover:border-emerald-400/50'
              : 'bg-white/80 backdrop-blur-xl border border-emerald-200/50 shadow-xl hover:shadow-2xl hover:border-emerald-400/50'
          ]"
        >
          <div class="flex items-center justify-between">
            <div>
              <p
                :class="[
                  'text-sm font-semibold transition-all duration-300',
                  isDark ? 'text-emerald-400' : 'text-emerald-600'
                ]"
              >
                已发布
              </p>
              <p
                :class="[
                  'text-3xl font-bold transition-all duration-300',
                  isDark ? 'text-white' : 'text-gray-900'
                ]"
              >
                {{ exams.filter(e => e.status === 'published').length }}
              </p>
            </div>
            <div
              :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110',
                isDark ? 'bg-emerald-500/20' : 'bg-emerald-100'
              ]"
            >
              <svg xmlns="http://www.w3.org/2000/svg"
                :class="[
                  'h-7 w-7 transition-all duration-300',
                  isDark ? 'text-emerald-400' : 'text-emerald-600'
                ]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
          </div>
          <div
            :class="[
              'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300',
              isDark ? 'bg-gradient-to-br from-emerald-500/5 to-green-500/5' : 'bg-gradient-to-br from-emerald-500/5 to-green-500/5'
            ]"
          ></div>
        </div>

        <div
          :class="[
            'group relative overflow-hidden rounded-3xl p-6 transition-all duration-500 hover:scale-105 hover:-translate-y-2',
            isDark
              ? 'bg-gradient-to-br from-amber-500/10 to-yellow-600/10 backdrop-blur-xl border border-amber-500/20 hover:border-amber-400/50'
              : 'bg-white/80 backdrop-blur-xl border border-amber-200/50 shadow-xl hover:shadow-2xl hover:border-amber-400/50'
          ]"
        >
          <div class="flex items-center justify-between">
            <div>
              <p
                :class="[
                  'text-sm font-semibold transition-all duration-300',
                  isDark ? 'text-amber-400' : 'text-amber-600'
                ]"
              >
                草稿
              </p>
              <p
                :class="[
                  'text-3xl font-bold transition-all duration-300',
                  isDark ? 'text-white' : 'text-gray-900'
                ]"
              >
                {{ exams.filter(e => e.status === 'draft').length }}
              </p>
            </div>
            <div
              :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110',
                isDark ? 'bg-amber-500/20' : 'bg-amber-100'
              ]"
            >
              <svg xmlns="http://www.w3.org/2000/svg"
                :class="[
                  'h-7 w-7 transition-all duration-300',
                  isDark ? 'text-amber-400' : 'text-amber-600'
                ]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </div>
          </div>
          <div
            :class="[
              'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300',
              isDark ? 'bg-gradient-to-br from-amber-500/5 to-yellow-500/5' : 'bg-gradient-to-br from-amber-500/5 to-yellow-500/5'
            ]"
          ></div>
        </div>

        <div
          :class="[
            'group relative overflow-hidden rounded-3xl p-6 transition-all duration-500 hover:scale-105 hover:-translate-y-2',
            isDark
              ? 'bg-gradient-to-br from-purple-500/10 to-purple-600/10 backdrop-blur-xl border border-purple-500/20 hover:border-purple-400/50'
              : 'bg-white/80 backdrop-blur-xl border border-purple-200/50 shadow-xl hover:shadow-2xl hover:border-purple-400/50'
          ]"
        >
          <div class="flex items-center justify-between">
            <div>
              <p
                :class="[
                  'text-sm font-semibold transition-all duration-300',
                  isDark ? 'text-purple-400' : 'text-purple-600'
                ]"
              >
                已结束
              </p>
              <p
                :class="[
                  'text-3xl font-bold transition-all duration-300',
                  isDark ? 'text-white' : 'text-gray-900'
                ]"
              >
                {{ exams.filter(e => e.status === 'closed').length }}
              </p>
            </div>
            <div
              :class="[
                'w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110',
                isDark ? 'bg-purple-500/20' : 'bg-purple-100'
              ]"
            >
              <svg xmlns="http://www.w3.org/2000/svg"
                :class="[
                  'h-7 w-7 transition-all duration-300',
                  isDark ? 'text-purple-400' : 'text-purple-600'
                ]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
          </div>
          <div
            :class="[
              'absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300',
              isDark ? 'bg-gradient-to-br from-purple-500/5 to-purple-500/5' : 'bg-gradient-to-br from-purple-500/5 to-purple-500/5'
            ]"
          ></div>
        </div>
      </div>

      <!-- 搜索和操作栏 -->
      <div class="flex items-center justify-between mb-6">
        <div class="relative w-64">
          <input
            type="text"
            v-model="searchQuery"
            placeholder="搜索试卷标题..."
            class="w-full pl-10 pr-4 py-2 bg-white/5 border border-black rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-white/50 absolute left-3 top-2.5" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4
            0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
          </svg>
        </div>
        <!-- 创建新试卷按钮 - 只有教师和管理员显示 -->
        <button
          v-if="User.role !== '学生'"
          @click="showCreateDialog = true"
          class="flex items-center px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 rounded-lg text-white transition-all shadow-lg hover:shadow-indigo-500/25"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule
              ="evenodd" />
          </svg>
          创建新试卷
        </button>
      </div>

      <!-- 试卷列表 -->
      <div class="backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl p-6 border border-black/10 shadow-lg">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div v-for="exam in exams" :key="exam.id" class="bg-white/5 rounded-xl p-6 border border-black/10  transition-all shadow-lg hover:shadow-xl duration-300">
            <div class="flex items-start justify-between mb-4">
              <div>
                <h3 :class="isDark?'text-white':'text-black'" class="text-lg font-semibold mb-2">{{ exam.title }}</h3>
                <p :class="isDark?'text-white':'text-black'" class=" text-sm">{{ subjects.find(s => s.id === exam.course_id)?.name || '未知科目' }}</p>
              </div>
              <span :class="{
                'px-2 py-1 rounded-full text-xs font-medium': true,
                'bg-green-500/20 text-green-400': exam.status === 'published',
                'bg-yellow-500/20 text-yellow-400': exam.status === 'draft',
                'bg-gray-500/20 text-gray-400': exam.status === 'closed'
              }">
                {{ exam.status === 'published' ? '已发布' : exam.status === 'draft' ? '草稿' : '已结束' }}
              </span>
            </div>

            <div class="space-y-3 mb-4">
              <div :class="isDark?'text-white/60':'text-black/60'" class="flex items-center text-sm ">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ exam.duration }}分钟
              </div>
              <div :class="isDark?'text-white/60':'text-black/60'" class="flex items-center text-sm">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                </svg>
                总分: {{ exam.total_score }}分
              </div>
              <div :class="isDark?'text-white/60':'text-black/60'" class="flex items-center text-sm">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                {{ new Date(exam.start_time).toLocaleDateString() }}
              </div>
            </div>

            <div class="flex space-x-2">
              <!-- 学生角色显示开始考试按钮 -->
              <template v-if="User.role === '学生'">
                <button
                  v-if="exam.status === 'published'"
                  @click="startExam(exam)"
                  :disabled="isExamButtonDisabled(exam)"
                  class="flex-1 px-3 py-2 rounded-lg transition-colors"
                  :class="isExamButtonDisabled(exam)
                    ? 'bg-gray-500/20 text-gray-400 cursor-not-allowed'
                    : 'bg-green-500/20 hover:bg-green-500/30 text-green-400'"
                >
                  {{ getStudentExamStatusText(exam) }}
                </button>
              </template>
              <!-- 教师和管理员显示编辑和删除按钮 -->
              <template v-else>
                <button @click="editExam(exam)" class="flex-1 px-3 py-2 bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-400 rounded-lg transition-colors">
                  编辑
                </button>
                <button @click="deleteExam(exam.id)" class="px-3 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0
0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1
0 00-1 1v3M4 7h16"/>
                  </svg>
                </button>
              </template>
            </div>
          </div>
        </div>

        <div v-if="exams.length === 0" class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-white/30 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <template v-if="User.role === '学生'">
            <p class="text-white/50">暂无考试安排</p>
          </template>
          <template v-else>
            <p class="text-white/50">暂无试卷数据</p>
            <button @click="showCreateDialog = true" class="mt-4 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors">
              创建第一份试卷
            </button>
          </template>
        </div>
      </div>
    </main>

    <!-- 创建/编辑试卷对话框 -->
    <div v-if="showCreateDialog" class="fixed inset-0 bg-black/80 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md bg-gradient-to-br from-gray-900/80 to-gray-800/80 rounded-2xl p-6 border border-white/10 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-xl font-bold text-white">{{ editingExam ? '编辑' : '创建' }}试卷</h3>
          <button @click="closeDialog" class="text-white/50 hover:text-white transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-6">
          <!-- 基本信息 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">📝 试卷标题</label>
              <input
                v-model="examForm.title"
                type="text"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                placeholder="请输入试卷标题"
              >
            </div>
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">📚 所属科目</label>
              <select
                v-model="examForm.course_id"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
                <option value="">选择科目</option>
                <option v-for="subject in subjects" :key="subject.id" :value="subject.id" class="bg-gray-800">
                  {{ subject.name }}
                </option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-white/70 mb-2">📄 试卷描述</label>
            <textarea
              v-model="examForm.description"
              rows="3"
              class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              placeholder="请输入试卷描述..."
            ></textarea>
          </div>

          <!-- 考试设置 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">⏰ 考试时长(分钟)</label>
              <input
                v-model.number="examForm.duration"
                type="number"
                min="1"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
            </div>
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">🎯 及格分数</label>
              <input
                v-model.number="examForm.pass_score"
                type="number"
                min="0"
                :max="examForm.total_score"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
            </div>
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">🔄 允许尝试次数</label>
              <input
                v-model.number="examForm.attempts_allowed"
                type="number"
                min="1"
                class="w-full px-4 py-3 bg-white
/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
            </div>
          </div>

          <!-- 时间设置 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">🕐 开始时间</label>
              <input
                v-model="examForm.start_time"
                type="datetime-local"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
            </div>
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">🕔 结束时间</label>
              <input
                v-model="examForm.end_time"
                type="datetime-local"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
            </div>
          </div>

          <!-- 题目管理 -->
          <div class="border-t border-white/10 pt-6">
            <div class="flex items-center justify-between mb-4">
              <h4 class="text-lg font-semibold text-white">📋 题目列表</h4>
              <button
                @click="showQuestionSelectDialog = true"
                class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1 inline" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
                </svg>
                添加题目
              </button>
            </div>

            <div v-if="examForm.questions.length === 0" class="text-center py-8 bg-white/5 rounded-xl">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-white/30 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2
V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <p class="text-white/50">暂无题目，点击上方按钮添加</p>
            </div>

            <div v-else class="space-y-3">
              <div v-for="(question, index) in examForm.questions" :key="question.id" class="bg-white/5 rounded-xl p-4 border border-white/10">
                <div class="flex items-start justify-between mb-3">
                  <div class="flex-1">
                    <div class="flex items-center space-x-3 mb-2">
                      <span class="w-6 h-6 bg-indigo-500/20 text-indigo-400 rounded-full text-xs flex items-center justify-center">
                        {{ index + 1 }}
                      </span>
                      <span class="text-sm text-white/70">{{ question.questionType }}</span>
                      <span class="text-sm text-green-400">{{ question.difficulty }}</span>
                    </div>
                    <p class="text-white text-sm line-clamp-2">{{ question.text }}</p>
                  </div>
                  <div class="flex items-center space-x-2 ml-4">
                    <button @click="moveQuestion(index, 'up')" :disabled="index === 0" class="p-1 text-white/50 hover:text-white disabled:opacity-30">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"/>
                      </svg>
                    </button>
                    <button @click="moveQuestion(index, 'down')" :disabled="index === examForm.questions.length - 1" class="p-1 text-white/50 hover:text-white disabled:opacity-30">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                      </svg>
                    </button>
                    <button @click="DeleteExamQuestion(examForm.id,question.id)" class="p-1 text-red-400 hover:text-red-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-sm text-white/60">知识点: {{ question.knowledge }}</span>
                  <div class="flex items-center space-x-2">
                    <span class="text-sm text-white/60">分值:</span>
                    <input
                      :value="question.score"
                      @input="updateQuestionScore(index, $event.target.value)"
                      type="number"
                      min="1"
                      class="w-16 px-2 py-1 bg-white/5 border border-white/10 rounded text-white text-sm"
                    >
                    <span class="text-sm text-white/60">分</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 总分显示 -->
            <div class="mt-4 p-4 bg-gradient-to-r from-indigo-500/10 to-purple-500/10 rounded-xl border border-indigo-400/20">
              <div class="flex items-center justify-between">
                <span class="text-white font-medium">试卷总分</span>
                <span class="text-2xl font-bold text-indigo-400">{{ examForm.total_score }}分</span>
              </div>
            </div>
          </div>

          <!-- 状态设置 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">🌐 公开状态</label>
              <select
                v-model="examForm.is_public"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus
:border-transparent"
              >
                <option :value="true" class="bg-gray-800">公开</option>
                <option :value="false" class="bg-gray-800">不公开</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-white/70 mb-2">📊 试卷状态</label>
              <select
                v-model="examForm.status"
                class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus
:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
                <option value="draft" class="bg-gray-800">草稿</option>
                <option value="published" class="bg-gray-800">已发布</option>
                <option value="closed" class="bg-gray-800">已结束</option>
              </select>
            </div>
          </div>

          <div class="flex space-x-4 pt-6 border-t border-white/10">
            <button
              @click="closeDialog"
              class="flex-1 px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-white transition-colors"
            >
              取消
            </button>
            <button
              @click="saveExam"
              class="flex-1 px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 rounded-xl text-white font-medium transition-all shadow-lg hover:shadow-indigo-500/25"
            >
              {{ editingExam ? '更新' : '创建' }}试卷
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 选择题目对话框 -->
    <div v-if="showQuestionSelectDialog" class="fixed inset-0 bg-black/80 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md bg-gradient-to-br from-gray-900/80 to-gray-800/80 rounded-2xl p-6 border border-white/10 w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-xl font-bold text-white">选择题目</h3>
          <button @click="showQuestionSelectDialog = false" class="text-white/50 hover:text-white transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6
l12 12" />
            </svg>
          </button>
        </div>

        <!-- 搜索栏 -->
        <div class="mb-6">
          <input
            v-model="questionSearch"
            type="text"
            placeholder="搜索题目内容、知识点、题型..."
            class="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          >
        </div>

        <!-- 题目列表 -->
        <div class="grid grid-cols-1 gap-4 max-h-96 overflow-y-auto">
          <div v-for="question in filteredQuestions" :key="question.id" class="bg-white/5 rounded-xl p-4 border border-white/10 hover:border-indigo-400/30 transition-colors">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <div class="flex items-center space-x-3 mb-2">
                  <span class="px-2 py-1 bg-indigo-500/20 text-indigo-400 rounded-full text-xs">
                    {{ question.questionType }}
                  </span>
                  <span class="px-2 py-1 bg-green-500/20 text-green-400 rounded-full text-xs">
                    {{ question.difficulty }}
                  </span>
                  <span class="text-sm text-white/60">{{ question.knowledge }}</span>
                  <span class="text-sm text-white/60">{{ question.score }}</span>
                </div>
                <p class="text-white text-sm mb-2">{{ question.text }}</p>
                <p class="text-white/60 text-xs">答案: {{ question.answer }}</p>
              </div>
              <button
                @click="addQuestionToExam(question)"
                :disabled="examForm.questions.some(q => q.id === question.id)"
                class="ml-4 px-3 py-1 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 disabled:cursor-not-allowed rounded-lg text-white text-sm transition-colors"
              >
                {{ examForm.questions.some(q => q.id === question.id) ? '已添加' : '添加' }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="filteredQuestions.length === 0" class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-white/30 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <p class="text-white/50">暂无题目数据</p>
        </div>

        <div class="flex justify-end pt-6 border-t border-white/10 mt-6">
          <button
            @click="showQuestionSelectDialog = false"
            class="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
          >
            完成
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: linear-gradient(45deg, rgba(99, 102, 241, 0.6), rgba(139, 92, 246, 0.6));
  border-radius: 4px;
  transition: all 0.3s ease;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(45deg, rgba(99, 102, 241, 0.8), rgba(139, 92, 246, 0.8));
}

/* 文字截断 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 渐变背景动画 */
.gradient-bg {
  background: linear-gradient(-45deg, #667eea, #764ba2, #f093fb, #f5576c);
  background-size: 400% 400%;
  animation: gradient 15s ease infinite;
}

@keyframes gradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 全局动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 添加动画类 */
.fade-in-up {
  animation: fadeInUp 0.6s ease-out;
}

.slide-in-right {
  animation: slideInRight 0.6s ease-out;
}

.scale-in {
  animation: scaleIn 0.4s ease-out;
}

/* 玻璃拟态效果增强 */
.glass-effect {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 悬停效果增强 */
.hover-lift {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-lift:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

/* 卡片样式 */
.card-modern {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  transition: all 0.3s ease;
}

.card-modern:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(99, 102, 241, 0.4);
  transform: translateY(-5px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

/* 按钮样式 */
.btn-modern {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.btn-modern:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.btn-modern:active {
  transform: translateY(0);
}

/* 输入框样式 */
.input-modern {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  color: white;
  transition: all 0.3s ease;
}

.input-modern:focus {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(99, 102, 241, 0.5);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
  outline: none;
}

.input-modern::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .grid-cols-1 {
    grid-template-columns: 1fr;
  }

  .md\\:grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 深色模式优化 */
.dark .card-modern {
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .card-modern:hover {
  background: rgba(0, 0, 0, 0.6);
  border-color: rgba(99, 102, 241, 0.3);
}

.dark .input-modern {
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.dark .input-modern:focus {
  background: rgba(0, 0, 0, 0.6);
  border-color: rgba(99, 102, 241, 0.5);
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.loading {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

/* 状态指示器 */
.status-indicator {
  position: relative;
  display: inline-block;
}

.status-indicator::before {
  content: '';
  position: absolute;
  top: 50%;
  left: -8px;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-published::before {
  background: #10b981;
}

.status-draft::before {
  background: #f59e0b;
}

.status-closed::before {
  background: #6b7280;
}
</style>
