<script setup>
import { ref, onMounted, computed } from 'vue'
import { get, post } from "@/net/index.js";
import { formatDate } from "@/time/Data.js";
import { message, Modal } from "ant-design-vue";
import {isDark} from "@/stores/theme.js";

const [messageApi, contextHolder] = message.useMessage();
const courses = ref([])
const allCourses = ref([]) // 管理员可见的所有课程
const users = ref([]) // 用户列表
const loading = ref(false)
const activeTab = ref('courses') // 管理员选项卡
const searchQuery = ref('') // 搜索查询
// 新增：课程成员对话框状态
const showMembersDialog = ref(false)
const showAddMembersDialog = ref(false)
const currentCourse = ref(null)
const courseMembers = ref([])
const availableUsers = ref([])
const selectedUsers = ref([])

// 计算属性：过滤后的可用用户
const filteredAvailableUsers = computed(() => {
  if (!searchQuery.value.trim()) {
    return availableUsers.value
  }
  const query = searchQuery.value.toLowerCase().trim()
  return availableUsers.value.filter(user =>
    user.account.toLowerCase().includes(query) ||
    user.id.toString().includes(query) ||
    (user.name && user.name.toLowerCase().includes(query))
  )
})
// 获取个人信息
const user = ref({})
const isAdmin = computed(() => user.value.role === '管理员')

// 获取个人信息
const fetchUserInfo = () => {
  return new Promise((resolve, reject) => {
    get('api/exam/current', {}, (msg, data) => {
      user.value = data
      console.log('用户信息:', user.value)
      resolve(data)
    }, (error) => {
      reject(error)
    })
  })
}
//获取所用户
const AllUser=()=>{
  return new Promise((resolve, reject) => {
    get('api/exam/AllUser', {}, (msg, data) => {
      users.value = data
      console.log('获取到的用户数据:', users.value)
      console.log('用户数据长度:', data ? data.length : 0)
      resolve(data || [])
    }, (error) => {
      console.error('获取用户数据失败:', error)
      reject(error)
    })
  })
}

