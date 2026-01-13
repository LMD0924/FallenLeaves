<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from "@/net/index.js";
import { message, Modal } from "ant-design-vue"
import { isDark } from "@/stores/theme.js" // 统一使用主题存储

const [messageApi, contextHolder] = message.useMessage()

// 当前激活的标签页
const activeTab = ref('all')

// 搜索查询
const searchQuery = ref('')

// 对话框状态
const showAddDialog = ref(false)
const editingItem = ref(null)

// 表单数据 - 根据数据库结构调整
const formData = ref({
  id: '',
  teacher_id: '',
  course_id: '',
  questionType: '单选题',
  text: '',
  score: '5',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  optionE: '',
  optionF: '',
  optionG: '',
  answer: '',
  knowledge: '',
  difficulty: '中等',
  status: '已发布'
})

// 题型选项
const questionTypes = [
  { value: '单选题', emoji: '🔘' },
  { value: '多选题', emoji: '☑️' },
  { value: '判断题', emoji: '✔️' },
  { value: '填空题', emoji: '📝' },
  { value: '简答题', emoji: '✏️' },
  { value: '编程题', emoji: '💻' }
]

// 难度选项
const difficultyLevels = [
  { value: '简单', emoji: '😊' },
  { value: '中等', emoji: '😐' },
  { value: '困难', emoji: '😨' }
]

// 状态选项
const statusOptions = [
  { value: '已发布', emoji: '🟢' },
  { value: '待审核', emoji: '🟡' },
  { value: '已禁用', emoji: '🔴' },
  { value: '草稿', emoji: '📄' }
]

// 知识点选项
const knowledgePoints = ['代数', '几何', '函数', '算法', '数据结构', '网络', '数据库']

// 获取个人信息
const User = ref({})
const getUser = () => {
  return new Promise((resolve, reject) => {
    get('api/exam/current', {},
      (message, data) => {
        User.value = data
        resolve(data)
      }, (error) => {
        reject(error)
      })
  })
}

// 科目选项
const subjects = ref([])
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

// 题目数据
const questions = ref([])

// 过滤后的题目
const filteredQuestions = computed(() => {
  let result = questions.value

  if (activeTab.value !== 'all') {
    result = result.filter(q => q.questionType === activeTab.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(q =>
      q.text?.toLowerCase().includes(query) ||
      q.knowledge?.toLowerCase().includes(query))
  }

  return result
})

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
            resolve(data)
          })
        } else {
          questions.value = data
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

// 处理正确选项选择
const handleCorrectOptionChange = (optionLetter) => {
  if (formData.value.questionType === '单选题') {
    // 单选题：直接设置答案为选项字母
    formData.value.answer = optionLetter;
  } else if (formData.value.questionType === '多选题') {
    // 多选题：处理多选逻辑
    const currentAnswers = formData.value.answer ? formData.value.answer.split(',') : [];
    const index = currentAnswers.indexOf(optionLetter);

    if (index > -1) {
      // 如果已经选中，则移除
      currentAnswers.splice(index, 1);
    } else {
      // 如果未选中，则添加
      currentAnswers.push(optionLetter);
    }

    formData.value.answer = currentAnswers.sort().join(',');
  }
}

// 检查选项是否被选中为正确答案
const isOptionCorrect = (optionLetter) => {
  if (formData.value.questionType === '单选题') {
    return formData.value.answer === optionLetter;
  } else if (formData.value.questionType === '多选题') {
    return formData.value.answer ? formData.value.answer.split(',').includes(optionLetter) : false;
  }
  return false;
}

// 编辑题目
const editQuestion = (question) => {
  editingItem.value = question
  formData.value = { ...question }
  showAddDialog.value = true
}

// 删除题目
const deleteQuestion = (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这道题目吗？此操作不可恢复。',
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    onOk() {
      // 使用UpdateQuestionStatus接口，将状态设置为"已删除"或类似状态
      // 或者如果后端有DeleteQuestion接口，使用该接口
      post('api/exam/UpdateQuestionStatus', { 
        id: id, 
        status: '已删除' 
      }, (message) => {
        messageApi.success(message || '删除成功')
        fetchQuestions()
      }, (error) => {
        messageApi.error('删除失败: ' + error)
      })
    }
  })
}

