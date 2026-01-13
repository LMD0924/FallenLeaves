<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import webSocketService from '@/net/websocket.js'
import { useNotification } from '@/services/notificationService.js'

const router = useRouter()
const { initNotificationService, cleanupNotificationService } = useNotification()

// 全局 WebSocket 处理器
const globalWsHandlerId = 'global-notification'

const initGlobalWebSocket = () => {
  // 先注销可能存在的旧处理器，避免重复注册
  webSocketService.unregisterHandler(globalWsHandlerId)

  const handlers = {
    onConnected: () => {
    },

    onDisconnected: () => {
      console.log('全局WebSocket连接断开')
    },

    // 全局处理器专注于系统级消息，新消息通知主要由ExamSidebar处理
    system_message: (systemData) => {
      console.log('收到系统消息:', systemData)
      showGlobalNotification({
        title: '系统提示',
        content: systemData.message || '系统状态更新'
      })
    },

    // 其他消息处理器...
  }

  webSocketService.registerHandler(globalWsHandlerId, handlers)
}

const showGlobalNotification = (noticeData) => {
  // 使用浏览器的 Notification API 或自定义通知
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(noticeData.title, {
      body: noticeData.content,
      icon: '/favicon.ico'
    })
  }
}

// 请求通知权限
const requestNotificationPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().then(permission => {
    })
  }
}

onMounted(() => {
  // 请求通知权限
  requestNotificationPermission()

  // 初始化全局 WebSocket
  initGlobalWebSocket()

  // 监听路由变化，在登录后重新连接 WebSocket
  router.afterEach((to, from) => {
    // 如果是从登录页面跳转过来，确保WebSocket处理器已注册
    if (from.path === '/') {
      setTimeout(() => {
        initGlobalWebSocket() // 确保处理器已正确注册
        if (!webSocketService.isConnected) {
          webSocketService.connect()
        }
      }, 500)
    }
  })
})

onUnmounted(() => {
  webSocketService.unregisterHandler(globalWsHandlerId)
  cleanupNotificationService()
})
</script>

<template>


  <RouterView />
</template>

<style scoped>

</style>

