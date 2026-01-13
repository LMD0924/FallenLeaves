<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 顶部标题和筛选 -->
    <div class="bg-white shadow-sm border-b">
      <div class="container mx-auto px-6 py-4">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 class="text-2xl font-bold text-gray-800">考试记录管理</h1>
            <p class="text-gray-600 mt-1">查看和管理学生的考试情况</p>
          </div>

          <div class="flex flex-wrap items-center gap-3">
            <!-- 试卷选择 -->
            <a-select
              v-model:value="selectedExamId"
              placeholder="选择试卷"
              style="width: 200px"
              @change="loadExamRecords"
              :loading="loadingExams"
            >
              <a-select-option v-for="exam in exams" :key="exam.id" :value="exam.id">
                {{ exam.title }}
              </a-select-option>
            </a-select>

            <!-- 状态筛选 -->
            <a-select
              v-model:value="statusFilter"
              placeholder="筛选状态"
              style="width: 140px"
              @change="loadExamRecords"
            >
              <a-select-option value="all">全部状态</a-select-option>
              <a-select-option value="not_started">未开始</a-select-option>
              <a-select-option value="in_progress">考试中</a-select-option>
              <a-select-option value="submitted">已提交</a-select-option>
              <a-select-option value="graded">已批改</a-select-option>
            </a-select>

            <!-- 搜索框 -->
            <a-input-search
              v-model:value="searchText"
              placeholder="搜索学生"
              style="width: 250px"
              @search="handleSearch"
            />

            <!-- 刷新按钮 -->
            <a-button @click="loadExamRecords" :loading="loading">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container mx-auto px-6 py-8">
      <!-- 统计卡片 -->
      <div v-if="selectedExamId" class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="bg-white rounded-xl shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-blue-100 rounded-lg mr-4">
              <UserOutlined class="text-2xl text-blue-600" />
            </div>
            <div>
              <p class="text-gray-500 text-sm">总人数</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.totalStudents || 0 }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-green-100 rounded-lg mr-4">
              <CheckCircleOutlined class="text-2xl text-green-600" />
            </div>
            <div>
              <p class="text-gray-500 text-sm">已提交</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.submitted || 0 }}</p>
              <p class="text-sm text-gray-400 mt-1">{{ stats.submittedPercentage || 0 }}%</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-yellow-100 rounded-lg mr-4">
              <ClockCircleOutlined class="text-2xl text-yellow-600" />
            </div>
            <div>
              <p class="text-gray-500 text-sm">考试中</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.inProgress || 0 }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow p-6">
          <div class="flex items-center">
            <div class="p-3 bg-red-100 rounded-lg mr-4">
              <WarningOutlined class="text-2xl text-red-600" />
            </div>
            <div>
              <p class="text-gray-500 text-sm">未开始</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.notStarted || 0 }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 考试记录表格 -->
      <div class="bg-white rounded-xl shadow overflow-hidden">
        <div class="p-6 border-b">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-800">考试记录列表</h2>
            <div class="text-sm text-gray-500">
              共 {{ records.length }} 条记录
            </div>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="records"
          :loading="loading"
          :pagination="pagination"
          @change="handleTableChange"
          row-key="id"
        >
          <!-- 学生信息列 -->
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'student'">
              <div class="flex items-center">
                <div class="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center mr-3">
                  <UserOutlined class="text-indigo-600" />
                </div>
                <div>
                  <p class="font-medium text-gray-800">{{ record.student_name || record.student_account || '未知' }}</p>
                  <p class="text-sm text-gray-500">ID: {{ record.student_id }}</p>
                </div>
              </div>
            </template>

            <!-- 状态列 -->
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ getStatusText(record.status) }}
              </a-tag>
            </template>

            <!-- 分数列 -->
            <template v-else-if="column.key === 'score'">
              <div class="flex items-center">
                <span class="text-lg font-bold" :class="{
                  'text-green-600': record.status === 'graded',
                  'text-gray-400': record.status !== 'graded'
                }">
                  {{ record.total_score || 0 }}
                </span>
                <span v-if="record.status === 'graded'" class="text-gray-500 ml-2">
                  / {{ examTotalScore }}
                </span>
              </div>
            </template>

            <!-- 用时列 -->
            <template v-else-if="column.key === 'time'">
              <div v-if="record.time_spent > 0">
                <span class="font-medium">{{ formatTime(record.time_spent) }}</span>
              </div>
              <span v-else class="text-gray-400">--</span>
            </template>

            <!-- 操作列 -->
            <template v-else-if="column.key === 'actions'">
              <div class="flex items-center space-x-2">
                <!-- 查看详情 -->
                <a-button
                  size="small"
                  @click="viewRecordDetail(record)"
                  :disabled="record.status === 'not_started'"
                >
                  <EyeOutlined />
                  查看
                </a-button>

                <!-- 批改按钮 -->
                <a-button
                  v-if="record.status === 'submitted'"
                  size="small"
                  type="primary"
                  @click="startGrading(record)"
                >
                  <EditOutlined />
                  批改
                </a-button>

                <!-- 重新批改 -->
                <a-button
                  v-if="record.status === 'graded'"
                  size="small"
                  @click="startGrading(record)"
                >
                  <RedoOutlined />
                  重新批改
                </a-button>

                <!-- 导出答案 -->
                <a-button
                  v-if="record.status === 'submitted' || record.status === 'graded'"
                  size="small"
                  @click="exportAnswers(record)"
                >
                  <DownloadOutlined />
                  导出
                </a-button>
              </div>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 批改功能已迁移到独立页面 GradeExam.vue -->

    <!-- 详情弹窗 -->
    <a-modal
      v-model:visible="detailModal.visible"
      :title="`考试详情 - ${detailModal.studentName || ''}`"
      width="800px"
      @cancel="closeDetailModal"
    >
      <RecordDetail
        v-if="detailModal.visible && detailModal.recordId"
        :record-id="detailModal.recordId"
      />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  ReloadOutlined,
  UserOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  EyeOutlined,
  EditOutlined,
  RedoOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import { message, Table as ATable, Modal as AModal } from 'ant-design-vue'
