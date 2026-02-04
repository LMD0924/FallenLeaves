<script setup>
import {ref, onMounted, onUnmounted, computed, watch} from 'vue'
import { isDark, toggleTheme, currentTheme } from '@/stores/theme.js'
import router from "@/router/index.js";
import {get, post} from "@/net/index.js";
import { useNotification } from '@/services/notificationService'
import TopNotificationBar from '@/components/TopNotificationBar.vue'
import webSocketService from '@/net/websocket'

// 响应式状态
const isCollapsed = ref(true) // 默认折叠
const isMobileOpen = ref(false)
const activeNavItem = ref('仪表盘')
const isSidebarHovered = ref(false)
const currentParent = ref(null) // 当前父级菜单
const User = ref({})

// 通知相关 - 正确使用 useNotification
const notificationService = useNotification()
const {
  activeNotifications,
  removeNotification,
  addNotification,  // 直接解构出 addNotification
  initNotificationService
} = notificationService

// 处理通知点击事件
const handleNotificationClick = (notification) => {
  console.log('通知被点击:', notification)
  // 根据通知类型进行不同的跳转或操作
  if (notification.type === 'assignment') {
    router.push('/assignments')
  } else if (notification.type === 'exam') {
    router.push('/exams')
  } else if (notification.type === 'system') {
    router.push('/notifications')
  }
  // 可以在这里添加更多的类型处理逻辑
}

// WebSocket 处理器 ID
const wsHandlerId = 'sidebar-notifications'

// 初始化通知服务
const initSidebarNotifications = () => {
  // 先注销可能存在的旧处理器，避免重复注册
  webSocketService.unregisterHandler(wsHandlerId)

  const handlers = {
    // 连接建立
    onConnected: () => {
      // 连接成功后立即尝试获取最新消息
      setTimeout(() => {
        getLatestNotice()
      }, 500)
    },

    // 连接断开
    onDisconnected: () => {
    },

    // 新消息通知
    new_notice: (noticeData) => {

      // 显示新发布的消息
      showLatestNotice(noticeData, '新消息发布')
    }
  }

  webSocketService.registerHandler(wsHandlerId, handlers)

  // 检查连接状态，如果未连接则尝试连接
  if (!webSocketService.isConnected) {
    console.log('尝试建立WebSocket连接...')
    webSocketService.connect()
  }
}

// 显示最新消息 - 修复引用问题
const showLatestNotice = (noticeData, type = '最新消息') => {

  const notification = {
    id: noticeData.id || Date.now(),
    title: `${type}: ${noticeData.title}`,
    content: noticeData.content,
    type: noticeData.type,
    priority: noticeData.priority,
    time: noticeData.time || new Date().toISOString(),
    userId: noticeData.userId,
    publisher: noticeData.publisher || '系统'
  }

  // 使用正确的引用 ✅
  addNotification(notification)

  // 播放提示音
  playNotificationSound()

  // 显示桌面通知
  showDesktopNotification(notification)
}

// 播放通知提示音
const playNotificationSound = () => {
  try {
    const audio = new Audio('data:audio/wav;base64,UklGRigAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQQAAAAAAA==')
    audio.volume = 0.2
    audio.play().catch(() => {
      // 忽略播放错误
    })
  } catch (error) {
    console.log('播放提示音失败:', error)
  }
}

// 显示桌面通知
const showDesktopNotification = (notification) => {
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(notification.title, {
      body: notification.content,
      icon: '/favicon.ico',
      tag: 'latest-notice'
    })
  }
}

// 请求通知权限
const requestNotificationPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
      }
    })
  }
}

// 获取最新发布的一条消息 - 添加详细日志
const getLatestNotice = async () => {
  try {
    const allNotices = await new Promise((resolve, reject) => {
      get('api/notice/SelectAllNotice', {}, (message, data) => {
        resolve(data || [])
      }, (error) => {
        reject(error)
      })
    })
    if (Array.isArray(allNotices) && allNotices.length > 0) {
      const latestNotice = allNotices[allNotices.length - 1]

      // 显示最新消息提示
      showLatestNotice(latestNotice, '最新公告')
    }
  } catch (error) {
    console.log('❌ 获取消息列表失败:', error)
  }
}