// 获取课程列表
const fetchCourses = async () => {
  try {
    await fetchUserInfo()
    console.log('当前用户信息:', user.value)

    loading.value = true
    if (isAdmin.value) {
      // 管理员获取所有课程
      const coursesData = await new Promise((resolve, reject) => {
        get('api/exam/AllCourse', {}, (msg, data) => {
          console.log('获取到的课程数据:', data)
          console.log('课程数据长度:', data ? data.length : 0)
          resolve(data || [])
        }, (error) => {
          console.error('获取课程数据失败:', error)
          reject(error)
        })
      })
      allCourses.value = coursesData
      // 默认显示全部课程
      courses.value = coursesData
      console.log('设置后的课程数据:', courses.value)
    } else if( user.value.role === '教师') {
      // 教师获取自己的课程
      const coursesData = await new Promise((resolve, reject) => {
        get('api/exam/SelectCourseByTeacherId', {
          teacher_id: user.value.id
        }, (msg, data) => {
          console.log('获取到的教师课程数据:', data)
          console.log('教师课程数据长度:', data ? data.length : 0)
          resolve(data || [])
        }, (error) => {
          console.error('获取教师课程数据失败:', error)
          reject(error)
        })
      })
      courses.value = coursesData
    }else{
      // 学生获取自己的课程
      const coursesData = await new Promise((resolve, reject) => {
        get('api/exam/SelectUserCourse', {
        }, (msg, data) => {
          console.log('获取到的学生课程数据:', data)
          console.log('学生课程数据长度:', data ? data.length : 0)
          resolve(data || [])
        }, (error) => {
          console.error('获取学生课程数据失败:', error)
          reject(error)
        })
      })
      courses.value = coursesData
    }
  } catch (error) {
    console.error('操作失败:', error)
    messageApi.error(error.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// 切换到课程管理标签页
const switchToCoursesTab = () => {
  fetchCourses()
  activeTab.value = 'courses'
  // 确保课程数据已加载
  if (allCourses.value.length === 0) {
    fetchCourses()
  } else {
    // 如果已有数据，重新应用筛选
    filterCourses('全部课程')
  }
}

// 切换到用户管理标签页
const switchToUsersTab = () => {
  activeTab.value = 'users'
  // 确保用户数据已加载
  if (users.value.length === 0) {
    AllUser()
  }
}

// 新增：打开课程成员对话框
const openMembersDialog = async (course) => {
  currentCourse.value = course
  if(user.value.role==='学生') course.id=course.course_id
  try {
    // 获取课程成员
    const members = await new Promise((resolve, reject) => {
      get('api/exam/SelectCourseUser', { course_id: course.id }, (msg, data) => {
        // 确保返回的是用户列表，而不是课程列表
        resolve(data || [])
      }, (error) => {
        reject(error)
      })
    })
    courseMembers.value = members
    console.log('课程成员:', courseMembers.value)
    showMembersDialog.value = true
  } catch (error) {
    messageApi.error('获取课程成员失败')
  }
}

// 新增：打开添加成员对话框
const openAddMembersDialog = async (course) => {
  currentCourse.value = course
  if(user.value.role==='学生') course.id=course.course_id
  try {
    // 先获取当前课程的成员列表，确保数据最新
    const members = await new Promise((resolve, reject) => {
      get('api/exam/SelectCourseUser', { course_id: course.id }, (msg, data) => {
        resolve(data || [])
      }, (error) => {
        reject(error)
      })
    })
    // 更新courseMembers数组
    courseMembers.value = members

    // 获取所有用户
    const users = await new Promise((resolve, reject) => {
      get('api/exam/AllUser', {}, (msg, data) => {
        // 过滤掉已经是课程成员的用户
        const courseMemberIds = new Set(courseMembers.value.map(m => m.user_id))
        const filteredUsers = data.filter(user =>
          !courseMemberIds.has(user.id)&&user.status==="审核通过" // 允许所有角色的用户加入，不再限制为学生或教师
        )
        resolve(filteredUsers || [])
      }, (error) => {
        reject(error)
      })
    })

    availableUsers.value = users
    selectedUsers.value = []
    showAddMembersDialog.value = true
  } catch (error) {
    messageApi.error('获取可添加用户失败')
  }
}

// 新增：添加用户到课程
const addUsersToCourse = async () => {
  if (selectedUsers.value.length === 0) {
    messageApi.error('请选择要添加的用户')
    return
  }

  try {
    // 为每个选中的用户单独调用API
    const promises = selectedUsers.value.map(userId => {
      // 找到选中用户的完整信息以获取其角色
      const selectedUser = availableUsers.value.find(u => u.id === userId);
      return new Promise((resolve, reject) => {
        post('api/exam/InsertCourseUser', {
          course_id: currentCourse.value.id,
          user_id: userId,
          role: selectedUser ? selectedUser.role : '学生', // 使用用户实际角色
          account:selectedUser.account
        }, (msg) => {
          resolve(msg)
        }, (errorMsg) => {
          reject(errorMsg)
        })
      })
    })

    // 等待所有请求完成
    await Promise.all(promises)

    messageApi.success('用户添加成功')
    showAddMembersDialog.value = false
    // 刷新成员列表
    openMembersDialog(currentCourse.value)
  } catch (error) {
    messageApi.error('添加用户失败: ' + error)
  }
}

// 新增：从课程移除用户
const removeUserFromCourse = (userId) => {
  Modal.confirm({
    title: '确认移除用户',
    content: '确定要将此用户从课程中移除吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      // 确保currentCourse和userId都有效
      if (!currentCourse.value || !currentCourse.value.id || !userId) {
        messageApi.error('参数错误，无法移除用户')
        return
      }

      console.log('移除用户参数:', { userId, courseId: currentCourse.value.id })

      post('api/exam/DeleteCourseUser', {
        user_id: userId,
        course_id: currentCourse.value.id
      }, (msg) => {
        messageApi.success(msg)
        // 刷新成员列表
        openMembersDialog(currentCourse.value)
      }, (errorMsg) => {
        messageApi.error(errorMsg || '移除用户失败')
        console.error('移除用户失败:', errorMsg)
      })
    }
  })
}

// 对话框状态
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showDeleteConfirm = ref(false)
const showUserDialog = ref(false)
const showAuditDialog = ref(false)

// 表单数据
const courseForm = ref({
  id:'',
  name:'',
  score:0,
  teacher_name:'',
  teacher_id:'',
  status: '未审核'
})

const userForm = ref({
  id: '',
  account: '',
  name: '',
  role: 'teacher',
  email: '',
  phone: ''
})

const auditForm = ref({
  id: '',
  score:'',
  name:'',
  status: '审核通过',
})

// 打开创建课程对话框
const openCreateDialog = async () => {
  try {
    // 确保获取最新的用户信息
    await fetchUserInfo()

    courseForm.value = {
      id: '',
      name: '',
      score: 0,
      teacher_id: user.value.id,
      teacher_name: user.value.name || user.value.account || '未知教师',
      status: '未审核'
    }
    showCreateDialog.value = true
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 即使获取失败，也使用默认值
    courseForm.value = {
      id: '',
      name: '',
      score: 0,
      teacher_id: '',
      teacher_name: '未知教师',
      status: '未审核'
    }
    showCreateDialog.value = true
  }
}

// 打开编辑课程对话框
const openEditDialog = async (course) => {
  // 确保获取最新的用户信息
  try {
    await fetchUserInfo()
    // 如果课程没有teacher_name或为空，使用当前用户信息
    const teacherName = course.teacher_name && course.teacher_name.trim()
      ? course.teacher_name
      : (user.value.name || user.value.account || '未知教师')

    courseForm.value = {
      ...course,
      teacher_name: teacherName,
      teacher_id: user.value.id // 确保teacher_id是当前用户ID
    }
    showEditDialog.value = true
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 即使获取失败，也使用course原有数据
    courseForm.value = { ...course }
    showEditDialog.value = true
  }
}

// 打开用户编辑对话框
const openUserDialog = (userData = null) => {
  if (userData) {
    userForm.value = { ...userData }
  } else {
    userForm.value = {
      id: '',
      account: '',
      name: '',
      role: '教师',
      email: '',
      phone: ''
    }
  }
  showUserDialog.value = true
}

// 打开审核对话框
const openAuditDialog = (course) => {
  auditForm.value = {
    id: course.id,
    score:course.score,
    name:course.name,
    status: course.status,
  }
  showAuditDialog.value = true
}

// 保存课程
const saveCourse = (isCreate) => {
  // 表单验证
  if (!courseForm.value.name || !courseForm.value.name.trim()) {
    messageApi.error('请输入课程名称')
    return
  }
  if (!courseForm.value.score || courseForm.value.score <= 0) {
    messageApi.error('请输入有效的学分')
    return
  }

  const url = isCreate ? 'api/exam/InsertCourse' : 'api/exam/UpdateCourse'
  post(url, courseForm.value, (msg) => {
    messageApi.success(msg)
    fetchCourses()
    if (isCreate) {
      showCreateDialog.value = false
    } else {
      showEditDialog.value = false
    }
  }, (errorMsg) => {
    messageApi.error(errorMsg || '操作失败')
  })
}

// 保存用户
const saveUser = (isCreate) => {
  // 表单验证
  if (!userForm.value.account || !userForm.value.account.trim()) {
    messageApi.error('请输入账号')
    return
  }
  if (!userForm.value.name || !userForm.value.name.trim()) {
    messageApi.error('请输入姓名')
    return
  }
  if (isCreate && (!userForm.value.password || !userForm.value.password.trim())) {
    messageApi.error('请输入密码')
    return
  }

  const url = isCreate ? 'api/exam/InsertUser' : 'api/exam/UpdateUser'
  post(url, userForm.value, (msg) => {
    messageApi.success(msg)
    // 刷新用户数据
    AllUser()
    showUserDialog.value = false
  }, (errorMsg) => {
    messageApi.error(errorMsg || '操作失败')
  })
}

// 审核课程
const auditCourse = () => {
  if (!auditForm.value.status) {
    messageApi.error('请选择审核结果')
    return
  }

  post('api/exam/UpdateCourse', auditForm.value, (msg) => {
    messageApi.success(msg)
    fetchCourses()
    showAuditDialog.value = false
  }, (errorMsg) => {
    messageApi.error(errorMsg || '审核失败')
  })
}

// 删除课程
const deleteCourse = () => {
  post('api/exam/DeleteCourse', { id: courseForm.value.id }, (msg) => {
    messageApi.success(msg)
    fetchCourses()
    showDeleteConfirm.value = false
  }, (errorMsg) => {
    messageApi.error(errorMsg || '删除失败')
  })
}

// 删除用户
const deleteUser = (userId) => {
  Modal.confirm({
    title: '确认删除用户',
    content: '此操作将永久删除该用户，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      post('api/exam/DeleteUser', { id: userId }, (msg) => {
        messageApi.success(msg)
        // 刷新用户数据
        AllUser()
      }, (errorMsg) => {
        messageApi.error(errorMsg || '删除失败')
      })
    }
  })
}

