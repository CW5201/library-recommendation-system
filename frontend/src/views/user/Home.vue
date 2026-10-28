<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BookCard from '@/components/BookCard.vue'
import { bookApi, categoryApi, recommendApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { coverUrl } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()

const hot = ref([])
const fresh = ref([])
const personal = ref([])
const categories = ref([])
const totalBooks = ref(0)
const loading = ref(true)
const keyword = ref('')

const quickTags = ['计算机', '人工智能', '文学', '历史', '经济学']
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const heroBooks = computed(() => hot.value.slice(0, 3))

function search() {
  router.push({ path: '/books', query: keyword.value ? { keyword: keyword.value } : {} })
}

function searchTag(tag) {
  router.push({ path: '/books', query: { keyword: tag } })
}

onMounted(async () => {
  const tasks = [
    recommendApi.hot(8).catch(() => []),
    recommendApi.newBooks(8).catch(() => []),
    categoryApi.list().catch(() => []),
    bookApi.page({ page: 1, size: 1 }).catch(() => ({ total: 0 }))
  ]
  if (auth.isLoggedIn) {
    tasks.push(recommendApi.personal(4).catch(() => []))
  }
  const [hotData, newData, categoryData, pageData, personalData] = await Promise.all(tasks)
  hot.value = hotData || []
  fresh.value = newData || []
  categories.value = categoryData || []
  totalBooks.value = pageData?.total || 0
  personal.value = personalData || []
  loading.value = false
})
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="page-shell hero-inner">
        <div class="hero-copy fade-up">
          <div class="welcome">
            <span class="dot"></span>
            {{ auth.isLoggedIn ? `${greeting}，${auth.displayName}` : '欢迎来到知书图书馆' }}
          </div>
          <h1>在合适的时机，<br />遇见一本<span>对的书</span>。</h1>
          <p class="lead">
            结合你的借阅偏好、馆藏内容与相似读者行为，知书为每一位读者构建专属的阅读路径。
          </p>

          <div class="search-box">
            <el-icon class="search-icon"><Search /></el-icon>
            <el-input
              v-model="keyword"
              placeholder="搜索书名、作者、ISBN 或出版社"
              size="large"
              clearable
              @keyup.enter="search"
            />
            <el-button type="primary" size="large" @click="search">搜索馆藏</el-button>
          </div>

          <div class="quick">
            <span>热门搜索</span>
            <button v-for="tag in quickTags" :key="tag" @click="searchTag(tag)">{{ tag }}</button>
          </div>

          <div class="stats">
            <div>
              <strong>{{ totalBooks || '—' }}</strong>
              <span>馆藏图书</span>
            </div>
            <div>
              <strong>{{ categories.length || '—' }}</strong>
              <span>学科分类</span>
            </div>
            <div>
              <strong>AI</strong>
              <span>智能图书助手</span>
            </div>
          </div>
        </div>

        <div class="hero-visual">
          <div class="visual-ring ring-a"></div>
          <div class="visual-ring ring-b"></div>
          <div class="book-stack">
            <div
              v-for="(book, index) in heroBooks"
              :key="book.id"
              class="float-book"
              :class="`fb-${index + 1}`"
              @click="router.push(`/books/${book.id}`)"
            >
              <img :src="coverUrl(book.cover)" :alt="book.title" />
            </div>
            <div v-if="!heroBooks.length" class="placeholder-stack">
              <span></span><span></span><span></span>
            </div>
          </div>
          <div v-if="hot[0]" class="spotlight" @click="router.push(`/books/${hot[0].id}`)">
            <span class="tag">本周热门</span>
            <strong>{{ hot[0].title }}</strong>
            <small>{{ hot[0].author }}</small>
            <div class="spot-meta">
              <el-icon><StarFilled /></el-icon>
              {{ Number(hot[0].ratingAvg || 0).toFixed(1) }}
              <span>·</span>
              {{ hot[0].borrowCount }} 次借阅
            </div>
          </div>
        </div>
      </div>

      <div class="hero-wave"></div>
    </section>

    <div class="page-shell page-stack">
      <section class="category-strip">
        <div class="strip-head">
          <div>
            <h2>按学科探索</h2>
            <p>从感兴趣的方向开始，发现更多馆藏好书</p>
          </div>
          <router-link class="more" to="/books">全部图书 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div class="cat-list">
          <button
            v-for="cat in categories"
            :key="cat.id"
            class="cat-item"
            @click="router.push({ path: '/books', query: { categoryId: cat.id } })"
          >
            <el-icon><Collection /></el-icon>
            <span>{{ cat.name }}</span>
            <small>{{ cat.description }}</small>
          </button>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div>
            <h2>馆藏热门</h2>
            <p class="sub">综合借阅次数、浏览量、评分与收藏热度计算</p>
          </div>
          <router-link class="more" to="/books?sort=hot">查看更多 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <div v-else class="book-grid">
          <BookCard v-for="(book, index) in hot" :key="book.id" :book="book" :rank="index + 1" />
        </div>
      </section>

      <section v-if="auth.isLoggedIn" class="section personal-section">
        <div class="personal-head">
          <div>
            <span class="chip accent"><el-icon><MagicStick /></el-icon>为你定制</span>
            <h2>猜你喜欢</h2>
            <p class="sub">根据你的浏览、收藏、评分与借阅行为实时生成</p>
          </div>
          <router-link class="more" to="/recommendations">查看完整推荐 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div v-if="personal.length" class="book-grid">
          <BookCard
            v-for="book in personal"
            :key="book.id"
            :book="book"
            show-reason
            show-algorithm
          />
        </div>
        <div v-else-if="!loading" class="panel empty-block">
          <el-icon class="icon"><Reading /></el-icon>
          <p>还没有足够的行为数据，先去浏览或收藏几本书吧。</p>
          <el-button type="primary" plain @click="router.push('/books')">去发现图书</el-button>
        </div>
      </section>

      <section v-else class="login-banner">
        <div class="banner-icon"><el-icon><UserFilled /></el-icon></div>
        <div class="banner-text">
          <h3>登录，解锁你的专属推荐</h3>
          <p>记录阅读偏好，获得基于协同过滤与内容分析的个性化书单。</p>
        </div>
        <el-button type="primary" round size="large" @click="router.push('/login')">立即登录</el-button>
      </section>

      <section class="ai-banner">
        <div class="ai-copy">
          <span class="chip"><el-icon><ChatDotRound /></el-icon>AI 阅读伙伴</span>
          <h2>不知道读什么？<br />直接问 AI 图书助手。</h2>
          <p>用自然语言描述你的兴趣或学习目标，系统会检索馆藏并给出带出处的阅读建议。</p>
          <div class="ai-actions">
            <el-button
              type="primary"
              round
              size="large"
              @click="router.push(auth.isLoggedIn ? '/ai' : '/login?redirect=/ai')"
            >
              开始对话
            </el-button>
            <el-button round size="large" @click="router.push(auth.isLoggedIn ? '/recommendations' : '/books')">
              浏览推荐
            </el-button>
          </div>
        </div>
        <div class="ai-chat-mock">
          <div class="mock-user">我想系统学习机器学习，有哪些入门书？</div>
          <div class="mock-ai">
            <div class="mock-avatar"><el-icon><MagicStick /></el-icon></div>
            <div>
              <p>根据馆藏，我为你找到 3 本适合入门的书：</p>
              <div class="mock-books">
                <span v-for="book in fresh.slice(0, 3)" :key="book.id">{{ book.title }}</span>
                <span v-if="!fresh.length">机器学习基础</span>
              </div>
              <small>内容基于馆藏检索结果生成</small>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div>
            <h2>新书上架</h2>
            <p class="sub">最近出版的馆藏图书，先读为快</p>
          </div>
          <router-link class="more" to="/books?sort=new">查看更多 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div class="book-grid">
          <BookCard v-for="book in fresh" :key="book.id" :book="book" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding-bottom: 40px;
}

.hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(900px 420px at 78% 18%, rgba(200, 137, 60, 0.14), transparent 60%),
    linear-gradient(140deg, #f8faf7 0%, #edf4ef 52%, #e3eee8 100%);
  border-bottom: 1px solid #e0e9e4;
}

.hero-inner {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 1.18fr 0.82fr;
  gap: 30px;
  padding-top: 56px;
  padding-bottom: 64px;
  min-height: 500px;
  align-items: center;
}

.hero-copy {
  max-width: 620px;
}

.welcome {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5b7166;
  padding: 6px 13px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid #dce8e1;
  border-radius: 999px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #3b8a6e;
  box-shadow: 0 0 0 4px rgba(59, 138, 110, 0.14);
}

h1 {
  margin-top: 20px;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.25;
  letter-spacing: 0.02em;
  font-weight: 700;
  color: #173f33;
}

h1 span {
  color: var(--accent);
  position: relative;
}

h1 span::after {
  content: '';
  position: absolute;
  left: 2px;
  right: 2px;
  bottom: 4px;
  height: 8px;
  border-radius: 4px;
  background: rgba(200, 137, 60, 0.18);
  z-index: -1;
}

.lead {
  margin-top: 18px;
  max-width: 520px;
  color: #61746b;
  font-size: 15px;
  line-height: 1.9;
}