// 导航项数据 - 重构为支持二级菜单
const navSections = ref([
  {
    label: '主页',
    items: [
      {
        id: 'ExamSidebar',
        icon: '🏠',
        text: '仪表盘',
        roles: ['管理员', '教师', '学生'],
        path: '/ExamSidebar'
      }
    ]
  },
  {
    label: '系统管理',
    items: [
      {
        id: 'UserManage',
        icon: '👥',
        text: '用户管理',
        roles: ['管理员'],
        path: '/UserManage'
      },
      {
        id: 'CourseManage',
        icon: '📚',
        text: '课程管理',
        roles: ['管理员', '教师'],
        path: '/CourseManage',
        children: [
          {
            id: 'CourseManage',
            icon: '📋',
            text: '课程列表',
            roles: ['管理员', '教师'],
            path: '/CourseManage'
          },
          {
            id: 'CourseManage',
            icon: '➕',
            text: '添加课程',
            roles: ['管理员', '教师'],
            path: '/CourseManage'
          }
        ]
      },
      {
        id: 'ClassManage',
        icon: '👨‍👩‍👧‍👦',
        text: '班级管理',
        roles: ['管理员', '教师'],
        path: '/ClassManage'
      }
    ]
  },
  {
    label: '考试中心',
    items: [
      {        id: 'exam_manage',        icon: '📝',        text: '考试管理',        roles: ['管理员', '教师'],        children: [          {            id: 'exam_list',            icon: '📋',            text: '考试列表',            roles: ['管理员', '教师'],            path: '/CreateExam'          },          {            id: 'create_exam',            icon: '➕',            text: '创建考试',            roles: ['管理员', '教师'],            path: '/CreateExam'          },          {            id: 'paper_manage',            icon: '📄',            text: '试卷管理',            roles: ['管理员', '教师'],            path: '/CreateExam'          }        ]      },
      {        id: 'exam_record',        icon: '📋',        text: '考试记录',        roles: ['管理员', '教师'],        path: '/ExamRecord'      },
      {        id: 'grade_exam',        icon: '✍️',        text: '考试批阅',        roles: ['管理员', '教师'],        path: '/GradeExam'      },
      {
        id: 'QuestionType',
        icon: '🧮',
        text: '题库管理',
        roles: ['管理员', '教师'],
        path: '/QuestionType'
      },
      {        id: 'my_exams',        icon: '🎯',        text: '我的考试',        roles: ['学生'],        children: [          {            id: 'available_exams',            icon: '📋',            text: '可参加考试',            roles: ['学生'],            path: '/CreateExam'          },          {            id: 'exam_history',            icon: '🕐',            text: '考试历史',            roles: ['学生'],            path: '/CreateExam'          },          {            id: 'grade_center',            icon: '📊',            text: '成绩查询',            roles: ['学生'],            path: '/GradeCenter'          }        ]      }
    ]
  },
  {
    label: '学习中心',
    items: [
      {
        id: 'CourseManage',
        icon: '🎒',
        text: '我的课程',
        roles: ['学生'],
        path: '/CourseManage'
      },
      {
        id: 'study_records',
        icon: '📖',
        text: '学习记录',
        roles: ['学生'],
        path: '/study-records'
      },
      {
        id: 'grade_analysis',
        icon: '📈',
        text: '成绩分析',
        roles: ['学生'],
        path: '/grade-analysis'
      }
    ]
  },
  {
    label: '数据分析',
    items: [
      {
        id: 'exam_statistics',
        icon: '📊',
        text: '考试统计',
        roles: ['管理员', '教师'],
        path: '/exam-statistics'
      },
      {
        id: 'grade_distribution',
        icon: '📉',
        text: '成绩分布',
        roles: ['管理员', '教师'],
        path: '/grade-distribution'
      },
      {
        id: 'student_performance',
        icon: '🎓',
        text: '学生表现',
        roles: ['管理员', '教师'],
        path: '/student-performance'
      }
    ]
  },
  {
    label: '通知与消息',
    items: [
      {
        id: 'notice_center',
        icon: '📢',
        text: '通知中心',
        roles: ['管理员', '教师', '学生'],
        children: [
          {
            id: 'NoticeList',
            icon: '📋',
            text: '通知列表',
            roles: ['管理员', '教师', '学生'],
            path: '/NoticeList'
          },
          {
            id: 'Notice',
            icon: '✏️',
            text: '发布通知',
            roles: ['管理员', '教师'],
            path: '/Notice'
          }
        ]
      },
      {
        id: 'NoticeList',
        icon: '💬',
        text: '消息中心',
        roles: ['管理员', '教师', '学生'],
        path: '/NoticeList'
      },
      {
        id: 'ExamLeave',
        icon: '🏠',
        text: '请假管理',
        roles: ['管理员', '教师'],
        children: [
          {
            id: 'ExamLeave',
            icon: '📝',
            text: '请假申请',
            roles: ['学生'],
            path: '/ExamLeave'
          },
          {
            id: 'ExamLeave',
            icon: '✅',
            text: '审批请假',
            roles: ['管理员', '教师'],
            path: '/ExamLeave'
          }
        ]
      }
    ]
  },
  {
    label: '个人中心',
    items: [
      {
        id: 'ExamUser',
        icon: '👤',
        text: '个人信息',
        roles: ['管理员', '教师', '学生'],
        path: '/ExamUser'
      },
      {
        id: 'ExamUser',
        icon: '⚙️',
        text: '账户设置',
        roles: ['管理员', '教师', '学生'],
        children: [
          {
            id: 'ExamUser',
            icon: 'ℹ️',
            text: '基本信息',
            roles: ['管理员', '教师', '学生'],
            path: '/ExamUser'
          },
          {
            id: 'security_settings',
            icon: '🔒',
            text: '安全设置',
            roles: ['管理员', '教师', '学生'],
            path: '/account/security'
          },
          {
            id: 'notification_settings',
            icon: '🔔',
            text: '通知设置',
            roles: ['管理员', '教师', '学生'],
            path: '/account/notifications'
          }
        ]
      }
    ]
  }
])

