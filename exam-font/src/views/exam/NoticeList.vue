<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import { get, post } from "@/net/index.js"
import { message, Modal } from "ant-design-vue"
import { isDark } from "@/stores/theme.js"
import { formatDate } from "@/time/Data.js"
import webSocketService from '@/net/websocket.js'
import NotificationToast from '@/components/NotificationToast.vue'

const [messageApi, contextHolder] = message.useMessage()

// 状态管理
const activeCategory = ref('all')
const searchQuery = ref('')
const selectedMessage = ref(null)
const showDetailModal = ref(false)
const currentUser = ref({})
const loading = ref(false)

// 消息数据 - 根据notice表结构
const messages = ref([])

// 通知队列
const notificationQueue = ref([])
const activeNotifications = ref([])
const maxNotifications = 3
// WebSocket 处理器 ID
const wsHandlerId = 'notice-center'

// 分类选项 - 基于notice表的type字段
const categories = [
  { id: 'all', name: '全部消息', color: 'gray', icon: '📬', count: 0 },
  { id: 'unread', name: '未读消息', color: 'blue', icon: '📥', count: 0 },
  { id: 'important', name: '重要消息', color: 'orange', icon: '⭐', count: 0 },
  { id: '一般通知', name: '一般通知', color: 'blue', icon: '📢', count: 0 },
  { id: '重要通知', name: '重要通知', color: 'orange', icon: '⚠️', count: 0 },
  { id: '紧急通知', name: '紧急通知', color: 'red', icon: '🚨', count: 0 },
  { id: '系统维护', name: '系统维护', color: 'purple', icon: '🔧', count: 0 },
  { id: '政策公告', name: '政策公告', color: 'green', icon: '📜', count: 0 },
  { id: '活动通知', name: '活动通知', color: 'pink', icon: '🎉', count: 0 }
]

// 优先级选项 - 基于notice表的priority字段
const priorityOptions = [
  { value: '低优先级', label: '低优先级', color: 'gray' },
  { value: '中优先级', label: '中优先级', color: 'blue' },
  { value: '高优先级', label: '高优先级', color: 'orange' },
  { value: '紧急', label: '紧急', color: 'red' }
]

// 接收对象选项 - 基于notice表的receive字段
const receiveOptions = [
  { value: '全体师生', label: '全体师生', icon: '👥' },
  { value: '全体学生', label: '全体学生', icon: '🎓' },
  { value: '全体教师', label: '全体教师', icon: '👨‍🏫' },
  { value: '高一年级', label: '高一年级', icon: '1️⃣' },
  { value: '高二年级', label: '高二年级', icon: '2️⃣' },
  { value: '高三年级', label: '高三年级', icon: '3️⃣' },
  { value: '行政人员', label: '行政人员', icon: '💼' },
  { value: '班主任', label: '班主任', icon: '📋' },
  { value: '班干部', label: '班干部', icon: '⭐' }
]

// 获取当前用户信息
const getCurrentUser = async () => {
  try {
    const data = await new Promise((resolve, reject) => {
      get('api/user/current', {}, (message, data) => {
        resolve(data)
      }, (error) => {
        reject(error)
      })
    })
    currentUser.value = data
    return data
  } catch (error) {
    console.error('获取用户信息失败:', error)
    messageApi.warning('获取用户信息失败，将使用默认名称')
    currentUser.value = { name: '测试用户', account: 'test', id: 1 }
  }
}

// 计算属性 - 过滤消息
const filteredMessages = computed(() => {
  let result = messages.value

  // 分类过滤
  if (activeCategory.value !== 'all') {
    if (activeCategory.value === 'unread') {
      result = result.filter(msg => !msg.read)
    } else if (activeCategory.value === 'important') {
      result = result.filter(msg => msg.important)
    } else {
      result = result.filter(msg => msg.type === activeCategory.value)
    }
  }

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(msg =>
      msg.title.toLowerCase().includes(query) ||
      msg.content.toLowerCase().includes(query) ||
      (msg.user && msg.user.name && msg.user.name.toLowerCase().includes(query))
    )
  }

  return result.sort((a, b) => new Date(b.time) - new Date(a.time))
})

