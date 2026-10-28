<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'
import { behaviorText, borrowStatusText, roleText } from '@/utils/format'

const loading = ref(true)
const overview = ref(null)
const trendRef = ref(null)
const categoryRef = ref(null)
const behaviorRef = ref(null)
const hotRef = ref(null)
const roleRef = ref(null)
const statusRef = ref(null)

const charts = []
const palette = ['#2f7a61', '#c8893c', '#5d8fa8', '#8a7bb0', '#b56a58', '#7f9c6a', '#c4a35a', '#5d9c98']

function mount(el, option) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  charts.push(chart)
}

function baseTooltip(extra = {}) {
  return {
    backgroundColor: 'rgba(20, 46, 38, 0.92)',
    borderWidth: 0,
    textStyle: { color: '#e8f2ed', fontSize: 11 },
    ...extra
  }
}

function renderTrend(data) {
  mount(trendRef.value, {
    grid: { left: 44, right: 20, top: 26, bottom: 34 },
    tooltip: baseTooltip({ trigger: 'axis' }),
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#dfe8e2' } },
      axisTick: { show: false },
      axisLabel: { color: '#93a29a', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f4f1' } },
      axisLabel: { color: '#93a29a', fontSize: 10 }
    },
    series: [
      {
        name: '借阅量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: data.map((item) => item.value),
        lineStyle: { width: 2.5, color: '#2f7a61' },
        itemStyle: { color: '#2f7a61', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(47, 122, 97, 0.24)' },
            { offset: 1, color: 'rgba(47, 122, 97, 0.015)' }
          ])
        }
      }
    ]
  })
}

function renderDonut(el, data, centerText) {
  mount(el, {
    color: palette,
    tooltip: baseTooltip({ trigger: 'item', formatter: '{b}<br/>{c} 本 ({d}%)' }),
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'middle',
      itemWidth: 8,
      itemHeight: 8,
      icon: 'circle',
      textStyle: { color: '#6f8177', fontSize: 10 },
      formatter: (name) => (name.length > 6 ? `${name.slice(0, 6)}…` : name)
    },
    graphic: [
      {
        type: 'text',
        left: '29%',
        top: '42%',
        style: {
          text: String(centerText),
          textAlign: 'center',
          fill: '#244a3b',
          fontSize: 24,
          fontWeight: 700
        }
      },
      {
        type: 'text',
        left: '30%',
        top: '55%',
        style: {
          text: '总计',
          textAlign: 'center',
          fill: '#9aa8a0',
          fontSize: 10
        }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['34%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
        label: { show: false },
        emphasis: { scale: true, scaleSize: 5 },
        data: data.map((item) => ({ name: item.name, value: Number(item.value) }))
      }
    ]
  })
}

function renderBehavior(data) {
  const mapped = data.map((item) => ({
    name: behaviorText(item.name),
    value: Number(item.value)
  }))
  mount(behaviorRef.value, {
    grid: { left: 60, right: 24, top: 16, bottom: 28 },
    tooltip: baseTooltip({ trigger: 'axis', axisPointer: { type: 'shadow' } }),
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f4f1' } },
      axisLabel: { color: '#93a29a', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: mapped.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6f8177', fontSize: 10 }
    },
    series: [
      {
        type: 'bar',
        data: mapped.map((item, index) => ({
          value: item.value,
          itemStyle: {
            color: palette[index % palette.length],
            borderRadius: [0, 5, 5, 0]
          }
        })),
        barWidth: 13,
        label: { show: true, position: 'right', color: '#8a9a92', fontSize: 10 }
      }
    ]
  })
}

function renderHot(data) {
  const items = data.slice().reverse()
  mount(hotRef.value, {
    grid: { left: 8, right: 34, top: 12, bottom: 24, containLabel: true },
    tooltip: baseTooltip({ trigger: 'axis', axisPointer: { type: 'shadow' } }),
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f4f1' } },
      axisLabel: { color: '#93a29a', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => (item.name.length > 10 ? `${item.name.slice(0, 10)}…` : item.name)),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6f8177', fontSize: 10, width: 130, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        data: items.map((item) => Number(item.value)),
        barWidth: 11,
        itemStyle: {
          borderRadius: [0, 5, 5, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#c8893c' },
            { offset: 1, color: '#e0b478' }
          ])
        },
        label: { show: true, position: 'right', color: '#a3742d', fontSize: 10 }
      }
    ]
  })
}