.search-box {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 8px 8px 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(23, 63, 51, 0.1);
  border: 1px solid #e0e9e3;
  max-width: 580px;
}

.search-icon {
  color: #8ea098;
  font-size: 18px;
}

.search-box :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent;
  padding-left: 2px;
}

.search-box :deep(.el-input__inner) {
  font-size: 14px;
}

.search-box .el-button {
  height: 42px;
  padding: 0 24px;
  border-radius: 11px;
  flex: none;
}

.quick {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #84958d;
}

.quick button {
  border: none;
  background: transparent;
  color: #5d7468;
  font-size: 12px;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 6px;
  transition: background 0.2s ease, color 0.2s ease;
}

.quick button:hover {
  color: var(--brand);
  background: #e2eee8;
}

.stats {
  margin-top: 32px;
  display: flex;
  gap: 36px;
}

.stats div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.stats strong {
  font-size: 26px;
  color: #214d3f;
  letter-spacing: 0.02em;
}

.stats span {
  font-size: 11px;
  color: #879890;
}

.hero-visual {
  position: relative;
  min-height: 380px;
}

.visual-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(31, 92, 74, 0.13);
}

.ring-a {
  width: 340px;
  height: 340px;
  right: 16px;
  top: 20px;
}

.ring-b {
  width: 250px;
  height: 250px;
  right: 62px;
  top: 64px;
  border-style: dashed;
}

.book-stack {
  position: absolute;
  right: 40px;
  top: 34px;
  width: 280px;
  height: 300px;
}

.float-book {
  position: absolute;
  width: 142px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(18, 50, 39, 0.24);
  cursor: pointer;
  transition: transform 0.35s ease;
  background: #fff;
}

.float-book img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.float-book:hover {
  transform: translateY(-8px) rotate(0deg) !important;
  z-index: 6;
}

.fb-1 {
  left: 64px;
  top: 30px;
  transform: rotate(-7deg);
  z-index: 3;
}

.fb-2 {
  left: 20px;
  top: 74px;
  transform: rotate(-14deg);
  z-index: 2;
  opacity: 0.94;
}

.fb-3 {
  left: 112px;
  top: 86px;
  transform: rotate(8deg);
  z-index: 1;
  opacity: 0.9;
}

.placeholder-stack {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  position: absolute;
  right: 50px;
  bottom: 40px;
}

