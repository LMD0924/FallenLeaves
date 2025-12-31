// @/services/notificationService.js
import { ref } from 'vue'

// 全局通知状态
const notificationQueue = ref([])
const activeNotifications = ref([])
const maxNotifications = 3
// 移除 isInitialized 限制

export const useNotification = () => {
  // 添加通知到队列
  const addNotification = (notification) => {
    notificationQueue.value.push(notification)
    processNotificationQueue()
  }

  // 移除通知
  const removeNotification = (notificationId) => {
    const index = activeNotifications.value.findIndex(n => n.id === notificationId)
    if (index > -1) {
      activeNotifications.value.splice(index, 1)
    }
    processNotificationQueue()
  }

  // 处理通知队列
  const processNotificationQueue = () => {
    while (activeNotifications.value.length < maxNotifications && notificationQueue.value.length > 0) {
      const notification = notificationQueue.value.shift()
      activeNotifications.value.push(notification)
    }
  }

  // 初始化通知服务（每次登录都调用）
  const initNotificationService = () => {
    console.log('初始化全局通知服务')
    // 可以在这里添加其他初始化逻辑
  }

  // 清理通知服务（退出登录时调用）
  const cleanupNotificationService = () => {
    notificationQueue.value = []
    activeNotifications.value = []
    console.log('清理全局通知服务')
  }

  return {
    notificationQueue,
    activeNotifications,
    addNotification,
    removeNotification,
    initNotificationService,
    cleanupNotificationService
  }
}
