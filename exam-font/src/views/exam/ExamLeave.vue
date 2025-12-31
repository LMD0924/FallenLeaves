<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from "@/net/index.js"
import { message } from "ant-design-vue"
import { isDark } from "@/stores/theme.js"

const [messageApi, contextHolder] = message.useMessage()

// 当前激活的标签页
const activeTab = ref('all')

// 搜索查询
const searchQuery = ref('')

// 对话框状态
const showApplyDialog = ref(false)
const showDetailDialog = ref(false)
const showApproveDialog = ref(false)
const selectedLeave = ref(null)
const userId = ref({})

// 表单数据
const leaveForm = ref({
  reason:'',
  startTime:new Date().toISOString().split('T')[0],
  endTime:new Date().toISOString().split('T')[0],
  type:'事假',
  time:new Date().toISOString(),
  status:'待审批',
  day:0
})

// 审批表单
const approveForm = ref({
  comment: '',
  status: '已批准'
})

// 请假类型选项
const leaveTypes = [
  { value: '事假', emoji: '📅', color: 'blue' },
  { value: '病假', emoji: '🤒', color: 'green' },
  { value: '年假', emoji: '🏖️', color: 'orange' },
  { value: '婚假', emoji: '💍', color: 'pink' },
  { value: '产假', emoji: '👶', color: 'purple' },
  { value: '丧假', emoji: '⚰️', color: 'gray' }
]

// 状态选项
const statusOptions = [
  { value: '待审批', label: '待审批', emoji: '🟡', color: 'yellow' },
  { value: '已批准', label: '已批准', emoji: '🟢', color: 'green' },
  { value: '已拒绝', label: '已拒绝', emoji: '🔴', color: 'red' },
  { value: '已注销', label: '已注销', emoji: '⚫', color: 'gray' }
]

// 请假数据
const leaves = ref([])

// 用户角色和权限
const userRole = ref('学生')
const currentUser = ref({})

// 判断是否为管理员
const isAdmin = computed(() => {
  return userRole.value === '管理员' || userRole.value === 'manager'
})

// 获取当前用户信息
const getCurrentUser = () => {
  return new Promise((resolve, reject) => {
    get('api/exam/current', {}, (message, data) => {
      currentUser.value = data
      userId.value = data.id
      userRole.value = data.role || 'user' // 获取用户角色，默认为user
      resolve(data)
      console.log("当前用户：", currentUser.value, "角色：", userRole.value)
    }, (error) => {
      reject(error)
    })
  })
}

// 计算请假天数
const calculateDay = () => {
  if (leaveForm.value.startTime && leaveForm.value.endTime) {
    const start = new Date(leaveForm.value.startTime)
    const end = new Date(leaveForm.value.endTime)
    const diffTime = Math.abs(end - start)
    const diffDay = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
    leaveForm.value.day = diffDay
  }
}

// 过滤后的请假记录
const filteredLeaves = computed(() => {
  let result = leaves.value

  if (activeTab.value !== 'all') {
    if (activeTab.value === '待审批') {
      result = result.filter(vacation => vacation.status === '待审批')
    } else if (activeTab.value === '已批准') {
      result = result.filter(vacation => vacation.status === '已批准')
    } else if (activeTab.value === '已拒绝') {
      result = result.filter(vacation => vacation.status === '已拒绝')
    } else {
      result = result.filter(vacation => vacation.type === activeTab.value)
    }
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(vacation =>
      vacation.reason?.toLowerCase().includes(query) ||
      vacation.type?.toLowerCase().includes(query) ||
      statusOptions.find(s => s.value === vacation.status)?.label.includes(query)
    )
  }

  return result
})

// 统计数据
const stats = computed(() => {
  const total = leaves.value.length
  const pending = leaves.value.filter(vacation => vacation.status === '待审批').length
  const approved = leaves.value.filter(vacation => vacation.status === '已批准').length
  const rejected = leaves.value.filter(vacation => vacation.status === '已拒绝').length

  const sickLeaveUsed = leaves.value
    .filter(vacation => vacation.type === '病假' && vacation.status === '已批准')
    .reduce((sum, vacation) => sum + vacation.day, 0)

  const personalLeaveUsed = leaves.value
    .filter(vacation => vacation.type === '事假' && vacation.status === '已批准')
    .reduce((sum, vacation) => sum + vacation.day, 0)

  return {
    total,
    pending,
    approved,
    rejected,
    sickLeaveUsed,
    personalLeaveUsed,
    sickLeaveRemaining: Math.max(0, 15 - sickLeaveUsed), // 假设病假年度限额15天
    personalLeaveRemaining: Math.max(0, 10 - personalLeaveUsed) // 假设事假年度限额10天
  }
})