// 切换课程状态筛选
const filterCourses = (status) => {
  console.log('筛选状态:', status, '全部课程数量:', allCourses.value.length)
  if (status === 'all') {
    courses.value = [...allCourses.value]
  } else {
    courses.value = allCourses.value.filter(c => c.status === status)
  }
  console.log('筛选后课程数量:', courses.value.length)
}

// 防抖函数
let searchTimeout = null

// 搜索课程
const searchCourses = () => {
  // 清除之前的定时器
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }

  // 设置新的定时器，300ms后执行搜索
  searchTimeout = setTimeout(() => {
    if (!searchQuery.value.trim()) {
      // 如果搜索框为空，显示当前筛选结果
      filterCourses('all')
      return
    }

    const query = searchQuery.value.toLowerCase().trim()
    courses.value = allCourses.value.filter(course =>
      course.name?.toLowerCase().includes(query) ||
      course.teacher_name?.toLowerCase().includes(query) ||
      course.id?.toString().includes(query)
    )
  }, 300)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
  filterCourses('all')
}

// 初始化加载
onMounted(async () => {
  AllUser()
  await fetchCourses()

  // 如果是管理员，也预加载用户数据
  if (isAdmin.value) {
    await AllUser()
  }

  // 添加键盘事件监听
  const handleKeydown = (event) => {
    if (event.key === 'Escape' && searchQuery.value) {
      clearSearch()
    }
  }

  document.addEventListener('keydown', handleKeydown)

  // 组件卸载时移除事件监听
  return () => {
    document.removeEventListener('keydown', handleKeydown)
  }
})
</script>

