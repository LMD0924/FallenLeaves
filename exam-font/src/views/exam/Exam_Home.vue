<script setup>
import { isDark, toggleTheme, currentTheme } from '@/stores/theme.js'
// 主题切换功能

// 功能卡片数据
const features = [
  {
    title: '在线考试',
    description: '支持多种题型，实时答题，自动计时，提供流畅的考试体验。',
    icon: '<svg class="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path></svg>',
    iconBg: 'bg-blue-100',
    items: ['单选题、多选题、判断题', '填空题、简答题', '实时倒计时提醒'],
    itemColor: 'bg-blue-500'
  },
  {
    title: '智能防作弊',
    description: '多重安全机制，确保考试公平公正，维护考试严肃性。',
    icon: '<svg class="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>',
    iconBg: 'bg-green-100',
    items: ['人脸识别验证', '屏幕监控检测', '异常行为预警'],
    itemColor: 'bg-green-500'
  },
  {
    title: '自动评分',
    description: '客观题自动评分，主观题辅助评分，快速生成成绩报告。',
    icon: '<svg class="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>',
    iconBg: 'bg-purple-100',
    items: ['客观题即时评分', '主观题评分模板', '详细成绩分析报告'],
    itemColor: 'bg-purple-500'
  },
  {
    title: '题库管理',
    description: '强大的题库管理系统，支持试题分类、标签和批量导入。',
    icon: '<svg class="w-8 h-8 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>',
    iconBg: 'bg-orange-100',
    items: ['智能试题分类', '批量导入导出', '试题难度分级'],
    itemColor: 'bg-orange-500'
  },
  {
    title: '成绩分析',
    description: '多维度的成绩统计分析，可视化报表，助力教学改进。',
    icon: '<svg class="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path></svg>',
    iconBg: 'bg-red-100',
    items: ['成绩分布统计', '知识点掌握分析', '个人成长轨迹'],
    itemColor: 'bg-red-500'
  },
  {
    title: '系统设置',
    description: '灵活的考试配置，满足不同场景需求，支持个性化定制。',
    icon: '<svg class="w-8 h-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>',
    iconBg: 'bg-indigo-100',
    items: ['考试时间配置', '题目随机排序', '自定义考试规则'],
    itemColor: 'bg-indigo-500'
  }
]

// 系统优势数据
const advantages = [
  {
    title: '高效便捷',
    description: '一键组卷，自动评分，大幅提升工作效率',
    icon: '<svg class="w-10 h-10 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>',
    iconBg: 'bg-blue-100'
  },
  {
    title: '安全可靠',
    description: '多重安全机制，保障考试数据安全',
    icon: '<svg class="w-10 h-10 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>',
    iconBg: 'bg-green-100'
  },
  {
    title: '智能分析',
    description: '深度数据分析，提供精准教学建议',
    icon: '<svg class="w-10 h-10 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z"></path></svg>',
    iconBg: 'bg-purple-100'
  },
  {
    title: '多端支持',
    description: 'PC端、移动端完美适配，随时随地考试',
    icon: '<svg class="w-10 h-10 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>',
    iconBg: 'bg-orange-100'
  }
]
</script>

<template>
  <div :class="['min-h-screen', isDark ? 'bg-black' : 'bg-gradient-to-br from-blue-50 to-indigo-100']">
    <!-- 主要内容 -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <!-- 欢迎区域 -->
      <section class="mt-24 text-center mb-16">
        <h2 class="text-4xl font-bold mb-4" :class="isDark ? 'text-white' : 'text-gray-900'">
          欢迎使用智能考试系统
        </h2>
        <p class="text-xl max-w-3xl mx-auto" :class="isDark ? 'text-gray-300' : 'text-gray-600'">
          为企业、学校和教育机构提供全方位的在线考试解决方案，<br>
          让考试管理更简单、更高效、更安全。
        </p>
      </section>

      <!-- 核心功能展示 -->
      <section class="mb-16">
        <h3 class="text-2xl font-bold text-center mb-12" :class="isDark ? 'text-white' : 'text-gray-900'">核心功能</h3>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- 功能卡片循环渲染 -->
          <div
            v-for="feature in features"
            :key="feature.title"
            :class="[
              'rounded-2xl shadow-lg p-8 hover:shadow-xl transition-shadow',
              isDark ? 'bg-black border border-white/20' : 'bg-white'
            ]"
          >
            <div :class="['w-16 h-16', feature.iconBg, 'rounded-2xl flex items-center justify-center mb-6']">
              <div v-html="feature.icon"></div>
            </div>
            <h4 class="text-xl font-semibold mb-3" :class="isDark ? 'text-white' : 'text-gray-900'">
              {{ feature.title }}
            </h4>
            <p class="mb-4" :class="isDark ? 'text-gray-300' : 'text-gray-600'">
              {{ feature.description }}
            </p>
            <ul class="text-sm space-y-2" :class="isDark ? 'text-gray-400' : 'text-gray-500'">
              <li
                v-for="(item, index) in feature.items"
                :key="index"
                class="flex items-center"
              >
                <span :class="['w-2 h-2', feature.itemColor, 'rounded-full mr-3']"></span>
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </section>

      <!-- 特色优势 -->
      <section class="rounded-2xl shadow-lg p-12 mb-16" :class="isDark ? 'bg-black border border-white/20' : 'bg-white'">
        <h3 class="text-2xl font-bold text-center mb-12" :class="isDark ? 'text-white' : 'text-gray-900'">系统优势</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          <!-- 系统优势循环渲染 -->
          <div v-for="advantage in advantages" :key="advantage.title" class="text-center">
            <div :class="['w-20 h-20', advantage.iconBg, 'rounded-2xl flex items-center justify-center mx-auto mb-4']">
              <div v-html="advantage.icon"></div>
            </div>
            <h4 class="text-lg font-semibold mb-2" :class="isDark ? 'text-white' : 'text-gray-900'">
              {{ advantage.title }}
            </h4>
            <p class="text-sm" :class="isDark ? 'text-gray-400' : 'text-gray-600'">
              {{ advantage.description }}
            </p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
/* 可以添加自定义样式 */
</style>
