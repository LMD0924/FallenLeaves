<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { isDark } from '@/stores/theme.js'

const props = defineProps({
  notification: {
    type: Object,
    required: true
  },
  duration: {
    type: Number,
    default: 5000
  }
})

const emit = defineEmits(['close', 'click'])
const isVisible = ref(false)
const isHiding = ref(false)
const progress = ref(100)
let timer = null
let progressInterval = null

const handleClose = () => {
  isHiding.value = true
  setTimeout(() => {
    emit('close', props.notification.id)
  }, 400)
}

const handleClick = () => {
  emit('click', props.notification)
  handleClose()
}

const pauseProgress = () => {
  if (progressInterval) {
    clearInterval(progressInterval)
  }
}

const resumeProgress = () => {
  if (props.duration > 0) {
    startProgressTimer()
  }
}

const getNotificationTypeClass = (type) => {
  const typeMap = {
    'info': {
      bg: 'bg-gradient-to-r from-blue-500 to-blue-600',
      border: 'border-blue-400/30',
      glow: 'shadow-blue-500/25',
      icon: '🔵'
    },
    'success': {
      bg: 'bg-gradient-to-r from-emerald-500 to-green-600',
      border: 'border-emerald-400/30',
      glow: 'shadow-emerald-500/25',
      icon: '✅'
    },
    'warning': {
      bg: 'bg-gradient-to-r from-amber-500 to-orange-600',
      border: 'border-amber-400/30',
      glow: 'shadow-amber-500/25',
      icon: '⚠️'
    },
    'error': {
      bg: 'bg-gradient-to-r from-rose-500 to-red-600',
      border: 'border-rose-400/30',
      glow: 'shadow-rose-500/25',
      icon: '❌'
    }
  }
  return typeMap[type] || {
    bg: 'bg-gradient-to-r from-indigo-500 to-purple-600',
    border: 'border-indigo-400/30',
    glow: 'shadow-indigo-500/25',
    icon: '📢'
  }
}

const getProgressColor = (type) => {
  const colorMap = {
    'info': 'bg-blue-200',
    'success': 'bg-emerald-200',
    'warning': 'bg-amber-200',
    'error': 'bg-rose-200'
  }
  return colorMap[type] || 'bg-indigo-200'
}

const startProgressTimer = () => {
  if (props.duration > 0) {
    const stepTime = 50
    const steps = props.duration / stepTime
    const decrement = 100 / steps

    progressInterval = setInterval(() => {
      progress.value = Math.max(0, progress.value - decrement)
      if (progress.value <= 0) {
        handleClose()
      }
    }, stepTime)
  }
}

const notificationStyle = computed(() => {
  const typeClass = getNotificationTypeClass(props.notification.type || 'info')
  return {
    background: typeClass.bg,
    borderColor: typeClass.border,
    boxShadow: `0 10px 40px -10px ${typeClass.glow}, 0 0 20px -5px ${typeClass.glow}`
  }
})

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  setTimeout(() => {
    isVisible.value = true
  }, 50)

  if (props.duration > 0) {
    startProgressTimer()
  }
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
  if (progressInterval) clearInterval(progressInterval)
})
</script>

<template>
  <div
    :class="[
      'fixed top-4 right-4 z-500 w-96 transition-all duration-500 ease-out transform',
      'rounded-2xl border backdrop-blur-lg overflow-hidden cursor-pointer',
      'hover:scale-105 hover:shadow-2xl active:scale-95',
      isVisible ? 'translate-x-0 opacity-100' : 'translate-x-full opacity-0',
      isHiding ? 'translate-x-full opacity-0 scale-95' : '',
      isDark ? 'bg-white/5 text-white' : 'bg-white/90 text-gray-800'
    ]"
    :style="notificationStyle"
    @mouseenter="pauseProgress"
    @mouseleave="resumeProgress"
    @click="handleClick"
  >
    <!-- 进度条 -->
    <div class="absolute top-0 left-0 right-0 h-1 bg-black/10">
      <div
        class="h-full transition-all duration-100 ease-linear"
        :class="getProgressColor(notification.type || 'info')"
        :style="{ width: `${progress}%` }"
      ></div>
    </div>

    <div class="p-5">
      <!-- 头部 -->
      <div class="flex items-start justify-between mb-3">
        <div class="flex items-center space-x-3">
          <!-- 图标容器 -->
          <div class="relative">
            <div class="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center backdrop-blur-sm border border-white/30">
              <span class="text-lg">{{ getNotificationTypeClass(notification.type || 'info').icon }}</span>
            </div>
            <!-- 脉冲动画 -->
            <div class="absolute inset-0 rounded-xl bg-white/30 animate-ping opacity-75"></div>
          </div>

          <div>
            <h3 class="font-bold text-sm tracking-wide">
              {{ notification.title || '系统通知' }}
            </h3>
            <p class="text-xs  mt-0.5">
              {{ formatTime(notification.time) }}
            </p>
          </div>
        </div>

        <!-- 关闭按钮 -->
        <button
          @click.stop="handleClose"
          :class="[
            'w-8 h-8 rounded-full flex items-center justify-center transition-all duration-200',
            'active:bg-white/30 backdrop-blur-sm'
          ]"
          aria-label="关闭通知"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <!-- 内容 -->
      <div class="mb-4">
        <p class="text-sm leading-relaxed line-clamp-3">
          {{ notification.content }}
        </p>
      </div>

      <!-- 底部信息 -->
      <div class="flex items-center justify-between pt-3 border-t border-white/20">
        <div class="flex items-center space-x-2">
          <span class="text-xs px-2 py-1 rounded-full backdrop-blur-sm">
            {{ notification.type || 'info' }}
          </span>
          <span v-if="notification.priority" class="text-xs px-2 py-1 rounded-full bg-white/20 backdrop-blur-sm">
            {{ notification.priority }}
          </span>
        </div>
        <div class="text-xs text-white/60">
          点击查看详情
        </div>
      </div>
    </div>

    <!-- 装饰性元素 -->
    <div class="absolute top-0 left-0 w-20 h-20 bg-white/10 rounded-full -translate-x-10 -translate-y-10"></div>
    <div class="absolute bottom-0 right-0 w-16 h-16 bg-white/5 rounded-full translate-x-8 translate-y-8"></div>
  </div>
</template>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 自定义动画 */
@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}

/* 玻璃态效果增强 */
.backdrop-blur-lg {
  backdrop-filter: blur(20px);
}

/* 悬停效果增强 */
.hover-scale {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-scale:hover {
  transform: translateY(-2px) scale(1.02);
}
</style>