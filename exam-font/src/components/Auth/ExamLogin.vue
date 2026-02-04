<script setup>
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { message } from "ant-design-vue"
import { get, post } from "@/net/index.js"
import router from "@/router/index.js"
import webSocketService from '@/net/websocket.js'
import { useNotification } from '@/services/notificationService.js'

const [messageApi, contextHolder] = message.useMessage()
const { initNotificationService } = useNotification()

// ============ 状态管理 ============
const currentStep = ref(0) // 0: 选择登录方式, 1: 用户名密码登录, 2: 手机验证码登录, 3: 注册
const isRegister = ref(false)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)
const smsLoginLoading = ref(false)
const sendingCode = ref(false)

// 验证码倒计时
const countdown = ref(0)
let countdownTimer = null

// ============ 表单数据 ============
const loginMethod = ref('password') // 'password' 或 'sms'

// 用户名密码登录表单
const loginForm = reactive({
  username: '',
  password: '',
  role: '学生',
})

// 手机验证码登录表单
const smsLoginForm = reactive({
  phone: '',
  code: '',
  role: '学生',
})

// 注册表单数据
const registerForm = reactive({
  account: '',    // 账号（用户名）
  username: '',   // 姓名
  password: '',
  role: '学生',
  status: '未审核'
})

// ============ 计算属性 ============
const codeButtonText = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}秒后重发`
  }
  return sendingCode.value ? '发送中...' : '发送验证码'
})

const isCodeButtonDisabled = computed(() => {
  return sendingCode.value || countdown.value > 0 || !smsLoginForm.phone
})

const isPhoneValid = computed(() => {
  const phone = smsLoginForm.phone
  return /^1[3-9]\d{9}$/.test(phone)
})

// ============ 方法 ============
const selectLoginMethod = (method) => {
  loginMethod.value = method
  if (method === 'password') {
    currentStep.value = 1
  } else {
    currentStep.value = 2
  }
}

const goBack = () => {
  if (currentStep.value > 0) {
    currentStep.value = 0
  } else if (isRegister.value) {
    isRegister.value = false
  }
}

const toggleForm = () => {
  isRegister.value = !isRegister.value
  currentStep.value = 0
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!isPhoneValid.value) {
    messageApi.warning('请输入正确的手机号')
    return
  }

  sendingCode.value = true

  try {
    // 先检查验证码状态（使用后端接口）
    const statusResponse = await get(`/api/auth/status/${smsLoginForm.phone}`)
    if (statusResponse.code === 200) {
      if (statusResponse.data.canResend === false) {
        const waitTime = statusResponse.data.waitTime || 60
        messageApi.warning(`请等待${waitTime}秒后重发`)
        sendingCode.value = false
        return
      }
    }

    // 发送验证码（使用你的后端接口）
    const response = await post('/api/auth/sendCode', {
      phone: smsLoginForm.phone
    })

    if (response.code === 200) {
      messageApi.success('验证码发送成功')
      // 开始倒计时60秒
      startCountdown(60)

      // 开发环境下显示验证码
      if (response.data.code) {
        console.log('验证码：', response.data.code)
        smsLoginForm.code = response.data.code // 自动填充验证码
      }
    } else {
      messageApi.warning(response.message || '验证码发送失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    messageApi.error('网络错误，请稍后重试')
  } finally {
    sendingCode.value = false
  }
}

// 开始倒计时
const startCountdown = (seconds) => {
  countdown.value = seconds
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// 用户名密码登录（使用你的 ExamLogin 接口）
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    messageApi.warning('请输入账号和密码')
    return
  }

  loginLoading.value = true
  post('/api/auth/Login', {
  username: loginForm.username,
    password: loginForm.password,
    role: loginForm.role
},(message,data)=>{
    messageApi.success(message)
    handleLoginSuccess(data)
    loginLoading.value = false
  })
}

// 手机验证码登录（使用你的 phoneLogin 接口）
const handleSmsLogin = async () => {
  if (!isPhoneValid.value) {
    messageApi.warning('请输入正确的手机号')
    return
  }

  if (!smsLoginForm.code) {
    messageApi.warning('请输入验证码')
    return
  }

  smsLoginLoading.value = true

  try {
    const response = await post('/api/auth/phoneLogin', {
      phone: smsLoginForm.phone,
      code: smsLoginForm.code,
      role: smsLoginForm.role,
    })

    if (response.code === 200) {
      messageApi.success(response.message)
      handleLoginSuccess(response.data) // data 就是 token 字符串
    } else {
      messageApi.warning(response.message)
    }
  } catch (error) {
    console.error('登录失败:', error)
    messageApi.error('登录失败，请检查网络')
  } finally {
    smsLoginLoading.value = false
  }
}

// 登录成功处理
const handleLoginSuccess = (token) => {
  if (!token) {
    messageApi.error('登录失败：未获取到令牌')
    return
  }

  // 保存token
  localStorage.setItem('authToken', token)

  // 可以在这里解析token获取用户信息（如果有）
  try {
    // 如果你的JWT包含用户信息，可以解析
    const payload = JSON.parse(atob(token.split('.')[1]))
    localStorage.setItem('userInfo', JSON.stringify(payload))
  } catch (e) {
    console.warn('解析token失败，可能需要从其他接口获取用户信息')
  }

  // 初始化通知服务
  initNotificationService()

  // 延迟一下确保token已设置
  setTimeout(() => {
    webSocketService.connect()
  }, 500)

  // 跳转到首页
  setTimeout(() => {
    router.push('/ExamSidebar')
  }, 500)
}

// 注册逻辑（使用你的 ExamRegister 接口）
const handleRegister = async () => {
  if (!registerForm.account || !registerForm.username || !registerForm.password) {
    messageApi.warning('请填写所有必填项')
    return
  }

  registerLoading.value = true

  try {
    const response = await post('/api/auth/Register', registerForm)

    if (response.code === 200) {
      messageApi.success(response.message)
      // 清空表单
      registerForm.account = ''
      registerForm.username = ''
      registerForm.password = ''
      registerForm.role = '学生'
      registerForm.status = '未审核'

      // 切换到登录
      toggleForm()
    } else {
      messageApi.warning(response.message)
    }
  } catch (error) {
    console.error('注册失败:', error)
    messageApi.error('注册失败，请稍后重试')
  } finally {
    registerLoading.value = false
  }
}

// 监听手机号变化
watch(() => smsLoginForm.phone, (newPhone) => {
  if (newPhone && newPhone.length === 11 && !isPhoneValid.value) {
    // 可以在这里显示提示
    console.log('手机号格式不正确')
  }
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <contextHolder />
  <div class="bg-black relative min-h-screen flex items-center justify-center p-4">
    <div class="w-full max-w-4xl bg-white/10 backdrop-blur-md shadow-2xl rounded-2xl overflow-hidden flex border border-white/20">

      <!-- 左侧表单区域 -->
      <div class="w-1/2 p-8 transition-all duration-500 ease-in-out" :class="{
        'translate-x-full': isRegister,
        'translate-x-0': !isRegister
      }">

        <!-- 选择登录方式 -->
        <div v-if="currentStep === 0 && !isRegister" class="space-y-6">
          <div class="text-center">
            <h1 class="text-3xl font-bold text-white">教育管理平台</h1>
            <p class="text-gray-300 mt-2">请选择登录方式</p>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div
              @click="selectLoginMethod('password')"
              class="bg-white/10 border-2 border-transparent hover:border-blue-500/50 rounded-xl p-6 text-center cursor-pointer transition-all hover:scale-105 group"
            >
              <div class="w-16 h-16 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-300 mx-auto mb-4 group-hover:bg-blue-500/30 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-white mb-2">账号密码登录</h3>
              <p class="text-sm text-gray-300">管理员/教师/学生</p>
            </div>

            <div
              @click="selectLoginMethod('sms')"
              class="bg-white/10 border-2 border-transparent hover:border-green-500/50 rounded-xl p-6 text-center cursor-pointer transition-all hover:scale-105 group"
            >
              <div class="w-16 h-16 rounded-full bg-green-500/20 flex items-center justify-center text-green-300 mx-auto mb-4 group-hover:bg-green-500/30 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z" />
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-white mb-2">手机验证码登录</h3>
              <p class="text-sm text-gray-300">考生快速登录</p>
            </div>
          </div>

          <div class="text-center text-sm text-gray-300">
            还没有账户? <a @click="toggleForm" class="text-blue-400 hover:text-blue-300 cursor-pointer">立即注册</a>
          </div>
        </div>

        <!-- 用户名密码登录表单 -->
        <div v-else-if="currentStep === 1 && !isRegister" class="space-y-6">
          <div class="text-center">
            <div class="flex items-center justify-between mb-4">
              <button @click="goBack" class="text-gray-300 hover:text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
              </button>
              <h1 class="text-2xl font-bold text-white">账号密码登录</h1>
              <div class="w-6"></div>
            </div>
          </div>

          <form @submit.prevent="handleLogin" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">账号</label>
              <input
                v-model="loginForm.username"
                type="text"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                placeholder="请输入账号"
              >
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">密码</label>
              <div class="relative">
                <input
                  v-model="loginForm.password"
                  :type="showLoginPassword ? 'text' : 'password'"
                  required
                  class="w-full px-4 py-3 pr-12 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                  placeholder="请输入密码"
                >
                <button
                  type="button"
                  @click="showLoginPassword = !showLoginPassword"
                  class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300"
                >
                  <svg v-if="showLoginPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                </button>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-2">身份</label>
              <div class="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  @click="loginForm.role = '管理员'"
                  :class="{
                    'bg-blue-500/30 border-blue-400': loginForm.role === '管理员',
                    'bg-white/10 border-white/10': loginForm.role !== '管理员'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">管理员</span>
                </button>

                <button
                  type="button"
                  @click="loginForm.role = '教师'"
                  :class="{
                    'bg-purple-500/30 border-purple-400': loginForm.role === '教师',
                    'bg-white/10 border-white/10': loginForm.role !== '教师'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M10.394 2.08a1 1 0 00-.788 0l-7 3a1 1 0 000 1.84L5.25 8.051a.999.999 0 01.356-.257l4-1.714a1 1 0 11.788 1.838L7.667 9.088l1.94.831a1 1 0 00.787 0l7-3a1 1 0 000-1.838l-7-3zM3.31 9.397L5 10.12v4.102a8.969 8.969 0 00-1.05-.174 1 1 0 01-.89-.89 11.115 11.115 0 01.25-3.762zM9.3 16.573A9.026 9.026 0 007 14.935v-3.957l1.818.78a3 3 0 002.364 0l5.508-2.361a11.026 11.026 0 01.25 3.762 1 1 0 01-.89.89 8.968 8.968 0 00-5.35 2.524 1 1 0 01-1.4 0zM6 18a1 1 0 001-1v-2.065a8.935 8.935 0 00-2-.712V17a1 1 0 001 1z" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">教师</span>
                </button>

                <button
                  type="button"
                  @click="loginForm.role = '学生'"
                  :class="{
                    'bg-green-500/30 border-green-400': loginForm.role === '学生',
                    'bg-white/10 border-white/10': loginForm.role !== '学生'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center text-green-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">学生</span>
                </button>
              </div>
            </div>

            <button
              type="submit"
              class="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-lg hover:from-blue-700 hover:to-blue-800 transition-all duration-200 transform hover:scale-[1.02] shadow-lg"
              :disabled="loginLoading"
            >
              <span v-if="!loginLoading">登录</span>
              <span v-else class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                登录中...
              </span>
            </button>
          </form>

          <div class="text-center text-sm text-gray-300">
            使用其他方式登录? <a @click="selectLoginMethod('sms')" class="text-green-400 hover:text-green-300 cursor-pointer">手机验证码登录</a>
          </div>
        </div>

        <!-- 手机验证码登录表单 -->
        <div v-else-if="currentStep === 2 && !isRegister" class="space-y-6">
          <div class="text-center">
            <div class="flex items-center justify-between mb-4">
              <button @click="goBack" class="text-gray-300 hover:text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
              </button>
              <h1 class="text-2xl font-bold text-white">手机验证码登录</h1>
              <div class="w-6"></div>
            </div>
          </div>

          <form @submit.prevent="handleSmsLogin" class="space-y-4">
            <!-- 手机号输入 -->
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">手机号</label>
              <div class="relative">
                <input
                  v-model="smsLoginForm.phone"
                  type="tel"
                  maxlength="11"
                  required
                  :class="{
                    'border-green-500': isPhoneValid && smsLoginForm.phone.length === 11,
                    'border-red-500': !isPhoneValid && smsLoginForm.phone.length === 11
                  }"
                  class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 transition text-white placeholder-gray-400"
                  placeholder="请输入11位手机号"
                >
                <div v-if="smsLoginForm.phone" class="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <svg v-if="isPhoneValid" class="h-5 w-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                  </svg>
                  <svg v-else-if="smsLoginForm.phone.length === 11" class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                  </svg>
                </div>
              </div>
              <div v-if="smsLoginForm.phone && smsLoginForm.phone.length === 11" class="mt-1 text-xs">
                <span v-if="isPhoneValid" class="text-green-400">✓ 手机号格式正确</span>
                <span v-else class="text-red-400">✗ 手机号格式不正确</span>
              </div>
            </div>

            <!-- 验证码输入 -->
            <div>
              <div class="flex justify-between items-center mb-1">
                <label class="block text-sm font-medium text-gray-300">验证码</label>
                <button
                  type="button"
                  @click="sendVerificationCode"
                  :disabled="isCodeButtonDisabled"
                  :class="{
                    'bg-green-600 hover:bg-green-700': !isCodeButtonDisabled,
                    'bg-gray-600 cursor-not-allowed': isCodeButtonDisabled
                  }"
                  class="px-4 py-1 text-sm rounded-md text-white transition-colors"
                >
                  {{ codeButtonText }}
                </button>
              </div>
              <input
                v-model="smsLoginForm.code"
                type="text"
                maxlength="6"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 transition text-white placeholder-gray-400"
                placeholder="请输入6位验证码"
              >
            </div>

            <!-- 角色选择（短信登录默认学生） -->
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-2">身份</label>
              <div class="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  @click="smsLoginForm.role = '学生'"
                  :class="{
                    'bg-green-500/30 border-green-400': smsLoginForm.role === '学生',
                    'bg-white/10 border-white/10': smsLoginForm.role !== '学生'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center text-green-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">学生</span>
                </button>
              </div>
              <p class="text-xs text-gray-400 mt-2">短信验证码登录适用于学生角色</p>
            </div>

            <button
              type="submit"
              class="w-full py-3 px-4 bg-gradient-to-r from-green-600 to-green-700 text-white rounded-lg hover:from-green-700 hover:to-green-800 transition-all duration-200 transform hover:scale-[1.02] shadow-lg"
              :disabled="smsLoginLoading"
            >
              <span v-if="!smsLoginLoading">登录</span>
              <span v-else class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                登录中...
              </span>
            </button>
          </form>

          <div class="text-center text-sm text-gray-300">
            使用其他方式登录? <a @click="selectLoginMethod('password')" class="text-blue-400 hover:text-blue-300 cursor-pointer">账号密码登录</a>
          </div>
        </div>

        <!-- 注册表单 -->
        <div v-else-if="isRegister" class="space-y-6">
          <div class="text-center">
            <div class="flex items-center justify-between mb-4">
              <button @click="goBack" class="text-gray-300 hover:text-white">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
              </button>
              <h1 class="text-2xl font-bold text-white">用户注册</h1>
              <div class="w-6"></div>
            </div>
            <p class="text-gray-300">填写注册信息，等待管理员审核</p>
          </div>

          <form @submit.prevent="handleRegister" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">
                账号 <span class="text-red-400">*</span>
                <span class="text-xs text-gray-400 ml-2">(用于登录)</span>
              </label>
              <input
                v-model="registerForm.username"
                type="text"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                placeholder="请输入账号"
              >
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">
                姓名 <span class="text-red-400">*</span>
              </label>
              <input
                v-model="registerForm.account"
                type="text"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                placeholder="请输入真实姓名"
              >
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">
                密码 <span class="text-red-400">*</span>
              </label>
              <div class="relative">
                <input
                  v-model="registerForm.password"
                  :type="showRegisterPassword ? 'text' : 'password'"
                  required
                  class="w-full px-4 py-3 pr-12 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                  placeholder="设置密码"
                >
                <button
                  type="button"
                  @click="showRegisterPassword = !showRegisterPassword"
                  class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-300"
                >
                  <svg v-if="showRegisterPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- 角色选择 -->
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-2">选择角色</label>
              <div class="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  @click="registerForm.role = '管理员'"
                  :class="{
                    'bg-blue-500/30 border-blue-400': registerForm.role === '管理员',
                    'bg-white/10 border-white/10': registerForm.role !== '管理员'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">管理员</span>
                </button>

                <button
                  type="button"
                  @click="registerForm.role = '教师'"
                  :class="{
                    'bg-purple-500/30 border-purple-400': registerForm.role === '教师',
                    'bg-white/10 border-white/10': registerForm.role !== '教师'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M10.394 2.08a1 1 0 00-.788 0l-7 3a1 1 0 000 1.84L5.25 8.051a.999.999 0 01.356-.257l4-1.714a1 1 0 11.788 1.838L7.667 9.088l1.94.831a1 1 0 00.787 0l7-3a1 1 0 000-1.838l-7-3zM3.31 9.397L5 10.12v4.102a8.969 8.969 0 00-1.05-.174 1 1 0 01-.89-.89 11.115 11.115 0 01.25-3.762zM9.3 16.573A9.026 9.026 0 007 14.935v-3.957l1.818.78a3 3 0 002.364 0l5.508-2.361a11.026 11.026 0 01.25 3.762 1 1 0 01-.89.89 8.968 8.968 0 00-5.35 2.524 1 1 0 01-1.4 0zM6 18a1 1 0 001-1v-2.065a8.935 8.935 0 00-2-.712V17a1 1 0 001 1z" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">教师</span>
                </button>

                <button
                  type="button"
                  @click="registerForm.role = '学生'"
                  :class="{
                    'bg-green-500/30 border-green-400': registerForm.role === '学生',
                    'bg-white/10 border-white/10': registerForm.role !== '学生'
                  }"
                  class="flex flex-col items-center p-3 rounded-lg border transition-colors group"
                >
                  <div class="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center text-green-300 mb-1">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                    </svg>
                  </div>
                  <span class="text-xs font-medium text-white">学生</span>
                </button>
              </div>
            </div>

            <div class="bg-blue-500/10 border border-blue-500/30 rounded-lg p-3">
              <p class="text-sm text-blue-200">
                <span class="font-medium">注意：</span>注册后需要等待管理员审核通过才能登录。
              </p>
            </div>

            <button
              type="submit"
              class="w-full py-3 px-4 bg-gradient-to-r from-purple-600 to-purple-700 text-white rounded-lg hover:from-purple-700 hover:to-purple-800 transition-all duration-200 transform hover:scale-[1.02] shadow-lg"
              :disabled="registerLoading"
            >
              <span v-if="!registerLoading">提交注册</span>
              <span v-else class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                注册中...
              </span>
            </button>
          </form>

          <div class="text-center text-sm text-gray-300">
            已有账户? <a @click="toggleForm" class="text-blue-400 hover:text-blue-300 cursor-pointer">立即登录</a>
          </div>
        </div>
      </div>

      <!-- 右侧介绍区域 -->
      <div class="w-1/2 bg-gradient-to-br from-blue-900/80 via-purple-900/80 to-indigo-900/80 text-white p-8 flex flex-col justify-center items-center text-center transition-all duration-500 ease-in-out relative overflow-hidden backdrop-blur-sm" :class="{
        'translate-x-[-100%]': isRegister,
        'translate-x-0': !isRegister
      }">
        <!-- 背景装饰 -->
        <div class="absolute inset-0 bg-gradient-to-br from-blue-600/20 via-purple-600/20 to-indigo-600/20"></div>
        <div class="absolute top-0 left-0 w-32 h-32 bg-blue-500/10 rounded-full -translate-x-16 -translate-y-16"></div>
        <div class="absolute bottom-0 right-0 w-24 h-24 bg-purple-500/10 rounded-full translate-x-12 translate-y-12"></div>

        <!-- 不同状态下的介绍 -->
        <template v-if="!isRegister">
          <div v-if="currentStep === 0" class="space-y-6 relative z-10">
            <div class="w-20 h-20 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h2 class="text-3xl font-bold">教育管理平台</h2>
            <p class="text-blue-200">多种登录方式，方便快捷</p>
          </div>

          <div v-else-if="currentStep === 1" class="space-y-6 relative z-10">
            <div class="w-20 h-20 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
            </div>
            <h2 class="text-3xl font-bold">账号密码登录</h2>
            <p class="text-blue-200">适用于所有用户类型</p>
            <p class="text-sm text-gray-300">管理员、教师、学生均可使用</p>
            <button
              @click="selectLoginMethod('sms')"
              class="px-6 py-2 bg-white/10 backdrop-blur-sm text-white rounded-lg font-medium hover:bg-white/20 transition-all duration-200 border border-white/20"
            >
              切换到手机验证码登录
            </button>
          </div>

          <div v-else-if="currentStep === 2" class="space-y-6 relative z-10">
            <div class="w-20 h-20 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <h2 class="text-3xl font-bold">手机验证码登录</h2>
            <p class="text-blue-200">考生快速登录，无需记住密码</p>
            <p class="text-sm text-gray-300">输入手机号，获取验证码即可登录</p>
            <button
              @click="selectLoginMethod('password')"
              class="px-6 py-2 bg-white/10 backdrop-blur-sm text-white rounded-lg font-medium hover:bg-white/20 transition-all duration-200 border border-white/20"
            >
              切换到账号密码登录
            </button>
          </div>
        </template>

        <div v-else class="space-y-6 relative z-10">
          <div class="w-20 h-20 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
          </div>
          <h2 class="text-3xl font-bold">加入我们</h2>
          <p class="text-blue-200">选择您的角色，开始使用平台</p>
          <button
            @click="toggleForm"
            class="px-6 py-2 bg-white/10 backdrop-blur-sm text-white rounded-lg font-medium hover:bg-white/20 transition-all duration-200 border border-white/20"
          >
            返回登录
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
/* 原有样式保持不变 */
.transition-all {
  transition-property: all;
}

.duration-500 {
  transition-duration: 500ms;
}

.ease-in-out {
  transition-timing-function: ease-in-out;
}

.overflow-hidden {
  overflow: hidden;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

.group:hover {
  transform: translateY(-2px);
}

input:focus {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.countdown {
  animation: pulse 2s infinite;
}
</style>
