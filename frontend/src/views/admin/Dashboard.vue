<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { borrowStatusText, borrowStatusType, formatDateTime } from '@/utils/format'

const router = useRouter()
const data = ref(null)
const loading = ref(true)
const rebuilding = ref(false)

const stats = computed(() => {
  if (!data.value) return []
  return [
    {
      label: '馆藏图书',
      value: data.value.bookCount,
      sub: `${data.value.categoryCount} 个学科分类`,
      icon: 'Reading',
      tone: 'green'
    },
    {
      label: '注册读者',
      value: data.value.userCount,
      sub: '包含管理员账号',
      icon: 'User',
      tone: 'blue'
    },
    {
      label: '当前借阅',
      value: data.value.activeBorrowCount,
      sub: `今日新增 ${data.value.todayBorrowCount} 笔`,
      icon: 'Tickets',
      tone: 'orange'
    },
    {
      label: '累计借阅',
      value: data.value.borrowCount,
      sub: '历史全部借阅记录',
      icon: 'Document',
      tone: 'teal'
    },
    {
      label: '行为记录',
      value: data.value.behaviorCount,
      sub: '浏览 / 收藏 / 评分 / 借阅',
      icon: 'DataAnalysis',
      tone: 'purple'
    },
    {
      label: 'AI 对话',
      value: data.value.aiChatCount,
      sub: '已保存的问答消息',
      icon: 'ChatDotRound',
      tone: 'gold'
    }
  ]
})

const today = computed(() =>
  new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
)

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.overview()
  } finally {
    loading.value = false
  }
}

