<script setup>
import {computed, onMounted, ref} from 'vue'
import {get, post} from "@/net/index.js"
import {message, Modal} from "ant-design-vue"
import { isDark } from "@/stores/theme.js"
import { formatDate } from "@/time/Data.js"

const [messageApi, contextHolder] = message.useMessage()

// 班级数据
const classes = ref([])
const filteredClasses = ref([])
const currentClass = ref(null)
const showClassDetail = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const User=ref({})
// 学生数据
const students = ref([])
const filteredStudents = ref([])
const studentSearchQuery = ref('')

// 控制"添加学生"弹窗
const showAddStudentModal = ref(false)
const currentAddingClassId = ref(null)
const selectedStudentIds = ref([])
const availableStudents = ref([])
const availableStudentSearchQuery = ref('')
const filteredAvailableStudents = ref([])

// 搜索和筛选
const searchQuery = ref('')
const selectedStatus = ref('all')
const selectedGrade = ref('all')

// 表单数据
const classForm = ref({
  id: '',
  name: '',
  grade: '',
  major: '',
  teacher_id: '',
  size: 0,
  status: 'active',
  introduction: ''
})

// 状态选项
const statusOptions = [
  { value: 'active', label: '活跃', color: 'green' },
  { value: 'inactive', label: '停用', color: 'red' },
  { value: 'graduated', label: '已毕业', color: 'blue' }
]

// 年级选项
const gradeOptions = ['2024级', '2023级', '2022级', '2021级', '2020级']

// 专业选项
const majorOptions = ['计算机科学与技术', '软件工程', '人工智能', '数据科学', '网络安全']

// 获取个人信息 - 改进版本
const UserInfo = () => {
  return new Promise((resolve, reject) => {
    get('api/exam/current', {}, (message, data) => {
      User.value = data || {}
      console.log("个人信息：", User.value)
      resolve(data)
    }, (error) => {
      console.error("获取用户信息失败:", error)
      User.value = {}
      reject(error)
    })
  })
}

// 计算属性：判断是否为管理员
const isAdmin = computed(() => {
  return User.value.role === '管理员'
})

// 获取所有老师
const teacherOptions = ref([])

const fetchTeachers = () => {
  get('api/exam/AllTeacher', {}, (message, data) => {
    teacherOptions.value = data
    console.log("老师数据：", teacherOptions.value)
  })
}

// 根据teacher_id获取教师姓名的方法
const getTeacherName = (teacherId) => {
  if (!teacherId) return '未分配'
  const teacher = teacherOptions.value.find(t => t.user_id === teacherId)
  return teacher ? teacher.account : '未知教师'
}

// 获取班级数据
const fetchClasses = () => {
  get('api/exam/AllClass', {}, (message, data) => {
    classes.value = data
    filteredClasses.value = data
  })
}

// 获取所有学生
const fetchAllStudents = () => {
  get('api/exam/AllUser', {}, (message, data) => {
    students.value = data
    availableStudents.value = students.value.filter(student =>
      student.status === '审核通过' && student.role === '学生' && student.class_id === null
    )
    filteredAvailableStudents.value = [...availableStudents.value]
    console.log("所有学生：", students.value)
    console.log("筛选后的学生：", filteredAvailableStudents.value)
  })
}

// 搜索和筛选班级
const filterClasses = () => {
  filteredClasses.value = classes.value.filter(cls => {
    const matchesSearch = cls.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      cls.major.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      getTeacherName(cls.teacher_id).toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesStatus = selectedStatus.value === 'all' || cls.status === selectedStatus.value
    const matchesGrade = selectedGrade.value === 'all' || cls.grade === selectedGrade.value

    return matchesSearch && matchesStatus && matchesGrade
  })
}

// 获取班级学生
const getClassStudents = (classId) => {
  get('api/exam/AllUser', {},
    (message, data) => {
      filteredStudents.value = data.filter(user => user.role === '学生' && user.class_id === classId)
      console.log("班级学生：", filteredStudents.value)
    })
}

// 搜索和筛选学生
const filterStudents = () => {
  filteredStudents.value = students.value.filter(student => {
    return student.name?.toLowerCase().includes(studentSearchQuery.value.toLowerCase()) ||
      student.user_id?.toString().includes(studentSearchQuery.value.toLowerCase())
  })
}

// 查看班级详情
const viewClassDetail = (cls) => {
  getClassStudents(cls.id)
  currentClass.value = cls
  showClassDetail.value = true
}

