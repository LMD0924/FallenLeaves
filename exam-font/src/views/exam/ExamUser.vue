<script setup>
import {computed, onMounted, ref} from 'vue'
import {get, post, upload} from "@/net/index.js";
import {formatDate} from "@/time/Data.js";
import {message} from "ant-design-vue";
import { isDark } from "@/stores/theme.js" // 统一使用主题存储

const [messageApi,contextHolder]=message.useMessage();
// 用户数据
const user = ref({})
//获取个人信息
const getCurrentUser=()=>{
  get('api/exam/current',{},(message,data)=>{
    user.value=data
    console.log(user.value)
    // 在获取用户信息成功后调用getClass，确保user.value.class_id已存在
    if (user.value.class_id) {
      getClass()
    } else {
      console.log('用户没有班级ID')
    }
  })
}
const classInfo=ref({})
//获取班级信息
const getClass=()=>{
  get('api/exam/SelectClassById',{
    id:user.value.class_id
  },(message,data)=>{
    classInfo.value=data
    messageApi.success(message)
    console.log("班级",classInfo.value)
  })
}
// 编辑对话框状态
const showContactDialog = ref(false)
const editForm = ref({
  email: '',
  phone: '',
  locality: '',
  sex:'',
  general:''
})

// 编辑联系信息
const editContact = () => {
  editForm.value = {
    email: user.value.email,
    phone: user.value.phone,
    locality: user.value.locality
  }
  showContactDialog.value = true
}

// 保存联系信息
const saveContact = () => {
  post('api/exam/UpdateUserInfo',{
    id:user.value.id,
    user_id:user.value.id,
    account:user.value.account,
    username:user.value.username,
    password:user.value.password,
    role:user.value.role,
    status:user.value.status,
    sex:editForm.value.sex,
    general:editForm.value.general,
    locality:editForm.value.locality,
    email: editForm.value.email||user.value.email,
    phone: editForm.value.phone||user.value.phone,
  },(message,data)=>{
    messageApi.success(message)
    getCurrentUser()
  })
  showContactDialog.value = false
}

//更新头像
// 更新头像相关代码
const fileInput = ref(null)
const avatarLoading = ref(false)

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理头像上传
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    messageApi.error('只能上传图片文件')
    return
  }

  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    messageApi.error('文件大小不能超过10MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  avatarLoading.value = true

  try {
    upload("api/exam/updateAvatar", formData,
      (message, data) => {
        if (message === '上传成功') {
          user.value.avatar = data
          messageApi.success(message)
        } else {
          messageApi.error(message || '上传失败')
        }
      },
      (error) => {
        messageApi.error('请求失败: ' + (error.message || '未知错误'))
      }
    )
  } catch (error) {
    messageApi.error('处理上传时出错: ' + error.message)
  } finally {
    avatarLoading.value = false
    event.target.value = '' // 重置文件输入
  }
}

onMounted( ()=>{
 getCurrentUser()
})

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
    }
  }
})
</script>