// 验证表单
const validateForm = () => {
  if (!formData.value.text) {
    messageApi.error('请输入题目内容')
    return false
  }

  if (!formData.value.answer) {
    messageApi.error('请选择正确答案')
    return false
  }

  if (!formData.value.course_id || formData.value.course_id === '') {
    messageApi.error('请选择科目')
    return false
  }

  if (!formData.value.knowledge) {
    messageApi.error('请输入知识点')
    return false
  }

  // 选择题需要验证选项
  if (formData.value.questionType === '单选题' || formData.value.questionType === '多选题') {
    if (!formData.value.optionA || !formData.value.optionB) {
      messageApi.error('请至少填写选项A和选项B')
      return false
    }
  }

  return true
}

// 保存题目
const saveQuestion = () => {
  if (!validateForm()) {
    return
  }

  // 准备发送数据
  const requestData = {
    ...formData.value,
    teacher_id: User.value.id,
    // 确保数字字段是字符串（因为数据库中是varchar）
    score: formData.value.score.toString(),
    course_id: parseInt(formData.value.course_id) || null
  }

  const url = editingItem.value ? 'api/exam/UpdateQuestion' : 'api/exam/InsertQuestion'
  post(url, requestData, (message) => {
    messageApi.success(message)
    fetchQuestions()
    closeDialog()
  }, (error) => {
    messageApi.error('保存失败: ' + error)
  })
}

// 关闭对话框
const closeDialog = () => {
  showAddDialog.value = false
  editingItem.value = null
  formData.value = {
    id: '',
    teacher_id: '',
    course_id: '',
    questionType: '单选题',
    text: '',
    score: '5',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    optionE: '',
    optionF: '',
    optionG: '',
    answer: '',
    knowledge: '',
    difficulty: '中等',
    status: '已发布'
  }
}

// 初始化加载
onMounted(() => {
  getCourse()
  fetchQuestions()
})

// 主题样式类
const themeClasses = computed(() => {
  return {
    bg: isDark.value ? 'bg-black' : 'bg-gray-50',
    card: isDark.value
      ? 'backdrop-blur-md bg-white/5 border border-white/10'
      : 'bg-white border border-gray-200 shadow-md',
    text: {
      primary: isDark.value ? 'text-white' : 'text-gray-800',
      secondary: isDark.value ? 'text-white/70' : 'text-gray-600',
      muted: isDark.value ? 'text-white/50' : 'text-gray-500'
    },
    input: isDark.value
      ? 'bg-white/5 border border-white/10 text-white placeholder-white/40'
      : 'bg-gray-50 border border-gray-300 text-gray-800 placeholder-gray-400',
    button: {
      primary: 'bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white',
      secondary: isDark.value
        ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white'
        : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700',
      danger: isDark.value
        ? 'text-red-400 hover:text-red-300'
        : 'text-red-600 hover:text-red-700'
    },
    table: {
      header: isDark.value
        ? 'text-white/50 border-white/10'
        : 'text-gray-500 border-gray-200',
      row: isDark.value
        ? 'border-white/10 hover:bg-white/5'
        : 'border-gray-200 hover:bg-gray-50'
    }
  }
})
</script>