// 获取请假记录
const fetchLeaves = async () => {
  await getCurrentUser()
  return new Promise((resolve, reject) => {
    get('api/exam/GetVacation', {},
      (message, data) => {
        leaves.value = data;
        resolve(data)
        console.log("请假记录：", leaves.value)
        messageApi.success(message)
      }, (error) => {
        reject(error)
      })
  })
}

// 申请请假
const applyLeave = () => {
  if (!validateForm()) {
    return
  }
  return new Promise((resolve, reject) => {
    post('api/exam/InsertVacation', {
      ...leaveForm.value,
      userId: userId.value,
      startTime: new Date(leaveForm.value.startTime),
      endTime: new Date(leaveForm.value.endTime)
    }, (message, data) => {
      messageApi.success(message)
      closeApplyDialog()
      fetchLeaves()
      resolve(data)
    }, (error) => {
      reject(error)
    })
  })
}

// 查看详情
const viewDetail = (vacation) => {
  selectedLeave.value = vacation
  showDetailDialog.value = true
}

// 打开审批对话框
const openApproveDialog = (vacation, status) => {
  selectedLeave.value = vacation
  approveForm.value.status = status
  approveForm.value.comment = ''
  showApproveDialog.value = true
}

// 提交审批
const submitApprove = () => {
  if (!selectedLeave.value) return

  post('api/exam/UpdateVacation', {
    id: selectedLeave.value.id,
    status: approveForm.value.status,
  }, (message, data) => {
    messageApi.success(message)
    closeApproveDialog()
    fetchLeaves()
  }, (error) => {
    messageApi.error('审批操作失败')
  })
}

// 取消请假
const cancelLeave = (id) => {
  const vacation = leaves.value.find(l => l.id === id)
  if (vacation && vacation.status === '待审批') {
    post('api/exam/UpdateVacation', {
      id: id,
      status: '已注销',
      comment: '用户主动取消'
    }, (message, data) => {
      messageApi.success('请假申请已取消')
      fetchLeaves()
    }, (error) => {
      messageApi.error('取消失败')
    })
  } else {
    messageApi.warning('只能取消待审批的请假申请')
  }
}

// 验证表单
const validateForm = () => {
  if (!leaveForm.value.startTime) {
    messageApi.error('请选择开始日期')
    return false
  }

  if (!leaveForm.value.endTime) {
    messageApi.error('请选择结束日期')
    return false
  }

  if (new Date(leaveForm.value.startTime) > new Date(leaveForm.value.endTime)) {
    messageApi.error('结束日期不能早于开始日期')
    return false
  }

  if (!leaveForm.value.reason) {
    messageApi.error('请输入请假原因')
    return false
  }

  // 病假特殊验证
  if (leaveForm.value.type === '病假') {
    if (!leaveForm.value.symptoms) {
      messageApi.error('请输入病情症状')
      return false
    }
    if (!leaveForm.value.emergencyContact || !leaveForm.value.contactPhone) {
      messageApi.error('请填写紧急联系人信息')
      return false
    }
  }

  return true
}

// 关闭申请对话框
const closeApplyDialog = () => {
  showApplyDialog.value = false
  leaveForm.value = {
    type: '事假',
    startTime: new Date().toISOString().split('T')[0],
    endTime: new Date().toISOString().split('T')[0],
    day: 0,
    reason: '',
    emergencyContact: '',
    contactPhone: '',
    medicalCertificate: null,
    symptoms: ''
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  showDetailDialog.value = false
  selectedLeave.value = null
}

// 关闭审批对话框
const closeApproveDialog = () => {
  showApproveDialog.value = false
  selectedLeave.value = null
  approveForm.value = {
    comment: '',
    status: '已批准'
  }
}

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
      accent: isDark.value
        ? 'text-indigo-400 hover:text-indigo-300'
        : 'text-indigo-600 hover:text-indigo-700'
    },
    table: {
      header: isDark.value ? 'text-white/70' : 'text-gray-500',
      row: isDark.value ? 'divide-white/10' : 'divide-gray-200'
    }
  }
})