// 打开添加学生弹窗
const openAddStudent = (classId) => {
  currentAddingClassId.value = classId
  selectedStudentIds.value = []
  showAddStudentModal.value = true
}

// 搜索可选学生
const filterAvailableStudents = () => {
  filteredAvailableStudents.value = availableStudents.value.filter(student => {
    return student.account?.toLowerCase().includes(availableStudentSearchQuery.value.toLowerCase()) ||
      student.user_id?.toString().includes(availableStudentSearchQuery.value.toLowerCase())
  })
}

// 全选/取消全选学生
const handleSelectAll = (event) => {
  if (event.target.checked) {
    selectedStudentIds.value = filteredAvailableStudents.value.map(student => student.user_id)
  } else {
    selectedStudentIds.value = []
  }
}

// 选择/取消选择单个学生
const handleSelectStudent = (studentId, event) => {
  if (event.target.checked) {
    selectedStudentIds.value.push(studentId)
  } else {
    selectedStudentIds.value = selectedStudentIds.value.filter(id => id !== studentId)
  }
}

// 确认添加学生到班级
const confirmAddStudents = () => {
  if (selectedStudentIds.value.length === 0) {
    messageApi.warning('请至少选择一名学生')
    return
  }

  post('api/exam/JoinClass', {
    class_id: currentAddingClassId.value,
    id: selectedStudentIds.value
  }, (message) => {
    messageApi.success(`成功添加 ${selectedStudentIds.value.length} 名学生到班级`)
    fetchClasses()
    fetchAllStudents()
    showAddStudentModal.value = false
  }, (error) => {
    messageApi.error(error)
  })
}

// 编辑班级
const editClass = (cls) => {
  classForm.value = { ...cls }
  showEditModal.value = true
}

// 删除班级
const deleteClass = (id, name) => {
  Modal.confirm({
    title: '确认删除班级',
    content: `确定要删除班级 "${name}" 吗？此操作不可恢复。`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      post('api/exam/DeleteClass', {
        id: id
      }, (message) => {
        messageApi.success(message)
        fetchClasses()
      }, (error) => {
        messageApi.error(error)
      })
    }
  })
}

// 创建新班级
const createClass = () => {
  classForm.value = {
    id: '',
    name: '',
    grade: '',
    major: '',
    teacher_id: '',
    size: 0,
    status: 'active',
    introduction: ''
  }
  showCreateModal.value = true
}

// 保存班级
const saveClass = () => {
  if (classForm.value.id) {
    // 更新
    post('api/exam/UpdateClass', classForm.value, (message) => {
      messageApi.success(message)
      fetchClasses()
      showEditModal.value = false
    }, (error) => {
      messageApi.error(error)
    })
  } else {
    // 创建
    post('api/exam/InsertClass', classForm.value, (message) => {
      messageApi.success(message)
      fetchClasses()
      showCreateModal.value = false
    }, (error) => {
      messageApi.error(error)
    })
  }
}

// 初始化
onMounted(() => {
  UserInfo()
  fetchClasses()
  fetchAllStudents()
  fetchTeachers()
})

// 统计数据
const stats = computed(() => {
  const total = classes.value.length
  const active = classes.value.filter(cls => cls.status === 'active').length
  const inactive = classes.value.filter(cls => cls.status === 'inactive').length
  const graduated = classes.value.filter(cls => cls.status === 'graduated').length

  return { total, active, inactive, graduated }
})

// 防抖搜索
let searchTimeout = null
const searchClasses = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    filterClasses()
  }, 300)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
  filterClasses()
}
</script>

