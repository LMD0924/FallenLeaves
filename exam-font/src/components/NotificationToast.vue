<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
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

// 消息数据
const messages = ref([])

// 通知队列
const notificationQueue = ref([])
const activeNotifications = ref([])
const maxNotifications = 3

// WebSocket 处理器 ID
const wsHandlerId = 'notice-center'

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

// 获取消息列表
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

    messages.value = data.map(msg => ({
      id: msg.id,
      title: msg.title,
      content: msg.content,
      type: msg.type,
      priority: msg.priority,
      receive: msg.receive,
      time: msg.time,
      userId: msg.userId,
      user: msg.user || {
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

// 其他方法保持不变...
const updateCategoryCounts = () => { /* ... */ }
const filteredMessages = computed(() => { /* ... */ })
const viewMessage = (message) => { /* ... */ }
const getNoticeTypeStyle = (type) => { /* ... */ }
const getNoticeTypeColorClass = (type) => { /* ... */ }
const getReceiveIcon = (receiveValue) => { /* ... */ }
const formatTime = (timestamp) => { /* ... */ }

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
  <div>
    <contextHolder />

    <!-- 通知弹窗容器 -->
    <div class="fixed top-4 right-4 z-50 space-y-2">
      <NotificationToast
        v-for="notification in activeNotifications"
        :key="notification.id"
        :notification="notification"
        :duration="5000"
        @close="removeNotification"
      />
    </div>

    <!-- 原有的页面内容 -->
    <div :class="isDark?'bg-black':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="min-h-screen transition-colors duration-300">
      <!-- ... 原有的页面结构 -->
    </div>
  </div>
</template>
