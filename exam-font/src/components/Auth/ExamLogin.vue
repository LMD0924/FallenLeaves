<script setup>
import { ref, reactive } from 'vue'
import { message } from "ant-design-vue"
import {get, post} from "@/net/index.js"
import router from "@/router/index.js"
import webSocketService from '@/net/websocket.js'
import { useNotification } from '@/services/notificationService.js'
const [messageApi, contextHolder] = message.useMessage()
const { initNotificationService } = useNotification()
// 表单状态管理
const isRegister = ref(false)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

const toggleForm = () => {
  isRegister.value = !isRegister.value
}

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  role: '学生',
})

// 注册表单数据
const registerForm = reactive({
  username: '',
  account: '',
  password: '',
  role: '学生',
  status:'未审核'
})

// 登录逻辑
const handleLogin = () => {
  loginLoading.value = true
  post('/api/auth/ExamLogin', {
    username: loginForm.username,
    password: loginForm.password,
    role: loginForm.role,
  }, (message, data) => {
    messageApi.success(message);
    localStorage.setItem('authToken', data);
    // 登录成功后初始化 WebSocket 和通知服务
    initNotificationService()

    // 延迟一下确保 token 已设置
    setTimeout(() => {
      webSocketService.connect()
    }, 1000)
    setTimeout(()=>{
      router.push('/ExamSidebar')
    },1000)
  }, (errorMessage) => {
    messageApi.warning(errorMessage); // 这里加上
    loginLoading.value = false
  })
}

// 注册逻辑
const handleRegister = () => {
  registerLoading.value = true
  post('/api/auth/ExamRegister', registerForm, (message) => {
      //清空表单
      registerForm.username = ''
      registerForm.account = ''
      registerForm.password = ''
      registerForm.role = '学生'
      registerForm.status = '未审核'
    messageApi.success(message)
    toggleForm()
  }, () => {
    registerLoading.value = false
  })
}
</script>

<template>
  <contextHolder />
  <!-- 主内容 -->
  <div class="bg-black relative min-h-screen flex items-center justify-center p-4">
    <!-- 主容器 -->
    <div class="w-full max-w-4xl bg-white/10 backdrop-blur-md shadow-2xl rounded-2xl overflow-hidden flex border border-white/20">
      <!-- 左侧表单区域 -->
      <div class="w-1/2 p-8 transition-all duration-500 ease-in-out" :class="{'translate-x-full': isRegister}">
        <!-- 登录表单 -->
        <div v-if="!isRegister" class="space-y-6">
          <div class="text-center">
            <h1 class="text-3xl font-bold text-white">教育管理平台</h1>
            <p class="text-gray-300 mt-2">请登录您的账户</p>
          </div>

          <!-- 身份选择 -->
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
              <div class="flex justify-between items-center">
                <label class="block text-sm font-medium text-gray-300 mb-1">密码</label>
                <a href="#" class="text-sm text-blue-400 hover:text-blue-300">忘记密码?</a>
              </div>
              <div class="relative">
                <input
                  v-model="loginForm.password"
                  :type="showLoginPassword ? 'text' : 'password'"
                  required
                  class="w-full px-4 py-3 pr-12 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                  placeholder="• • • • • • • •"
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
            还没有账户? <a @click="toggleForm" class="text-blue-400 hover:text-blue-300 cursor-pointer">立即注册</a>
          </div>
        </div>

        <!-- 注册表单 -->
        <div v-else class="space-y-6">
          <div class="text-center">
            <h1 class="text-3xl font-bold text-white">创建账户</h1>
            <p class="text-gray-300 mt-2">加入教育管理平台</p>
          </div>

          <form @submit.prevent="handleRegister" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">用户名</label>
              <input
                v-model="registerForm.account"
                type="text"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                placeholder="设置您的用户名"
              >
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">账号</label>
              <input
                v-model="registerForm.username"
                type="text"
                required
                class="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition text-white placeholder-gray-400"
                placeholder="输入您的账号"
              >
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">密码</label>
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

            <button
              type="submit"
              class="w-full py-3 px-4 bg-gradient-to-r from-purple-600 to-purple-700 text-white rounded-lg hover:from-purple-700 hover:to-purple-800 transition-all duration-200 transform hover:scale-[1.02] shadow-lg"
              :disabled="registerLoading"
            >
              <span v-if="!registerLoading">注册</span>
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
      <div class="w-1/2 bg-gradient-to-br from-blue-900/80 via-purple-900/80 to-indigo-900/80 text-white p-8 flex flex-col justify-center items-center text-center transition-all duration-500 ease-in-out relative overflow-hidden backdrop-blur-sm" :class="{'translate-x-[-100%]': isRegister}">
        <!-- 背景装饰 -->
        <div class="absolute inset-0 bg-gradient-to-br from-blue-600/20 via-purple-600/20 to-indigo-600/20"></div>
        <div class="absolute top-0 left-0 w-32 h-32 bg-blue-500/10 rounded-full -translate-x-16 -translate-y-16"></div>
        <div class="absolute bottom-0 right-0 w-24 h-24 bg-purple-500/10 rounded-full translate-x-12 translate-y-12"></div>

        <!-- 登录状态下的介绍 -->
        <div v-if="!isRegister" class="space-y-6 relative z-10">
          <div class="w-20 h-20 bg-white/10 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 class="text-3xl font-bold">教育管理平台</h2>
          <p class="text-blue-200">教师与学生一站式管理解决方案</p>
          <button
            @click="toggleForm"
            class="mt-6 px-6 py-3 bg-white/10 backdrop-blur-sm text-white rounded-lg font-medium hover:bg-white/20 transition-all duration-200 border border-white/20"
          >
            立即注册
          </button>
        </div>

        <!-- 注册状态下的介绍 -->
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
            class="mt-6 px-6 py-3 bg-white/10 backdrop-blur-sm text-white rounded-lg font-medium hover:bg-white/20 transition-all duration-200 border border-white/20"
          >
            返回登录
          </button>
        </div>
      </div>
    </div>
  </div>
</template>


<style>
/* 平滑过渡效果 */
.transition-all {
  transition-property: all;
}

.duration-500 {
  transition-duration: 500ms;
}

.ease-in-out {
  transition-timing-function: ease-in-out;
}

/* 防止内容溢出 */
.overflow-hidden {
  overflow: hidden;
}

/* 按钮禁用状态 */
button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 动画效果 */
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
</style>
