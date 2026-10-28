<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import { bookApi, categoryApi } from '@/api'

const route = useRoute()
const router = useRouter()

const books = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])

const filters = reactive({
  keyword: '',
  categoryId: null,
  sort: 'default',
  page: 1,
  size: 12
})

const sorts = [
  { value: 'default', label: '最新上架' },
  { value: 'hot', label: '借阅热度' },
  { value: 'rating', label: '评分最高' },
  { value: 'new', label: '出版时间' },
  { value: 'title', label: '书名排序' }
]

function syncFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.categoryId = route.query.categoryId ? Number(route.query.categoryId) : null
  filters.sort = typeof route.query.sort === 'string' ? route.query.sort : 'default'
  filters.page = Number(route.query.page) || 1
}

async function load() {
  loading.value = true
  try {
    const data = await bookApi.page({
      page: filters.page,
      size: filters.size,
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      sort: filters.sort
    })
    books.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function pushQuery(resetPage = true) {
  const query = {}
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
  if (filters.categoryId) query.categoryId = filters.categoryId
  if (filters.sort && filters.sort !== 'default') query.sort = filters.sort
  if (!resetPage && filters.page > 1) query.page = filters.page
  router.push({ path: '/books', query })
}

function applyFilters() {
  pushQuery(true)
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.sort = 'default'
  router.push('/books')
}

function changePage(page) {
  filters.page = page
  pushQuery(false)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query,
  () => {
    syncFromRoute()
    load()
  },
  { immediate: true }
)

onMounted(async () => {
  categories.value = await categoryApi.list().catch(() => [])
})

function selectCategory(id) {
  filters.categoryId = id
  pushQuery(true)
}
</script>

<template>
  <div class="page-shell page-stack">
    <section class="list-hero">
      <div class="hero-copy">
        <span class="chip"><el-icon><Collection /></el-icon>馆藏检索</span>
        <h1 class="page-title">发现馆藏好书</h1>
        <p class="page-desc">按书名、作者、ISBN、出版社搜索，或结合学科分类与借阅热度筛选。</p>
      </div>
      <div class="hero-search">
        <el-input
          v-model="filters.keyword"
          size="large"
          placeholder="输入书名、作者、ISBN 或出版社"
          clearable
          @keyup.enter="applyFilters"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" @click="applyFilters">搜索</el-button>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <span class="label">学科分类</span>
        <div class="cat-chips">
          <button class="cat-chip" :class="{ active: !filters.categoryId }" @click="selectCategory(null)">
            全部
          </button>
          <button
            v-for="cat in categories"
            :key="cat.id"
            class="cat-chip"
            :class="{ active: filters.categoryId === cat.id }"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </button>
        </div>
      </div>
      <div class="filter-row bottom">
        <div class="result-info">
          共找到 <strong>{{ total }}</strong> 本图书
          <span v-if="filters.keyword">· 关键词“{{ filters.keyword }}”</span>
        </div>
        <div class="sort-box">
          <span class="label">排序</span>
          <el-select v-model="filters.sort" style="width: 130px" @change="applyFilters">
            <el-option v-for="item in sorts" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button text @click="resetFilters">重置</el-button>
        </div>
      </div>
    </section>

    <div v-loading="loading" class="result-area">
      <div v-if="books.length" class="book-grid">
        <BookCard v-for="book in books" :key="book.id" :book="book" />
      </div>
      <div v-else-if="!loading" class="panel empty-block">
        <el-icon class="icon"><Search /></el-icon>
        <h3>没有找到匹配的图书</h3>
        <p>试试更换关键词，或浏览其他学科分类。</p>
        <el-button type="primary" plain @click="resetFilters">查看全部馆藏</el-button>
      </div>
    </div>

    <div v-if="total > filters.size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next, jumper"
        :total="total"
        :page-size="filters.size"
        :current-page="filters.page"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<style scoped>
.list-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 30px;
  padding: 8px 2px 4px;
}

.hero-copy .chip {
  margin-bottom: 13px;
}

.hero-search {
  display: flex;
  gap: 8px;
  width: min(460px, 100%);
  flex: none;
}

.hero-search :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 0 1px #dfe8e2 inset;
}

.hero-search .el-button {
  height: 40px;
  border-radius: 10px;
  flex: none;
}

.filter-panel {
  padding: 20px 24px;
  border-radius: var(--radius);
  background: #fff;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.filter-row.bottom {
  align-items: center;
  justify-content: space-between;
  border-top: 1px dashed #e6ece8;
  padding-top: 14px;
}

.label {
  flex: none;
  font-size: 13px;
  color: #7c8c84;
  padding-top: 5px;
}

.cat-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.cat-chip {
  border: 1px solid transparent;
  background: #f4f7f4;
  color: #5c6e65;
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cat-chip:hover {
  color: var(--brand);
  background: #e8f1ec;
}

.cat-chip.active {
  background: var(--brand);
  color: #fff;
  border-color: var(--brand);
}

.result-info {
  font-size: 13px;
  color: #83928a;
}

.result-info strong {
  color: var(--brand);
  font-size: 16px;
}

.sort-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-box .label {
  padding-top: 0;
}

.result-area {
  min-height: 260px;
}

.empty-block h3 {
  margin-bottom: 6px;
  color: #4c6056;
  font-size: 16px;
}

.empty-block p {
  margin-bottom: 16px;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 8px 0 20px;
}

@media (max-width: 900px) {
  .list-hero {
    flex-direction: column;
    align-items: stretch;
  }
  .hero-search {
    width: 100%;
  }
  .filter-row {
    flex-direction: column;
    gap: 8px;
  }
  .filter-row.bottom {
    align-items: flex-start;
    gap: 12px;
  }
  .sort-box {
    flex-wrap: wrap;
  }
}
</style>