function renderRole(data) {
  const mapped = data.map((item) => ({ name: roleText(item.name), value: Number(item.value) }))
  mount(roleRef.value, {
    color: ['#2f7a61', '#c8893c', '#5d8fa8'],
    tooltip: baseTooltip({ trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' }),
    legend: {
      bottom: 0,
      itemWidth: 8,
      itemHeight: 8,
      icon: 'circle',
      textStyle: { color: '#6f8177', fontSize: 10 }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '65%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { scale: true },
        data: mapped
      }
    ]
  })
}

function renderStatus(data) {
  const mapped = data.map((item) => ({ name: borrowStatusText(item.name), value: Number(item.value) }))
  mount(statusRef.value, {
    color: ['#2f7a61', '#9aa8a0', '#c05e42'],
    tooltip: baseTooltip({ trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' }),
    legend: {
      bottom: 0,
      itemWidth: 8,
      itemHeight: 8,
      icon: 'circle',
      textStyle: { color: '#6f8177', fontSize: 10 }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '65%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 3 },
        label: { show: false },
        emphasis: { scale: true },
        data: mapped
      }
    ]
  })
}

function handleResize() {
  charts.forEach((chart) => chart.resize())
}

async function load() {
  loading.value = true
  try {
    const [overviewData, trendData, categoryData, behaviorData, hotData, roleData, statusData] =
      await Promise.all([
        adminApi.overview().catch(() => null),
        adminApi.borrowTrend(14).catch(() => []),
        adminApi.categoryStats().catch(() => []),
        adminApi.behaviorStats().catch(() => []),
        adminApi.hotBooks(10).catch(() => []),
        adminApi.roleStats().catch(() => []),
        adminApi.borrowStatus().catch(() => [])
      ])
    overview.value = overviewData
    await nextTick()
    renderTrend(trendData || [])
    renderDonut(categoryRef.value, categoryData || [], (categoryData || []).reduce((sum, item) => sum + Number(item.value), 0))
    renderBehavior(behaviorData || [])
    renderHot(hotData || [])
    renderRole(roleData || [])
    renderStatus(statusData || [])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((chart) => chart.dispose())
})
</script>

<template>
  <div v-loading="loading" class="stats-page">
    <section class="page-head">
      <div>
        <span class="eyebrow">ANALYTICS</span>
        <h1>统计分析</h1>
        <p>馆藏结构、借阅趋势与读者行为的可视化分析。</p>
      </div>
      <div class="head-pills">
        <span>近 14 天借阅趋势</span>
        <span>实时数据库统计</span>
      </div>
    </section>

    <section class="metric-row">
      <div class="metric">
        <span>馆藏图书</span>
        <strong>{{ overview?.bookCount ?? '—' }}</strong>
        <small>{{ overview?.categoryCount ?? '—' }} 个分类</small>
      </div>
      <div class="metric">
        <span>注册用户</span>
        <strong>{{ overview?.userCount ?? '—' }}</strong>
        <small>含管理员</small>
      </div>
      <div class="metric">
        <span>累计借阅</span>
        <strong>{{ overview?.borrowCount ?? '—' }}</strong>
        <small>当前在借 {{ overview?.activeBorrowCount ?? '—' }}</small>
      </div>
      <div class="metric">
        <span>行为记录</span>
        <strong>{{ overview?.behaviorCount ?? '—' }}</strong>
        <small>AI 对话 {{ overview?.aiChatCount ?? '—' }} 次</small>
      </div>
    </section>

    <section class="chart-card wide">
      <div class="chart-head">
        <div>
          <h3>借阅趋势</h3>
          <p>最近 14 天每日借阅数量</p>
        </div>
        <span class="legend-dot"><i></i>借阅量</span>
      </div>
      <div ref="trendRef" class="chart trend"></div>
    </section>

    <section class="chart-grid">
      <div class="chart-card">
        <div class="chart-head">
          <div>
            <h3>馆藏分类分布</h3>
            <p>各学科在架图书数量</p>
          </div>
        </div>
        <div ref="categoryRef" class="chart"></div>
      </div>

      <div class="chart-card">
        <div class="chart-head">
          <div>
            <h3>读者行为分布</h3>
            <p>浏览、收藏、评分与借阅占比</p>
          </div>
        </div>
        <div ref="behaviorRef" class="chart"></div>
      </div>
    </section>

    <section class="chart-grid">
      <div class="chart-card">
        <div class="chart-head">
          <div>
            <h3>热门图书 TOP 10</h3>
            <p>按累计借阅次数排序</p>
          </div>
        </div>
        <div ref="hotRef" class="chart hot"></div>
      </div>

      <div class="chart-card">
        <div class="chart-head">
          <div>
            <h3>用户角色构成</h3>
            <p>管理员与读者账号占比</p>
          </div>
        </div>
        <div ref="roleRef" class="chart small-pie"></div>
      </div>
    </section>

    <section class="chart-grid">
      <div class="chart-card">
        <div class="chart-head">
          <div>
            <h3>借阅状态分布</h3>
            <p>借阅中、已归还与逾期记录</p>
          </div>
        </div>
        <div ref="statusRef" class="chart small-pie"></div>
      </div>

      <div class="chart-card insight">
        <div class="chart-head">
          <div>
            <h3>数据洞察</h3>
            <p>基于当前统计结果自动生成</p>
          </div>
        </div>
        <div class="insight-body">
          <div class="insight-item">
            <span class="i-icon"><el-icon><Reading /></el-icon></span>
            <div>
              <strong>馆藏规模</strong>
              <p>当前在架 {{ overview?.bookCount ?? 0 }} 本图书，覆盖 {{ overview?.categoryCount ?? 0 }} 个学科分类。</p>
            </div>
          </div>
          <div class="insight-item">
            <span class="i-icon"><el-icon><Tickets /></el-icon></span>
            <div>
              <strong>借阅活跃度</strong>
              <p>累计借阅 {{ overview?.borrowCount ?? 0 }} 次，当前仍有 {{ overview?.activeBorrowCount ?? 0 }} 本在借。</p>
            </div>
          </div>
          <div class="insight-item">
            <span class="i-icon"><el-icon><MagicStick /></el-icon></span>
            <div>
              <strong>智能服务使用</strong>
              <p>读者已发起 {{ overview?.aiChatCount ?? 0 }} 次 AI 咨询，行为记录累计 {{ overview?.behaviorCount ?? 0 }} 条。</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.eyebrow {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--accent);
  font-weight: 700;
}

.page-head h1 {
  margin-top: 6px;
  font-size: 25px;
  color: #1c3d31;
}

.page-head p {
  margin-top: 6px;
  font-size: 12px;
  color: #8a998f;
}

.head-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.head-pills span {
  font-size: 10px;
  color: #6f8177;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 5px 10px;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.metric {
  padding: 15px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.metric span {
  font-size: 10px;
  color: #8b9a92;
}

.metric strong {
  font-size: 24px;
  color: #22493a;
}

.metric small {
  font-size: 9px;
  color: #a3b0a9;
}

.chart-card {
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 4px 14px rgba(24, 55, 44, 0.03);
  min-width: 0;
}

.chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.chart-head h3 {
  font-size: 14px;
  color: #2a4c3e;
}

.chart-head p {
  margin-top: 4px;
  font-size: 10px;
  color: #9aa8a0;
}

.legend-dot {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: #7c8e84;
}

.legend-dot i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2f7a61;
}

.chart {
  width: 100%;
  height: 300px;
}

.chart.trend {
  height: 290px;
}

.chart.hot {
  height: 340px;
}

.chart.small-pie {
  height: 255px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.insight-body {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding: 8px 2px 4px;
}

.insight-item {
  display: flex;
  gap: 11px;
  padding: 12px;
  border-radius: 11px;
  background: #f8faf8;
  border: 1px solid #edf2ee;
}

.i-icon {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: #e9f1ec;
  color: #3f725c;
  font-size: 15px;
}

.insight-item strong {
  font-size: 11px;
  color: #3a5145;
}

.insight-item p {
  margin-top: 4px;
  font-size: 10px;
  line-height: 1.7;
  color: #87968e;
}

@media (max-width: 1000px) {
  .metric-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .metric-row {
    grid-template-columns: 1fr 1fr;
  }
  .chart {
    height: 260px;
  }
}
</style>
