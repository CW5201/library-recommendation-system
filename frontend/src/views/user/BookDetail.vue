<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import { aiApi, behaviorApi, bookApi, recommendApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { coverUrl, formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const book = ref(null)
const similar = ref([])
const loading = ref(true)
const error = ref('')
const acting = ref(false)
const ratingVisible = ref(false)
const ratingValue = ref(0)
const reasonVisible = ref(false)
const reasonLoading = ref(false)
const reasonText = ref('')
const reasonAi = ref(false)

const bookId = computed(() => Number(route.params.id))
const available = computed(() => Number(book.value?.availableStock || 0))
const totalStock = computed(() => Number(book.value?.totalStock || 0))
const stockPercent = computed(() => {
  if (!totalStock.value) return 0
  return Math.round((available.value / totalStock.value) * 100)
})

function ensureLogin() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录后再操作')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function load() {
  if (!bookId.value) return
  loading.value = true
  error.value = ''
  const [detailResult, similarResult] = await Promise.allSettled([
    bookApi.detail(bookId.value),
    recommendApi.similar(bookId.value, 4)
  ])
  if (detailResult.status === 'fulfilled') {
    book.value = detailResult.value
    ratingValue.value = detailResult.value.myScore || 0
  } else {
    error.value = '图书不存在或已下架'
  }
  similar.value = similarResult.status === 'fulfilled' ? similarResult.value || [] : []
  loading.value = false
}

async function toggleFavorite() {
  if (!ensureLogin() || acting.value) return
  acting.value = true
  try {
    const data = await behaviorApi.favorite(bookId.value)
    book.value.favorited = data.favorited
    ElMessage.success(data.favorited ? '已加入收藏' : '已取消收藏')
  } finally {
    acting.value = false
  }
}

async function borrowBook() {
  if (!ensureLogin()) return
  try {
    await ElMessageBox.confirm(
      `确认借阅《${book.value.title}》吗？借期为 30 天，请按时归还。`,
      '借阅确认',
      { confirmButtonText: '确认借阅', cancelButtonText: '再想想', type: 'info' }
    )
  } catch {
    return
  }
  acting.value = true
  try {
    await behaviorApi.borrow(bookId.value)
    ElMessage.success('借阅成功，请在 30 天内归还')
    await load()
  } finally {
    acting.value = false
  }
}

function openRating() {
  if (!ensureLogin()) return
  ratingValue.value = book.value?.myScore || 0
  ratingVisible.value = true
}

async function submitRating() {
  if (!ratingValue.value) {
    ElMessage.warning('请选择评分')
    return
  }
  acting.value = true
  try {
    await behaviorApi.rate(bookId.value, ratingValue.value)
    ElMessage.success('评分成功，推荐结果将随之更新')
    ratingVisible.value = false
    await load()
  } finally {
    acting.value = false
  }
}

async function askReason() {
  if (!ensureLogin()) return
  reasonVisible.value = true
  reasonLoading.value = true
  reasonText.value = ''
  try {
    const data = await aiApi.recommendReason({
      bookId: bookId.value,
      algorithm: book.value?.algorithm || undefined
    })
    reasonText.value = data.reason
    reasonAi.value = data.aiGenerated
  } catch {
    reasonText.value = '暂时无法生成推荐理由，请稍后再试。'
  } finally {
    reasonLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => load()
)

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page-shell detail-page">
    <div v-if="error" class="panel empty-block error-box">
      <el-icon class="icon"><Warning /></el-icon>
      <h3>{{ error }}</h3>
      <el-button type="primary" plain @click="router.push('/books')">返回馆藏列表</el-button>
    </div>

    <template v-else-if="book">
      <div class="crumb">
        <router-link to="/books">馆藏图书</router-link>
        <el-icon><ArrowRight /></el-icon>
        <span>{{ book.categoryName }}</span>
        <el-icon><ArrowRight /></el-icon>
        <span class="current">{{ book.title }}</span>
      </div>

      <section class="detail-grid">
        <div class="visual-col">
          <div class="cover-stage">
            <div class="cover-glow"></div>
            <img :src="coverUrl(book.cover)" :alt="book.title" class="main-cover" />
          </div>
          <div class="quick-stats">
            <div>
              <strong>{{ Number(book.ratingAvg || 0).toFixed(1) }}</strong>
              <span>综合评分</span>
            </div>
            <div>
              <strong>{{ book.ratingCount || 0 }}</strong>
              <span>评分人数</span>
            </div>
            <div>
              <strong>{{ book.borrowCount || 0 }}</strong>
              <span>累计借阅</span>
            </div>
          </div>
        </div>

        <div class="content-col">
          <div class="tags">
            <span class="chip">{{ book.categoryName }}</span>
            <span v-if="book.status === 1" class="chip plain">在架可借</span>
            <span v-else class="chip plain">已下架</span>
          </div>

          <h1>{{ book.title }}</h1>
          <p class="author-line">
            <el-icon><User /></el-icon>{{ book.author }}
            <span v-if="book.publisher" class="publisher">· {{ book.publisher }}</span>
          </p>

          <div class="rating-line">
            <el-rate :model-value="Number(book.ratingAvg || 0)" disabled allow-half />
            <strong>{{ Number(book.ratingAvg || 0).toFixed(1) }}</strong>
            <span>{{ book.ratingCount || 0 }} 人评分</span>
            <span v-if="book.myScore" class="my-score">我的评分：{{ book.myScore }} 分</span>
          </div>

          <p class="intro">{{ book.description || '暂无简介。' }}</p>

          <div class="stock-panel">
            <div class="stock-line">
              <div>
                <span class="stock-label">馆藏状态</span>
                <strong :class="{ out: available === 0 }">
                  {{ available > 0 ? '可借阅' : '暂无可借库存' }}
                </strong>
              </div>
              <span class="stock-num">可借 {{ available }} / 馆藏 {{ totalStock }}</span>
            </div>
            <el-progress
              :percentage="stockPercent"
              :show-text="false"
              :stroke-width="7"
              :color="available > 0 ? '#2f7a61' : '#c46a4a'"
            />
          </div>

          <div class="actions">
            <el-button
              type="primary"
              size="large"
              :loading="acting"
              :disabled="available === 0 || book.borrowed"
              @click="borrowBook"
            >
              <el-icon><Tickets /></el-icon>
              {{ book.borrowed ? '已借阅' : available === 0 ? '暂无库存' : '立即借阅' }}
            </el-button>
            <el-button
              size="large"
              :type="book.favorited ? 'warning' : 'default'"
              :plain="!book.favorited"
              @click="toggleFavorite"
            >
              <el-icon><StarFilled v-if="book.favorited" /><Star v-else /></el-icon>
              {{ book.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="large" plain @click="openRating">
              <el-icon><EditPen /></el-icon>{{ book.myScore ? '修改评分' : '评分' }}
            </el-button>
            <el-button size="large" text class="ai-btn" @click="askReason">
              <el-icon><MagicStick /></el-icon>AI 推荐理由
            </el-button>
          </div>

          <div class="meta-panel">
            <div class="meta-row"><span>ISBN</span><strong>{{ book.isbn }}</strong></div>
            <div class="meta-row"><span>出版社</span><strong>{{ book.publisher || '—' }}</strong></div>
            <div class="meta-row"><span>出版日期</span><strong>{{ formatDate(book.publishDate) }}</strong></div>
            <div class="meta-row"><span>上架时间</span><strong>{{ formatDate(book.createTime) }}</strong></div>
            <div class="meta-row"><span>浏览次数</span><strong>{{ book.viewCount || 0 }}</strong></div>
            <div class="meta-row"><span>分类</span><strong>{{ book.categoryName }}</strong></div>
          </div>
        </div>
      </section>

      <section v-if="similar.length" class="section similar">
        <div class="section-head">
          <div>
            <h2>相似图书推荐</h2>
            <p class="sub">基于内容相似度与读者行为发现的关联馆藏</p>
          </div>
          <router-link class="more" to="/recommendations">更多推荐 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div class="book-grid">
          <BookCard v-for="item in similar" :key="item.id" :book="item" show-reason />
        </div>
      </section>
    </template>

    <el-dialog v-model="ratingVisible" title="为这本书评分" width="380px" align-center>
      <div class="rate-dialog">
        <p>{{ book?.title }}</p>
        <el-rate v-model="ratingValue" size="large" show-text :texts="['很差', '较差', '一般', '不错', '很棒']" />
        <small>你的评分会参与图书综合评分与个性化推荐计算。</small>
      </div>
      <template #footer>
        <el-button @click="ratingVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submitRating">提交评分</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reasonVisible" title="AI 推荐理由" width="540px" align-center>
      <div v-loading="reasonLoading" class="reason-dialog">
        <div class="reason-head">
          <span class="chip accent"><el-icon><MagicStick /></el-icon>{{ reasonAi ? 'AI 生成' : '智能分析' }}</span>
          <span class="muted">《{{ book?.title }}》</span>
        </div>
        <p class="reason-text">{{ reasonText || '正在结合馆藏信息与你的阅读偏好生成推荐理由…' }}</p>
        <small class="reason-tip">推荐理由由系统结合馆藏数据生成，仅供阅读参考。</small>
      </div>
      <template #footer>
        <el-button type="primary" @click="reasonVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-page {
  padding-top: 24px;
  padding-bottom: 64px;
}

.error-box {
  margin: 40px auto;
  max-width: 520px;
}

.error-box h3 {
  margin-bottom: 16px;
  color: #4c6056;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #8b9a92;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.crumb a:hover {
  color: var(--brand);
}

.crumb .el-icon {
  font-size: 10px;
}

.crumb .current {
  color: #52665c;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 48px;
  align-items: start;
}

.visual-col {
  position: sticky;
  top: 98px;
}

.cover-stage {
  position: relative;
  padding: 26px;
  border-radius: 24px;
  background: linear-gradient(155deg, #e8f0eb, #dae7e0);
  border: 1px solid #e1eae5;
  overflow: hidden;
}

.cover-glow {
  position: absolute;
  width: 220px;
  height: 220px;
  right: -80px;
  top: -80px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 137, 60, 0.2), transparent 68%);
}

.main-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 24px 44px rgba(22, 54, 42, 0.24);
}

.quick-stats {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 14px 6px;
}

.quick-stats div {
  text-align: center;
  border-right: 1px solid #edf1ee;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.quick-stats div:last-child {
  border-right: none;
}

.quick-stats strong {
  font-size: 18px;
  color: #235442;
}

.quick-stats span {
  font-size: 10px;
  color: #93a29a;
}

.content-col .tags {
  display: flex;
  gap: 7px;
}

h1 {
  margin-top: 16px;
  font-size: clamp(26px, 3vw, 38px);
  line-height: 1.3;
  letter-spacing: 0.01em;
  color: #1b3f33;
}

.author-line {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #61756b;
  font-size: 14px;
  flex-wrap: wrap;
}

.author-line .el-icon {
  color: #93a59c;
}

.publisher {
  color: #8b9a92;
}

.rating-line {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  color: #8b9a92;
  flex-wrap: wrap;
}

.rating-line :deep(.el-rate) {
  height: auto;
}

.rating-line strong {
  color: #a8742b;
  font-size: 18px;
}

.my-score {
  padding: 3px 9px;
  border-radius: 999px;
  background: #f8f1e5;
  color: #9a6927;
}

.intro {
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 14px;
  background: #f8faf8;
  border: 1px solid #e8eee9;
  color: #5e7067;
  font-size: 13px;
  line-height: 1.95;
}

.stock-panel {
  margin-top: 18px;
  padding: 16px 20px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--line);
}

