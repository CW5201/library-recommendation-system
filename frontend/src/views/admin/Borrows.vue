<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { borrowStatusText, borrowStatusType, formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: ''
})

const statusOptions = [
  { label: '借阅中', value: 'BORROWED' },
  { label: '已归还', value: 'RETURNED' },
  { label: '已逾期', value: 'OVERDUE' }
]

async function load() {
  loading.value = true
  try {
    const data = await adminApi.borrows({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.status = ''
  query.keyword = ''
  query.page = 1
  load()
}

async function returnBook(row) {
  try {
    await ElMessageBox.confirm(
      `确认办理《${row.bookTitle}》的归还手续吗？库存将自动加一。`,
      '归还办理',
      { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await adminApi.returnBorrow(row.id)
  ElMessage.success('归还办理成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <span class="eyebrow">BORROW MANAGEMENT</span>
        <h1>借阅管理</h1>
        <p>查看借阅记录，办理归还并跟踪逾期情况。</p>
      </div>
      <div class="summary">
        共 <strong>{{ total }}</strong> 条借阅记录
      </div>
    </section>

    <section class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="读者 / 图书关键词"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column label="读者" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="mini-avatar">{{ (row.nickname || row.username || '读').slice(0, 1) }}</span>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <span>@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="图书" min-width="230">
          <template #default="{ row }">
            <div class="book-cell">
              <img :src="row.cover || '/covers/book.svg'" :alt="row.bookTitle" />
              <div>
                <strong>{{ row.bookTitle }}</strong>
                <span>{{ row.bookAuthor }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="150">
          <template #default="{ row }">
            <span :class="{ overdue: row.overdue }">{{ formatDateTime(row.dueTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还时间" width="150">
          <template #default="{ row }">{{ row.returnTime ? formatDateTime(row.returnTime) : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="borrowStatusType(row.status)" effect="light" size="small">
              {{ borrowStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'BORROWED' || row.status === 'OVERDUE'"
              type="primary"
              link
              @click="returnBook(row)"
            >
              办理归还
            </el-button>
            <span v-else class="done">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
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

.summary {
  font-size: 11px;
  color: #87968e;
  padding: 7px 12px;
  border-radius: 9px;
  background: #fff;
  border: 1px solid var(--line);
}

.summary strong {
  color: var(--brand);
  font-size: 14px;
}

.toolbar {
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--line);
}

.filters {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  align-items: center;
}

.table-panel {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  overflow: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.mini-avatar {
  width: 28px;
  height: 28px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: #e9f1ec;
  color: #3f6c58;
  font-size: 11px;
  font-weight: 600;
}

.user-cell div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-cell strong {
  font-size: 11px;
  color: #3f564a;
}

.user-cell span {
  font-size: 9px;
  color: #9aa8a0;
}

.book-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.book-cell img {
  width: 36px;
  height: 48px;
  border-radius: 5px;
  object-fit: cover;
  background: #e8efeb;
  flex: none;
}

.book-cell div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.book-cell strong {
  font-size: 11px;
  color: #3a5145;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-cell span {
  font-size: 9px;
  color: #9aa8a0;
}

.overdue {
  color: #c45f42;
  font-weight: 600;
}

.done {
  font-size: 11px;
  color: #a0ada6;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