// 更新分类计数
const updateCategoryCounts = () => {
  categories.forEach(cat => {
    if (cat.id === 'all') {
      cat.count = messages.value.length
    } else if (cat.id === 'unread') {
      cat.count = messages.value.filter(msg => !msg.read).length
    } else if (cat.id === 'important') {
      cat.count = messages.value.filter(msg => msg.important).length
    } else {
      cat.count = messages.value.filter(msg => msg.type === cat.id).length
    }
  })
}

// 初始化 WebSocket
const initWebSocket = () => {
  const handlers = {
    // 连接建立
    onConnected: () => {
      console.log('WebSocket连接成功')
      messageApi.success('实时通知已连接')
    },

    // 连接断开
    onDisconnected: () => {
      console.log('WebSocket连接断开')
      messageApi.warning('实时通知已断开')
    },

    // 新消息通知
    new_notice: (noticeData) => {
      console.log('收到新通知:', noticeData)

      // 添加到通知队列
      const notification = {
        id: noticeData.id || Date.now(),
        title: noticeData.title,
        content: noticeData.content,
        type: noticeData.type,
        priority: noticeData.priority,
        time: noticeData.time || new Date().toISOString(),
        userId: noticeData.userId
      }

      addNotification(notification)

      // 如果当前在消息页面，刷新消息列表
      if (activeCategory.value === 'all' || activeCategory.value === noticeData.type) {
        fetchMessages()
      }
    },

    // 心跳响应
    pong: () => {
      console.log('收到心跳响应')
    },

    // 连接成功
    connected: (data) => {
      console.log('WebSocket连接成功:', data)
    },

    // 默认处理器
    default: (message) => {
      console.log('收到未知类型消息:', message)
    }
  }

  webSocketService.registerHandler(wsHandlerId, handlers)
  webSocketService.connect()
}

// 添加通知到队列
const addNotification = (notification) => {
  notificationQueue.value.push(notification)
  processNotificationQueue()
}

// 处理通知队列
const processNotificationQueue = () => {
  while (activeNotifications.value.length < maxNotifications && notificationQueue.value.length > 0) {
    const notification = notificationQueue.value.shift()
    activeNotifications.value.push(notification)
  }
}

// 移除通知
const removeNotification = (notificationId) => {
  const index = activeNotifications.value.findIndex(n => n.id === notificationId)
  if (index > -1) {
    activeNotifications.value.splice(index, 1)
  }
  processNotificationQueue()
}


// 获取消息列表 - 从notice表获取
const fetchMessages = async () => {
  try {
    loading.value = true
    await getCurrentUser()
    const data = await new Promise((resolve, reject) => {
      get('api/notice/SelectAllNotice', {}, (message, data) => {
        resolve(data || [])
      }, (error) => {
        reject(error)
      })
    })

    // 处理从数据库返回的数据，包含用户信息
    messages.value = data.map(msg => ({
      id: msg.id,
      title: msg.title,
      content: msg.content,
      type: msg.type,
      priority: msg.priority,
      receive: msg.receive,
      time: msg.time,
      userId: msg.userId,
      user: msg.user || { // 包含发布人信息
        name: msg.userName || '未知用户',
        avatar: msg.userAvatar || '/default-avatar.png',
        role: msg.userRole || '用户'
      },
      read: false,
      important: false
    }))

    updateCategoryCounts()
  } catch (error) {
    console.error('获取消息列表失败:', error)
    messageApi.error('获取消息列表失败')
  } finally {
    loading.value = false
  }
}

// 获取用户头像显示
const getUserAvatar = (user) => {
  if (user && user.avatar) {
    return user.avatar
  }
  // 默认头像或根据用户名生成
  return `/default-avatar.png`
}

// 获取用户显示名称
const getUserDisplayName = (user) => {
  if (user && user.name) {
    return user.name
  }
  if (user && user.account) {
    return user.account
  }
  return '未知用户'
}

// 获取用户角色
const getUserRole = (user) => {
  if (user && user.role) {
    return user.role
  }
  return '用户'
}

// 查看消息详情
const viewMessage = (message) => {
  selectedMessage.value = message
  showDetailModal.value = true
}

