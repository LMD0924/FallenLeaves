<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { get, post } from "@/net/index.js"
import { message, Modal } from "ant-design-vue"
import { isDark } from "@/stores/theme.js"
import { formatDate } from "@/time/Data.js"

const [messageApi, contextHolder] = message.useMessage()

// 当前用户
const currentUser = ref({})
const isLoading = ref(false)

// 对话框状态
const showPublishDialog = ref(false)
const showPreviewDialog = ref(false)
const selectedNotice = ref(null)

// 通知表单数据
const noticeForm = reactive({
  title: '',
  content: '',
  type: '一般通知',
  priority: '中优先级',
  receive: '全体师生',
  time: new Date().toISOString()
})

// 通知类型选项
const noticeTypes = [
  { value: '一般通知', label: '一般通知', color: 'blue', icon: '📢' },
  { value: '重要通知', label: '重要通知', color: 'orange', icon: '⚠️' },
  { value: '紧急通知', label: '紧急通知', color: 'red', icon: '🚨' },
  { value: '系统维护', label: '系统维护', color: 'purple', icon: '🔧' },
  { value: '政策公告', label: '政策公告', color: 'green', icon: '📜' },
  { value: '活动通知', label: '活动通知', color: 'pink', icon: '🎉' }
]

// 优先级选项
const priorityOptions = [
  { value: '低优先级', label: '低优先级', color: 'gray' },
  { value: '中优先级', label: '中优先级', color: 'blue' },
  { value: '高优先级', label: '高优先级', color: 'orange' },
  { value: '紧急', label: '紧急', color: 'red' }
]

// 接收对象选项
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

// 最近通知列表
const recentNotices = ref([])
const totalNoticesCount = ref(0)

// 处理内容输入
const handleContentInput = (event) => {
  // 如果超过255字符，自动截断
  if (noticeForm.content.length > 255) {
    noticeForm.content = noticeForm.content.substring(0, 255)
    messageApi.warning('消息内容已超过最大限制，自动截断')
  }
}

// 获取字符计数样式
const getCounterClass = () => {
  const length = noticeForm.content.length
  if (length >= 255) {
    return 'text-red-500'
  } else if (length >= 240) {
    return 'text-orange-500'
  } else {
    return isDark ? 'text-white/50' : 'text-gray-500'
  }
}

// 获取内容长度提示样式
const getContentLengthClass = () => {
  const length = noticeForm.content.length
  if (length >= 255) {
    return 'text-red-500 font-medium'
  } else if (length >= 240) {
    return 'text-orange-500'
  } else {
    return isDark ? 'text-white/50' : 'text-gray-500'
  }
}

// 获取进度条样式
const getProgressBarClass = () => {
  const length = noticeForm.content.length
  if (length >= 255) {
    return 'bg-red-500'
  } else if (length >= 240) {
    return 'bg-orange-500'
  } else {
    return 'bg-indigo-500'
  }
}

// 获取进度条宽度
const getProgressWidth = () => {
  const percentage = (noticeForm.content.length / 255) * 100
  return `${Math.min(percentage, 100)}%`
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
    // 设置默认用户信息
    currentUser.value = { name: '测试用户', account: 'test' }
  }
}

// 获取最近通知
const fetchRecentNotices = async () => {
  isLoading.value = true
  try {
    const data = await new Promise((resolve, reject) => {
      get('api/notice/SelectAllNotice', {}, (message, data) => {
        resolve(data || [])
      }, (error) => {
        reject(error)
      })
    })
    console.log("消息通知：",data)
    totalNoticesCount.value = data.length
    // 只显示最近5条通知，按时间倒序
    recentNotices.value = data
      .sort((a, b) => new Date(b.time) - new Date(a.time))
      .slice(0, 5)
  } catch (error) {
    console.error('获取通知列表失败:', error)
    messageApi.error('获取通知列表失败')
  } finally {
    isLoading.value = false
  }
}

