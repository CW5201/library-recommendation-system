<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import { recommendApi } from '@/api'

const router = useRouter()
const books = ref([])
const loading = ref(true)
const refreshing = ref(false)

const algorithms = computed(() => {
  const map = new Map()
  books.value.forEach((book) => {
    const key = book.algorithm || 'HYBRID'
    map.set(key, (map.get(key) || 0) + 1)
  })
  return map
})

const isColdStart = computed(() => books.value.some((book) => book.algorithm === 'COLD_START'))

const engines = [
  {
    name: '热门趋势',
    ratio: '35%',
    desc: '综合借阅、浏览、收藏与评分热度',
    icon: 'TrendCharts'
  },
  {
    name: '内容相似',
    ratio: '40%',
    desc: '分析你偏好图书的分类、主题与简介',
    icon: 'Reading'
  },
  {
    name: '协同过滤',
    ratio: '25%',
    desc: '参考相似读者群体的借阅与评分行为',
    icon: 'Connection'
  }
]

async function load(showMessage = false) {
  if (showMessage) refreshing.value = true
  else loading.value = true
  try {
    books.value = await recommendApi.personal(12)
    if (showMessage) ElMessage.success('推荐结果已更新')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="page-shell page-stack">
    <section class="rec-hero">
      <div class="hero-copy">
        <span class="chip light"><el-icon><MagicStick /></el-icon>PERSONALIZED FOR YOU</span>
        <h1>为你精选的阅读清单</h1>
        <p>
          系统实时读取你的浏览、收藏、评分与借阅行为，融合热门趋势、内容相似度与相似读者偏好，
          从馆藏中筛选出最值得你打开的图书。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" :loading="refreshing" @click="load(true)">
            <el-icon><Refresh /></el-icon>重新生成推荐
          </el-button>
          <el-button size="large" plain @click="router.push('/ai')">
            <el-icon><ChatDotRound /></el-icon>让 AI 帮我选书
          </el-button>
        </div>
      </div>

      <div class="engine-panel">
        <div class="panel-title">
          <span>推荐引擎</span>
          <small>HYBRID ENGINE</small>
        </div>
        <div v-for="engine in engines" :key="engine.name" class="engine-row">
          <span class="engine-icon"><el-icon><component :is="engine.icon" /></el-icon></span>
          <div class="engine-info">
            <div><strong>{{ engine.name }}</strong><b>{{ engine.ratio }}</b></div>
            <p>{{ engine.desc }}</p>
          </div>
        </div>
        <div class="panel-foot">
          当前结果 {{ books.length }} 本 · {{ algorithms.size }} 种策略共同参与
        </div>
      </div>
    </section>

    <el-alert
      v-if="isColdStart"
      title="推荐系统正在认识你"
      type="warning"
      :closable="false"
      show-icon
      description="你的行为数据还比较少，当前以热门与内容推荐为主。浏览、收藏、评分或借阅几本书后，个性化结果会更准确。"
    />

    <section class="section">
      <div class="section-head">
        <div>
          <h2>今日推荐</h2>
          <p class="sub">每次生成都会排除已收藏与已借阅的图书</p>
        </div>
        <span class="updated">
          <el-icon><Clock /></el-icon>
          实时计算
        </span>
      </div>

      <div v-loading="loading" class="rec-area">
        <div v-if="books.length" class="book-grid">
          <BookCard
            v-for="book in books"
            :key="book.id"
            :book="book"
            show-reason
            show-algorithm
            show-score
          />
        </div>
        <div v-else-if="!loading" class="panel empty-block">
          <el-icon class="icon"><MagicStick /></el-icon>
          <h3>暂时没有可推荐的图书</h3>
          <p>你已浏览或借阅了大部分馆藏，试试搜索其他分类，或与 AI 助手聊聊。</p>
          <div class="empty-actions">
            <el-button type="primary" plain @click="router.push('/books')">浏览馆藏</el-button>
            <el-button plain @click="router.push('/ai')">咨询 AI 助手</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="tips">
      <div class="tip-item">
        <span class="num">01</span>
        <div>
          <strong>行为越丰富，结果越准确</strong>
          <p>收藏、评分与借阅的权重高于普通浏览，完善行为可获得更贴合的兴趣推荐。</p>
        </div>
      </div>
      <div class="tip-item">
        <span class="num">02</span>
        <div>
          <strong>推荐不是信息茧房</strong>
          <p>系统保留热门与新书探索比例，帮助你发现兴趣之外的高质量馆藏。</p>
        </div>
      </div>
      <div class="tip-item">
        <span class="num">03</span>
        <div>
          <strong>可解释的推荐理由</strong>
          <p>每本推荐书都附带推荐依据，你也可以在详情页让 AI 生成更详细的理由。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.rec-hero {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 34px;
  align-items: center;
  padding: 38px;
  border-radius: 26px;
  background:
    radial-gradient(560px 300px at 88% 8%, rgba(200, 137, 60, 0.18), transparent 62%),
    linear-gradient(140deg, #1d5545, #123a2e 65%, #0d2e24);
  color: #e9f3ee;
  box-shadow: 0 24px 52px rgba(18, 54, 42, 0.2);
}

.chip.light {
  background: rgba(255, 255, 255, 0.1);
  color: #d9c08e;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-copy h1 {
  margin-top: 16px;
  font-size: clamp(28px, 3vw, 38px);
  letter-spacing: 0.02em;
}

.hero-copy p {
  margin-top: 15px;
  max-width: 560px;
  color: #a7c0b4;
  font-size: 13px;
  line-height: 1.95;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-actions .el-button:first-child {
  --el-button-bg-color: #d7a75f;
  --el-button-border-color: #d7a75f;
  --el-button-text-color: #213d32;
  --el-button-hover-bg-color: #e4b873;
  --el-button-hover-border-color: #e4b873;
}

.hero-actions .el-button:last-child {
  background: transparent;
  color: #d5e5dd;
  border-color: rgba(255, 255, 255, 0.25);
}

.engine-panel {
  background: rgba(255, 255, 255, 0.065);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  padding: 20px;
  backdrop-filter: blur(8px);
}

.panel-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}

.panel-title span {
  font-size: 14px;
  font-weight: 600;
}

.panel-title small {
  font-size: 8px;
  letter-spacing: 0.16em;
  color: #769487;
}

.engine-row {
  display: flex;
  gap: 11px;
  padding: 13px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.08);
}

.engine-icon {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: rgba(200, 137, 60, 0.16);
  color: #e2c48d;
  font-size: 15px;
}

.engine-info {
  flex: 1;
  min-width: 0;
}

.engine-info > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.engine-info strong {
  font-size: 12px;
  font-weight: 600;
}

.engine-info b {
  font-size: 12px;
  color: #d7b474;
}

.engine-info p {
  margin-top: 3px;
  font-size: 10px;
  color: #8da99b;
  line-height: 1.6;
}

.panel-foot {
  margin-top: 13px;
  font-size: 10px;
  color: #7f9c8e;
}

.updated {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8a9a92;
}

.rec-area {
  min-height: 240px;
}

.empty-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.tips {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.tip-item {
  display: flex;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
}

.num {
  font-size: 20px;
  font-weight: 700;
  color: #d7c3a2;
  line-height: 1.1;
  flex: none;
}

.tip-item strong {
  font-size: 13px;
  color: #3f554a;
}

.tip-item p {
  margin-top: 7px;
  font-size: 12px;
  line-height: 1.7;
  color: #8b9a92;
}

@media (max-width: 1000px) {
  .rec-hero {
    grid-template-columns: 1fr;
  }
  .tips {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .rec-hero {
    padding: 24px;
  }
  .hero-actions .el-button {
    flex: 1;
  }
}
</style>