<template>
  <contextHolder/>
  <div class="min-h-screen" :class="themeClasses.bg">
    <!-- 顶部导航 -->
    <header class="flex items-center justify-between p-6 border-b" :class="isDark ? 'bg-white/5 border-white/10' : 'bg-white border-gray-200'">
      <div class="flex items-center space-x-4">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="w-8 h-8 text-indigo-400">
          <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55c-2.21 0-4 1.79-4 4s1.79 4 4 4s4-1.79 4-4V7h4V3m-7 19c-1.66 0-3-1.34-3-3s1.34-3 3-3s3 1.34 3 3s-1.34 3-3 3z"/>
        </svg>
        <h1 :class="themeClasses.text.primary + ' text-2xl font-bold'">个人信息中心</h1>
      </div>
      <div class="flex items-center space-x-4">
        <button :class="isDark ? 'p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors' : 'p-2 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors'">
          <svg xmlns="http://www.w3.org/2000/svg" :class="isDark ? 'h-5 w-5 text-white' : 'h-5 w-5 text-gray-600'" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" />
          </svg>
        </button>
        <button :class="isDark ? 'p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors' : 'p-2 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors'">
          <svg xmlns="http://www.w3.org/2000/svg" :class="isDark ? 'h-5 w-5 text-white' : 'h-5 w-5 text-gray-600'" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="flex-1 p-8 overflow-auto">
      <div class="max-w-4xl mx-auto">
        <!-- 个人信息卡片 -->
        <div :class="themeClasses.card + ' rounded-2xl p-6 mb-8'">
          <div class="flex items-start space-x-6">
            <!-- 头像 -->
            <div class="relative">
              <img
                :src="user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                alt="用户头像"
                class="w-16 h-16 rounded-full border-2"
                :class="isDark ? 'border-white/20' : 'border-gray-200'"
              >
              <button
                @click="triggerFileInput"
                :class="themeClasses.button.primary + ' absolute -bottom-2 -right-2 p-2 rounded-full transition-colors'"
                :disabled="avatarLoading"
              >
                <svg v-if="!avatarLoading" xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-white" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
                <span v-else class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
              </button>
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleAvatarUpload"
              >
            </div>

            <!-- 基本信息 -->
            <div class="flex-1">
              <div class="flex items-center justify-between">
                <h2 :class="themeClasses.text.primary + ' text-2xl font-bold'">{{ user.account }}</h2>
                <span class="px-3 py-1 rounded-full text-xs font-medium bg-green-500/20 text-green-400">
                  {{ user.status }}
                </span>
              </div>
              <p :class="themeClasses.text.secondary + ' mt-1'">{{ user.phone }}</p>

              <div class="mt-4 grid grid-cols-2 gap-4">
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">用户ID</p>
                  <p :class="themeClasses.text.primary">{{ user.id }}</p>
                </div>
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">注册时间</p>
                  <p :class="themeClasses.text.primary">2025-08-01</p>
                </div>
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">最后登录</p>
                  <p :class="themeClasses.text.primary">{{formatDate(user.time)}}</p>
                </div>
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">账户类型</p>
                  <p :class="themeClasses.text.primary">{{user.role}}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 详细信息卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- 联系信息 -->
          <div :class="themeClasses.card + ' rounded-2xl p-6'">
            <div class="flex items-center justify-between mb-4">
              <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">联系信息</h3>
              <button @click="editContact" :class="themeClasses.button.accent + ' text-sm flex items-center'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
                编辑
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <p :class="themeClasses.text.muted + ' text-sm'">电子邮箱</p>
                <p :class="themeClasses.text.primary">{{ user.email || '未设置'}}</p>
              </div>
              <div>
                <p :class="themeClasses.text.muted + ' text-sm'">手机号码</p>
                <p :class="themeClasses.text.primary">{{ user.phone || '未设置'}}</p>
              </div>
              <div>
                <p :class="themeClasses.text.muted + ' text-sm'">联系地址</p>
                <p :class="themeClasses.text.primary">{{ user.locality || '未设置' }}</p>
              </div>
            </div>
          </div>

          <!-- 账户安全 -->
          <div :class="themeClasses.card + ' rounded-2xl p-6'">
            <div class="flex items-center justify-between mb-4">
              <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">账户安全</h3>
              <button :class="themeClasses.button.accent + ' text-sm flex items-center'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
                </svg>
                安全设置
              </button>
            </div>

            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">密码</p>
                  <p :class="themeClasses.text.primary">••••••••</p>
                </div>
                <button :class="themeClasses.button.accent + ' text-sm'">修改</button>
              </div>
              <div class="flex items-center justify-between">
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">双重认证</p>
                  <p :class="themeClasses.text.primary">{{ user.twoFactor ? '已启用' : '未启用' }}</p>
                </div>
                <button :class="themeClasses.button.accent + ' text-sm'">{{ user.twoFactor ? '管理' : '启用' }}</button>
              </div>
              <div class="flex items-center justify-between">
                <div>
                  <p :class="themeClasses.text.muted + ' text-sm'">登录设备</p>
                  <p :class="themeClasses.text.primary"> 台设备</p>
                </div>
                <button :class="themeClasses.button.accent + ' text-sm'">查看</button>
              </div>
            </div>
          </div>

          <!-- 教育/工作信息 -->
          <div :class="themeClasses.card + ' rounded-2xl p-6'">
            <div class="flex items-center justify-between mb-4">
              <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">{{ user.type === '教师' ? '工作信息' : '教育信息' }}</h3>
              <button :class="themeClasses.button.accent + ' text-sm flex items-center'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                </svg>
                编辑
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <p :class="themeClasses.text.muted + ' text-sm'">{{ user.type === '教师' ? '院系' : '学院' }}</p>
                <p :class="themeClasses.text.primary">{{ user.professional || '未设置' }}</p>
              </div>
              <div>
                <p :class="themeClasses.text.muted + ' text-sm'">{{ user.type === '教师' ? '职称' : '专业' }}</p>
                <p :class="themeClasses.text.primary">{{ user.professional || '未设置' }}</p>
              </div>
              <div v-if="user.type !== '教师'">
                <p :class="themeClasses.text.muted + ' text-sm'">班级</p>
                <p :class="themeClasses.text.primary">{{ classInfo.name || '未设置' }}</p>
              </div>
              <div v-if="user.type === '教师'">
                <p :class="themeClasses.text.muted + ' text-sm'">教授课程</p>
                <div class="flex flex-wrap gap-2 mt-1">
                  <span v-for="course in user.courses.slice(0, 3)" :key="course" class="px-2 py-1 text-xs rounded-full bg-indigo-500/20 text-indigo-300">
                    {{ course }}
                  </span>
                  <span v-if="user.courses.length > 3" class="px-2 py-1 text-xs rounded-full" :class="isDark ? 'bg-white/10 text-white/50' : 'bg-gray-100 text-gray-500'">
                    +{{ user.courses.length - 3 }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 活动统计 -->
          <div :class="themeClasses.card + ' rounded-2xl p-6'">
            <div class="flex items-center justify-between mb-4">
              <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">活动统计</h3>
              <button :class="themeClasses.button.accent + ' text-sm flex items-center'">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11.707 4.707a1 1 0 00-1.414-1.414L10 9.586 8.707 8.293a1 1 0 00-1.414 0l-2 2a1 1 0 001.414 1.414L8 10.414l1.293 1.293a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                </svg>
                详细报告
              </button>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="p-3 rounded-lg" :class="isDark ? 'bg-white/5' : 'bg-gray-50'">
                <p :class="themeClasses.text.muted + ' text-sm'">本月登录</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">暂无</p>
              </div>
              <div class="p-3 rounded-lg" :class="isDark ? 'bg-white/5' : 'bg-gray-50'">
                <p :class="themeClasses.text.muted + ' text-sm'">课程参与</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">暂无</p>
              </div>
              <div class="p-3 rounded-lg" :class="isDark ? 'bg-white/5' : 'bg-gray-50'">
                <p :class="themeClasses.text.muted + ' text-sm'">任务完成</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">暂无</p>
              </div>
              <div class="p-3 rounded-lg" :class="isDark ? 'bg-white/5' : 'bg-gray-50'">
                <p :class="themeClasses.text.muted + ' text-sm'">消息未读</p>
                <p :class="themeClasses.text.primary + ' text-2xl font-bold'">暂无</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 编辑联系信息对话框 -->
    <div v-if="showContactDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div :class="themeClasses.card + ' rounded-2xl p-6 w-full max-w-md'">
        <div class="flex items-center justify-between mb-4">
          <h3 :class="themeClasses.text.primary + ' text-lg font-medium'">编辑联系信息</h3>
          <button @click="showContactDialog = false" :class="themeClasses.text.muted + ' hover:' + (isDark ? 'text-white' : 'text-gray-800')">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">电子邮箱</label>
            <input
              v-model="editForm.email"
              type="email"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
            >
          </div>
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">手机号码</label>
            <input
              v-model="editForm.phone"
              type="tel"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
            >
          </div>
          <div>
            <label class="block text-sm font-medium mb-1" :class="themeClasses.text.secondary">联系地址</label>
            <input
              v-model="editForm.locality"
              type="text"
              :class="themeClasses.input + ' w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent'"
            >
          </div>

          <div class="pt-4 flex space-x-3">
            <button
              @click="showContactDialog = false"
              :class="themeClasses.button.secondary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              取消
            </button>
            <button
              @click="saveContact"
              :class="themeClasses.button.primary + ' flex-1 px-4 py-2 rounded-lg transition-colors'"
            >
              保存
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