// 计算当前显示的菜单列表
const activeMenuList = computed(() => {
  if (currentParent.value) {
    return currentParent.value.children || []
  }
  return navSections.value.flatMap(section =>
    section.items.filter(item => !item.roles || item.roles.includes(User.value.role))
  )
})

// 切换移动端菜单
const toggleMobileMenu = () => {
  isMobileOpen.value = !isMobileOpen.value
}

// 设置激活的导航项
const setActiveNavItem = (item) => {
  activeNavItem.value = item.id

  if (item.children && item.children.length > 0) {
    // 如果有子菜单，进入子菜单
    currentParent.value = item
  } else if (item.path) {
    // 如果是叶子节点，进行路由跳转
    router.push(item.path)
    // 在移动端点击后自动关闭侧边栏
    if (window.innerWidth <= 768) {
      isMobileOpen.value = false
    }
  }
}

// 返回上级菜单
const goBack = () => {
  currentParent.value = null
}

// 检查是否为当前激活项
const isActiveItem = (item) => {
  return activeNavItem.value === item.id
}

// 检查是否有子菜单
const hasChildren = (item) => {
  return item.children && item.children.length > 0
}

// 响应式处理
const handleResize = () => {
  if (window.innerWidth > 768) {
    isMobileOpen.value = false
  }
}

// 主题切换功能
const handleThemeToggle = () => {
  toggleTheme()
}

// 侧边栏悬停处理
const handleSidebarMouseEnter = () => {
  if (window.innerWidth > 1024) {
    isSidebarHovered.value = true
  }
}

const handleSidebarMouseLeave = () => {
  if (window.innerWidth > 1024) {
    isSidebarHovered.value = false
  }
}