// 初始化加载
onMounted(() => {
  fetchLeaves()
})
</script>

<template>
  <contextHolder />
  <div class="min-h-screen" :class="themeClasses.bg">
    <!-- 顶部导航 -->
    <header class="flex items-center justify-between p-6 border-b" :class="isDark ? 'bg-white/5 border-white/10' : 'bg-white border-gray-200'">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
          <path fill="currentColor" d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
        </svg>
        <h1 :class="themeClasses.text.primary + ' text-2xl font-bold'">请假管理系统</h1>
        <span v-if="isAdmin" class="px-3 py-1 bg-red-500/20 text-red-400 rounded-full text-sm font-medium">
          管理员模式
        </span>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="flex-1 p-8 overflow-auto">
      <div class="max-w-7xl mx-auto">
        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div :class="themeClasses.card + ' rounded-xl p-6 transition-all duration-300 hover:border-indigo-400/30'">
            <div class="flex items-center">
              <div class="p-3 bg-blue-500/20 rounded-lg">
                <span class="text-2xl">📊</span>
              </div>
              <div class="ml-4">
                <p :class="themeClasses.text.secondary + ' text-sm font-medium'">总申请数</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">{{ stats.total }}</p>
              </div>
            </div>
          </div>

          <div :class="themeClasses.card + ' rounded-xl p-6 transition-all duration-300 hover:border-yellow-400/30'">
            <div class="flex items-center">
              <div class="p-3 bg-yellow-500/20 rounded-lg">
                <span class="text-2xl">⏳</span>
              </div>
              <div class="ml-4">
                <p :class="themeClasses.text.secondary + ' text-sm font-medium'">待审批</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">{{ stats.pending }}</p>
              </div>
            </div>
          </div>

          <div :class="themeClasses.card + ' rounded-xl p-6 transition-all duration-300 hover:border-green-400/30'">
            <div class="flex items-center">
              <div class="p-3 bg-green-500/20 rounded-lg">
                <span class="text-2xl">✅</span>
              </div>
              <div class="ml-4">
                <p :class="themeClasses.text.secondary + ' text-sm font-medium'">剩余病假</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">{{ stats.sickLeaveRemaining }}天</p>
              </div>
            </div>
          </div>

          <div :class="themeClasses.card + ' rounded-xl p-6 transition-all duration-300 hover:border-orange-400/30'">
            <div class="flex items-center">
              <div class="p-3 bg-orange-500/20 rounded-lg">
                <span class="text-2xl">📅</span>
              </div>
              <div class="ml-4">
                <p :class="themeClasses.text.secondary + ' text-sm font-medium'">剩余事假</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">{{ stats.personalLeaveRemaining }}天</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作栏 -->
        <div :class="themeClasses.card + ' rounded-2xl p-6 mb-8'">
          <div class="flex flex-col md:flex-row md:items-center justify-between space-y-4 md:space-y-0">
            <div class="flex space-x-4">
              <div class="relative">
                <input
                  type="text"
                  v-model="searchQuery"
                  placeholder="搜索请假原因、类型..."
                  :class="themeClasses.input + ' w-full md:w-64 pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                >
                <svg xmlns="http://www.w3.org/2000/svg" :class="themeClasses.text.muted + ' h-5 w-5 absolute left-3 top-2.5'" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
                </svg>
              </div>
            </div>
            <button
              v-if="!isAdmin"
              @click="showApplyDialog = true"
              :class="themeClasses.button.primary + ' flex items-center px-6 py-3 rounded-lg font-medium transition-all duration-300 transform hover:scale-105'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
              </svg>
              申请请假
            </button>
          </div>
        </div>

        <!-- 标签页导航 -->
        <div class="flex mb-6 border-b overflow-x-auto" :class="isDark ? 'border-white/10' : 'border-gray-200'">
          <button
            @click="activeTab = 'all'"
            :class="[
              'px-6 py-3 font-medium border-b-2 border-transparent transition-all whitespace-nowrap',
              activeTab === 'all'
                ? 'text-indigo-400 border-indigo-400'
                : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
            ]"
          >
            📋 全部记录
          </button>
          <button
            v-for="type in leaveTypes"
            :key="type.value"
            @click="activeTab = type.value"
            :class="[
              'px-6 py-3 font-medium border-b-2 border-transparent transition-all whitespace-nowrap',
              activeTab === type.value
                ? 'text-indigo-400 border-indigo-400'
                : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
            ]"
          >
            {{ type.emoji }} {{ type.value }}
          </button>
          <button
            @click="activeTab = '待审批'"
            :class="[
              'px-6 py-3 font-medium border-b-2 border-transparent transition-all whitespace-nowrap',
              activeTab === '待审批'
                ? 'text-indigo-400 border-indigo-400'
                : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
            ]"
          >
            🟡 待审批
          </button>
          <button
            @click="activeTab = '已批准'"
            :class="[
              'px-6 py-3 font-medium border-b-2 border-transparent transition-all whitespace-nowrap',
              activeTab === '已批准'
                ? 'text-indigo-400 border-indigo-400'
                : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
            ]"
          >
            🟢 已批准
          </button>
          <button
            @click="activeTab = '已拒绝'"
            :class="[
              'px-6 py-3 font-medium border-b-2 border-transparent transition-all whitespace-nowrap',
              activeTab === '已拒绝'
                ? 'text-indigo-400 border-indigo-400'
                : themeClasses.text.secondary + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')
            ]"
          >
            🔴 已拒绝
          </button>
        </div>

        <!-- 请假记录列表 -->
        <div :class="themeClasses.card + ' rounded-2xl p-6'">
          <table class="min-w-full divide-y" :class="themeClasses.table.row">
            <thead>
            <tr>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">类型</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">时间范围</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">天数</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">原因</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">状态</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-left text-xs font-medium uppercase tracking-wider'">申请时间</th>
              <th :class="themeClasses.table.header + ' px-6 py-3 text-right text-xs font-medium uppercase tracking-wider'">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y" :class="themeClasses.table.row">
            <tr v-for="vacation in filteredLeaves" :key="vacation.id" class="transition-colors" :class="isDark ? 'hover:bg-white/5' : 'hover:bg-gray-50'">
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-${leaveTypes.find(t => t.value === vacation.type)?.color}-500/20 text-${leaveTypes.find(t => t.value === vacation.type)?.color}-400`">
                  {{ leaveTypes.find(t => t.value === vacation.type)?.emoji }}
                  {{ vacation.type }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                {{ vacation.startTime }} 至 {{ vacation.endTime }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                <span class="font-medium">{{ vacation.day }} 天</span>
              </td>
              <td class="px-6 py-4 text-sm max-w-xs truncate" :class="themeClasses.text.primary">
                {{ vacation.reason }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-${statusOptions.find(s => s.value === vacation.status)?.color}-500/20 text-${statusOptions.find(s => s.value === vacation.status)?.color}-400`">
                  {{ statusOptions.find(s => s.value === vacation.status)?.emoji }}
                  {{ statusOptions.find(s => s.value === vacation.status)?.label }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm" :class="themeClasses.text.primary">
                {{ vacation.time }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button @click="viewDetail(vacation)" :class="themeClasses.button.accent + ' mr-3'">详情</button>

                <!-- 管理员审批按钮 -->
                <button
                  v-if="isAdmin && vacation.status === '待审批'"
                  @click="openApproveDialog(vacation, '已批准')"
                  class="text-green-400 hover:text-green-300 mr-2"
                >
                  批准
                </button>
                <button
                  v-if="isAdmin && vacation.status === '待审批'"
                  @click="openApproveDialog(vacation, '已拒绝')"
                  class="text-red-400 hover:text-red-300 mr-2"
                >
                  拒绝
                </button>

                <!-- 用户取消按钮 -->
                <button
                  v-if="!isAdmin && vacation.status === '待审批'"
                  @click="cancelLeave(vacation.id)"
                  class="text-red-400 hover:text-red-300"
                >
                  取消
                </button>
              </td>
            </tr>
            <tr v-if="filteredLeaves.length === 0">
              <td colspan="7" class="px-6 py-4 text-center text-sm" :class="themeClasses.text.muted">
                暂无请假记录
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>

    <!-- 申请请假对话框 -->
    <div v-if="showApplyDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div :class="themeClasses.card + ' rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto'">
        <div class="flex items-center justify-between mb-6">
          <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">申请请假</h3>
          <button @click="closeApplyDialog" :class="themeClasses.text.muted + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-6">
          <!-- 请假类型 -->
          <div>
            <label class="block text-sm font-medium mb-3" :class="themeClasses.text.secondary">📝 请假类型</label>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
              <button
                v-for="type in leaveTypes"
                :key="type.value"
                @click="leaveForm.type = type.value"
                :class="[
                  'p-4 rounded-lg border-2 transition-all text-left',
                  leaveForm.type === type.value
                    ? `border-${type.color}-400 bg-${type.color}-500/10`
                    : themeClasses.button.secondary
                ]"
              >
                <div class="flex items-center space-x-2">
                  <span class="text-xl">{{ type.emoji }}</span>
                  <span :class="themeClasses.text.primary + ' font-medium'">{{ type.value }}</span>
                </div>
              </button>
            </div>
          </div>

          <!-- 时间范围 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📅 开始日期</label>
              <input
                v-model="leaveForm.startTime"
                @change="calculateDay"
                type="date"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📅 结束日期</label>
              <input
                v-model="leaveForm.endTime"
                @change="calculateDay"
                type="date"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              >
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">⏱️ 请假天数</label>
              <input
                v-model="leaveForm.day"
                type="number"
                readonly
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg bg-gray-200'"
              >
            </div>
          </div>

          <!-- 病假特殊信息 -->
          <div v-if="leaveForm.type === '病假'" class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">🤒 病情症状</label>
              <textarea
                v-model="leaveForm.symptoms"
                rows="2"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                placeholder="请描述具体症状..."
              ></textarea>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📞 紧急联系人</label>
                <input
                  v-model="leaveForm.emergencyContact"
                  type="text"
                  :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                  placeholder="请输入紧急联系人姓名"
                >
              </div>
              <div>
                <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📱 联系电话</label>
                <input
                  v-model="leaveForm.contactPhone"
                  type="tel"
                  :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
                  placeholder="请输入联系电话"
                >
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">🏥 医疗证明</label>
              <input
                type="file"
                accept=".jpg,.jpeg,.png,.pdf"
                :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              >
              <p :class="themeClasses.text.muted + ' text-xs mt-1'">支持 JPG, PNG, PDF 格式，最大 10MB</p>
            </div>
          </div>

          <!-- 请假原因 -->
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">📄 请假原因</label>
            <textarea
              v-model="leaveForm.reason"
              rows="3"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              placeholder="请详细说明请假原因..."
            ></textarea>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="closeApplyDialog"
              :class="themeClasses.button.secondary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              取消
            </button>
            <button
              @click="applyLeave"
              :class="themeClasses.button.primary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              提交申请
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 请假详情对话框 -->
    <div v-if="showDetailDialog && selectedLeave" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div :class="themeClasses.card + ' rounded-2xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto'">
        <div class="flex items-center justify-between mb-6">
          <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">请假详情</h3>
          <button @click="closeDetailDialog" :class="themeClasses.text.muted + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-6">
          <!-- 基本信息 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 :class="themeClasses.text.secondary + ' text-sm font-medium mb-3'">基本信息</h4>
              <div class="space-y-3">
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">请假类型:</span>
                  <span :class="themeClasses.text.primary">
                    {{ selectedLeave.type }}
                  </span>
                </div>
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">时间范围:</span>
                  <span :class="themeClasses.text.primary">
                    {{ selectedLeave.startTime }} 至 {{ selectedLeave.endTime }}
                  </span>
                </div>
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">请假天数:</span>
                  <span :class="themeClasses.text.primary + ' font-medium'">
                    {{ selectedLeave.day }} 天
                  </span>
                </div>
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">申请状态:</span>
                  <span :class="`px-2 py-1 rounded-full text-xs font-medium bg-${statusOptions.find(s => s.value === selectedLeave.status)?.color}-500/20 text-${statusOptions.find(s => s.value === selectedLeave.status)?.color}-400`">
                    {{ statusOptions.find(s => s.value === selectedLeave.status)?.label }}
                  </span>
                </div>
              </div>
            </div>

            <div>
              <h4 :class="themeClasses.text.secondary + ' text-sm font-medium mb-3'">申请信息</h4>
              <div class="space-y-3">
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">申请人:</span>
                  <span :class="themeClasses.text.primary">{{ selectedLeave.account }}</span>
                </div>
                <div class="flex justify-between">
                  <span :class="themeClasses.text.muted">申请时间:</span>
                  <span :class="themeClasses.text.primary">{{ selectedLeave.time }}</span>
                </div>
                <div v-if="selectedLeave.approveBy" class="flex justify-between">
                  <span :class="themeClasses.text.muted">审批人:</span>
                  <span :class="themeClasses.text.primary">{{ selectedLeave.approveBy }}</span>
                </div>
                <div v-if="selectedLeave.approveTime" class="flex justify-between">
                  <span :class="themeClasses.text.muted">审批时间:</span>
                  <span :class="themeClasses.text.primary">{{ selectedLeave.approveTime }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 请假原因 -->
          <div>
            <h4 :class="themeClasses.text.secondary + ' text-sm font-medium mb-2'">请假原因</h4>
            <p :class="themeClasses.text.primary + ' p-3 rounded-lg ' + (isDark ? 'bg-white/5' : 'bg-gray-50')">
              {{ selectedLeave.reason }}
            </p>
          </div>

          <!-- 病假特殊信息 -->
          <div v-if="selectedLeave.type === '病假' && selectedLeave.symptoms">
            <h4 :class="themeClasses.text.secondary + ' text-sm font-medium mb-2'">病情症状</h4>
            <p :class="themeClasses.text.primary + ' p-3 rounded-lg ' + (isDark ? 'bg-white/5' : 'bg-gray-50')">
              {{ selectedLeave.symptoms }}
            </p>
          </div>

          <!-- 审批意见 -->
          <div v-if="selectedLeave.comment">
            <h4 :class="themeClasses.text.secondary + ' text-sm font-medium mb-2'">审批意见</h4>
            <p :class="themeClasses.text.primary + ' p-3 rounded-lg ' + (isDark ? 'bg-white/5' : 'bg-gray-50')">
              {{ selectedLeave.comment }}
            </p>
          </div>

          <!-- 管理员审批操作 -->
          <div v-if="isAdmin && selectedLeave.status === '待审批'" class="pt-4 flex space-x-3 border-t" :class="isDark ? 'border-white/10' : 'border-gray-200'">
            <button
              @click="openApproveDialog(selectedLeave, '已批准')"
              class="flex-1 px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg transition-colors"
            >
              批准申请
            </button>
            <button
              @click="openApproveDialog(selectedLeave, '已拒绝')"
              class="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
            >
              拒绝申请
            </button>
          </div>

          <div v-else class="pt-4">
            <button
              @click="closeDetailDialog"
              :class="themeClasses.button.secondary + ' w-full px-4 py-2 rounded-lg transition-colors'"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 审批对话框 -->
    <div v-if="showApproveDialog && selectedLeave" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div :class="themeClasses.card + ' rounded-2xl p-6 w-full max-w-md'">
        <div class="flex items-center justify-between mb-6">
          <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">
            {{ approveForm.status === '已批准' ? '批准请假申请' : '拒绝请假申请' }}
          </h3>
          <button @click="closeApproveDialog" :class="themeClasses.text.muted + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-2" :class="themeClasses.text.secondary">
              {{ approveForm.status === '已批准' ? '📝 审批意见（可选）' : '📝 拒绝原因（建议填写）' }}
            </label>
            <textarea
              v-model="approveForm.comment"
              rows="3"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
              :placeholder="approveForm.status === '已批准' ? '请输入审批意见...' : '请输入拒绝原因...'"
            ></textarea>
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="closeApproveDialog"
              :class="themeClasses.button.secondary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              取消
            </button>
            <button
              @click="submitApprove"
              :class="[
                'flex-1 px-4 py-2 rounded-lg transition-colors text-white',
                approveForm.status === '已批准'
                  ? 'bg-green-500 hover:bg-green-600'
                  : 'bg-red-500 hover:bg-red-600'
              ]"
            >
              {{ approveForm.status === '已批准' ? '批准' : '拒绝' }}
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