<template>
  <contextHolder />
  <div class="min-h-screen" :class="themeClasses.bg">
    <!-- 顶部导航 -->
    <header class="flex items-center justify-between p-6 border-b" :class="isDark ? 'bg-white/5 border-white/10' : 'bg-white border-gray-200'">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
          <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
        </svg>
        <h1 :class="themeClasses.text.primary + ' text-2xl font-bold'">题型信息管理</h1>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="flex-1 p-8 overflow-auto">
      <!-- 切换标签 -->
      <div class="flex mb-8 border-b" :class="isDark ? 'border-white/10' : 'border-gray-200'">
        <button
          @click="activeTab = 'all'"
          :class="[
            'px-6 py-3 font-medium border-b-2 border-transparent transition-all',
            activeTab === 'all'
              ? 'text-indigo-400 border-indigo-400'
              : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
          ]"
        >
          📚 全部题型
        </button>
        <button
          v-for="questionType in questionTypes"
          :key="questionType.value"
          @click="activeTab = questionType.value"
          :class="[
            'px-6 py-3 font-medium border-b-2 border-transparent transition-all',
            activeTab === questionType.value
              ? 'text-indigo-400 border-indigo-400'
              : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
          ]"
        >
          {{ questionType.emoji }} {{ questionType.value }}
        </button>
      </div>

      <!-- 搜索和操作栏 -->
      <div class="flex items-center justify-between mb-6">
        <div class="relative w-64">
          <input
            type="text"
            v-model="searchQuery"
            placeholder="搜索题目内容、知识点..."
            :class="themeClasses.input + ' w-full pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
          >
          <svg xmlns="http://www.w3.org/2000/svg" :class="themeClasses.text.muted + ' h-5 w-5 absolute left-3 top-2.5'" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
          </svg>
        </div>
        <button
          @click="showAddDialog = true"
          :class="themeClasses.button.primary + ' flex items-center px-4 py-2 rounded-lg transition-colors'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          添加题目
        </button>
      </div>

      <!-- 题目列表 -->
      <div :class="themeClasses.card + ' rounded-2xl p-6'">
        <table class="min-w-full divide-y" :class="themeClasses.table.row">
          <thead>
          <tr>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">题型</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">题目内容</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">答案</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">科目</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">知识点</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">难度</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">分值</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">状态</th>
            <th :class="themeClasses.table.header + ' px-6 py-3 text-right text-xs font-medium uppercase tracking-wider'">操作</th>
          </tr>
          </thead>
          <tbody class="divide-y" :class="themeClasses.table.row">
          <tr v-for="question in filteredQuestions" :key="question.id" class="transition-colors" :class="isDark ? 'hover:bg-white/5' : 'hover:bg-gray-200'">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium" :class="themeClasses.text.primary">
                <span class="inline-flex items-center">
                  {{ questionTypes.find(t => t.value === question.questionType)?.emoji || '✏️' }}
                  <span class="ml-1">{{ question.questionType }}</span>
                </span>
            </td>
            <td class="px-6 py-4 text-sm max-w-xs truncate" :class="themeClasses.text.primary">
              {{ question.text }}
            </td>
            <td class="px-6 py-4 text-sm max-w-xs truncate" :class="themeClasses.text.primary">
              {{ question.answer }}
            </td>
            <td class="px-6 py-4 text-sm" :class="themeClasses.text.primary">
              {{ subjects.find(s => s.id === question.course_id)?.name || question.course_id }}
            </td>
            <td class="px-6 py-4 text-sm" :class="themeClasses.text.primary">
              {{ question.knowledge }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                <span class="inline-flex items-center">
                  {{ difficultyLevels.find(d => d.value === question.difficulty)?.emoji || '😐' }}
                  <span class="ml-1">{{ question.difficulty }}</span>
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                <span class="inline-flex items-center bg-indigo-500/20 text-indigo-400 px-3 py-1 rounded-full text-xs font-medium">
                  ⭐ {{ question.score }}分
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                <span :class="{
                  'px-3 py-1 rounded-full text-xs font-medium inline-flex items-center': true,
                  'bg-green-500/20 text-green-400': question.status === '已发布',
                  'bg-yellow-500/20 text-yellow-400': question.status === '待审核',
                  'bg-red-500/20 text-red-400': question.status === '已禁用',
                  'bg-blue-500/20 text-blue-400': question.status === '草稿'
                }">
                  {{ statusOptions.find(s => s.value === question.status)?.emoji || '🟡' }}
                  <span class="ml-1">{{ question.status }}</span>
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
              <button @click="editQuestion(question)" :class="themeClasses.button.primary.replace('bg-gradient-to-r', 'text') + ' mr-3'">编辑</button>
              <button @click="deleteQuestion(question.id)" :class="themeClasses.button.danger">删除</button>
            </td>
          </tr>
          <tr v-if="filteredQuestions.length === 0">
            <td colspan="9" class="px-6 py-4 text-center text-sm" :class="themeClasses.text.muted">
              暂无题目数据
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- 添加/编辑题目对话框 -->
    <div v-if="showAddDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div :class="themeClasses.card + ' rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto'">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-medium" :class="themeClasses.text.primary">{{ editingItem ? '编辑' : '添加' }}题目</h3>
          <button @click="closeDialog" :class="themeClasses.text.muted + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <!-- 题型选择 -->
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📝 题型</label>
            <select
              v-model="formData.questionType"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
            >
              <option v-for="questionType in questionTypes" :key="questionType.value" :value="questionType.value" :class="isDark ? 'bg-gray-800' : 'bg-white'">
                {{ questionType.emoji }} {{ questionType.value }}
              </option>
            </select>
          </div>

          <!-- 题目内容 -->
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📄 题目内容</label>
            <textarea
              v-model="formData.text"
              rows="3"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              placeholder="请输入题目内容..."
            ></textarea>
          </div>

          <!-- 选择题选项 -->
          <div v-if="formData.questionType === '单选题' || formData.questionType === '多选题'">
            <label class="block text-sm font-medium mb-2" :class="themeClasses.text.secondary">📋 选项（点击选项内容设为正确答案）</label>
            <div class="space-y-3">
              <div v-for="option in ['A', 'B', 'C', 'D', 'E', 'F', 'G']" :key="option" class="flex items-center space-x-3">
                <!-- 选项选择按钮 -->
                <div
                  :class="{
                    'w-8 h-8 flex items-center justify-center rounded-full cursor-pointer transition-colors relative': true,
                    'bg-green-500/20 text-green-400 border border-green-400': isOptionCorrect(option),
                    'bg-white/5 text-white/50 border border-white/10 hover:bg-white/10': !isOptionCorrect(option) && isDark,
                    'bg-gray-100 text-gray-500 border border-gray-300 hover:bg-gray-200': !isOptionCorrect(option) && !isDark
                  }"
                  @click="handleCorrectOptionChange(option)"
                  :title="isOptionCorrect(option) ? '当前是正确答案' : '设为正确答案'"
                >
                  {{ option }}
                  <span v-if="isOptionCorrect(option)" class="absolute -top-1 -right-1 text-xs">✓</span>
                </div>

                <!-- 选项内容输入框 -->
                <input
                  v-model="formData['option' + option]"
                  type="text"
                  :class="themeClasses.input + ' flex-1 px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                  :placeholder="`输入选项${option}的内容`"
                >
              </div>
            </div>

            <!-- 显示当前正确答案 -->
            <div class="mt-3 p-3 rounded-lg" :class="isDark ? 'bg-white/5' : 'bg-gray-50'">
              <div v-if="formData.answer" class="text-sm text-green-400">
                <span v-if="formData.questionType === '单选题'">
                  当前正确答案：选项 {{ formData.answer }}
                </span>
                <span v-else>
                  当前正确答案：选项 {{ formData.answer.split(',').join('、') }}
                </span>
              </div>
              <div v-else class="text-sm text-yellow-400">
                {{ formData.questionType === '单选题' ? '请选择一个正确答案' : '请选择至少一个正确答案' }}
              </div>
            </div>
          </div>

          <!-- 非选择题的答案输入框 -->
          <div v-else>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">✅ 答案</label>
            <textarea
              v-model="formData.answer"
              rows="2"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              placeholder="请输入题目答案..."
            ></textarea>
          </div>

          <!-- 科目和知识点 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📚 科目</label>
              <select
                v-model="formData.course_id"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                :disabled="subjects.length === 1 && subjects[0].id === -1"
              >
                <option value="" :class="isDark ? 'bg-gray-800' : 'bg-white'" v-if="subjects.length === 1 && subjects[0].id === -1">
                  选择科目(暂无科目或科目未审核通过)
                </option>
                <option value="" :class="isDark ? 'bg-gray-800' : 'bg-white'">选择科目</option>
                <option
                  v-for="subject in subjects"
                  :key="subject.id"
                  :value="subject.id === -1 ? '' : subject.id"
                  :class="isDark ? 'bg-gray-800' : 'bg-white'"
                >
                  {{ subject.name }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">🧠 知识点</label>
              <input
                v-model="formData.knowledge"
                type="text"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                placeholder="请输入知识点"
              >
            </div>
          </div>

          <!-- 分值、难度和状态 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">⭐ 分值</label>
              <input
                v-model="formData.score"
                type="number"
                min="1"
                max="100"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                placeholder="请输入分值"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📊 难度</label>
              <select
                v-model="formData.difficulty"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              >
                <option v-for="level in difficultyLevels" :key="level.value" :value="level.value" :class="isDark ? 'bg-gray-800' : 'bg-white'">
                  {{ level.emoji }} {{ level.value }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">🔄 状态</label>
              <select
                v-model="formData.status"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              >
                <option v-for="status in statusOptions" :key="status.value" :value="status.value" :class="isDark ? 'bg-gray-800' : 'bg-white'">
                  {{ status.emoji }} {{ status.value }}
                </option>
              </select>
            </div>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="closeDialog"
              :class="themeClasses.button.secondary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              取消
            </button>
            <button
              @click="saveQuestion"
              :class="themeClasses.button.primary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              保存
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 浅色模式滚动条 */
:deep(.light) ::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
}

:deep(.light) ::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
}

:deep(.light) ::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>
