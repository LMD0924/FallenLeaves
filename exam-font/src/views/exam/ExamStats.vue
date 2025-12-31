<template>
  <div :class="['min-h-screen transition-all duration-500', isDark ? 'bg-black' : 'bg-gray-50']">
    <!-- 顶部导航 -->
    <header class="p-6 border-b border-white/10">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
            </svg>
          </div>
          <div>
            <h1 class="text-2xl font-bold text-white">考试统计与批改</h1>
            <p class="text-sm text-white/60">查看成绩分布、统计分析、手动批改</p>
          </div>
        </div>
        <div class="flex items-center space-x-4">
          <select v-model="selectedExam" class="px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white">
            <option value="">选择考试</option>
            <option v-for="exam in exams" :value="exam.id">{{ exam.title }}</option>
          </select>
        </div>
      </div>
    </header>

    <main class="p-8">
      <!-- 统计概览卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="glass-card p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/60">参加人数</p>
              <p class="text-3xl font-bold text-white">{{ stats.participants || 0 }}</p>
            </div>
            <div class="w-14 h-14 rounded-2xl bg-blue-500/20 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5 1.597a8.5 8.5 0 01-10.5 0"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="glass-card p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/60">平均分</p>
              <p class="text-3xl font-bold text-white">{{ stats.average_score?.toFixed(1) || '0.0' }}</p>
            </div>
            <div class="w-14 h-14 rounded-2xl bg-green-500/20 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="glass-card p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/60">最高分</p>
              <p class="text-3xl font-bold text-white">{{ stats.max_score || 0 }}</p>
            </div>
            <div class="w-14 h-14 rounded-2xl bg-yellow-500/20 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="glass-card p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-white/60">及格率</p>
              <p class="text-3xl font-bold text-white">{{ stats.pass_rate ? (stats.pass_rate * 100).toFixed(1) + '%' : '0%' }}</p>
            </div>
            <div class="w-14 h-14 rounded-2xl bg-purple-500/20 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-7 w-7 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- 左侧：成绩分布图 -->
        <div class="lg:col-span-2">
          <div class="glass-card p-6">
            <h3 class="text-lg font-semibold text-white mb-6">成绩分布</h3>
            <div class="h-64">
              <!-- 这里可以集成图表库，如Chart.js或ECharts -->
              <div class="flex items-end h-48 space-x-2">
                <div v-for="(bar, idx) in scoreDistribution" :key="idx"
                     class="flex-1 flex flex-col items-center">
                  <div class="w-full bg-gradient-to-t from-blue-500 to-indigo-500 rounded-t-lg"
                       :style="{ height: (bar.value / maxDistribution) * 100 + '%' }"></div>
                  <span class="text-xs text-white/60 mt-2">{{ bar.range }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 待批改列表 -->
          <div class="glass-card p-6 mt-6">
            <div class="flex items-center justify-between mb-6">
              <h3 class="text-lg font-semibold text-white">待批改试卷</h3>
              <span class="px-3 py-1 bg-yellow-500/20 text-yellow-400 rounded-full text-sm">
                {{ pendingGrading.length }} 份
              </span>
            </div>

            <div class="space-y-4">
              <div v-for="record in pendingGrading" :key="record.id"
                   class="flex items-center justify-between p-4 bg-white/5 rounded-xl border border-white/10 hover:border-indigo-400/30 transition-colors">
                <div>
                  <p class="text-white font-medium">{{ record.student_name }}</p>
                  <p class="text-sm text-white/60">学号: {{ record.student_id }}</p>
                  <p class="text-xs text-white/40">提交时间: {{ formatTime(record.submit_time) }}</p>
                </div>
                <div class="flex items-center space-x-3">
                  <span class="px-3 py-1 bg-yellow-500/20 text-yellow-400 rounded-full text-sm">
                    待批改
                  </span>
                  <button @click="startGrading(record)"
                          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white transition-colors">
                    开始批改
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：统计分析 -->
        <div>
          <div class="glass-card p-6 mb-6">
            <h3 class="text-lg font-semibold text-white mb-4">题目正确率排行</h3>
            <div class="space-y-3">
              <div v-for="(question, idx) in questionStats" :key="question.id"
                   class="flex items-center justify-between p-3 bg-white/5 rounded-lg">
                <div class="flex-1">
                  <p class="text-sm text-white truncate">第{{ idx + 1 }}题: {{ question.content }}</p>
                  <div class="w-full h-1.5 bg-white/10 rounded-full mt-1 overflow-hidden">
                    <div class="h-full bg-green-500 rounded-full"
                         :style="{ width: (question.correct_rate * 100) + '%' }"></div>
                  </div>
                </div>
                <span class="ml-3 text-sm text-white/70">{{ (question.correct_rate * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </div>

          <div class="glass-card p-6">
            <h3 class="text-lg font-semibold text-white mb-4">快速操作</h3>
            <div class="space-y-3">
              <button @click="exportGrades"
                      class="w-full flex items-center justify-between p-3 bg-white/5 hover:bg-white/10 rounded-lg transition-colors group">
                <div class="flex items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-400 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                  <span class="text-white">导出成绩单</span>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-white/40 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>

              <button @click="sendNotifications"
                      class="w-full flex items-center justify-between p-3 bg-white/5 hover:bg-white/10 rounded-lg transition-colors group">
                <div class="flex items-center">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-400 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
                  </svg>
                  <span class="text-white">发送成绩通知</span>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-white/40 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const selectedExam = ref('')
const exams = ref([])
const stats = ref({})
const pendingGrading = ref([])
const questionStats = ref([])

// 模拟数据
const scoreDistribution = ref([
  { range: '0-59', value: 5 },
  { range: '60-69', value: 12 },
  { range: '70-79', value: 18 },
  { range: '80-89', value: 25 },
  { range: '90-100', value: 15 }
])

const maxDistribution = computed(() => Math.max(...scoreDistribution.value.map(d => d.value)))

// 方法
const startGrading = (record) => {
  // 跳转到批改页面
  console.log('开始批改记录:', record)
}

const exportGrades = () => {
  // 导出成绩
  console.log('导出成绩')
}

const sendNotifications = () => {
  // 发送通知
  console.log('发送成绩通知')
}

const formatTime = (time) => {
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
/*.glass-card {
  @apply backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl border border-white/10;
}*/
</style>
