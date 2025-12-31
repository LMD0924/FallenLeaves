// @/net/websocket.js
class WebSocketService {
  constructor() {
    this.socket = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000
    this.messageHandlers = new Map()
    this.isConnected = false
  }

  connect() {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/ws/notifications`

      this.socket = new WebSocket(wsUrl)

      this.socket.onopen = () => {
        console.log('WebSocket连接已建立')
        this.isConnected = true
        this.reconnectAttempts = 0
        this.onConnected()
      }

      this.socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          this.handleMessage(message)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }

      this.socket.onclose = (event) => {
        console.log('WebSocket连接已关闭:', event.code, event.reason)
        this.isConnected = false
        this.onDisconnected()
        this.handleReconnect()
      }

      this.socket.onerror = (error) => {
        console.error('WebSocket错误:', error)
        this.isConnected = false
      }

    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.handleReconnect()
    }
  }

  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      const delay = this.reconnectInterval * this.reconnectAttempts
      console.log(`尝试重新连接... (${this.reconnectAttempts}/${this.maxReconnectAttempts})，延迟${delay}ms`)

      this.reconnectTimer = setTimeout(() => {
        // 确保在重新连接前，前一个socket已经关闭
        if (this.socket) {
          try {
            this.socket.close()
          } catch (e) {
            console.warn('关闭旧socket时出错:', e)
          }
        }
        this.connect()
      }, delay)
    } else {
      console.error('WebSocket重连次数已达上限，请刷新页面重试')
    }
  }

  onConnected() {
    console.log('WebSocket连接已完全建立，当前处理器数量:', this.messageHandlers.size)
    
    // 清除可能存在的重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    
    // 发送心跳
    this.startHeartbeat()

    // 通知所有处理器连接已建立
    this.messageHandlers.forEach((handler, handlerId) => {
      if (handler.onConnected) {
        try {
          console.log(`通知处理器 ${handlerId} 连接已建立`)
          handler.onConnected()
        } catch (error) {
          console.error(`处理器 ${handlerId} 的onConnected回调执行失败:`, error)
        }
      }
    })
  }

  onDisconnected() {
    // 停止心跳
    this.stopHeartbeat()

    // 通知所有处理器连接已断开
    this.messageHandlers.forEach(handler => {
      if (handler.onDisconnected) {
        handler.onDisconnected()
      }
    })
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected) {
        this.send('ping')
      }
    }, 30000) // 30秒发送一次心跳
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
    }
  }

  send(message) {
    if (this.isConnected && this.socket) {
      if (typeof message === 'object') {
        this.socket.send(JSON.stringify(message))
      } else {
        this.socket.send(message)
      }
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  handleMessage(message) {
    try {
      const { type, data } = message
      console.log(`收到WebSocket消息: 类型=${type}`)

      // 处理特定类型的消息
      let messageHandled = false
      this.messageHandlers.forEach((handler, handlerId) => {
        if (handler[type]) {
          try {
            handler[type](data)
            messageHandled = true
          } catch (error) {
            console.error(`处理器 ${handlerId} 处理消息类型 ${type} 失败:`, error)
          }
        } else if (handler.default) {
          try {
            handler.default(message)
            messageHandled = true
          } catch (error) {
            console.error(`处理器 ${handlerId} 的default处理器执行失败:`, error)
          }
        }
      })
      
      // 如果消息未被任何处理器处理，记录日志
      if (!messageHandled && type !== 'pong' && type !== 'connected') {
        console.log(`未处理的WebSocket消息类型: ${type}`)
      }
    } catch (error) {
      console.error('处理WebSocket消息时发生错误:', error)
    }
  }

  // 注册消息处理器
  registerHandler(handlerId, handlers) {
    this.messageHandlers.set(handlerId, handlers)
  }

  // 注销消息处理器
  unregisterHandler(handlerId) {
    this.messageHandlers.delete(handlerId)
  }

  disconnect() {
    console.log('主动断开WebSocket连接')
    this.stopHeartbeat()
    
    // 清除重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    
    if (this.socket) {
      try {
        this.socket.close(1000, '正常关闭')
      } catch (error) {
        console.warn('关闭socket时出错:', error)
      }
      this.socket = null
    }
    this.isConnected = false
    this.reconnectAttempts = 0
  }
}

// 创建单例实例
const webSocketService = new WebSocketService()

export default webSocketService
