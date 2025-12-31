<template>
  <div class="min-h-screen bg-black">
    <header class="p-6 border-b border-white/10">
      <div class="flex items-center space-x-4">
        <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-white">我的成绩中心</h1>
          <p class="text-sm text-white/60">查看历史成绩与学习分析</p>
        </div>
      </div>
    </header>

    <main class="p-8">
      <!-- 成绩概览 -->
      <div class="glass-card p-6 mb-8">
        <h3 class="text-lg font-semibold text-white mb-6">📊 成绩概览</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="text-center p-4 bg-white/5 rounded-xl">
            <p class="text-sm text-white/60">已参加考试</p>
            <p class="text-2xl font-bold text-white mt-2">{{ stats.total_exams }}</p>
          </div>
          <div class="text-center p-4 bg-white/5 rounded-xl">
            <p class="text-sm text-white/60">平均分</p>
            <p class="text-2xl font-bold text-green-400 mt-2">{{ stats.average_score?.toFixed(1) }}</p>
          </div>
          <div class="text-center p-4 bg-white/5 rounded-xl">
            <p class="text-sm text-white/60">最高分</p>
            <p class="text-2xl font-bold text-yellow-400 mt-2">{{ stats.max_score }}</p>
          </div>
          <div class="text-center p-4 bg-white/5 rounded-xl">
            <p class="text-sm text-white/60">班级排名</p>
            <p class="text-2xl font-bold text-indigo-400 mt-2">{{ stats.rank || '--' }}</p>
          </div>
        </div>
      </div>

      <!-- 成绩趋势图 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
        <div class="lg:col-span-2">
          <div class="glass-card p-6">
            <h3 class="text-lg font-semibold text-white mb-6">📈 成绩趋势</h3>
            <!-- 成绩趋势图表 -->
            <div class="h-64 flex items-end space-x-2">
              <div v-for="(score, idx) in trendData" :key="idx"
                   class="flex-1 flex flex-col items-center">
                <div class="w-3/4 bg-gradient-to-t from-indigo-500 to-purple-500 rounded-t-lg"
                     :style="{ height: (score / 100) * 200 + 'px' }"></div>
                <span class="text-xs text-white/60 mt-2">{{ score }}分</span>
                <span class="text-xs text-white/40 mt-1">第{{ idx + 1 }}次</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 科目分析 -->
        <div>
          <div class="glass-card p-6">
            <h3 class="text-lg font-semibold text-white mb-4">📚 科目表现</h3>
            <div class="space-y-3">
              <div v-for="subject in subjectStats" :key="subject.id"
                   class="flex items-center justify-between">
                <span class="text-white text-sm">{{ subject.name }}</span>
                <div class="flex items-center space-x-3">
                  <div class="w-16 h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div class="h-full bg-green-500 rounded-full"
                         :style="{ width: (subject.score / subject.total) * 100 + '%' }"></div>
                  </div>
                  <span class="text-sm text-white/70">{{ subject.score }}/{{ subject.total }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 成绩详情表格 -->
      <div class="glass-card p-6">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-lg font-semibold text-white">📋 成绩详情</h3>
          <div class="flex space-x-3">
            <select v-model="filterSubject" class="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm">
              <option value="">全部科目</option>
              <option v-for="subject in subjects" :value="subject.id">{{ subject.name }}</option>
            </select>
            <select v-model="sortBy" class="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm">
              <option value="date_desc">最新在前</option>
              <option value="score_desc">高分在前</option>
              <option value="score_asc">低分在前</option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
            <tr class="border-b border-white/10">
              <th class="pb-3 text-left text-sm text-white/60 font-medium">考试名称</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">科目</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">考试时间</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">得分</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">总分</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">状态</th>
              <th class="pb-3 text-left text-sm text-white/60 font-medium">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="record in filteredRecords" :key="record.id" class="border-b border-white/5 hover:bg-white/5">
              <td class="py-4">
                <p class="text-white font-medium">{{ record.exam_title }}</p>
                <p class="text-sm text-white/60">{{ record.teacher_name }}</p>
              </td>
              <td class="py-4">
                  <span class="px-3 py-1 bg-white/10 text-white rounded-full text-xs">
                    {{ record.course_name }}
                  </span>
              </td>
              <td class="py-4 text-white/70 text-sm">{{ formatDate(record.exam_date) }}</td>
              <td class="py-4">
                  <span :class="[
                    'text-lg font-bold',
                    record.score >= record.pass_score ? 'text-green-400' : 'text-red-400'
                  ]">
                    {{ record.score }}
                  </span>
              </td>
              <td class="py-4 text-white/70">{{ record.total_score }}</td>
              <td class="py-4">
                  <span :class="[
                    'px-3 py-1 rounded-full text-xs font-medium',
                    record.status === 'graded' ? 'bg-green-500/20 text-green-400' :
                    record.status === 'submitted' ? 'bg-yellow-500/20 text-yellow-400' :
                    'bg-gray-500/20 text-gray-400'
                  ]">
                    {{ record.status === 'graded' ? '已批改' : '待批改' }}
                  </span>
              </td>
              <td class="py-4">
                <button @click="viewExamDetail(record)"
                        class="px-3 py-1 bg-indigo-600 hover:bg-indigo-700 rounded-lg text-white text-sm transition-colors">
                  查看详情
                </button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredRecords.length === 0" class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-white/30 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <p class="text-white/50">暂无成绩记录</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const filterSubject = ref('')
const sortBy = ref('date_desc')
const subjects = ref([
  { id: 1, name: '高等数学' },
  { id: 2, name: '大学英语' },
  { id: 3, name: '计算机基础' }
])

const gradeRecords = ref([
  {
    id: 1,
    exam_title: '高等数学期中考试',
    course_name: '高等数学',
    teacher_name: '张老师',
    exam_date: '2024-11-15',
    score: 85,
    total_score: 100,
    pass_score: 60,
    status: 'graded'
  },
  // 更多记录...
])

const stats = ref({
  total_exams: 8,
  average_score: 78.5,
  max_score: 95,
  rank: '12/45'
})

const trendData = ref([78, 82, 85, 79, 88, 85, 90])
const subjectStats = ref([
  { name: '高等数学', score: 85, total: 100 },
  { name: '大学英语', score: 78, total: 100 },
  { name: '计算机基础', score: 92, total: 100 }
])

// 计算属性：过滤和排序成绩记录
const filteredRecords = computed(() => {
  let records = [...gradeRecords.value]

  // 科目过滤
  if (filterSubject.value) {
    records = records.filter(r => r.course_id === filterSubject.value)
  }

  // 排序
  if (sortBy.value === 'date_desc') {
    records.sort((a, b) => new Date(b.exam_date) - new Date(a.exam_date))
  } else if (sortBy.value === 'score_desc') {
    records.sort((a, b) => b.score - a.score)
  } else if (sortBy.value === 'score_asc') {
    records.sort((a, b) => a.score - b.score)
  }

  return records
})

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

const viewExamDetail = (record) => {
  console.log('查看考试详情:', record)
  // 跳转到详情页面
}
</script>