.stock-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  gap: 12px;
}

.stock-label {
  font-size: 12px;
  color: #8a9a92;
  margin-right: 8px;
}

.stock-line strong {
  font-size: 14px;
  color: #2a6d55;
}

.stock-line strong.out {
  color: #b95d42;
}

.stock-num {
  font-size: 12px;
  color: #7d8d85;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.actions .el-button {
  border-radius: 11px;
}

.actions .el-button--large {
  padding: 0 20px;
}

.ai-btn {
  color: #9a6927;
}

.meta-panel {
  margin-top: 24px;
  border-top: 1px dashed #e1e8e3;
  padding-top: 8px;
}

.meta-row {
  display: flex;
  gap: 16px;
  padding: 9px 2px;
  font-size: 12px;
  border-bottom: 1px solid #f1f4f2;
}

.meta-row span {
  width: 84px;
  color: #93a29a;
  flex: none;
}

.meta-row strong {
  color: #52665c;
  font-weight: 500;
  word-break: break-all;
}

.similar {
  margin-top: 54px;
  padding-top: 30px;
  border-top: 1px solid #e5ebe7;
}

.rate-dialog {
  text-align: center;
  padding: 6px 0 2px;
}

.rate-dialog p {
  font-size: 14px;
  color: #4d6157;
  margin-bottom: 14px;
}

.rate-dialog small {
  display: block;
  margin-top: 14px;
  font-size: 11px;
  color: #93a29a;
}

.reason-dialog {
  min-height: 90px;
}

.reason-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.reason-text {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f8faf8;
  border: 1px solid #e8eee9;
  color: #4d6157;
  font-size: 13px;
  line-height: 1.95;
  white-space: pre-wrap;
}

.reason-tip {
  display: block;
  margin-top: 12px;
  font-size: 11px;
  color: #9aa9a1;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 26px;
  }
  .cover-stage {
    padding: 16px;
  }
}

@media (max-width: 680px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .visual-col {
    position: static;
    max-width: 300px;
    margin: 0 auto;
    width: 100%;
  }
  .actions .el-button {
    flex: 1;
  }
  .actions .ai-btn {
    flex-basis: 100%;
    justify-content: center;
  }
}
</style>
