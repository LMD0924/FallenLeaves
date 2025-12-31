import * as echarts from 'echarts';
import {get} from "@/net/index.js";
import {userUserStore} from "@/stores/userStore.js";

// 获取优先级统计数据的函数
const fetchPriorityStats = (callback) => {
  // 从Pinia store获取用户信息
  const userStore = userUserStore();
  if (!userStore.user || !userStore.user.id) {
    console.error('无法获取用户ID，用户信息不存在')
    // 如果无法获取用户ID，使用默认数据
    callback({ high: 0, medium: 0, low: 0 })
    return
  }

  get('/api/memo/priorityStats', { userId: userStore.user.id }, (message, data, status) => {
    console.log('获取到优先级统计数据:', data)
    callback(data)
  }, (errorMessage) => {
    console.error('获取优先级统计数据失败:', errorMessage)
    // 如果API调用失败，使用默认数据
    callback({ high: 0, medium: 0, low: 0 })
  })
}

// 柱状图初始化函数
export function initBarChart(chartDom) {
  const myChart = echarts.init(chartDom);

  // 初始空配置
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['高', '中', '低']
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: '优先级',
      type: 'bar',
      data: [0, 0, 0] // 初始数据
    }]
  };

  myChart.setOption(option);

  // 获取API数据并更新图表
  fetchPriorityStats((data) => {
    const updateOption = {
      series: [{
        data: [
          data.high || 0,
          data.medium || 0,
          data.low || 0
        ]
      }]
    };
    myChart.setOption(updateOption);
  });

  return myChart;
}

// 饼图初始化函数
export function initPieChart(chartDom) {
  const myChart = echarts.init(chartDom);

  const option = {
    title: {
      text: '备忘录优先级分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['高', '中', '低']
    },
    series: [{
      name: 'Priority',
      type: 'pie',
      radius: '50%',
      data: [
        { value: 0, name: '高' },
        { value: 0, name: '中' },
        { value: 0, name: '低' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  };

  myChart.setOption(option);

  // 获取API数据并更新图表
  fetchPriorityStats((data) => {
    const updateOption = {
      series: [{
        data: [
          { value: data.high || 0, name: '高' },
          { value: data.medium || 0, name: '中' },
          { value: data.low || 0, name: '低' }
        ]
      }]
    };
    myChart.setOption(updateOption);
  });

  return myChart;
}