// 获取登录信息 - 添加登录检测
const getUserInfo = () => {
  get('api/user/current', {},
    (message, data) => {
      User.value = data
      // 用户信息获取成功后，初始化通知服务
      initNotificationService()
      initSidebarNotifications()
      requestNotificationPermission()
      updateEndLoginTime()
      // 保留直接获取最新消息的逻辑，作为WebSocket的补充
      setTimeout(() => {
        getLatestNotice()
      }, 1000)
    },
    (error) => {
      User.value = { name: '未知用户', role: 'guest' }
    }
  )
}

/*
* 当用户进入主页时更新最后的登录时间
* */
const updateEndLoginTime=()=>{
  let params={
    id:User.value.id,
    endLoginTime:new Date().toISOString()
  }
  post('api/user/updateUserInfo',params,(message,data)=>{})
}

// 生命周期钩子
onMounted(() => {
  getUserInfo()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  // 清理WebSocket资源，避免内存泄漏
  webSocketService.unregisterHandler(wsHandlerId)
  // 注意：不要在这里调用disconnect()，因为可能有其他组件也在使用WebSocket
})
</script>

<template>
  <div
    :class="[
      'suite-shell flex transition-all duration-300',
      isDark ? 'suite-shell--dark' : 'suite-shell--light'
    ]"
  >
    <!-- 顶部通知栏容器 - 类似移动端QQ和微信的顶部消息提示 -->
    <div class="top-notifications-container">
      <TopNotificationBar
        v-for="notification in activeNotifications"
        :key="notification.id"
        :notification="notification"
        :duration="5000"
        @close="removeNotification"
        @click="handleNotificationClick"
      />
    </div>
    <!-- 主内容区域 -->
    <main class="suite-main w-full ml-20">
      <RouterView />
    </main>

    <!-- 移动端菜单按钮 -->
    <button
      @click="toggleMobileMenu"
      :class="[
        'lg:hidden fixed top-5 left-5 z-50 w-12 h-12 rounded-2xl border-none text-lg cursor-pointer transition-all duration-300 backdrop-blur-lg mobile-menu-btn flex items-center justify-center',
        isDark ? 'bg-black text-gray-200 hover:bg-gray-900 border border-gray-700' : 'bg-white/80 text-gray-700 hover:bg-white border border-gray-200/80 shadow-lg'
      ]"
    >
      <i class="fas fa-bars"></i>
    </button>

    <!-- 主题切换按钮 -->
    <button
      @click="handleThemeToggle"
      :class="[
        'fixed bottom-5 right-5 z-50 w-12 h-12 rounded-2xl border-none text-lg cursor-pointer transition-all duration-300 flex items-center justify-center backdrop-blur-lg theme-toggle shadow-lg hover:scale-105',
        isDark ? 'bg-black text-yellow-400 hover:bg-gray-900 border border-gray-700' : 'bg-white/80 text-orange-500 hover:bg-white/90 border border-gray-200/80'
      ]"
    >
      {{ isDark ? '🌞' : '🌙' }}
    </button>

    <!-- 侧边栏容器 -->
    <div class="relative z-40">
      <!-- 侧边栏 -->
      <div
        @mouseenter="handleSidebarMouseEnter"
        @mouseleave="handleSidebarMouseLeave"
        :class="[
          'fixed left-0 top-0 h-screen flex flex-col transition-all duration-500 ease-out z-50 backdrop-blur-xl',
          'lg:translate-x-0',
          (isSidebarHovered || !isCollapsed) ? 'w-72' : 'w-20',
          isMobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
          isDark
            ? 'bg-black border-r border-gray-800 shadow-2xl'
            : 'bg-white/80 border-r border-gray-200/80 shadow-2xl'
        ]"
      >
        <!-- 侧边栏头部 -->
        <div
          :class="[
            'p-6 flex items-center gap-4 relative transition-all duration-500 border-b',
            isDark ? 'border-gray-800' : 'border-gray-200/50'
          ]"
        >
          <div class="brand-mark">
            <span>N</span>
          </div>
          <div
            :class="[
              'brand-label text-2xl font-bold transition-all duration-500',
              (isSidebarHovered || !isCollapsed) ? 'opacity-100' : 'opacity-0 w-0'
            ]"
          >
            Nexus
          </div>
        </div>

        <!-- 返回按钮（当有子菜单时显示） -->
        <div
          v-if="currentParent && (isSidebarHovered || !isCollapsed)"
          :class="[
            'px-6 py-4 border-b transition-all duration-300 cursor-pointer group back-button',
            isDark ? 'border-gray-800 hover:bg-gray-900' : 'border-gray-200/50 hover:bg-gray-50/80'
          ]"
          @click="goBack"
        >
          <div class="flex items-center text-sm font-semibold transition-all duration-300 group-hover:translate-x-1">
            <i class="fas fa-arrow-left mr-3 transition-transform duration-300 group-hover:-translate-x-1"></i>
            <span :class="isDark ? 'text-gray-300' : 'text-gray-700'">返回上级</span>
          </div>
        </div>

        <!-- 当前菜单标题 -->
        <div
          v-if="currentParent && (isSidebarHovered || !isCollapsed)"
          :class="[
            'px-6 py-3 text-xs font-bold uppercase tracking-wider transition-colors duration-300',
            isDark ? 'text-gray-500 bg-gray-900' : 'text-gray-500 bg-gray-100/50'
          ]"
        >
          {{ currentParent.text }}
        </div>

        <!-- 侧边栏导航 -->
        <div
          :class="[
            'flex-1 py-6 overflow-y-auto smooth-scroll',
            'max-h-[calc(100vh-200px)]',
            'custom-scrollbar'
          ]"
        >
          <!-- 显示主菜单 -->
          <template v-if="!currentParent">
            <div
              v-for="section in navSections"
              :key="section.label"
              class="mb-8"
            >
              <div
                :class="[
                  'text-xs font-bold uppercase tracking-wider px-6 pb-3 transition-all duration-500',
                  (isSidebarHovered || !isCollapsed) ? 'opacity-100' : 'opacity-0 h-0 p-0 m-0',
                  isDark ? 'text-gray-500' : 'text-gray-500'
                ]"
              >
                {{ section.label }}
              </div>
              <div
                v-for="item in section.items"
                :key="item.id"
                v-show="!item.roles || item.roles.includes(User.role)"
                @click="setActiveNavItem(item)"
                :class="[
                  'group relative mx-4 mb-2 px-4 py-3 rounded-xl transition-all duration-500 cursor-pointer border menu-item-card',
                  isDark
                    ? 'border-gray-800 hover:border-purple-500/50 hover:bg-gray-900 text-gray-300'
                    : 'border-gray-200/80 hover:border-purple-400/50 hover:bg-white/80 text-gray-600',
                  isActiveItem(item)
                    ? (isDark
                        ? 'bg-gradient-to-r from-purple-500/20 to-pink-500/20 border-purple-500 text-white shadow-lg shadow-purple-500/20'
                        : 'bg-gradient-to-r from-purple-50 to-pink-50 border-purple-400/80 text-purple-700 shadow-lg shadow-purple-500/10')
                    : ''
                ]"
              >
                <div class="flex items-center gap-3">
                  <div
                    :class="[
                      'w-8 h-8 rounded-xl flex items-center justify-center text-lg transition-all duration-500 group-hover:scale-110 group-hover:rotate-12',
                      isActiveItem(item)
                        ? (isDark ? 'bg-purple-500/30 text-purple-300' : 'bg-purple-100 text-purple-600')
                        : (isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500')
                    ]"
                  >
                    {{ item.icon }}
                  </div>
                  <div
                    :class="[
                      'font-semibold transition-all duration-500 flex-1',
                      (isSidebarHovered || !isCollapsed) ? 'opacity-100' : 'opacity-0 w-0'
                    ]"
                  >
                    {{ item.text }}
                  </div>
                  <!-- 子菜单指示器 -->
                  <div
                    v-if="hasChildren(item) && (isSidebarHovered || !isCollapsed)"
                    :class="[
                      'text-xs transition-all duration-500 group-hover:translate-x-1',
                      isDark ? 'text-gray-600 group-hover:text-purple-400' : 'text-gray-400 group-hover:text-purple-500'
                    ]"
                  >
                    <i class="fas fa-chevron-right"></i>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 显示子菜单 -->
          <template v-else>
            <div
              v-for="item in activeMenuList"
              :key="item.id"
              v-show="!item.roles || item.roles.includes(User.role)"
              @click="setActiveNavItem(item)"
              :class="[
                'group relative mx-4 mb-2 px-4 py-3 rounded-xl transition-all duration-500 cursor-pointer border menu-item-card animate-fade-in',
                isDark
                  ? 'border-gray-800 hover:border-purple-500/50 hover:bg-gray-900 text-gray-300'
                  : 'border-gray-200/80 hover:border-purple-400/50 hover:bg-white/80 text-gray-600',
                isActiveItem(item)
                  ? (isDark
                      ? 'bg-gradient-to-r from-purple-500/20 to-pink-500/20 border-purple-500 text-white shadow-lg shadow-purple-500/20'
                      : 'bg-gradient-to-r from-purple-50 to-pink-50 border-purple-400/80 text-purple-700 shadow-lg shadow-purple-500/10')
                  : ''
              ]"
            >
              <div class="flex items-center gap-3">
                <div
                  :class="[
                    'w-8 h-8 rounded-xl flex items-center justify-center text-lg transition-all duration-500 group-hover:scale-110',
                    isActiveItem(item)
                      ? (isDark ? 'bg-purple-500/30 text-purple-300' : 'bg-purple-100 text-purple-600')
                      : (isDark ? 'bg-gray-800 text-gray-400' : 'bg-gray-100 text-gray-500')
                  ]"
                >
                  {{ item.icon }}
                </div>
                <div
                  :class="[
                    'font-semibold transition-all duration-500 flex-1',
                    (isSidebarHovered || !isCollapsed) ? 'opacity-100' : 'opacity-0 w-0'
                  ]"
                >
                  {{ item.text }}
                </div>
                <!-- 子菜单指示器 -->
                <div
                  v-if="hasChildren(item) && (isSidebarHovered || !isCollapsed)"
                  :class="[
                    'text-xs transition-all duration-500 group-hover:translate-x-1',
                    isDark ? 'text-gray-600 group-hover:text-purple-400' : 'text-gray-400 group-hover:text-purple-500'
                  ]"
                >
                  <i class="fas fa-chevron-right"></i>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 用户区域 -->
        <div
          :class="[
            'p-6 flex items-center gap-4 transition-all duration-500 border-t',
            isDark ? 'border-gray-800' : 'border-gray-200/50'
          ]"
        >
          <img
            :src="User.avatar"
            alt="头像"
            @click="router.push('/ExamUser')"
            :class="[
              'rounded-2xl cursor-pointer w-12 h-12 user-avatar border-2 transition-all duration-500 hover:scale-105',
              isDark ? 'border-purple-500' : 'border-purple-400/50'
            ]"
          >
          <div
            :class="[
              'transition-all duration-500 overflow-hidden',
              (isSidebarHovered || !isCollapsed) ? 'opacity-100' : 'opacity-0 w-0'
            ]"
          >
            <div
              :class="[
                'font-bold mb-1 transition-all duration-500',
                isDark ? 'text-white' : 'text-gray-900'
              ]"
            >
              {{ User.account || '用户' }}
            </div>
            <div
              :class="[
                'text-xs font-semibold transition-all duration-500 px-2 py-1 rounded-full',
                isDark ? 'text-purple-300 bg-purple-500/20' : 'text-purple-600 bg-purple-100'
              ]"
            >
              {{ User.role || '用户' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.suite-shell {
  min-height: 100vh;
  background-image: var(--page-bg);
  background-attachment: fixed;
}

.suite-shell--dark {
  background-image: var(--page-bg);
}

.suite-shell--light {
  background-image: var(--page-bg);
}

.suite-main {
  min-height: 100vh;
  padding: clamp(1.5rem, 2.5vw, 3rem) clamp(1.5rem, 4vw, 4.5rem);
  background: transparent;
}

.brand-mark {
  width: 3rem;
  height: 3rem;
  border-radius: 1.3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #fff;
  background: linear-gradient(135deg, var(--accent-strong), var(--accent));
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.25);
}

.brand-label {
  color: var(--accent-strong);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* 自定义滚动条 - 隐藏但保持滚动功能 */
.custom-scrollbar {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.custom-scrollbar::-webkit-scrollbar {
  display: none;
}

/* 高级动画效果 */
@keyframes fadeInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes slideInFromLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 20px rgba(180, 139, 79, 0.25);
  }
  50% {
    box-shadow: 0 0 32px rgba(196, 163, 98, 0.55);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-5px);
  }
}