import { get, post } from '@/net/index.js' // 移除了GradingPanel的导入

// 响应式状态
const loading = ref(false)
const loadingExams = ref(false)
const selectedExamId = ref(null)
const statusFilter = ref('all')
const searchText = ref('')
const exams = ref([])
const records = ref([])
const stats = reactive({
  totalStudents: 0,
  submitted: 0,
  inProgress: 0,
  notStarted: 0,
  submittedPercentage: 0
})

// 弹窗状态
const detailModal = reactive({
  visible: false,
  recordId: null,
  studentName: ''
})

// 表格列定义
const columns = [
  {
    title: '学生信息',
    key: 'student',
    width: 200
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    align: 'center'
  },
  {
    title: '开始时间',
    dataIndex: 'start_time',
    key: 'start_time',
    width: 180,
    align: 'center'
  },
  {
    title: '结束时间',
    dataIndex: 'end_time',
    key: 'end_time',
    width: 180,
    align: 'center'
  },
  {
    title: '用时',
    key: 'time',
    width: 120,
    align: 'center'
  },
  {
    title: '得分',
    key: 'score',
    width: 120,
    align: 'center'
  },
  {
    title: '尝试次数',
    dataIndex: 'attempt_count',
    key: 'attempt_count',
    width: 100,
    align: 'center'
  },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    align: 'center'
  }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
})

// 计算试卷总分
const examTotalScore = ref(0)

// 加载考试列表
const loadExams = async () => {
  loadingExams.value = true
  try {
    await new Promise((resolve, reject) => {
      get('/api/exam/list', {},
        (msg, data) => {
          exams.value = data
          if (data.length > 0 && !selectedExamId.value) {
            selectedExamId.value = data[0].id
            loadExamRecords()
          }
          resolve(data)
        },
        (error) => {
          message.error('加载考试列表失败')
          reject(error)
        }
      )
    })
  } catch (error) {
    console.error('加载考试列表失败:', error)
  } finally {
    loadingExams.value = false
  }
}