async function rebuildIndex() {
  try {
    await ElMessageBox.confirm(
      '将重新计算全部在架图书的语义向量索引，图书较多时可能需要一些时间。确认继续吗？',
      '重建向量索引',
      { confirmButtonText: '开始重建', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  rebuilding.value = true
  try {
    const result = await adminApi.rebuildIndex()
    ElMessage.success(`索引重建完成，共 ${result.indexed ?? result.count ?? '—'} 本图书`)
  } finally {
    rebuilding.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <section class="dash-head">
      <div>
        <span class="eyebrow">DASHBOARD</span>
        <h1>数据概览</h1>
        <p>{{ today }} · 图书馆运行与推荐服务状态一览</p>
      </div>
      <div class="head-actions">
        <el-button plain @click="rebuildIndex" :loading="rebuilding">
          <el-icon><Refresh /></el-icon>重建向量索引
        </el-button>
        <el-button type="primary" @click="router.push('/admin/books')">
          <el-icon><Plus /></el-icon>新增图书
        </el-button>
      </div>
    </section>

    <section class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card" :class="item.tone">
        <div class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="stat-body">
          <span>{{ item.label }}</span>
          <strong>{{ item.value ?? '—' }}</strong>
          <small>{{ item.sub }}</small>
        </div>
        <div class="stat-glow"></div>
      </div>
    </section>

    <section class="dash-grid">
      <div class="panel recent-panel">
        <div class="panel-head">
          <div>
            <h3>最近借阅</h3>
            <p>最新的借阅与归还动态</p>
          </div>
          <el-button text @click="router.push('/admin/borrows')">全部记录 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <el-table :data="data?.recentBorrows || []" size="small" style="width: 100%">
          <el-table-column label="读者" min-width="110">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="mini-avatar">{{ (row.nickname || row.username || '读').slice(0, 1) }}</span>
                <div>
                  <strong>{{ row.nickname || row.username }}</strong>
                  <small>@{{ row.username }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="bookTitle" label="图书" min-width="180" show-overflow-tooltip />
          <el-table-column label="借阅时间" width="140">
            <template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="92">
            <template #default="{ row }">
              <el-tag size="small" :type="borrowStatusType(row.status)" effect="light">
                {{ borrowStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!data?.recentBorrows?.length" class="table-empty">暂无借阅记录</div>
      </div>

      <div class="panel hot-panel">
        <div class="panel-head">
          <div>
            <h3>热门图书</h3>
            <p>按综合热度排序的馆藏</p>
          </div>
          <el-button text @click="router.push('/admin/books')">图书管理 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <div class="hot-list">
          <div
            v-for="(book, index) in data?.hotBooks || []"
            :key="book.id"
            class="hot-row"
            @click="router.push(`/books/${book.id}`)"
          >
            <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <div class="hot-info">
              <strong>{{ book.title }}</strong>
              <span>{{ book.categoryName }} · {{ book.author }}</span>
            </div>
            <div class="hot-value">
              <b>{{ book.borrowCount || 0 }}</b>
              <small>借阅</small>
            </div>
          </div>
          <div v-if="!data?.hotBooks?.length" class="table-empty">暂无数据</div>
        </div>
      </div>
    </section>

    <section class="quick-grid">
      <button class="quick" @click="router.push('/admin/users')">
        <span class="q-icon"><el-icon><User /></el-icon></span>
        <div><strong>用户管理</strong><small>角色、状态与账号维护</small></div>
        <el-icon class="q-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick" @click="router.push('/admin/categories')">
        <span class="q-icon"><el-icon><Files /></el-icon></span>
        <div><strong>分类管理</strong><small>维护学科分类体系</small></div>
        <el-icon class="q-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick" @click="router.push('/admin/stats')">
        <span class="q-icon"><el-icon><PieChart /></el-icon></span>
        <div><strong>统计分析</strong><small>借阅趋势与行为分布</small></div>
        <el-icon class="q-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick" @click="router.push('/admin/borrows')">
        <span class="q-icon"><el-icon><Tickets /></el-icon></span>
        <div><strong>借阅管理</strong><small>借阅记录与归还处理</small></div>
        <el-icon class="q-arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.dash-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.eyebrow {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--accent);
  font-weight: 700;
}

.dash-head h1 {
  margin-top: 7px;
  font-size: 27px;
  letter-spacing: 0.02em;
  color: #1c3d31;
}

.dash-head p {
  margin-top: 7px;
  font-size: 12px;
  color: #8a998f;
}

.head-actions {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 4px 14px rgba(24, 55, 44, 0.035);
}

.stat-icon {
  width: 42px;
  height: 42px;
  flex: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 20px;
  background: #e9f2ed;
  color: #2a6a54;
}

.stat-card.blue .stat-icon { background: #e9eff6; color: #40658d; }
.stat-card.orange .stat-icon { background: #fbf0e3; color: #b1762d; }
.stat-card.teal .stat-icon { background: #e6f2f2; color: #2f7b78; }
.stat-card.purple .stat-icon { background: #f0edf8; color: #6a5a9a; }
.stat-card.gold .stat-icon { background: #f9f0db; color: #a67c2c; }

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.stat-body > span {
  font-size: 11px;
  color: #8b9a92;
}

.stat-body strong {
  font-size: 27px;
  line-height: 1.2;
  color: #22493a;
  letter-spacing: 0.01em;
}

.stat-body small {
  font-size: 10px;
  color: #a0ada6;
}

.stat-glow {
  position: absolute;
  width: 90px;
  height: 90px;
  right: -40px;
  bottom: -50px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(31, 92, 74, 0.08), transparent 70%);
}

.dash-grid {
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 16px;
  align-items: start;
}

.panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 14px rgba(24, 55, 44, 0.035);
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-head h3 {
  font-size: 15px;
  color: #294c3e;
}

.panel-head p {
  margin-top: 4px;
  font-size: 10px;
  color: #9aa8a0;
}

.panel-head .el-button {
  font-size: 11px;
  color: #628074;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: #e9f1ec;
  color: #3f6c58;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 600;
  flex: none;
}

.user-cell div {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.user-cell strong {
  font-size: 11px;
  color: #485d52;
}

.user-cell small {
  font-size: 9px;
  color: #a0ada6;
}

.table-empty {
  padding: 24px;
  text-align: center;
  font-size: 11px;
  color: #a5b1aa;
}

.hot-list {
  display: flex;
  flex-direction: column;
}

.hot-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 4px;
  border-bottom: 1px solid #f0f4f1;
  cursor: pointer;
  border-radius: 7px;
  transition: background 0.2s ease;
}

.hot-row:hover {
  background: #f7faf8;
}

.hot-row:last-child {
  border-bottom: none;
}

.rank {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  color: #7c8e84;
  background: #f0f4f1;
}

.rank.top {
  background: #f6ecdd;
  color: #a3742d;
}

.hot-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hot-info strong {
  font-size: 11px;
  color: #41584c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-info span {
  font-size: 9px;
  color: #9aa8a0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-value {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex: none;
}

.hot-value b {
  font-size: 12px;
  color: #3f6c58;
}

.hot-value small {
  font-size: 8px;
  color: #a5b1aa;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px;
  border-radius: 13px;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.quick:hover {
  border-color: #bcd0c6;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(24, 55, 44, 0.06);
}

.q-icon {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #eef4f0;
  color: #4d7c67;
  font-size: 16px;
}

.quick div {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.quick strong {
  font-size: 12px;
  color: #3d5549;
}

.quick small {
  font-size: 9px;
  color: #a0ada6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.q-arrow {
  color: #bdc9c3;
  font-size: 11px;
  flex: none;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 980px) {
  .dash-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .stat-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
  .dash-head h1 {
    font-size: 23px;
  }
}
</style>