/* 菜单卡片悬停效果 */
.menu-item-card {
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(18px);
  background: var(--surface-muted);
  border: 1px solid var(--border-regular);
  box-shadow: var(--shadow-soft);
}

.menu-item-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.25),
      transparent);
  transition: left 0.6s ease;
}

.menu-item-card:hover::before {
  left: 100%;
}

/* 激活项的光效 */
.menu-item-card:active {
  transform: scale(0.98);
  transition: transform 0.1s ease;
}

/* 淡入动画 */
.animate-fade-in {
  animation: fadeInUp 0.5s ease-out;
}

.animate-fade-in:nth-child(1) { animation-delay: 0.05s; }
.animate-fade-in:nth-child(2) { animation-delay: 0.1s; }
.animate-fade-in:nth-child(3) { animation-delay: 0.15s; }
.animate-fade-in:nth-child(4) { animation-delay: 0.2s; }
.animate-fade-in:nth-child(5) { animation-delay: 0.25s; }

/* 用户头像动画 */
.user-avatar {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  background: var(--surface-contrast);
}

.user-avatar:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.25);
}

.user-avatar::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(120deg, var(--accent-strong), var(--accent));
  border-radius: 16px;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.user-avatar:hover::after {
  opacity: 1;
  animation: rotate 3s linear infinite;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 主题切换按钮动画 */
.theme-toggle {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--surface-muted);
  border: 1px solid var(--border-regular);
}