// 获取通知类型的样式
const getNoticeTypeStyle = (type) => {
  const typeInfo = categories.find(t => t.id === type)
  return {
    borderColor: `border-l-${typeInfo?.color || 'blue'}-500`,
    bgColor: `bg-${typeInfo?.color || 'blue'}-50`,
    darkBgColor: `dark:bg-${typeInfo?.color || 'blue'}-900/20`
  }
}

// 获取通知类型的颜色类
const getNoticeTypeColorClass = (type) => {
  const typeInfo = categories.find(t => t.id === type)
  return `bg-${typeInfo?.color || 'blue'}-500`
}

// 获取接收对象的图标
const getReceiveIcon = (receiveValue) => {
  const receiveInfo = receiveOptions.find(r => r.value === receiveValue)
  return receiveInfo?.icon || '👥'
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '未知时间'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return formatDate(date)
}

// 防抖搜索
let searchTimeout = null
const searchMessages = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    // 搜索逻辑已经在 computed 中处理
  }, 300)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
}

// 初始化加载
onMounted(() => {
  fetchMessages()
  initWebSocket()
})

onUnmounted(() => {
  // 清理 WebSocket
  webSocketService.unregisterHandler(wsHandlerId)
  webSocketService.disconnect()
})
</script>

<template>
  <contextHolder />
  <div :class="isDark?'bg-black':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="min-h-screen transition-colors duration-300">
    <!-- 顶部导航 -->
    <header class="flex items-center justify-between border-b p-6 shadow-md rounded-xl hover:shadow-lg duration-200 mb-8" :class="isDark?'border-white/20':'border-gray-200'">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400 animate-pulse">
          <path fill="currentColor" d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 9h-2V5h2v6zm0 4h-2v-2h2v2z"/>
        </svg>
        <div>
          <h1 :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">消息中心</h1>
          <p :class="isDark?'text-white/70':'text-gray-600'" class="text-sm">及时获取重要通知和消息</p>
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <div class="flex items-center space-x-2 px-3 py-1 rounded-full" :class="isDark?'bg-white/5':'bg-gray-100'">
          <div class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
          <span :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ currentUser.name || currentUser.account || '访客' }}</span>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="flex flex-1 p-6 gap-6 max-w-7xl mx-auto">
      <!-- 左侧菜单栏 -->
      <div class="w-80 flex-shrink-0">
        <div :class="isDark?'bg-black border border-white/50':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="rounded-2xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <!-- 用户信息 -->
          <div class="text-center mb-8">
            <img :src="getUserAvatar(currentUser)" :alt="getUserDisplayName(currentUser)" class="w-16 h-16 rounded-full mx-auto mb-2 border-2 border-indigo-400">
            <h3 :class="isDark?'text-white':'text-gray-900'" class="font-semibold">{{ getUserDisplayName(currentUser) }}</h3>
            <p :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">{{ getUserRole(currentUser) }} · 欢迎回来</p>
          </div>

          <!-- 搜索框 -->
          <div class="relative mb-6">
            <input
              v-model="searchQuery"
              @input="searchMessages"
              type="text"
              placeholder="搜索消息标题、内容或发布人..."
              :class="[
                'w-full px-4 py-3 pl-10 border rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all duration-300',
                isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900'
              ]"
            >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-3 top-3.5" :class="isDark?'text-white/50':'text-gray-400'" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
            </svg>
          </div>

          <!-- 分类菜单 -->
          <nav class="space-y-2">
            <button
              v-for="category in categories"
              :key="category.id"
              @click="activeCategory = category.id"
              :class="[
                'w-full flex items-center justify-between p-4 rounded-xl transition-all duration-200 group',
                activeCategory === category.id
                  ? `bg-indigo-500 text-white shadow-lg`
                  : isDark
                    ? 'text-white/70 hover:bg-white/5 hover:text-white'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              ]"
            >
              <div class="flex items-center space-x-3">
                <div class="text-xl transition-transform duration-200 group-hover:scale-110">
                  {{ category.icon }}
                </div>
                <span class="font-medium">{{ category.name }}</span>
              </div>
              <span
                :class="[
                  'px-2 py-1 rounded-full text-xs font-medium min-w-8 text-center transition-colors',
                  activeCategory === category.id
                    ? 'bg-white/20 text-white'
                    : isDark
                      ? 'bg-white/10 text-white/70'
                      : 'bg-gray-100 text-gray-600'
                ]"
              >
                {{ category.count }}
              </span>
            </button>
          </nav>

          <!-- 统计信息 -->
          <div class="mt-8 pt-6 border-t" :class="isDark?'border-white/10':'border-gray-200'">
            <div class="grid grid-cols-2 gap-4 text-center">
              <div>
                <div :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">{{ categories.find(c => c.id === 'unread')?.count || 0 }}</div>
                <div :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">未读消息</div>
              </div>
              <div>
                <div :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">{{ categories.find(c => c.id === 'important')?.count || 0 }}</div>
                <div :class="isDark?'text-white/60':'text-gray-500'" class="text-sm">重要消息</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧消息列表 -->
      <div class="flex-1">
        <div :class="isDark?'bg-black border border-white/50':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="rounded-2xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1 min-h-[600px]">
          <!-- 列表头部 -->
          <div class="flex items-center justify-between p-6 border-b" :class="isDark?'border-white/10':'border-gray-200'">
            <div>
              <h2 :class="isDark?'text-white':'text-gray-900'" class="text-xl font-semibold">
                {{ categories.find(c => c.id === activeCategory)?.name }}
              </h2>
              <p :class="isDark?'text-white/60':'text-gray-500'" class="text-sm mt-1">
                共 {{ filteredMessages.length }} 条消息
              </p>
            </div>
            <button
              @click="fetchMessages"
              class="px-4 py-2 rounded-lg transition-colors flex items-center"
              :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
              </svg>
              刷新
            </button>
          </div>

          <!-- 消息列表 -->
          <div class="p-6 space-y-4 max-h-[500px] overflow-y-auto custom-scrollbar">
            <!-- 加载状态 -->
            <div v-if="loading" class="text-center py-12">
              <div class="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-indigo-500 mx-auto mb-4"></div>
              <span :class="isDark?'text-white/50':'text-gray-500'">正在加载消息数据...</span>
            </div>

            <!-- 消息项 -->
            <div
              v-for="message in filteredMessages"
              :key="message.id"
              @click="viewMessage(message)"
              :class="[
                'p-4 rounded-xl border transition-all duration-200 cursor-pointer group transform hover:-translate-x-1',
                isDark ? 'border-white/10 hover:border-indigo-400/30' : 'border-gray-200 hover:border-indigo-300',
                message.read ? 'opacity-70' : 'shadow-sm',
                !message.read && 'bg-indigo-50/50 dark:bg-indigo-500/10'
              ]"
            >
              <div class="flex items-start space-x-4">
                <!-- 消息图标 -->
                <div class="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center text-white" :class="getNoticeTypeColorClass(message.type)">
                  {{ categories.find(c => c.id === message.type)?.icon || '📢' }}
                </div>

                <!-- 消息内容 -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex items-center space-x-3">
                      <h3
                        :class="[
                          'font-semibold truncate',
                          isDark ? 'text-white' : 'text-gray-900',
                          !message.read && 'font-bold'
                        ]"
                      >
                        {{ message.title }}
                      </h3>
                      <span v-if="!message.read" class="w-2 h-2 bg-red-500 rounded-full animate-pulse flex-shrink-0"></span>
                    </div>
                    <div class="flex items-center space-x-2 flex-shrink-0">
                      <span
                        class="text-xs px-2 py-1 rounded-full border"
                        :class="[
                          message.priority === '紧急' ? 'text-red-400 border-red-400/30' :
                          message.priority === '高优先级' ? 'text-orange-400 border-orange-400/30' :
                          message.priority === '中优先级' ? 'text-blue-400 border-blue-400/30' :
                          'text-gray-400 border-gray-400/30'
                        ]"
                      >
                        {{ message.priority }}
                      </span>
                      <span class="text-xs" :class="isDark?'text-white/50':'text-gray-500'">
                        {{ formatTime(message.time) }}
                      </span>
                    </div>
                  </div>

                  <p class="text-sm mb-3 line-clamp-2" :class="isDark?'text-white/70':'text-gray-600'">
                    {{ message.content }}
                  </p>

                  <div class="flex items-center justify-between">
                    <div class="flex items-center space-x-4">
                      <!-- 发布人信息 -->
                      <div class="flex items-center space-x-2">
                        <img
                          :src="getUserAvatar(message.user)"
                          :alt="getUserDisplayName(message.user)"
                          class="w-6 h-6 rounded-full border"
                        >
                        <span class="text-xs" :class="isDark?'text-white/60':'text-gray-500'">
                          {{ getUserDisplayName(message.user) }}
                        </span>
                        <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 dark:bg-white/10" :class="isDark?'text-white/50':'text-gray-500'">
                          {{ getUserRole(message.user) }}
                        </span>
                      </div>

                      <div class="flex items-center space-x-4 text-xs" :class="isDark?'text-white/50':'text-gray-500'">
                        <span class="flex items-center space-x-1">
                          <span>{{ getReceiveIcon(message.receive) }}</span>
                          <span>{{ message.receive }}</span>
                        </span>
                        <span>类型: {{ message.type }}</span>
                      </div>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="flex items-center space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        class="p-2 rounded-lg transition-colors"
                        :class="isDark ? 'hover:bg-white/10' : 'hover:bg-gray-100'"
                        title="标记重要"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :class="isDark ? 'text-white/50' : 'text-gray-400'" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredMessages.length === 0 && !loading" class="text-center py-16">
              <div class="w-24 h-24 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-gray-200 to-gray-300 dark:from-white/10 dark:to-white/5 flex items-center justify-center">
                <span class="text-4xl">📭</span>
              </div>
              <h3 class="text-xl font-semibold mb-2" :class="isDark?'text-white':'text-gray-900'">暂无消息</h3>
              <p :class="isDark?'text-white/60':'text-gray-500'">当前分类下还没有任何消息</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息详情模态框 -->
    <Modal
      v-model:open="showDetailModal"
      title="消息详情"
      width="600px"
      :footer="null"
      @cancel="showDetailModal = false"
    >
      <div v-if="selectedMessage" class="p-4">
        <div
          class="p-5 rounded-xl border-l-4 mb-4"
          :class="[
            getNoticeTypeStyle(selectedMessage.type).borderColor,
            getNoticeTypeStyle(selectedMessage.type).bgColor,
            getNoticeTypeStyle(selectedMessage.type).darkBgColor
          ]"
        >
          <div class="flex items-start justify-between mb-3">
            <h4 class="text-lg font-semibold">{{ selectedMessage.title }}</h4>
            <span
              class="text-xs px-2 py-1 rounded-full text-white"
              :class="getNoticeTypeColorClass(selectedMessage.type)"
            >
              {{ selectedMessage.type }}
            </span>
          </div>
          <div class="mb-4 whitespace-pre-line">
            {{ selectedMessage.content }}
          </div>
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-500 dark:text-gray-400">
            <!-- 发布人信息 -->
            <div class="flex items-center space-x-2">
              <img
                :src="getUserAvatar(selectedMessage.user)"
                :alt="getUserDisplayName(selectedMessage.user)"
                class="w-6 h-6 rounded-full border"
              >
              <span>发布人: {{ getUserDisplayName(selectedMessage.user) }}</span>
              <span class="px-1.5 py-0.5 rounded bg-gray-100 dark:bg-white/10 text-xs">
                {{ getUserRole(selectedMessage.user) }}
              </span>
            </div>
            <span>优先级: {{ selectedMessage.priority }}</span>
            <span>接收对象: {{ getReceiveIcon(selectedMessage.receive) }} {{ selectedMessage.receive }}</span>
            <span>发布时间: {{ formatDate(selectedMessage.time) }}</span>
          </div>
        </div>
        <div class="flex justify-end gap-2">
          <button
            @click="showDetailModal = false"
            class="px-4 py-2 border border-gray-300 rounded-lg transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.8);
}

/* 深色模式滚动条 */
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
}

.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
