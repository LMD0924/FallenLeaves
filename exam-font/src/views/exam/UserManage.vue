x<script setup>
import {ref, computed, onMounted} from 'vue'
import {get, post} from "@/net/index.js";
import { message } from "ant-design-vue"
import {isDark} from "@/stores/theme.js"

const [messageApi, contextHolder] = message.useMessage()

// 当前激活的标签页
const activeTab = ref('teachers')

// 搜索查询
const searchQuery = ref('')

// 对话框状态
const showAddDialog = ref(false)
const editingItem = ref(null)

// 表单数据
const formData = ref({
  id: '',
  account: '',
  username: '',
  password: '',
  user_id: '',
  status: '',
  phone: '',
  email: '',
  professional:''
})

// 状态选项
const teacherStatuses = ['审核通过', '审核不通过', '未审核']
const studentStatuses = ['审核通过', '审核不通过', '未审核']

// 修改初始化值为数组
const teachers = ref([])
const students = ref([])

const filteredTeachers = computed(() => {
  return teachers.value.filter(teacher =>
    teacher.account?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    teacher.user_id?.toString().includes(searchQuery.value) ||
    teacher.status?.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const filteredStudents = computed(() => {
  return students.value.filter(student =>
    student.account?.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    student.user_id?.toString().includes(searchQuery.value) ||
    student.status?.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 确保API返回的数据结构正确
const AllTeacher = () => {
  get('api/exam/AllTeacher', {}, (message, data) => {
    teachers.value = Array.isArray(data) ? data.map(item => ({
      ...item,
      account: item.account || '暂未开发或无数据',
      department: item.department || '暂未开发或无数据',
      email: item.email || '暂未开发或无数据',
      courses: item.courses || [],
    })) : []
    console.log("教师数据:", teachers.value)
  })
}

const AllStudent = () => {
  get('api/exam/AllStudent', {}, (message, data) => {
    students.value = Array.isArray(data) ? data.map(item => ({
      ...item,
      account: item.account || item.account || '暂未开发或无数据',
      class: item.class || '暂未开发或无数据',
      professional: item.professional || '暂未开发或无数据',
      email: item.email || '暂未开发或无数据',
    })) : []
    console.log("学生数据:", students.value)
  })
}

//根据身份管理用户信息
const currentUserRole=ref('')
const User=ref({})
const isEditSelf=ref(false)
const getCurrentUser=()=>{
  get('api/exam/current',{},(message,data)=>{
    currentUserRole.value=data.role
    User.value=data
    console.log("当前用户:", User.value)
  })
}

// 编辑教师
const editTeacher = (teacher) => {
  editingItem.value = teacher
  formData.value = { ...teacher }
  isEditSelf.value=teacher.user_id===User.value.id
  showAddDialog.value = true
}

// 编辑学生
const editStudent = (student) => {
  editingItem.value = student
  formData.value = { ...student }
  isEditSelf.value=student.user_id===User.value.id
  showAddDialog.value = true
}

// 删除教师
const deleteTeacher = (id) => {
  teachers.value = teachers.value.filter(teacher => teacher.id !== id)
}

// 删除学生
const deleteStudent = (id) => {
  students.value = students.value.filter(student => student.id !== id)
}

// 关闭对话框
const closeDialog = () => {
  showAddDialog.value = false
  editingItem.value = null
  formData.value = {
    id: '',
    account: '',
    username: '',
    password: '',
    department: '',
    class: '',
    professional:'',
    status: '未审核'
  }
}

// 保存项目
const saveItem = () => {
  if (editingItem.value) {
    // 更新现有用户
    post('/api/exam/UpdateUserInfo', {
      id: formData.value.user_id,
      user_id:formData.value.user_id,
      account: formData.value.account,
      username: formData.value.username || editingItem.value.username,
      password: formData.value.password || editingItem.value.password,
      role: formData.value.role || editingItem.value.role,
      phone: formData.value.phone || editingItem.value.phone,
      email: formData.value.email || editingItem.value.email,
      status: formData.value.status,
      professional:formData.value.professional
    }, (message) => {
      messageApi.success(message)
      // 重新加载数据
      if (activeTab.value === 'teachers') {
        AllTeacher()
      } else {
        AllStudent()
      }
    }, (errorMessage) => {
      messageApi.error(errorMessage)
    })
  } else {
    // 添加新用户
    if (activeTab.value === 'teachers') {
      teachers.value.push({ ...formData.value })
    } else {
      students.value.push({ ...formData.value })
    }
    messageApi.success('添加成功')
  }
  closeDialog()
}

onMounted(()=>{
  getCurrentUser()
  AllTeacher()
  AllStudent()
})
</script>

<template>
  <contextHolder />
  <div :class="isDark ? 'bg-black' : 'bg-gradient-to-br from-blue-50 via-white to-indigo-50'" class="min-h-screen p-6">
    <div class="max-w-7xl mx-auto">
      <!-- 页面标题区域 -->
      <div class="mb-8">
        <div class="flex items-center justify-between">
          <div>
            <h1 :class="isDark ? 'text-white' : 'text-gray-900'" class="text-4xl font-bold mb-3">用户管理</h1>
            <p :class="isDark ? 'text-gray-400' : 'text-gray-600'" class="text-lg">管理系统中的教师和学生账户信息</p>
          </div>
          <div class="flex items-center space-x-4">
            <!-- 统计卡片 -->
            <div :class="isDark ? 'bg-gray-900 border-gray-700' : 'bg-white border-gray-200'"
                 class="px-6 py-4 rounded-2xl border backdrop-blur-sm">
              <div class="text-center">
                <div :class="isDark ? 'text-blue-400' : 'text-blue-600'" class="text-2xl font-bold">{{ teachers.length }}</div>
                <div :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">教师总数</div>
              </div>
            </div>
            <div :class="isDark ? 'bg-gray-900 border-gray-700' : 'bg-white border-gray-200'"
                 class="px-6 py-4 rounded-2xl border backdrop-blur-sm">
              <div class="text-center">
                <div :class="isDark ? 'text-green-400' : 'text-green-600'" class="text-2xl font-bold">{{ students.length }}</div>
                <div :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">学生总数</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容卡片 -->
      <div :class="isDark ? 'bg-gray-900/80 border-gray-700' : 'bg-white/80 border-gray-200'"
           class="rounded-3xl border backdrop-blur-xl shadow-2xl overflow-hidden">

        <!-- 标签页导航 -->
        <div :class="isDark ? 'border-gray-700' : 'border-gray-200'" class="border-b">
          <div class="flex items-center justify-between px-8 py-6">
            <div class="flex space-x-1">
              <button
                @click="activeTab = 'teachers'"
                :class="[
                  'px-6 py-3 rounded-xl font-semibold text-sm transition-all duration-300 relative',
                  activeTab === 'teachers'
                    ? isDark
                      ? 'bg-blue-500 text-white shadow-lg shadow-blue-500/25'
                      : 'bg-blue-500 text-white shadow-lg shadow-blue-500/25'
                    : isDark
                      ? 'text-gray-400 hover:text-white hover:bg-gray-800'
                      : 'text-gray-500 hover:text-gray-700 hover:bg-gray-100'
                ]"
              >
                教师管理
                <div v-if="activeTab === 'teachers'" class="absolute bottom-0 left-1/2 transform -translate-x-1/2 w-1/2 h-0.5 bg-white rounded-full"></div>
              </button>
              <button
                @click="activeTab = 'students'"
                :class="[
                  'px-6 py-3 rounded-xl font-semibold text-sm transition-all duration-300 relative',
                  activeTab === 'students'
                    ? isDark
                      ? 'bg-green-500 text-white shadow-lg shadow-green-500/25'
                      : 'bg-green-500 text-white shadow-lg shadow-green-500/25'
                    : isDark
                      ? 'text-gray-400 hover:text-white hover:bg-gray-800'
                      : 'text-gray-500 hover:text-gray-700 hover:bg-gray-100'
                ]"
              >
                学生管理
                <div v-if="activeTab === 'students'" class="absolute bottom-0 left-1/2 transform -translate-x-1/2 w-1/2 h-0.5 bg-white rounded-full"></div>
              </button>
            </div>

            <!-- 搜索栏 -->
            <div class="relative w-80">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索用户ID、姓名或状态..."
                :class="[
                  'w-full px-4 py-3 pl-12 rounded-xl border backdrop-blur-sm transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800/50 border-gray-600 text-white placeholder-gray-400 focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white/50 border-gray-300 text-gray-900 placeholder-gray-500 focus:ring-blue-500 focus:border-transparent'
                ]"
              />
              <svg class="absolute left-4 top-3.5 h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
            </div>
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="p-8">
          <!-- 教师管理 -->
          <div v-if="activeTab === 'teachers'" class="space-y-6">
            <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
              <div
                v-for="teacher in filteredTeachers"
                :key="teacher.id"
                :class="[
                  'group relative p-6 rounded-2xl border backdrop-blur-sm transition-all duration-500 hover:scale-105 cursor-pointer',
                  isDark
                    ? 'bg-gray-800/50 border-gray-700 hover:border-blue-500/50 hover:shadow-2xl hover:shadow-blue-500/10'
                    : 'bg-white/50 border-gray-200 hover:border-blue-400/50 hover:shadow-2xl hover:shadow-blue-500/10'
                ]"
                @click="editTeacher(teacher)"
              >
                <!-- 状态指示器 -->
                <div class="absolute top-4 right-4">
                  <div :class="{
                    'w-3 h-3 rounded-full': true,
                    'bg-green-500': teacher.status === '审核通过',
                    'bg-red-500': teacher.status === '审核不通过',
                    'bg-yellow-500': teacher.status === '未审核'
                  }"></div>
                </div>

                <!-- 用户头像和基本信息 -->
                <div class="flex items-center space-x-4 mb-4">
                  <div :class="[
                    'w-14 h-14 rounded-2xl flex items-center justify-center text-white font-bold text-xl shadow-lg',
                    isDark ? 'bg-gradient-to-br from-blue-500 to-blue-600' : 'bg-gradient-to-br from-blue-500 to-blue-600'
                  ]">
                    {{ teacher.account?.charAt(0) || 'T' }}
                  </div>
                  <div class="flex-1">
                    <h3 :class="isDark ? 'text-white' : 'text-gray-900'" class="font-bold text-lg truncate">
                      {{ teacher.account }}
                    </h3>
                    <p :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">
                      ID: {{ teacher.user_id }}
                    </p>
                  </div>
                </div>

                <!-- 详细信息 -->
                <div class="space-y-3">
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">部门</span>
                    <span :class="isDark ? 'text-white' : 'text-gray-900'" class="font-medium text-sm">{{ teacher.department }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">邮箱</span>
                    <span :class="isDark ? 'text-blue-400' : 'text-blue-600'" class="font-medium text-sm truncate ml-2">{{ teacher.email }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">状态</span>
                    <span :class="{
                      'px-3 py-1 rounded-full text-xs font-semibold': true,
                      'bg-green-500/20 text-green-600': teacher.status === '审核通过',
                      'bg-red-500/20 text-red-600': teacher.status === '审核不通过',
                      'bg-yellow-500/20 text-yellow-600': teacher.status === '未审核'
                    }">
                      {{ teacher.status }}
                    </span>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex space-x-2 mt-4 pt-4 border-t" :class="isDark ? 'border-gray-700' : 'border-gray-200'">
                  <button
                    @click.stop="editTeacher(teacher)"
                    :class="[
                      'flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-300',
                      isDark
                        ? 'bg-blue-500/20 text-blue-400 hover:bg-blue-500 hover:text-white'
                        : 'bg-blue-500/10 text-blue-600 hover:bg-blue-500 hover:text-white'
                    ]"
                  >
                    编辑
                  </button>
                  <button
                    @click.stop="deleteTeacher(teacher.id)"
                    :class="[
                      'flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-300',
                      isDark
                        ? 'bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white'
                        : 'bg-red-500/10 text-red-600 hover:bg-red-500 hover:text-white'
                    ]"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredTeachers.length === 0" class="text-center py-12">
              <div :class="isDark ? 'text-gray-500' : 'text-gray-400'" class="text-lg">暂无教师数据</div>
            </div>
          </div>

          <!-- 学生管理 -->
          <div v-else-if="activeTab === 'students'" class="space-y-6">
            <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
              <div
                v-for="student in filteredStudents"
                :key="student.id"
                :class="[
                  'group relative p-6 rounded-2xl border backdrop-blur-sm transition-all duration-500 hover:scale-105 cursor-pointer',
                  isDark
                    ? 'bg-gray-800/50 border-gray-700 hover:border-green-500/50 hover:shadow-2xl hover:shadow-green-500/10'
                    : 'bg-white/50 border-gray-200 hover:border-green-400/50 hover:shadow-2xl hover:shadow-green-500/10'
                ]"
                @click="editStudent(student)"
              >
                <!-- 状态指示器 -->
                <div class="absolute top-4 right-4">
                  <div :class="{
                    'w-3 h-3 rounded-full': true,
                    'bg-green-500': student.status === '审核通过',
                    'bg-red-500': student.status === '审核不通过',
                    'bg-yellow-500': student.status === '未审核'
                  }"></div>
                </div>

                <!-- 用户头像和基本信息 -->
                <div class="flex items-center space-x-4 mb-4">
                  <div :class="[
                    'w-14 h-14 rounded-2xl flex items-center justify-center text-white font-bold text-xl shadow-lg',
                    isDark ? 'bg-gradient-to-br from-green-500 to-green-600' : 'bg-gradient-to-br from-green-500 to-green-600'
                  ]">
                    {{ student.account?.charAt(0) || 'S' }}
                  </div>
                  <div class="flex-1">
                    <h3 :class="isDark ? 'text-white' : 'text-gray-900'" class="font-bold text-lg truncate">
                      {{ student.account }}
                    </h3>
                    <p :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">
                      ID: {{ student.user_id }}
                    </p>
                  </div>
                </div>

                <!-- 详细信息 -->
                <div class="space-y-3">
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">班级</span>
                    <span :class="isDark ? 'text-white' : 'text-gray-900'" class="font-medium text-sm">{{ student.class }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">专业</span>
                    <span :class="isDark ? 'text-white' : 'text-gray-900'" class="font-medium text-sm">{{ student.professional }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">邮箱</span>
                    <span :class="isDark ? 'text-green-400' : 'text-green-600'" class="font-medium text-sm truncate ml-2">{{ student.email }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-sm">状态</span>
                    <span :class="{
                      'px-3 py-1 rounded-full text-xs font-semibold': true,
                      'bg-green-500/20 text-green-600': student.status === '审核通过',
                      'bg-red-500/20 text-red-600': student.status === '审核不通过',
                      'bg-yellow-500/20 text-yellow-600': student.status === '未审核'
                    }">
                      {{ student.status }}
                    </span>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex space-x-2 mt-4 pt-4 border-t" :class="isDark ? 'border-gray-700' : 'border-gray-200'">
                  <button
                    @click.stop="editStudent(student)"
                    :class="[
                      'flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-300',
                      isDark
                        ? 'bg-green-500/20 text-green-400 hover:bg-green-500 hover:text-white'
                        : 'bg-green-500/10 text-green-600 hover:bg-green-500 hover:text-white'
                    ]"
                  >
                    编辑
                  </button>
                  <button
                    @click.stop="deleteStudent(student.id)"
                    :class="[
                      'flex-1 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-300',
                      isDark
                        ? 'bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white'
                        : 'bg-red-500/10 text-red-600 hover:bg-red-500 hover:text-white'
                    ]"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredStudents.length === 0" class="text-center py-12">
              <div :class="isDark ? 'text-gray-500' : 'text-gray-400'" class="text-lg">暂无学生数据</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <div v-if="showAddDialog" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div
        :class="isDark ? 'bg-gray-900 border-gray-700' : 'bg-white border-gray-200'"
        class="rounded-3xl border backdrop-blur-xl shadow-2xl w-full max-w-md transform transition-all duration-300 scale-100"
      >
        <div class="p-8">
          <div class="flex items-center justify-between mb-6">
            <h3 :class="isDark ? 'text-white' : 'text-gray-900'" class="text-2xl font-bold">
              {{ editingItem ? '编辑用户' : '添加用户' }}
            </h3>
            <button
              @click="closeDialog"
              :class="isDark ? 'text-gray-400 hover:text-white' : 'text-gray-400 hover:text-gray-600'"
              class="transition-colors"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>

          <form @submit.prevent="saveItem" class="space-y-6">
            <div>
              <label :class="isDark ? 'text-gray-300' : 'text-gray-700'" class="block text-sm font-semibold mb-3">账户</label>
              <input
                :readonly="!isEditSelf"
                v-model="formData.account"
                type="text"
                :class="[
                  'w-full px-4 py-3 rounded-xl border transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:ring-blue-500 focus:border-transparent',
                  !isEditSelf ? 'opacity-70 cursor-not-allowed' : ''
                ]"
                required
              >
            </div>

            <div>
              <label :class="isDark ? 'text-gray-300' : 'text-gray-700'" class="block text-sm font-semibold mb-3">姓名</label>
              <input
                :readonly="!isEditSelf"
                v-model="formData.user_id"
                type="text"
                :class="[
                  'w-full px-4 py-3 rounded-xl border transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:ring-blue-500 focus:border-transparent',
                  !isEditSelf ? 'opacity-70 cursor-not-allowed' : ''
                ]"
                required
              >
            </div>

            <div>
              <label :class="isDark ? 'text-gray-300' : 'text-gray-700'" class="block text-sm font-semibold mb-3">专业</label>
              <input
                v-model="formData.professional"
                type="text"
                :class="[
                  'w-full px-4 py-3 rounded-xl border transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:ring-blue-500 focus:border-transparent'
                ]"
                required
                placeholder="请输入专业"
              >
            </div>

            <div>
              <label :class="isDark ? 'text-gray-300' : 'text-gray-700'" class="block text-sm font-semibold mb-3">状态</label>
              <select
                v-model="formData.status"
                :class="[
                  'w-full px-4 py-3 rounded-xl border transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800 border-gray-600 text-white focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white border-gray-300 text-gray-900 focus:ring-blue-500 focus:border-transparent'
                ]"
                required
              >
                <option v-for="status in (activeTab === 'teachers') ? teacherStatuses : studentStatuses" :key="status" :value="status">
                  {{ status }}
                </option>
              </select>
            </div>

            <div>
              <label :class="isDark ? 'text-gray-300' : 'text-gray-700'" class="block text-sm font-semibold mb-3">邮箱</label>
              <input
                :readonly="!isEditSelf"
                v-model="formData.email"
                type="email"
                :class="[
                  'w-full px-4 py-3 rounded-xl border transition-all duration-300 focus:outline-none focus:ring-2',
                  isDark
                    ? 'bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:ring-blue-500 focus:border-transparent'
                    : 'bg-white border-gray-300 text-gray-900 placeholder-gray-500 focus:ring-blue-500 focus:border-transparent',
                  !isEditSelf ? 'opacity-70 cursor-not-allowed' : ''
                ]"
              >
              <p :class="isDark ? 'text-gray-400' : 'text-gray-500'" class="text-xs mt-2">
                只能编辑自己的邮箱信息
              </p>
            </div>

            <div class="flex space-x-4 pt-4">
              <button
                type="button"
                @click="closeDialog"
                :class="[
                  'flex-1 py-3 px-6 rounded-xl font-semibold transition-all duration-300',
                  isDark
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600 hover:text-white'
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                ]"
              >
                取消
              </button>
              <button
                type="submit"
                :class="[
                  'flex-1 py-3 px-6 rounded-xl font-semibold text-white transition-all duration-300',
                  activeTab === 'teachers'
                    ? 'bg-blue-500 hover:bg-blue-600 shadow-lg shadow-blue-500/25'
                    : 'bg-green-500 hover:bg-green-600 shadow-lg shadow-green-500/25'
                ]"
              >
                保存更改
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 自定义滚动条 */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.5) transparent;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.7);
}

.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(75, 85, 99, 0.5);
}

.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(75, 85, 99, 0.7);
}

/* 卡片悬停动画 */
@keyframes cardHover {
  0% {
    transform: scale(1);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
  100% {
    transform: scale(1.02);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  }
}

.group:hover {
  animation: cardHover 0.3s ease-out forwards;
}

/* 对话框入场动画 */
@keyframes dialogEnter {
  0% {
    opacity: 0;
    transform: scale(0.9) translateY(-10px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.fixed.inset-0.bg-black\/50 .rounded-3xl {
  animation: dialogEnter 0.3s ease-out;
}
</style>