.theme-toggle:hover {
  transform: scale(1.08);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

/* 移动端菜单按钮动画 */
.mobile-menu-btn {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--surface-muted);
  border: 1px solid var(--border-regular);
}

.mobile-menu-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 25px rgba(0, 0, 0, 0.18);
}

/* 返回按钮动画 */
.back-button {
  position: relative;
  overflow: hidden;
}

.back-button::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent-strong), var(--accent));
  transition: width 0.4s ease;
}

.back-button:hover::before {
  width: 100%;
}

/* 玻璃拟态效果增强 */
.backdrop-blur-xl {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* 平滑滚动 */
.smooth-scroll {
  scroll-behavior: smooth;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar-mobile {
    animation: slideInFromLeft 0.4s ease-out;
  }

  .sidebar-mobile-closing {
    animation: slideInFromLeft 0.4s ease-in reverse;
  }
}

/* 减少动画支持 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 自定义宽度类 */
.w-72 {
  width: 18rem;
}
</style>

<style>
/* 全局样式 */
* {
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-sans, 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif);
  background: inherit;
}

/* 自定义选择文本样式 */
::selection {
  background-color: var(--accent-soft, rgba(180, 139, 79, 0.24));
  color: inherit;
}

::-moz-selection {
  background-color: var(--accent-soft, rgba(180, 139, 79, 0.24));
  color: inherit;
}

/* 打印样式 */
@media print {
  .no-print {
    display: none !important;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  :root {
    --border-opacity: 1;
  }
}

/* 暗色主题全局样式 */
.dark body {
  background: inherit;
}

.dark {
  color-scheme: dark;
}
</style>