// 发布通知
const publishNotice = async () => {
  if (!validateForm()) {
    return
  }

  isLoading.value = true
  try {
    await new Promise((resolve, reject) => {
      post('api/notice/InsertNotice', {
        ...noticeForm,
        userId: currentUser.value.id,
        time: new Date().toISOString()
      }, (msg) => {
        resolve(msg)
      }, (error) => {
        reject(error)
      })
    })

    messageApi.success('发布成功')
    closePublishDialog()
    await fetchRecentNotices() // 刷新列表
  } catch (error) {
    messageApi.error('发布失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 预览通知
const previewNotice = () => {
  if (!validateForm()) {
    // 预览时也进行基本验证，但只提示不阻止
    messageApi.info('建议填写完整的标题和内容以获得更好的预览效果')
  }

  selectedNotice.value = {
    ...noticeForm,
    userId: currentUser.value.id,
    time: new Date().toISOString()
  }
  showPreviewDialog.value = true
}

// 验证表单
const validateForm = () => {
  if (!noticeForm.title?.trim()) {
    messageApi.error('请输入通知标题')
    return false
  }

  if (!noticeForm.content?.trim()) {
    messageApi.error('请输入通知内容')
    return false
  }

  if (noticeForm.content.length > 255) {
    messageApi.error('消息内容不能超过255个字符')
    return false
  }

  if (!noticeForm.receive) {
    messageApi.error('请选择接收对象')
    return false
  }

  return true
}

// 关闭发布对话框
const closePublishDialog = () => {
  showPublishDialog.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  noticeForm.title = ''
  noticeForm.content = ''
  noticeForm.type = '一般通知'
  noticeForm.priority = '中优先级'
  noticeForm.receive = '全体师生'
}

// 关闭预览对话框
const closePreviewDialog = () => {
  showPreviewDialog.value = false
  selectedNotice.value = null
}

// 获取通知类型的样式
const getNoticeTypeStyle = (type) => {
  const typeInfo = noticeTypes.find(t => t.value === type)
  return {
    borderColor: `border-l-${typeInfo?.color || 'blue'}-500`,
    bgColor: `bg-${typeInfo?.color || 'blue'}-50`,
    darkBgColor: `dark:bg-${typeInfo?.color || 'blue'}-900/20`
  }
}

// 获取通知类型的颜色类
const getNoticeTypeColorClass = (type) => {
  const typeInfo = noticeTypes.find(t => t.value === type)
  return `bg-${typeInfo?.color || 'blue'}-500`
}

// 获取接收对象的图标
const getReceiveIcon = (receiveValue) => {
  const receiveInfo = receiveOptions.find(r => r.value === receiveValue)
  return receiveInfo?.icon || '👥'
}

// 监听表单变化，自动保存草稿
watch(
  () => [noticeForm.title, noticeForm.content],
  ([newTitle, newContent]) => {
    // 可以在这里实现草稿保存逻辑
    if (newTitle || newContent) {
      // 模拟保存草稿
      // localStorage.setItem('notice_draft', JSON.stringify(noticeForm))
    }
  }
)

// 初始化加载
onMounted(async () => {
  // 先获取用户信息
  await getCurrentUser()
  // 再获取通知列表
  await fetchRecentNotices()
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
          <h1 :class="isDark?'text-white':'text-gray-900'" class="text-2xl font-bold">消息发布系统</h1>
          <p :class="isDark?'text-white/70':'text-gray-600'" class="text-sm">快速发布和管理通知消息</p>
        </div>
      </div>
      <div class="flex items-center space-x-4">
        <div class="flex items-center space-x-2 px-3 py-1 rounded-full" :class="isDark?'bg-white/5':'bg-gray-100'">
          <div class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
          <span :class="isDark?'text-white':'text-gray-700'" class="text-sm">{{ currentUser.name || currentUser.account || '访客' }}</span>
        </div>
        <button
          @click="showPublishDialog = true"
          :disabled="isLoading"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 rounded-lg text-white transition-all duration-300 flex items-center disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          发布消息
        </button>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="flex-1 p-6">
      <div class="max-w-6xl mx-auto">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <!-- 左侧：发布表单 -->
          <div class="lg:col-span-2">
            <div :class="isDark?'bg-black border border-white/50':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="shadow-lg p-6 md:p-8 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1 rounded-xl">
              <h2 class="text-xl font-bold mb-6 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 mr-2 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                发布新消息
              </h2>

              <!-- 消息表单 -->
              <div class="space-y-6">
                <!-- 标题 -->
                <div>
                  <label class="block text-sm font-medium mb-2">
                    消息标题 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="noticeForm.title"
                    type="text"
                    placeholder="请输入消息标题"
                    class="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all duration-300 focus:outline-none"
                    :class="isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900'"
                    @keyup.enter="publishNotice"
                  >
                </div>

                <!-- 类型、优先级和接收对象 -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <!-- 消息类型 -->
                  <div>
                    <label class="block text-sm font-medium mb-2">
                      消息类型
                    </label>
                    <select
                      v-model="noticeForm.type"
                      class="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all duration-300 focus:outline-none"
                      :class="isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900'"
                    >
                      <option v-for="type in noticeTypes" :key="type.value" :value="type.value">
                        {{ type.icon }} {{ type.label }}
                      </option>
                    </select>
                  </div>

                  <!-- 优先级 -->
                  <div>
                    <label class="block text-sm font-medium mb-2">
                      优先级
                    </label>
                    <select
                      v-model="noticeForm.priority"
                      class="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all duration-300 focus:outline-none"
                      :class="isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900'"
                    >
                      <option v-for="priority in priorityOptions" :key="priority.value" :value="priority.value">
                        {{ priority.label }}
                      </option>
                    </select>
                  </div>

                  <!-- 接收对象 -->
                  <div>
                    <label class="block text-sm font-medium mb-2">
                      接收对象 <span class="text-red-500">*</span>
                    </label>
                    <select
                      v-model="noticeForm.receive"
                      class="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all duration-300 focus:outline-none"
                      :class="isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900'"
                    >
                      <option v-for="receive in receiveOptions" :key="receive.value" :value="receive.value">
                        {{ receive.icon }} {{ receive.label }}
                      </option>
                    </select>
                  </div>
                </div>

                <!-- 内容 -->
                <div>
                  <label class="block text-sm font-medium mb-2">
                    消息内容 <span class="text-red-500">*</span>
                    <span class="text-xs font-normal ml-2" :class="isDark?'text-white/50':'text-gray-500'">
      (最多255个字符)
    </span>
                  </label>
                  <textarea
                    v-model="noticeForm.content"
                    rows="8"
                    placeholder="请输入消息详细内容...（最多255个字符）"
                    maxlength="255"
                    class="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all resize-vertical focus:outline-none"
                    :class="[
      isDark ? 'bg-black border-white/20 text-white' : 'bg-white border-gray-200 text-gray-900',
      noticeForm.content.length >= 240 ? 'border-orange-500' : '',
      noticeForm.content.length >= 255 ? 'border-red-500' : ''
    ]"
                    @input="handleContentInput"
                  ></textarea>

                  <!-- 字符计数显示 -->
                  <div class="flex justify-between items-center mt-2">
                    <div class="text-xs" :class="getContentLengthClass()">
      <span v-if="noticeForm.content.length >= 255" class="flex items-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
        </svg>
        已达到最大字符限制
      </span>
                      <span v-else-if="noticeForm.content.length >= 240" class="flex items-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1 text-orange-500" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
        </svg>
        接近字符限制，请缩短内容
      </span>
                    </div>
                    <div class="text-xs font-medium" :class="getCounterClass()">
                      {{ noticeForm.content.length }}/255
                    </div>
                  </div>

                  <!-- 进度条 -->
                  <div class="w-full rounded-full h-1.5 mt-1">
                    <div
                      class="h-1.5 rounded-full transition-all duration-300"
                      :class="getProgressBarClass()"
                      :style="{ width: getProgressWidth() }"
                    ></div>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex gap-4 pt-4">
                  <button
                    @click="previewNotice"
                    :disabled="isLoading"
                    class="flex-1 px-6 py-3 rounded-xl transition-all duration-300 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 flex items-center justify-center disabled:opacity-60 disabled:cursor-not-allowed"
                    :class="isDark ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-white' : 'bg-gray-100 hover:bg-gray-200 border border-gray-300 text-gray-700'"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                      <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                    </svg>
                    预览消息
                  </button>
                  <button
                    @click="publishNotice"
                    :disabled="isLoading"
                    class="flex-1 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 rounded-xl text-white transition-all duration-300 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 flex items-center justify-center disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    <svg v-if="!isLoading" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {{ isLoading ? '发布中...' : '立即发布' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：最近消息 -->
          <div class="space-y-6">
            <!-- 消息预览 -->
            <div :class="isDark?'bg-black border border-white/50':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="rounded-2xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <h3 class="text-lg font-bold mb-4">
                消息预览
              </h3>

              <div
                class="p-4 rounded-xl border-l-4 transition-all duration-300 animate-fadeIn"
                :class="[
                  getNoticeTypeStyle(noticeForm.type).borderColor,
                  getNoticeTypeStyle(noticeForm.type).bgColor,
                  getNoticeTypeStyle(noticeForm.type).darkBgColor
                ]"
              >
                <div class="flex items-start justify-between mb-2">
                  <h4 class="font-semibold line-clamp-1">{{ noticeForm.title || '消息标题' }}</h4>
                  <span
                    class="text-xs px-2 py-1 rounded-full text-white"
                    :class="getNoticeTypeColorClass(noticeForm.type)"
                  >
                    {{ noticeForm.type || '一般通知' }}
                  </span>
                </div>
                <p class="text-sm mb-3 line-clamp-3">
                  {{ noticeForm.content || '消息内容将在这里显示...' }}
                </p>
                <div class="flex flex-wrap items-center gap-2 text-xs">
                  <span>优先级: {{ noticeForm.priority || '中优先级' }}</span>
                  <span>接收: {{ noticeForm.receive || '全体师生' }}</span>
                  <span class="flex-grow"></span>
                  <span>发布人: {{ currentUser.name || currentUser.account || '当前用户' }}</span>
                </div>
              </div>
            </div>

            <!-- 最近消息 -->
            <div :class="isDark?'bg-black border border-white/50':'bg-gradient-to-br from-blue-50 to-indigo-100'" class="rounded-2xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-bold">
                  最近我的发布
                </h3>
                <span v-if="totalNoticesCount > 5" class="text-xs px-2 py-1 rounded-full bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-300">
                  共 {{ totalNoticesCount }} 条
                </span>
              </div>

              <div class="space-y-4 max-h-96 overflow-y-auto pr-2 custom-scrollbar">
                <div v-if="isLoading" class="flex justify-center py-8">
                  <div class="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-indigo-500"></div>
                </div>

                <template v-else>
                  <div
                    v-for="notice in recentNotices"
                    :key="notice.id"
                     >
                    <div v-if="currentUser.id===notice.userId" class="p-4 rounded-lg border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-300 cursor-pointer transform hover:-translate-x-1"
                    >
                      <div class="flex items-start justify-between mb-2">
                        <h4 class="font-medium text-sm line-clamp-1">{{ notice.title }}</h4>
                        <span
                          class="text-xs px-2 py-1 rounded-full text-white shrink-0 ml-2"
                          :class="getNoticeTypeColorClass(notice.type)"
                        >
                        {{ notice.type }}
                      </span>
                      </div>
                      <p class="text-xs line-clamp-2 mb-2">{{ notice.content }}</p>
                      <div class="flex items-center justify-between text-xs">
                        <span>{{ getReceiveIcon(notice.receive) }} {{ notice.receive || '全体师生' }}</span>
                        <span>{{ formatDate(notice.time) }}</span>
                      </div>
                    </div>
                  </div>

                  <div v-if="recentNotices.length === 0" class="text-center py-8">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-2 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9m0 0l2 2M9 5l2-2" />
                    </svg>
                    <p class="text-sm">暂无消息记录</p>
                    <button
                      @click="showPublishDialog = true"
                      class="mt-3 px-3 py-1 text-xs text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300"
                    >
                      发布第一条消息
                    </button>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 预览对话框 -->
    <Modal
      v-model:open="showPreviewDialog"
      title="消息预览"
      width="600px"
      :footer="null"
      @cancel="closePreviewDialog"
    >
      <div v-if="selectedNotice" class="p-4">
        <div
          class="p-5 rounded-xl border-l-4 mb-4"
          :class="[
            getNoticeTypeStyle(selectedNotice.type).borderColor,
            getNoticeTypeStyle(selectedNotice.type).bgColor,
            getNoticeTypeStyle(selectedNotice.type).darkBgColor
          ]"
        >
          <div class="flex items-start justify-between mb-3">
            <h4 class="text-lg font-semibold text-gray-900 dark:text-gray-100">{{ selectedNotice.title }}</h4>
            <span
              class="text-xs px-2 py-1 rounded-full text-white"
              :class="getNoticeTypeColorClass(selectedNotice.type)"
            >
              {{ selectedNotice.type }}
            </span>
          </div>
          <div class="text-gray-600 dark:text-gray-300 mb-4 whitespace-pre-line">
            {{ selectedNotice.content }}
          </div>
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-500 dark:text-gray-400">
            <span>优先级: {{ selectedNotice.priority }}</span>
            <span>接收对象: {{ getReceiveIcon(selectedNotice.receive) }} {{ selectedNotice.receive }}</span>
            <span>发布人: {{ selectedNotice.author }}</span>
            <span>发布时间: {{ formatDate(selectedNotice.publishTime) }}</span>
          </div>
        </div>
        <div class="flex justify-end gap-2">
          <button
            @click="closePreviewDialog"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          >
            关闭
          </button>
          <button
            @click="publishNotice"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors"
          >
            立即发布
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.line-clamp-1 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.line-clamp-2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.line-clamp-3 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.resize-vertical {
  resize: vertical;
}

/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.5);
  border-radius: 20px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(156, 163, 175, 0.8);
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fadeIn {
  animation: fadeIn 0.3s ease-out;
}

/* 深色模式下的过渡 */
.dark-transition {
  transition: background-color 0.3s, color 0.3s, border-color 0.3s;
}
</style>