<template>
  <contextHolder/>
  <div :class="isDark?'bg-black':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="min-h-screen">
    <!-- 顶部导航 -->
    <header class="flex items-center justify-between border-b p-6 shadow-md rounded-xl hover:shadow-lg duration-200" :class="isDark?'border-white/20':'border-gray-200'">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
          <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
        </svg>
        <h1 :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">课程管理系统 - {{ isAdmin ? '管理员' : '教师' }}面板</h1>
      </div>
      <div class="flex items-center space-x-4">
        <div class="flex items-center space-x-2 px-3 py-1 rounded-full" :class="isDark?'bg-white/5':'bg-gray-100'">
          <div class="w-2 h-2 rounded-full bg-green-500"></div>
          <span :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ user.name || user.account }}</span>
          <span :class="isDark?'text-white':'text-gray-700'" class="text-xs px-2 py-0.5 rounded-full">
              {{ isAdmin ? '管理员' : '教师' }}
            </span>
        </div>
        <button
          v-if="!isAdmin"
          @click="openCreateDialog"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors flex items-center"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          新增课程
        </button>
        <button
          v-if="isAdmin"
          @click="openUserDialog"
          class="px-4 py-2 bg-green-600 hover:bg-green-700 rounded-lg text-white transition-colors flex items-center"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6z" />
          </svg>
          新增用户
        </button>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="flex-1 p-8 overflow-auto">
      <div class="max-w-7xl mx-auto">
        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div class="backdrop-blur-md rounded-2xl p-6 border shadow-md hover:shadow-lg duration-300" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
            <p :class="isDark?'text-white':'text-gray-700'" class="text-sm">总课程数</p>
            <p :class="isDark?'text-white':'text-gray-900'" class="text-3xl font-bold mt-2">{{ isAdmin ? allCourses.length : courses.length }}</p>
          </div>
          <div class="backdrop-blur-md rounded-2xl p-6 border shadow-md hover:shadow-lg duration-300" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
            <p :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ isAdmin ? '待审核课程' : '本学期课程' }}</p>
            <p :class="isDark?'text-white':'text-gray-900'" class="text-3xl font-bold mt-2">
              {{ isAdmin ? allCourses.filter(c => c.status === '未审核').length : courses.filter(c => c.semester === '2025-1').length }}
            </p>
          </div>
          <div class="backdrop-blur-md rounded-2xl p-6 border shadow-md hover:shadow-lg duration-300" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
            <p :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ isAdmin ? '总用户数' : '平均学分' }}</p>
            <p :class="isDark?'text-white':'text-gray-900'" class="text-3xl font-bold mt-2">
              {{ isAdmin ? users.length : (courses.length > 0 ? (courses.reduce((sum, c) => sum + c.score, 0) / courses.length).toFixed(1) : 0 )}}
            </p>
          </div>
          <div class="backdrop-blur-md rounded-2xl p-6 border shadow-md hover:shadow-lg duration-300" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
            <p :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ isAdmin ? '活跃教师' : '总容量' }}</p>
            <p :class="isDark?'text-white':'text-gray-900'" class="text-3xl font-bold mt-2">
              {{ isAdmin ? users.filter(u => u.role === 'teacher').length : courses.reduce((sum, c) => sum + c.capacity, 0) }}
            </p>
          </div>
        </div>

        <!-- 管理员选项卡 -->
        <div class="flex space-x-1 mb-6">
          <button
            @click="switchToCoursesTab"
            :class="[
              'px-4 py-2 text-sm font-medium',
              activeTab === 'courses'
                ? 'text-indigo-400 border-b-2 border-indigo-400'
                : isDark
                    ? 'text-white/50 hover:text-white'
                    : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            课程管理
          </button>
          <button
            @click="switchToUsersTab"
            :class="[
              'px-4 py-2 text-sm font-medium',
              activeTab === 'users'
                ? 'text-indigo-400 border-b-2 border-indigo-400'
                : isDark
                    ? 'text-white/50 hover:text-white'
                    : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            用户管理
          </button>
          <button
            @click="activeTab = 'stats'"
            :class="[
              'px-4 py-2 text-sm font-medium',
              activeTab === 'stats'
                ? 'text-indigo-400 border-b-2 border-indigo-400'
                : isDark
                    ? 'text-white/50 hover:text-white'
                    : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            统计
          </button>
        </div>

        <!-- 课程列表 -->
        <div v-if="activeTab === 'courses' || !isAdmin" class="backdrop-blur-md rounded-2xl p-6 border" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center space-x-4">
              <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">{{ isAdmin ? '课程审核' : '我的课程' }}</h3>
              <span :class="isDark?'text-white':'text-gray-600'" v-if="searchQuery" class="text-sm">
                (搜索 "{{ searchQuery }}" 找到 {{ courses.length }} 条结果)
              </span>
            </div>
            <div class="flex items-center space-x-4">
              <div class="relative">
                <input
                  v-model="searchQuery"
                  @input="searchCourses"
                  type="text"
                  placeholder="搜索课程... (按ESC清除)"
                  :class="[
                    'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent pl-10 pr-10',
                    isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                  ]"
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
                v-if="isAdmin"
                @change="filterCourses($event.target.value)"
                :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="未审核">待审核</option>
                <option value="审核通过">已通过</option>
                <option value="审核未通过">未通过</option>
                <option value="all" selected>全部课程</option>
              </select>
              <select
                v-else
                :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option>全部学期</option>
                <option>2025-1</option>
                <option>2024-2</option>
              </select>
            </div>
          </div>

          <div class="overflow-x-auto">
            <div v-if="loading" class="grid grid-cols-1 gap-6">
              <div class="text-center py-12" :class="isDark?'text-white/50':'text-gray-500'">
                <div class="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-indigo-500 mx-auto mb-4"></div>
                <span>正在加载课程数据...</span>
              </div>
            </div>
            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              <div v-for="course in courses" :key="course.id"
                   class="rounded-xl p-5 border transition-all hover:scale-[1.02] group"
                   :class="isDark ? 'bg-white/5 border-white/10 hover:border-indigo-400/30' : 'bg-white border-gray-200 hover:border-indigo-300 shadow-sm'">
                <!-- 课程头部 -->
                <div class="flex justify-between items-start mb-4">
                  <div>
                    <h4 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-semibold mb-1 truncate" :title="course.name">{{ course.name }}</h4>
                    <p v-if="user.role==='学生'" :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">课程ID: {{ course.course_id }}</p>
                    <p v-else :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">课程ID: {{ course.id }}</p>
                  </div>
                  <span :class="{
                  'px-2 py-1 rounded-full text-xs font-medium': true,
                  'bg-green-500/20 text-green-400': course.status === '审核通过',
                  'bg-yellow-500/20 text-yellow-400': course.status === '未审核',
                  'bg-red-500/20 text-red-400': course.status === '审核未通过'
                }">
                  {{ course.status }}
                </span>
                </div>

                <!-- 课程信息 -->
                <div class="space-y-3 mb-5">
                  <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                      <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                    </svg>
                    {{ course.teacher_name || course.teacherName || '未知教师' }}
                  </div>
                  <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 01-1.898-.316l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
                    </svg>
                    学分: {{ course.score || 0 }}
                  </div>
                  <div class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 0 00-1-1H6zm0 2h8v1H6V4zm5 5a1 1 0 00-1 1v3a1 1 0 102 0v-3a1 1 0 00-1-1zm-4 0a1 1 0 00-1 1v3a1 1 0 102 0v-3a1 1 0 00-1-1zm8 0a1 1 0 00-1 1v3a1 1 0 102 0v-3a1 1 0 00-1-1z" clip-rule="evenodd" />
                    </svg>
                    学期: {{ course.semester || '本学期' }}
                  </div>
                  <div v-if="isAdmin" class="flex items-center text-sm" :class="isDark?'text-white/60':'text-gray-500'">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd" />
                    </svg>
                    提交: {{ formatDate(course.create_time) || '————' }}
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex justify-between pt-4 border-t" :class="isDark?'border-white/10':'border-gray-200'">
                  <div class="flex space-x-2">
                    <!-- 查看成员按钮 -->
                    <button
                      @click="openMembersDialog(course)"
                      class="p-2 rounded-full transition-colors group relative"
                      :class="isDark ? 'bg-blue-500/20 hover:bg-blue-500/30 text-blue-400' : 'bg-blue-100 hover:bg-blue-200 text-blue-600'"
                      title="查看课程成员"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z" />
                      </svg>
                      <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                            :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                      查看成员
                    </span>
                    </button>

                    <!-- 拉取用户按钮 -->
                    <button
                      v-if="user.role!=='学生'"
                      @click="openAddMembersDialog(course)"
                      class="p-2 rounded-full transition-colors group relative"
                      :class="isDark ? 'bg-green-500/20 hover:bg-green-500/30 text-green-400' : 'bg-green-100 hover:bg-green-200 text-green-600'"
                      title="拉取用户到课程"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                      </svg>
                      <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                            :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                      添加成员
                    </span>
                    </button>
                  </div>

                  <div class="flex space-x-2">
                    <!-- 审核/编辑按钮 -->
                    <button
                      v-if="isAdmin && course.status === '未审核'"
                      @click="openAuditDialog(course)"
                      class="p-2 rounded-full transition-colors group relative"
                      :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                      title="审核"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2h-1V9z" clip-rule="evenodd" />
                      </svg>
                      <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                            :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                      审核
                    </span>
                    </button>
                    <button
                      v-if="!isAdmin || course.status !== '未审核'"
                      v-show="user.role!=='学生'"
                      @click="openEditDialog(course)"
                      class="p-2 rounded-full transition-colors group relative"
                      :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                      :title="isAdmin ? '查看' : '编辑'"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-400" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                      </svg>
                      <span class="absolute -top-8 left-1/2 transform -translate-x-1/2 text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
                            :class="isDark ? 'bg-gray-800 text-white' : 'bg-gray-700 text-white'">
                      {{ isAdmin ? '查看' : '编辑' }}
                    </span>
                    </button>

                    <!-- 删除按钮 -->
                    <button
                      v-if="isAdmin"
                      @click="() => { courseForm.id = course.id; showDeleteConfirm = true }"
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
            <div v-if="courses.length === 0 && !loading" class="text-center py-12">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto mb-4" :class="isDark?'text-white/30':'text-gray-300'" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
              <p :class="isDark?'text-white/50':'text-gray-500'">暂无课程数据</p>
              <button v-if="!isAdmin" @click="openCreateDialog" class="mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors">
                创建第一门课程
              </button>
            </div>

          </div>

          <div class="flex items-center justify-between mt-6 pt-6 border-t" :class="isDark?'border-white/10':'border-gray-200'">
            <div class="text-sm" :class="isDark?'text-white/50':'text-gray-500'">
              显示 {{ courses.length }} 条中的 1-{{ courses.length }} 条
            </div>
            <div class="flex space-x-2">
              <button class="px-3 py-1 rounded-lg border transition-colors" :class="isDark?'bg-white/5 border-white/10 text-white/50 hover:bg-white/10':'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'">
                上一页
              </button>
              <button class="px-3 py-1 rounded-lg bg-indigo-600 text-white">
                1
              </button>
              <button class="px-3 py-1 rounded-lg border transition-colors" :class="isDark?'bg-white/5 border-white/10 text-white/50 hover:bg-white/10':'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'">
                下一页
              </button>
            </div>
          </div>

          <!-- 分页控件 -->
        </div>

        <!-- 用户管理 -->
        <div v-if="isAdmin && activeTab === 'users'" class="backdrop-blur-md rounded-2xl p-6 border" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
          <div class="flex items-center justify-between mb-6">
            <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">用户管理</h3>
            <div class="flex items-center space-x-4">
              <div class="relative">
                <input
                  type="text"
                  placeholder="搜索用户..."
                  :class="[
                    'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent pl-10',
                    isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                  ]"
                >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-3 top-2.5" :class="isDark?'text-white/50':'text-gray-400'" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
                </svg>
              </div>
              <select :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]">
                <option>全部角色</option>
                <option>管理员</option>
                <option>教师</option>
                <option>学生</option>
              </select>
            </div>
          </div>

          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
              <tr class="text-left border-b" :class="isDark?'text-white/50 border-white/10':'text-gray-500 border-gray-200'">
                <th class="pb-3 px-4">用户ID</th>
                <th class="pb-3 px-4">账号</th>
                <th class="pb-3 px-4">姓名</th>
                <th class="pb-3 px-4">角色</th>
                <th class="pb-3 px-4">邮箱</th>
                <th class="pb-3 px-4">电话</th>
                <th class="pb-3 px-4">注册时间</th>
                <th class="pb-3 px-4 text-right">操作</th>
              </tr>
              </thead>
              <tbody v-if="loading">
              <tr>
                <td colspan="8" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                  <div class="flex flex-col items-center space-y-2">
                    <div class="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-indigo-500"></div>
                    <span class="text-sm">正在加载用户数据...</span>
                  </div>
                </td>
              </tr>
              </tbody>
              <tbody v-else>
              <tr v-for="user in users" :key="user.id" class="border-b transition-colors" :class="isDark?'border-white/10 hover:bg-white/5':'border-gray-200 hover:bg-gray-50'">
                <td class="py-4 px-4 font-mono" :class="isDark?'text-white':'text-gray-900'">{{ user.id }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ user.account }}</td>
                <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ user.name }}</td>
                <td class="py-4 px-4">
                                      <span :class="{
                                      'px-2 py-1 rounded-full text-xs font-medium': true,
                                      'bg-purple-500/20 text-purple-400': user.role === '管理员',
                                      'bg-blue-500/20 text-blue-400': user.role === '教师',
                                      'bg-green-500/20 text-green-400': user.role === '学生'
                                    }">
                                      {{
                                          user.role === '管理员' ? '管理员' :
                                            user.role === '教师' ? '教师' : '学生'
                                        }}
                                    </span>
                </td>
                <td class="py-4 px-4 text-sm" :class="isDark?'text-white':'text-gray-900'">{{ user.email || '-' }}</td>
                <td class="py-4 px-4 text-sm" :class="isDark?'text-white':'text-gray-900'">{{ user.phone || '-' }}</td>
                <td class="py-4 px-4 text-sm" :class="isDark?'text-white':'text-gray-900'">{{ formatDate(user.create_time) }}</td>
                <td class="py-4 px-4 text-right">
                  <div class="flex justify-end space-x-2">
                    <button
                      @click="openUserDialog(user)"
                      class="p-2 rounded-full transition-colors"
                      :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                      title="编辑"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-indigo-400" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                      </svg>
                    </button>
                    <button
                      @click="deleteUser(user.id)"
                      class="p-2 rounded-full transition-colors"
                      :class="isDark ? 'bg-white/5 hover:bg-white/10' : 'bg-gray-100 hover:bg-gray-200'"
                      title="删除"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="users.length === 0">
                <td colspan="8" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                  暂无数据
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between mt-6 pt-6 border-t" :class="isDark?'border-white/10':'border-gray-200'">
            <div class="text-sm" :class="isDark?'text-white/50':'text-gray-500'">
              显示 {{ users.length }} 条中的 1-{{ users.length }} 条
            </div>
            <div class="flex space-x-2">
              <button class="px-3 py-1 rounded-lg border transition-colors" :class="isDark?'bg-white/5 border-white/10 text-white/50 hover:bg-white/10':'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'">
                上一页
              </button>
              <button class="px-3 py-1 rounded-lg bg-indigo-600 text-white">
                1
              </button>
              <button class="px-3 py-1 rounded-lg border transition-colors" :class="isDark?'bg-white/5 border-white/10 text-white/50 hover:bg-white/10':'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'">
                下一页
              </button>
            </div>
          </div>
        </div>

        <!-- 数据统计 -->
        <div v-if="isAdmin && activeTab === 'stats'" class="backdrop-blur-md rounded-2xl p-6 border" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-gray-50 border-gray-200'">
          <div class="flex items-center justify-between mb-6">
            <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">数据统计</h3>
            <div class="flex items-center space-x-4">
              <select :class="[
                  'px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]">
                <option>本学期</option>
                <option>2025-1</option>
                <option>2024-2</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- 课程状态统计 -->
            <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-white border-gray-200'">
              <h4 :class="isDark?'text-white/70':'text-gray-700'" class="mb-4">课程状态分布</h4>
              <div class="h-64 flex items-center justify-center">
                <div :class="isDark?'text-white/50':'text-gray-400'">(图表区域 - 使用ECharts等库实现)</div>
              </div>
            </div>

            <!-- 教师课程数量统计 -->
            <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-white border-gray-200'">
              <h4 :class="isDark?'text-white/70':'text-gray-700'" class="mb-4">教师课程数量TOP5</h4>
              <div class="h-64 flex items-center justify-center">
                <div :class="isDark?'text-white/50':'text-gray-400'">(图表区域 - 使用ECharts等库实现)</div>
              </div>
            </div>

            <!-- 课程学分分布 -->
            <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-white border-gray-200'">
              <h4 :class="isDark?'text-white/70':'text-gray-700'" class="mb-4">课程学分分布</h4>
              <div class="h-64 flex items-center justify-center">
                <div :class="isDark?'text-white/50':'text-gray-400'">(图表区域 - 使用ECharts等库实现)</div>
              </div>
            </div>

            <!-- 用户增长趋势 -->
            <div class="rounded-xl p-6 border" :class="isDark?'bg-white/5 border-white/10':'bg-white border-gray-200'">
              <h4 :class="isDark?'text-white/70':'text-gray-700'" class="mb-4">用户增长趋势</h4>
              <div class="h-64 flex items-center justify-center">
                <div :class="isDark?'text-white/50':'text-gray-400'">(图表区域 - 使用ECharts等库实现)</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 课程成员对话框 -->
    <div v-if="showMembersDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">课程成员 - {{ currentCourse?.name }}</h3>
          <button @click="showMembersDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
            <tr class="text-left border-b" :class="isDark?'text-white/50 border-white/10':'text-gray-500 border-gray-200'">
              <th class="pb-3 px-4">用户ID</th>
              <th class="pb-3 px-4">姓名</th>
              <th class="pb-3 px-4">角色</th>
              <th class="pb-3 px-4 text-right">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="member in courseMembers" :key="member.id" class="border-b transition-colors" :class="isDark?'border-white/10 hover:bg-white/5':'border-gray-200 hover:bg-gray-50'">
              <td class="py-4 px-4 font-mono" :class="isDark?'text-white':'text-gray-900'">{{ member.user_id }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ member.account }}</td>
              <td class="py-4 px-4">
                  <span :class="{
                    'px-2 py-1 rounded-full text-xs font-medium': true,
                    'bg-purple-500/20 text-purple-400': member.role === '管理员',
                    'bg-blue-500/20 text-blue-400': member.role === '教师',
                    'bg-green-500/20 text-green-400': member.role === '学生'
                  }">
                    {{ member.role }}
                  </span>
              </td>
              <td class="py-4 px-4 text-right">
                <button
                 v-if="user.role!=='学生'"
                  @click="removeUserFromCourse(member.user_id)"
                  class="p-2 rounded-full transition-colors"
                  :class="isDark ? 'bg-red-500/20 hover:bg-red-500/30 text-red-400' : 'bg-red-100 hover:bg-red-200 text-red-600'"
                  title="移除用户"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                </button>
              </td>
            </tr>
            <tr v-if="courseMembers.length === 0">
              <td colspan="5" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                暂无成员数据
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="flex justify-end pt-4 border-t mt-4" :class="isDark?'border-white/10':'border-gray-200'">
          <button
            @click="showMembersDialog = false"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 添加成员对话框 -->
    <div v-if="showAddMembersDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">添加成员 - {{ currentCourse?.name }}</h3>
          <button @click="showAddMembersDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="mb-4">
          <input
            type="text"
            v-model="searchQuery"
            placeholder="搜索用户..."
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
              <th class="pb-3 px-4">选择</th>
              <th class="pb-3 px-4">用户ID</th>
              <th class="pb-3 px-4">姓名</th>
              <th class="pb-3 px-4">角色</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="user in filteredAvailableUsers" :key="user.id" class="border-b transition-colors" :class="isDark?'border-white/10 hover:bg-white/5':'border-gray-200 hover:bg-gray-50'">
              <td class="py-4 px-4">
                <input
                  type="checkbox"
                  :value="user.id"
                  v-model="selectedUsers"
                  class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 rounded"
                  :class="isDark?'border-white/10':'border-gray-300'"
                >
              </td>
              <td class="py-4 px-4 font-mono" :class="isDark?'text-white':'text-gray-900'">{{ user.id }}</td>
              <td class="py-4 px-4" :class="isDark?'text-white':'text-gray-900'">{{ user.account }}</td>
              <td class="py-4 px-4">
                  <span :class="{
                    'px-2 py-1 rounded-full text-xs font-medium': true,
                    'bg-purple-500/20 text-purple-400': user.role === '管理员',
                    'bg-blue-500/20 text-blue-400': user.role === '教师',
                    'bg-green-500/20 text-green-400': user.role === '学生'
                  }">
                    {{ user.role }}
                  </span>
              </td>
            </tr>
            <tr v-if="availableUsers.length === 0">
              <td colspan="4" class="py-8 text-center" :class="isDark?'text-white/50':'text-gray-500'">
                暂无可用用户
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="flex justify-between pt-4 border-t mt-4" :class="isDark?'border-white/10':'border-gray-200'">
          <div class="text-sm" :class="isDark?'text-white/50':'text-gray-500'">
            已选择 {{ selectedUsers.length }} 个用户
          </div>
          <div class="flex space-x-2">
            <button
              @click="showAddMembersDialog = false"
              class="px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="addUsersToCourse"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              添加选中用户
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建课程对话框 -->
    <div v-if="showCreateDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">创建新课程</h3>
          <button @click="showCreateDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程名称</label>
              <input
                v-model="courseForm.name"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入课程名称"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程代码</label>
              <input
                v-model="courseForm.code"
                type="text"
                disabled
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="课程代码默认生成，无需输入"
              >
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">学分</label>
              <input
                v-model="courseForm.score"
                type="number"
                min="1"
                max="10"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">学期</label>
              <select
                  disabled
                  v-model="courseForm.semester"
                  :class="[
                    'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                    isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                  ]"
                >
                  <option value="2025-1">2025-1</option>
                  <option value="2024-2">2024-2</option>
                  <option value="2024-1">2024-1</option>
                </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">容量</label>
              <input
                v-model="courseForm.capacity"
                type="number"
                min="1"
                max="200"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                disabled
                placeholder="暂未开发"
              >
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">授课教师</label>
            <input
              v-model="courseForm.teacher_name"
              type="text"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
              placeholder="请输入授课老师"
            >
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程描述</label>
            <textarea
              v-model="courseForm.description"
              rows="3"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
              disabled
              placeholder="请输入课程描述"
            ></textarea>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showCreateDialog = false"
              class="flex-1 px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="saveCourse(true)"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              创建课程
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑课程对话框 -->
    <div v-if="showEditDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">{{ isAdmin ? '查看课程' : '编辑课程信息' }}</h3>
          <button @click="showEditDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程名称</label>
              <input
                v-model="courseForm.name"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程代码</label>
              <input
                v-model="courseForm.id"
                type="text"
                disabled
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">学分</label>
              <input
                v-model="courseForm.score"
                type="number"
                min="1"
                max="10"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">学期</label>
              <select
                  v-model="courseForm.semester"
                  disabled
                  :class="[
                    'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                    isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                  ]"
                >
                  <option value="2025-1">2025-1</option>
                  <option value="2024-2">2024-2</option>
                  <option value="2024-1">2024-1</option>
                </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">容量</label>
              <input
                disabled
                v-model="courseForm.capacity"
                type="number"
                min="1"
                max="200"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">授课教师</label>
            <input
              v-model="courseForm.teacher_name"
              type="text"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
              placeholder="请输入授课老师"
            >
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程描述</label>
            <textarea
              v-model="courseForm.description"
              rows="3"
              :disabled="isAdmin"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
            ></textarea>
          </div>

          <div v-if="isAdmin">
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程状态</label>
            <select
              v-model="courseForm.status"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
            >
              <option value="未审核">未审核</option>
              <option value="审核通过">审核通过</option>
              <option value="审核未通过">审核未通过</option>
            </select>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showEditDialog = false"
              class="flex-1 px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              关闭
            </button>
            <button
              v-if="!isAdmin"
              @click="saveCourse(false)"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              保存更改
            </button>
            <button
              v-if="isAdmin"
              @click="saveCourse(false)"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              保存更改
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 审核课程对话框 -->
    <div v-if="showAuditDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">课程审核</h3>
          <button @click="showAuditDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">课程名称</label>
              <div class="px-3 py-2 border rounded-lg" :class="isDark?'bg-white/5 border-white/10 text-white':'bg-gray-50 border-gray-300 text-gray-900'">
                {{ courseForm.name }}
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">授课教师</label>
              <div class="px-3 py-2 border rounded-lg" :class="isDark?'bg-white/5 border-white/10 text-white':'bg-gray-50 border-gray-300 text-gray-900'">
                {{ courseForm.teacher_name }}
              </div>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">审核结果</label>
            <div class="flex space-x-4">
              <label :class="[
    'flex-1 px-4 py-2 rounded-lg border flex items-center justify-center cursor-pointer',
    auditForm.status === '审核通过'
      ? 'bg-green-600/20 border-green-500'
      : isDark
        ? 'bg-white/5 border-white/10 hover:bg-white/10'
        : 'bg-gray-100 border-gray-300 hover:bg-gray-200'
  ]">
                <input
                  type="radio"
                  v-model="auditForm.status"
                  value="审核通过"
                  class="hidden"
                >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" :class="[
      auditForm.status === '审核通过' ? 'text-green-500' : (isDark ? 'text-white/50' : 'text-gray-400')
    ]" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
                <span :class="[
      'font-medium',
      auditForm.status === '审核通过' ? 'text-green-400' : (isDark ? 'text-white/70' : 'text-gray-700')
    ]">通过</span>
              </label>
              <label :class="[
    'flex-1 px-4 py-2 rounded-lg border flex items-center justify-center cursor-pointer',
    auditForm.status === '审核未通过'
      ? 'bg-red-600/20 border-red-500'
      : isDark
        ? 'bg-white/5 border-white/10 hover:bg-white/10'
        : 'bg-gray-100 border-gray-300 hover:bg-gray-200'
  ]">
                <input
                  type="radio"
                  v-model="auditForm.status"
                  value="审核未通过"
                  class="hidden"
                >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" :class="[
      auditForm.status === '审核未通过' ? 'text-red-500' : (isDark ? 'text-white/50' : 'text-gray-400')
    ]" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                </svg>
                <span :class="[
      'font-medium',
      auditForm.status === '审核未通过' ? 'text-red-400' : (isDark ? 'text-white/70' : 'text-gray-700')
    ]">拒绝</span>
              </label>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">审核意见</label>
            <textarea
              v-model="auditForm.feedback"
              rows="3"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
              ]"
              placeholder="请输入审核意见(可选)"
            ></textarea>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showAuditDialog = false"
              class="flex-1 px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="auditCourse"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              提交审核
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户编辑对话框 -->
    <div v-if="showUserDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-2xl max-h-[80vh] overflow-y-auto custom-scrollbar" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">{{ userForm.id ? '编辑用户' : '新增用户' }}</h3>
          <button @click="showUserDialog = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">账号</label>
              <input
                v-model="userForm.account"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入登录账号"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">姓名</label>
              <input
                v-model="userForm.name"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入用户姓名"
              >
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">角色</label>
              <select
                v-model="userForm.role"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
              >
                <option value="admin">管理员</option>
                <option value="teacher">教师</option>
              </select>
            </div>
            <div v-if="!userForm.id">
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">初始密码</label>
              <input
                v-model="userForm.password"
                type="password"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入初始密码"
              >
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">邮箱</label>
              <input
                v-model="userForm.email"
                type="email"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入邮箱"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="isDark?'text-white/70':'text-gray-700'">电话</label>
              <input
                v-model="userForm.phone"
                type="tel"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent',
                  isDark ? 'bg-white/5 border-white/10 text-white' : 'bg-white border-gray-300 text-gray-900'
                ]"
                placeholder="请输入电话"
              >
            </div>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showUserDialog = false"
              class="flex-1 px-4 py-2 rounded-lg transition-colors"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              取消
            </button>
            <button
              @click="saveUser(!userForm.id)"
              class="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
            >
              {{ userForm.id ? '保存更改' : '创建用户' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div class="backdrop-blur-md rounded-2xl p-6 border w-full max-w-md" :class="isDark?'bg-gradient-to-br from-white/5 to-white/3 border-white/10':'bg-white border-gray-200'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="isDark?'text-white':'text-gray-900'" class="text-lg font-medium">确认删除课程</h3>
          <button @click="showDeleteConfirm = false" :class="isDark?'text-white/50 hover:text-white':'text-gray-400 hover:text-gray-600'">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <p :class="isDark?'text-white/70':'text-gray-600'" class="mb-6">您确定要删除课程 "{{ courseForm.name }}" 吗？此操作无法撤销。</p>

        <div class="flex space-x-3">
          <button
            @click="showDeleteConfirm = false"
            class="flex-1 px-4 py-2 rounded-lg transition-colors"
            :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
          >
            取消
          </button>
          <button
            @click="deleteCourse"
            class="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg text-white transition-colors"
          >
            确认删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 自定义滚动条样式 */
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

/* 卡片悬停动画 */
.course-card {
  transition: all 0.3s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

/* 按钮工具提示动画 */
.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}
</style>