<template>
  <context-holder />
  <div :class="isDark?'bg-black':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="min-h-screen">
    <div class="max-w-7xl mx-auto p-6">
      <!-- 头部 -->
      <header class="flex items-center justify-between border-b p-6 shadow-md rounded-xl hover:shadow-lg duration-200 mb-8" :class="isDark?'border-white/20':'border-gray-200'">
        <div class="flex items-center space-x-4">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
            <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
          </svg>
          <div>
            <h1 :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">班级管理系统</h1>
            <p :class="isDark?'text-white/70':'text-gray-600'" class="text-sm">高效管理学校班级信息</p>
          </div>
        </div>
        <button
          @click="createClass"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors flex items-center"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          创建新班级
        </button>
      </header>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div v-for="(stat, index) in [
          { label: '班级总数', value: stats.total, color: 'blue' },
          { label: '活跃班级', value: stats.active, color: 'green' },
          { label: '停用班级', value: stats.inactive, color: 'red' },
          { label: '已毕业', value: stats.graduated, color: 'purple' }
        ]" :key="index"
             class="backdrop-blur-md rounded-2xl p-6 border shadow-md hover:shadow-lg duration-300"
             :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
          <p :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ stat.label }}</p>
          <p :class="isDark?'text-white':'text-gray-900'" class="text-3xl font-bold mt-2">{{ stat.value }}</p>
        </div>
      </div>

      <!-- 搜索和筛选 -->
      <div class="backdrop-blur-md rounded-2xl p-6 border mb-8" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center space-x-4">
            <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">班级列表</h3>
            <span :class="isDark?'text-white':'text-gray-600'" v-if="searchQuery" class="text-sm">
              (搜索 "{{ searchQuery }}" 找到 {{ filteredClasses.length }} 条结果)
            </span>
          </div>
          <div class="flex items-center space-x-4">
            <div class="relative">
              <input
                v-model="searchQuery"
                @input="searchClasses"
                type="text"
                placeholder="搜索班级... (按ESC清除)"
                :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent pl-10 pr-10',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                @keyup.esc="clearSearch"
              >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-3 top-2.5" :class="isDark?'text-white/50':'text-gray-400'" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
              </svg>
              <button
                v-if="searchQuery"
                @click="clearSearch"
                class="absolute right-3 top-2.5 transition-colors"
                :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'"
                title="清除搜索"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            <select
              v-model="selectedStatus"
              @change="filterClasses"
              :class="[
                'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
            >
              <option value="all">所有状态</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select
              v-model="selectedGrade"
              @change="filterClasses"
              :class="[
                'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
            >
              <option value="all">所有年级</option>
              <option v-for="grade in gradeOptions" :key="grade" :value="grade">
                {{ grade }}
              </option>
            </select>
          </div>
        </div>

        <!-- 班级列表 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <div v-for="cls in filteredClasses" :key="cls.id"
               class="rounded-xl p-5 border transition-all hover:scale-[1.02] group"
               :class="isDark ? 'bg-white/5 border-white/10 hover:border-indigo-400/30' : 'bg-white border-gray-200 hover:border-indigo-300 shadow-sm'">
            <!-- 班级头部 -->
            <div class="flex justify-between items-start mb-4">
              <div>
                <h4 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-semibold mb-1 truncate" :title="cls.name">{{ cls.name }}</h4>
                <p :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">ID: {{ cls.id }}</p>
              </div>
              <span :class="{
                'px-2 py-1 rounded-full text-xs font-medium': true,
                'bg-green-500/20 text-green-400': cls.status === 'active',
                'bg-yellow-500/20 text-yellow-400': cls.status === 'inactive',
                'bg-red-500/20 text-red-400': cls.status === 'graduated'
              }">
                {{ statusOptions.find(s => s.value === cls.status)?.label }}
              </span>
            </div>

            <!-- 班级信息 -->
            <div class="space-y-3 mb-5">
              <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                </svg>
                {{ getTeacherName(cls.teacher_id) }}
              </div>
              <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 01-1.898-.316l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
                {{ cls.grade }} · {{ cls.major }}
              </div>
              <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
                </svg>
                学生: {{ cls.size || 0 }} 人
              </div>
              <div v-if="cls.introduction" class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2h-1V9z" clip-rule="evenodd" />
                </svg>
                简介: {{ cls.introduction }}
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex justify-between pt-4 border-t" :class="isDark?'border-white/10':'border-gray-200'">
              <div class="flex space-x-2">
                <!-- 查看详情按钮 -->
                <button
                  @click="viewClassDetail(cls)"
                  class="p-2 rounded-full transition-colors group relative"
                  :class="isDark ? 'bg-blue-500/20 hover:bg-blue-500/30 text-blue-400' : 'bg-blue-100 hover:bg-blue-200 text-blue-600'"
                  title="查看班级详情"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                    <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                  </svg>
                  <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                        :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                    查看详情
                  </span>
                </button>

                <!-- 添加学生按钮 -->
                <button
                  v-if="isAdmin"
                  @click="openAddStudent(cls.id)"
                  class="p-2 rounded-full transition-colors group relative"
                  :class="isDark ? 'bg-green-500/20 hover:bg-green-500/30 text-green-400' : 'bg-green-100 hover:bg-green-200 text-green-600'"
                  title="添加学生"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                  </svg>
                  <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                        :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                    添加学生
                  </span>
                </button>
              </div>

              <div class="flex space-x-2">
                <!-- 编辑按钮 -->
                <button
                  v-if="isAdmin"
                  @click="editClass(cls)"
                  class="p-2 rounded-full transition-colors group relative"
                  :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                  title="编辑"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-400" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                  </svg>
                  <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                        :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                    编辑
                  </span>
                </button>

                <!-- 删除按钮 -->
                <button
                  v-if="isAdmin"
                  @click="deleteClass(cls.id, cls.name)"
                  class="p-2 rounded-full transition-colors group relative"
                  :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                  title="删除"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                  <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                        :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                    删除
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredClasses.length === 0" class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto mb-4" :class="isDark?'text-white/30':'text-gray-300'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-2m2 0V9m0 12H5m14 0h2M5 21H3m2 0V9M5 21h2" />
          </svg>
          <p :class="isDark?'text-white/50':'text-gray-500'">暂无班级数据</p>
          <button @click="createClass" class="mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors">
            创建第一个班级
          </button>
        </div>
      </div>
    </div>

    <!-- 添加学生到班级弹窗 -->
    <div v-if="showAddStudentModal" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-4xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">添加学生到班级</h3>
          <button @click="showAddStudentModal = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="mb-4">
          <input
            type="text"
            v-model="availableStudentSearchQuery"
            @input="filterAvailableStudents"
            placeholder="搜索学生..."
            :class="[
              'w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
              isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
            ]"
          >
        </div>

        <div class="overflow-x-auto max-h-96">
          <table class="w-full">
            <thead>
            <tr class="text-left border-b" :class="isDark?'text-white/50 border-white/10':'text-gray-500 border-gray-200'">
              <th class="pb-3 px-4 w-12">
                <input
                  type="checkbox"
                  :class="isDark?'border-white/10':'border-gray-300'"
                  class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 rounded"
                  :checked="filteredAvailableStudents.length > 0 && selectedStudentIds.length === filteredAvailableStudents.length"
                  @change="handleSelectAll"
                >
              </th>
              <th class="pb-3 px-4">学生ID</th>
              <th class="pb-3 px-4">姓名</th>
              <th class="pb-3 px-4">性别</th>
              <th class="pb-3 px-4">联系电话</th>
              <th class="pb-3 px-4">邮箱</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="student in filteredAvailableStudents" :key="student.user_id" class="border-b transition-colors" :class="isDark?'border-white/10 hover:bg-white/5':'border-gray-200 hover:bg-gray-50'">
              <td class="py-4 px-4">
                <input
                  type="checkbox"
                  :value="student.user_id"
                  v-model="selectedStudentIds"
                  class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 rounded"
                  :class="isDark?'border-white/10':'border-gray-300'"
                >
              </td>
              <td class="py-4 px-4 font-mono" :class="isDark?'text-white':'text-gray-900'">{{ student.id }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.account }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.sex || '暂无' }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.phone || '未填写' }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.email || '未填写' }}</td>
            </tr>
            <tr v-if="filteredAvailableStudents.length === 0">
              <td colspan="6" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                暂无可用学生
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="flex justify-between pt-4 border-t mt-4" :class="isDark?'border-white/10':'border-gray-200'">
          <div class="text-sm" :class="isDark?'text-white/50':'text-gray-500'">
            已选择 {{ selectedStudentIds.length }} 名学生
          </div>
          <div class="flex space-x-2">
            <button
              @click="showAddStudentModal = false"
              class="px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="confirmAddStudents"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
              :disabled="selectedStudentIds.length === 0"
            >
              添加选中学生
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 班级详情模态框 -->
    <div v-if="showClassDetail" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-4xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">班级详情 - {{ currentClass?.name }}</h3>
          <button @click="showClassDetail = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 班级基本信息 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-gray-50 border-gray-200'">
            <h4 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium mb-4">班级信息</h4>
            <div class="space-y-3">
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">班级名称:</span>
                <span :class="isDark?'text-white':'text-gray-900'">{{ currentClass?.name }}</span>
              </div>
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">年级:</span>
                <span :class="isDark?'text-white':'text-gray-900'">{{ currentClass?.grade }}</span>
              </div>
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">专业:</span>
                <span :class="isDark?'text-white':'text-gray-900'">{{ currentClass?.major }}</span>
              </div>
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">班主任:</span>
                <span :class="isDark?'text-white':'text-gray-900'">{{ getTeacherName(currentClass?.teacher_id) }}</span>
              </div>
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">学生人数:</span>
                <span :class="isDark?'text-white':'text-gray-900'">{{ currentClass?.size || 0 }} 人</span>
              </div>
              <div class="flex justify-between">
                <span :class="isDark?'text-white/60':'text-gray-500'">状态:</span>
                <span :class="{
                  'px-2 py-1 rounded-full text-xs font-medium': true,
                  'bg-green-500/20 text-green-400': currentClass?.status === 'active',
                  'bg-yellow-500/20 text-yellow-400': currentClass?.status === 'inactive',
                  'bg-red-500/20 text-red-400': currentClass?.status === 'graduated'
                }">
                  {{ statusOptions.find(s => s.value === currentClass?.status)?.label }}
                </span>
              </div>
            </div>
          </div>
          <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-gray-50 border-gray-200'">
            <h4 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium mb-4">班级简介</h4>
            <p :class="isDark?'text-white/60':'text-gray-500'">{{ currentClass?.introduction || '暂无简介信息' }}</p>
          </div>
        </div>

        <!-- 学生列表 -->
        <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-gray-50 border-gray-200'">
          <div class="flex justify-between items-center mb-4">
            <h4 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">学生列表</h4>
            <div class="relative">
              <input
                v-model="studentSearchQuery"
                @input="filterStudents"
                type="text"
                placeholder="搜索学生..."
                :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent pl-10',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              />
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-3 top-2.5" :class="isDark?'text-white/50':'text-gray-400'" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
              </svg>
            </div>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
              <tr class="text-left border-b" :class="isDark?'text-white/50 border-white/10':'text-gray-500 border-gray-200'">
                <th class="pb-3 px-4">学生ID</th>
                <th class="pb-3 px-4">姓名</th>
                <th class="pb-3 px-4">性别</th>
                <th class="pb-3 px-4">联系电话</th>
                <th class="pb-3 px-4">邮箱</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="student in filteredStudents" :key="student.user_id" class="border-b transition-colors" :class="isDark?'border-white/10 hover:bg-white/5':'border-gray-200 hover:bg-gray-50'">
                <td class="py-4 px-4 font-mono" :class="isDark?'text-white':'text-gray-900'">{{ student.id }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.account }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.sex || '暂无' }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.phone || '未填写' }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ student.email || '未填写' }}</td>
              </tr>
              <tr v-if="filteredStudents.length === 0">
                <td colspan="5" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                  暂无学生数据
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="flex justify-end pt-4 border-t mt-4" :class="isDark?'border-white/10':'border-gray-200'">
          <button
            @click="showClassDetail = false"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 创建/编辑班级模态框 -->
    <div v-if="showCreateModal || showEditModal" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">
            {{ classForm.id ? '编辑班级' : '创建新班级' }}
          </h3>
          <button @click="showCreateModal = false; showEditModal = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">班级名称</label>
              <input
                v-model="classForm.name"
                type="text"
                required
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="例如：计算机科学与技术1班"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">年级</label>
              <select
                v-model="classForm.grade"
                required
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="" disabled selected hidden>选择年级</option>
                <option v-for="grade in gradeOptions" :key="grade" :value="grade">
                  {{ grade }}
                </option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">专业</label>
              <select
                v-model="classForm.major"
                required
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="" disabled selected hidden>选择专业</option>
                <option v-for="major in majorOptions" :key="major" :value="major">
                  {{ major }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">班主任</label>
              <select
                v-model="classForm.teacher_id"
                required
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="" disabled selected hidden>请选择教师</option>
                <option
                  v-for="teacher in teacherOptions"
                  :key="teacher.user_id"
                  :value="teacher.user_id"
                >
                  {{ teacher.account }}
                </option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">学生人数</label>
              <input
                v-model.number="classForm.size"
                type="number"
                min="0"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">状态</label>
              <select
                v-model="classForm.status"
                required
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="" disabled selected hidden>选择状态</option>
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">班级简介</label>
            <textarea
              v-model="classForm.introduction"
              rows="3"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
              placeholder="可选：班级简介或备注信息"
            ></textarea>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showCreateModal = false; showEditModal = false"
              class="flex-1 px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="saveClass"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              {{ classForm.id ? '保存更改' : '创建班级' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