.placeholder-stack span {
  display: block;
  width: 60px;
  border-radius: 8px;
  background: linear-gradient(160deg, #d9e6df, #c3d6cc);
}

.placeholder-stack span:nth-child(1) { height: 150px; }
.placeholder-stack span:nth-child(2) { height: 190px; }
.placeholder-stack span:nth-child(3) { height: 130px; }

.spotlight {
  position: absolute;
  left: -6px;
  bottom: 18px;
  width: 235px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e4ece7;
  box-shadow: 0 18px 38px rgba(23, 63, 51, 0.13);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: transform 0.25s ease;
}

.spotlight:hover {
  transform: translateY(-3px);
}

.spotlight .tag {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: #b07626;
  font-weight: 700;
}

.spotlight strong {
  display: block;
  margin-top: 7px;
  font-size: 15px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spotlight small {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #81938a;
}

.spot-meta {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #a7742c;
}

.spot-meta span {
  color: #b8c4bd;
}

.hero-wave {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 22px;
  background: linear-gradient(180deg, transparent, rgba(244, 246, 242, 0.9));
}

.category-strip {
  padding: 26px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.strip-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.strip-head h2 {
  font-size: 20px;
}

.strip-head p {
  margin-top: 5px;
  font-size: 13px;
  color: var(--muted);
}

.more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--brand);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.cat-list {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 13px 14px;
  border-radius: 12px;
  border: 1px solid #e6ede8;
  background: #f8faf8;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
  overflow: hidden;
}

.cat-item .el-icon {
  color: #6d8d7e;
  font-size: 17px;
}

.cat-item span {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #33493f;
}

.cat-item small {
  font-size: 10px;
  color: #93a29a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.cat-item:hover {
  background: var(--brand);
  border-color: var(--brand);
  transform: translateY(-2px);
}

.cat-item:hover .el-icon,
.cat-item:hover span,
.cat-item:hover small {
  color: #fff;
}

.cat-item:hover small {
  color: #b8d2c7;
}

.personal-section {
  padding: 26px;
  border-radius: 22px;
  background: linear-gradient(160deg, #fdfaf4, #f8f4ea);
  border: 1px solid #efe4d0;
}

.personal-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.personal-head h2 {
  margin-top: 9px;
  font-size: 24px;
}

.personal-head .sub {
  margin-top: 6px;
  color: #8b7e6a;
  font-size: 13px;
}

.personal-section .book-card:hover {
  background: #fffdf8;
  border-color: #eadfc9;
}

.login-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 26px 30px;
  border-radius: 20px;
  background: linear-gradient(120deg, #1d5545, #123a2e);
  color: #eaf3ee;
  box-shadow: 0 18px 36px rgba(17, 54, 42, 0.18);
}

.banner-icon {
  width: 52px;
  height: 52px;
  flex: none;
  border-radius: 15px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  color: #ecd9b6;
  font-size: 24px;
}

.banner-text {
  flex: 1;
}

.banner-text h3 {
  font-size: 18px;
}

.banner-text p {
  margin-top: 6px;
  font-size: 13px;
  color: #a9c3b7;
}

.login-banner .el-button {
  flex: none;
  --el-button-bg-color: #f0e2c8;
  --el-button-border-color: #f0e2c8;
  --el-button-text-color: #1d4a3b;
}

.ai-banner {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 40px;
  align-items: center;
  padding: 38px;
  border-radius: 24px;
  background:
    radial-gradient(500px 260px at 85% 0%, rgba(200, 137, 60, 0.13), transparent 65%),
    #173f33;
  color: #e8f2ed;
  overflow: hidden;
}

.ai-copy h2 {
  margin-top: 16px;
  font-size: 30px;
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.ai-copy p {
  margin-top: 14px;
  max-width: 430px;
  color: #a5bfb3;
  font-size: 13px;
  line-height: 1.9;
}

.ai-copy .chip {
  background: rgba(200, 137, 60, 0.16);
  color: #e3c58f;
}

.ai-actions {
  margin-top: 22px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-actions .el-button:first-child {
  --el-button-bg-color: #d7a75f;
  --el-button-border-color: #d7a75f;
  --el-button-text-color: #213d32;
  --el-button-hover-bg-color: #e4b873;
  --el-button-hover-border-color: #e4b873;
}

.ai-actions .el-button:last-child {
  background: transparent;
  color: #d5e5dd;
  border-color: rgba(255, 255, 255, 0.25);
}

.ai-chat-mock {
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  backdrop-filter: blur(6px);
}

.mock-user {
  align-self: flex-end;
  max-width: 86%;
  padding: 10px 14px;
  border-radius: 14px 14px 4px 14px;
  background: #d7a75f;
  color: #213d32;
  font-size: 12px;
  line-height: 1.6;
}

.mock-ai {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.mock-avatar {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  flex: none;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  color: #e8c78e;
}

.mock-ai > div:last-child {
  padding: 12px 14px;
  border-radius: 4px 14px 14px 14px;
  background: rgba(255, 255, 255, 0.075);
  font-size: 12px;
  line-height: 1.75;
  color: #c5d9cf;
}

.mock-books {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.mock-books span {
  padding: 6px 9px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 11px;
  color: #d9e8e1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mock-ai small {
  display: block;
  margin-top: 8px;
  font-size: 9px;
  color: #7e9c8f;
}

@media (max-width: 1050px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }
  .hero-visual {
    display: none;
  }
  .cat-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .ai-banner {
    grid-template-columns: 1fr;
    gap: 26px;
    padding: 30px;
  }
}

@media (max-width: 760px) {
  .hero-inner {
    min-height: auto;
    padding-top: 36px;
    padding-bottom: 40px;
  }
  h1 {
    font-size: 32px;
  }
  .stats {
    gap: 24px;
  }
  .stats strong {
    font-size: 22px;
  }
  .search-box {
    flex-wrap: wrap;
    padding: 10px;
  }
  .search-box .el-button {
    width: 100%;
  }
  .search-icon {
    display: none;
  }
  .cat-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .login-banner {
    flex-wrap: wrap;
  }
  .login-banner .el-button {
    width: 100%;
  }
  .ai-copy h2 {
    font-size: 24px;
  }
  .personal-section {
    padding: 18px;
  }
}
</style>