// 加载考试记录
const loadExamRecords = async () => {
  if (!selectedExamId.value) return

  loading.value = true
  try {
    const params = {
      examId: selectedExamId.value,
      status: statusFilter.value === 'all' ? '' : statusFilter.value,
      search: searchText.value,
      page: pagination.current,
      pageSize: pagination.pageSize
    }

    await new Promise((resolve, reject) => {
      get('/api/exam/record/list', params,
        (msg, data) => {
          records.value = data.records || []
          pagination.total = data.total || 0

          // 计算统计信息
          calculateStats(data.records || [])

          // 获取试卷总分
          if (data.exam_info) {
            examTotalScore.value = data.exam_info.total_score || 100
          }

          resolve(data)
        },
        (error) => {
          message.error('加载考试记录失败')
          reject(error)
        }
      )
    })
  } catch (error) {
    console.error('加载考试记录失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算统计信息
const calculateStats = (records) => {
  const statsData = {
    totalStudents: records.length,
    submitted: 0,
    inProgress: 0,
    notStarted: 0
  }

  records.forEach(record => {
    switch (record.status) {
      case 'submitted':
      case 'graded':
        statsData.submitted++
        break
      case 'in_progress':
        statsData.inProgress++
        break
      case 'not_started':
        statsData.notStarted++
        break
    }
  })

  statsData.submittedPercentage = statsData.totalStudents > 0
    ? Math.round((statsData.submitted / statsData.totalStudents) * 100)
    : 0

  Object.assign(stats, statsData)
}

// 表格状态工具函数
const getStatusColor = (status) => {
  switch (status) {
    case 'not_started': return 'default'
    case 'in_progress': return 'blue'
    case 'submitted': return 'orange'
    case 'graded': return 'green'
    default: return 'default'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'not_started': return '未开始'
    case 'in_progress': return '考试中'
    case 'submitted': return '已提交'
    case 'graded': return '已批改'
    default: return status
  }
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds) return '0分钟'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.current = 1
  loadExamRecords()
}

// 表格分页/排序变化
const handleTableChange = (newPagination) => {
  pagination.current = newPagination.current
  pagination.pageSize = newPagination.pageSize
  loadExamRecords()
}

// 查看详情
const viewRecordDetail = (record) => {
  detailModal.recordId = record.id
  detailModal.studentName = record.student_name || record.student_account || '学生'
  detailModal.visible = true
}

// 开始批改
import { useRouter } from 'vue-router'
const router = useRouter()

const startGrading = (record) => {
  // 跳转到独立的批改页面
  router.push({
    path: '/exam/GradeExam',
    query: {
      recordId: record.id,
      studentId: record.student_id,
      examId: selectedExamId.value
    }
  })
}

// 导出答案
const exportAnswers = async (record) => {
  try {
    await new Promise((resolve, reject) => {
      get('/api/exam/record/export-answers', {
          examRecordId: record.id,
          studentId: record.student_id,
          examId: selectedExamId.value
        },
        (msg, data) => {
          // 创建下载链接
          const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `考试答案_${record.student_name}_${new Date().toISOString().split('T')[0]}.json`
          document.body.appendChild(a)
          a.click()
          document.body.removeChild(a)
          URL.revokeObjectURL(url)

          message.success('导出成功')
          resolve(data)
        },
        (error) => {
          message.error('导出失败')
          reject(error)
        })
    })
  } catch (error) {
    console.error('导出失败:', error)
  }
}

// 关闭弹窗
const closeDetailModal = () => {
  detailModal.visible = false
  detailModal.recordId = null
  detailModal.studentName = ''
}

// 组件挂载时加载数据
onMounted(() => {
  loadExams()
})
</script>
