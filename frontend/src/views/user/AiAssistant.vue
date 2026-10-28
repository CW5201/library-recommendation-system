<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import MiniBook from '@/components/MiniBook.vue'
import { aiApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const activeTab = ref('chat')
const status = ref(null)
const statusLoading = ref(true)

const messages = ref([])
const sessionId = ref('')
const input = ref('')
const sending = ref(false)
const chatBodyRef = ref(null)
const chatNotice = ref('')

const searchQuery = ref('')
const searchResults = ref([])
const searching = ref(false)
const searched = ref(false)

const planForm = reactive({ goal: '', count: 5 })
const planResult = ref(null)
const planning = ref(false)

const quickPrompts = [
  '我想系统学习 Java 后端开发，应该读哪些书？',
  '推荐几本适合入门的人工智能图书',
  '帮我找一些提升写作能力的文学作品',
  '我想了解中国经济，有哪些通俗读物？'
]

const searchExamples = ['适合初学者的机器学习教材', '关于中国历史的经典著作', '提升编程思维的书籍', '经济学入门读物']
const planExamples = ['三个月内入门前端开发', '准备考研数学，补充数学思维', '系统了解人工智能与深度学习', '提升文学素养与写作能力']

const sessionKey = computed(() => `library_ai_session_${auth.user?.id || 'guest'}`)

const embeddingLabel = computed(() => {
  if (!status.value) return '检测中'
  return status.value.embeddingModel?.startsWith('local-') ? '本地向量检索' : '语义向量检索'
})

const greeting = () => ({
  role: 'assistant',
  content: '你好，我是知书 AI 图书助手。你可以告诉我学习目标、感兴趣的领域，或想解决的阅读需求，我会从馆藏中检索相关图书并给出建议。',
  relatedBooks: [],
  local: true
})

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function loadStatus() {
  statusLoading.value = true
  try {
    status.value = await aiApi.status()
  } catch {
    status.value = null
  } finally {
    statusLoading.value = false
  }
}

async function loadHistory() {
  const stored = localStorage.getItem(sessionKey.value)
  if (!stored) {
    messages.value = [greeting()]
    return
  }
  sessionId.value = stored
  try {
    const history = await aiApi.history(stored)
    if (history?.length) {
      messages.value = history.map((item) => ({
        role: item.role,
        content: item.content,
        relatedBooks: item.relatedBooks || [],
        time: item.createTime
      }))
      scrollToBottom()
    } else {
      messages.value = [greeting()]
    }
  } catch {
    messages.value = [greeting()]
  }
}

function newChat() {
  localStorage.removeItem(sessionKey.value)
  sessionId.value = ''
  chatNotice.value = ''
  messages.value = [greeting()]
  input.value = ''
  scrollToBottom()
}

async function sendMessage(text) {
  const content = (text ?? input.value).trim()
  if (!content || sending.value) return
  messages.value.push({ role: 'user', content })
  input.value = ''
  sending.value = true
  scrollToBottom()
  try {
    const data = await aiApi.chat({
      message: content,
      sessionId: sessionId.value || undefined
    })
    sessionId.value = data.sessionId
    localStorage.setItem(sessionKey.value, data.sessionId)
    chatNotice.value = data.fallback ? data.notice || '' : ''
    messages.value.push({
      role: 'assistant',
      content: data.answer,
      relatedBooks: data.relatedBooks || [],
      fallback: data.fallback
    })
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI 服务暂时不可用，请稍后再试。你仍然可以使用馆藏检索和个性化推荐。',
      relatedBooks: [],
      fallback: true
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

async function semanticSearch() {
  if (!searchQuery.value.trim() || searching.value) return
  searching.value = true
  searched.value = false
  try {
    searchResults.value = await aiApi.semanticSearch({
      query: searchQuery.value.trim(),
      limit: 8
    })
    searched.value = true
  } finally {
    searching.value = false
  }
}

async function generatePlan() {
  if (!planForm.goal.trim() || planning.value) return
  planning.value = true
  planResult.value = null
  try {
    planResult.value = await aiApi.readingPlan({
      goal: planForm.goal.trim(),
      count: planForm.count
    })
  } finally {
    planning.value = false
  }
}

function useSearchExample(text) {
  searchQuery.value = text
  semanticSearch()
}

function usePlanExample(text) {
  planForm.goal = text
}

onMounted(async () => {
  await Promise.all([loadStatus(), loadHistory()])
})
</script>

<template>
  <div class="page-shell page-stack">
    <section class="ai-hero">
      <div class="hero-copy">
        <span class="chip light"><el-icon><MagicStick /></el-icon>RAG · 馆藏增强问答</span>
        <h1>知书 AI 图书助手</h1>
        <p>
          用自然语言描述你的需求。助手会先检索馆藏向量索引，再结合检索结果生成回答，
          让每一条建议都有馆藏依据。
        </p>
        <div class="hero-status">
          <span class="status-item" :class="{ on: status?.llmConfigured }">
            <i></i>{{ status?.llmConfigured ? '大模型已连接' : '规则检索模式' }}
          </span>
          <span class="status-item on"><i></i>{{ embeddingLabel }}</span>
          <span v-if="status?.chatModel" class="model">{{ status.chatModel }}</span>
        </div>
      </div>
      <div class="hero-orb">
        <div class="orb-ring r1"></div>
        <div class="orb-ring r2"></div>
        <div class="orb-core"><el-icon><ChatDotRound /></el-icon></div>
        <span class="float-tag t1"><el-icon><Search /></el-icon>语义检索</span>
        <span class="float-tag t2"><el-icon><Reading /></el-icon>馆藏上下文</span>
        <span class="float-tag t3"><el-icon><MagicStick /></el-icon>智能生成</span>
      </div>
    </section>

    <el-alert
      v-if="status && !status.llmConfigured"
      title="AI 大模型尚未配置"
      type="warning"
      :closable="false"
      show-icon
      :description="status.message"
    />

    <section class="ai-panel">
      <el-tabs v-model="activeTab" class="ai-tabs">
        <el-tab-pane name="chat">
          <template #label>
            <span class="tab-label"><el-icon><ChatLineRound /></el-icon>智能问答</span>
          </template>

          <div class="chat-layout">
            <div class="chat-main">
              <div ref="chatBodyRef" class="chat-body">
                <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
                  <div v-if="msg.role === 'assistant'" class="avatar ai-avatar">
                    <el-icon><MagicStick /></el-icon>
                  </div>
                  <div class="message-content">
                    <div class="bubble">
                      <p>{{ msg.content }}</p>
                      <div v-if="msg.relatedBooks?.length" class="related">
                        <div class="related-head">
                          <span>馆藏参考</span>
                          <small>{{ msg.relatedBooks.length }} 本</small>
                        </div>
                        <div class="related-grid">
                          <MiniBook
                            v-for="book in msg.relatedBooks.slice(0, 4)"
                            :key="book.id"
                            :book="book"
                            similarity
                          />
                        </div>
                      </div>
                      <div v-if="msg.fallback" class="fallback-tip">
                        <el-icon><InfoFilled /></el-icon>
                        当前为检索结果模式，配置大模型 API 后可获得完整对话能力。
                      </div>
                    </div>
                    <span class="msg-time">{{ msg.role === 'user' ? '我' : '知书 AI' }}</span>
                  </div>
                  <div v-if="msg.role === 'user'" class="avatar user-avatar">
                    {{ auth.avatarText }}
                  </div>
                </div>

                <div v-if="sending" class="message assistant">
                  <div class="avatar ai-avatar"><el-icon><MagicStick /></el-icon></div>
                  <div class="typing">
                    <span></span><span></span><span></span>
                    <small>正在检索馆藏并组织回答…</small>
                  </div>
                </div>
              </div>

              <div v-if="chatNotice" class="chat-notice">
                <el-icon><InfoFilled /></el-icon>{{ chatNotice }}
              </div>

              <div class="chat-input">
                <el-input
                  v-model="input"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  resize="none"
                  placeholder="描述你的阅读需求，例如：我想入门机器学习，适合读什么书？"
                  maxlength="1000"
                  @keydown.enter.exact.prevent="sendMessage()"
                />
                <div class="input-bar">
                  <span class="hint">Enter 发送 · Shift + Enter 换行</span>
                  <div class="input-actions">
                    <el-button text @click="newChat">
                      <el-icon><Plus /></el-icon>新对话
                    </el-button>
                    <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="sendMessage()">
                      发送
                      <el-icon class="send-icon"><Promotion /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <aside class="chat-side">
              <div class="side-block">
                <h4>试试这样问</h4>
                <button v-for="prompt in quickPrompts" :key="prompt" class="prompt" @click="sendMessage(prompt)">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ prompt }}</span>
                </button>
              </div>
              <div class="side-block status-block">
                <h4>当前能力</h4>
                <div class="cap">
                  <span>大模型</span>
                  <b :class="{ off: !status?.llmConfigured }">{{ status?.llmConfigured ? '已配置' : '未配置' }}</b>
                </div>
                <div class="cap">
                  <span>向量模型</span>
                  <b>{{ status?.embeddingModel || '—' }}</b>
                </div>
                <div class="cap">
                  <span>检索来源</span>
                  <b>{{ status?.provider || '—' }}</b>
                </div>
              </div>
              <div class="side-tip">
                <el-icon><Warning /></el-icon>
                <p>AI 回答基于馆藏数据生成，借阅状态请以图书详情页为准。</p>
              </div>
            </aside>
          </div>
        </el-tab-pane>

        <el-tab-pane name="search">
          <template #label>
            <span class="tab-label"><el-icon><Search /></el-icon>语义检索</span>
          </template>

          <div class="tool-intro">
            <div>
              <h3>用一句话找到对的书</h3>
              <p>不必记住书名，描述主题、用途或阅读难度即可。系统会把查询转换为向量，与馆藏语义匹配。</p>
            </div>
            <span class="chip accent"><el-icon><Aim /></el-icon>语义相似度排序</span>
          </div>

          <div class="search-area">
            <el-input
              v-model="searchQuery"
              size="large"
              placeholder="例如：适合有一点编程基础的人学习深度学习的书"
              maxlength="300"
              clearable
              @keyup.enter="semanticSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" size="large" :loading="searching" @click="semanticSearch">语义检索</el-button>
          </div>

          <div class="example-row">
            <span>试试：</span>
            <button v-for="text in searchExamples" :key="text" @click="useSearchExample(text)">{{ text }}</button>
          </div>

          <div v-loading="searching" class="search-result">
            <template v-if="searched">
              <div v-if="searchResults.length" class="book-grid">
                <BookCard
                  v-for="book in searchResults"
                  :key="book.id"
                  :book="book"
                  show-similarity
                  show-reason
                />
              </div>
              <div v-else class="empty-block">
                <el-icon class="icon"><Search /></el-icon>
                <h3>没有找到语义相关的图书</h3>
                <p>尝试换一种描述方式，或减少限定词。</p>
              </div>
            </template>
            <div v-else class="search-placeholder">
              <el-icon><Reading /></el-icon>
              <p>检索结果会显示语义相关度，帮助你判断匹配程度。</p>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="plan">
          <template #label>
            <span class="tab-label"><el-icon><Notebook /></el-icon>阅读计划</span>
          </template>

          <div class="tool-intro">
            <div>
              <h3>生成你的主题阅读清单</h3>
              <p>描述学习目标与时间安排，助手会从馆藏中挑选图书，并给出阅读顺序与建议。</p>
            </div>
            <span class="chip accent"><el-icon><Calendar /></el-icon>目标驱动</span>
          </div>

          <div class="plan-form">
            <el-input
              v-model="planForm.goal"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="例如：我希望在三个月内系统入门前端开发，并补充计算机基础"
              maxlength="300"
              show-word-limit
            />
            <div class="plan-actions">
              <div class="count-select">
                <span>推荐数量</span>
                <el-select v-model="planForm.count" style="width: 100px">
                  <el-option v-for="n in [3, 5, 8, 10]" :key="n" :label="`${n} 本`" :value="n" />
                </el-select>
              </div>
              <el-button type="primary" size="large" :loading="planning" :disabled="!planForm.goal.trim()" @click="generatePlan">
                <el-icon><MagicStick /></el-icon>生成阅读计划
              </el-button>
            </div>
            <div class="example-row plan-examples">
              <span>示例：</span>
              <button v-for="text in planExamples" :key="text" @click="usePlanExample(text)">{{ text }}</button>
            </div>
          </div>

          <div v-loading="planning" class="plan-result">
            <template v-if="planResult">
              <div class="plan-content">
                <div class="plan-head">
                  <span class="chip" :class="{ accent: !planResult.aiGenerated }">
                    <el-icon><component :is="planResult.aiGenerated ? 'MagicStick' : 'DataAnalysis'" /></el-icon>
                    {{ planResult.aiGenerated ? 'AI 生成计划' : '系统推荐清单' }}
                  </span>
                  <span class="muted">目标：{{ planForm.goal }}</span>
                </div>
                <p class="plan-text">{{ planResult.content }}</p>
              </div>
              <div v-if="planResult.books?.length" class="plan-books">
                <h4>推荐书目</h4>
                <div class="book-grid">
                  <BookCard v-for="book in planResult.books" :key="book.id" :book="book" show-similarity />
                </div>
              </div>
            </template>
            <div v-else-if="!planning" class="search-placeholder">
              <el-icon><Notebook /></el-icon>
              <p>填写目标并生成计划，馆藏图书将按照阅读路径为你排列。</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="ai-foot">
      <div>
        <el-icon><Lock /></el-icon>
        <p>AI 请求由后端代理，API Key 不会暴露在浏览器中。馆藏检索与推荐服务在无大模型时仍可正常工作。</p>
      </div>
      <el-button text @click="router.push('/books')">继续浏览馆藏 <el-icon><ArrowRight /></el-icon></el-button>
    </section>
  </div>
</template>

<style scoped>
.ai-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 30px;
  align-items: center;
  min-height: 270px;
  padding: 38px 42px;
  border-radius: 26px;
  overflow: hidden;
  background:
    radial-gradient(620px 320px at 85% 10%, rgba(200, 137, 60, 0.2), transparent 62%),
    linear-gradient(140deg, #1c5344, #12392d 60%, #0d2c23);
  color: #e9f3ee;
  box-shadow: 0 24px 52px rgba(18, 54, 42, 0.2);
}

.chip.light {
  background: rgba(255, 255, 255, 0.1);
  color: #dcc18d;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-copy h1 {
  margin-top: 16px;
  font-size: clamp(30px, 3.3vw, 42px);
  letter-spacing: 0.02em;
}

.hero-copy p {
  margin-top: 14px;
  max-width: 560px;
  color: #a6c0b4;
  font-size: 13px;
  line-height: 1.95;
}

.hero-status {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.status-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #93af9f;
}

.status-item i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #778f84;
}

.status-item.on {
  color: #cfe2d8;
}

.status-item.on i {
  background: #66c49b;
  box-shadow: 0 0 0 3px rgba(102, 196, 155, 0.16);
}

.status-item:not(.on) i {
  background: #d19a57;
}

.model {
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  color: #b9cdc2;
  font-size: 10px;
}

.hero-orb {
  position: relative;
  height: 210px;
}

.orb-ring {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.13);
}

.r1 {
  width: 190px;
  height: 190px;
  animation: pulse 5s ease-in-out infinite;
}

.r2 {
  width: 130px;
  height: 130px;
  border-style: dashed;
  animation: spin 22s linear infinite;
}

@keyframes spin {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes pulse {
  50% { transform: translate(-50%, -50%) scale(1.05); opacity: 0.6; }
}

.orb-core {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 78px;
  height: 78px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  font-size: 34px;
  color: #f0dcb6;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(8px);
}

.float-tag {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #c4d8ce;
  font-size: 10px;
  backdrop-filter: blur(4px);
}

.t1 { left: 0; top: 30px; }
.t2 { right: 0; top: 74px; }
.t3 { left: 26px; bottom: 26px; }

.ai-panel {
  border-radius: 22px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
  padding: 8px 24px 24px;
}

.ai-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  gap: 22px;
}

.chat-main {
  min-width: 0;
  border: 1px solid #e6ece8;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fbfcfa;
}

.chat-body {
  height: 480px;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  scroll-behavior: smooth;
}

.chat-body::-webkit-scrollbar {
  width: 6px;
}

.chat-body::-webkit-scrollbar-thumb {
  background: #d4dfd9;
  border-radius: 3px;
}

.message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  max-width: 92%;
}

.message.user {
  align-self: flex-end;
}

.avatar {
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 600;
}

.ai-avatar {
  background: linear-gradient(145deg, #245d4b, #163f33);
  color: #ecd6ae;
  font-size: 15px;
}

.user-avatar {
  background: #e8eee9;
  color: #4a6257;
}

.message-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.message.user .message-content {
  align-items: flex-end;
}

.bubble {
  padding: 13px 16px;
  border-radius: 4px 14px 14px 14px;
  background: #fff;
  border: 1px solid #e6ece8;
  color: #45594f;
  font-size: 13px;
  line-height: 1.85;
  box-shadow: 0 3px 10px rgba(24, 55, 44, 0.03);
}

.message.user .bubble {
  border-radius: 14px 4px 14px 14px;
  background: var(--brand);
  border-color: var(--brand);
  color: #eef5f1;
}

.bubble p {
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-time {
  font-size: 9px;
  color: #a3b0a9;
  padding: 0 3px;
}

.related {
  margin-top: 13px;
  padding-top: 12px;
  border-top: 1px dashed #e6ece8;
}

.related-head {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #6d8177;
  margin-bottom: 8px;
}

.related-head small {
  color: #a2afa8;
}

.related-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
}

.related-grid :deep(.mini-book) {
  background: #f9fbf9;
  padding: 7px;
}

.related-grid :deep(img) {
  width: 32px;
  height: 44px;
}

.related-grid :deep(strong) {
  font-size: 11px;
}

.related-grid :deep(span) {
  font-size: 10px;
}

.related-grid :deep(.arrow) {
  display: none;
}

.fallback-tip {
  margin-top: 11px;
  padding-top: 10px;
  border-top: 1px dashed #e6ece8;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: #a08a64;
}

.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 4px 14px 14px 14px;
  background: #fff;
  border: 1px solid #e6ece8;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a7bcb1;
  animation: blink 1.2s infinite;
}

.typing span:nth-child(2) { animation-delay: 0.16s; }
.typing span:nth-child(3) { animation-delay: 0.32s; }

.typing small {
  margin-left: 8px;
  font-size: 10px;
  color: #9aa9a1;
}

@keyframes blink {
  0%, 60%, 100% { opacity: 0.35; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-2px); }
}

.chat-notice {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 18px;
  background: #fdf7ec;
  border-top: 1px solid #f3e7d2;
  font-size: 10px;
  color: #9a7434;
}

.chat-input {
  border-top: 1px solid #e6ece8;
  padding: 13px 16px 12px;
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  box-shadow: none !important;
  padding: 0;
  font-size: 13px;
  line-height: 1.7;
}

.input-bar {
  margin-top: 9px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hint {
  font-size: 10px;
  color: #a4b1aa;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.send-icon {
  margin-left: 3px;
}

.chat-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-block {
  padding: 17px;
  border-radius: 15px;
  background: #f7faf7;
  border: 1px solid #e8eee9;
}

.side-block h4 {
  font-size: 12px;
  color: #43584d;
  margin-bottom: 11px;
}

.prompt {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 9px 10px;
  margin-bottom: 7px;
  border: 1px solid #e3ebe6;
  border-radius: 9px;
  background: #fff;
  color: #5d7167;
  font-size: 11px;
  line-height: 1.6;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt:last-child {
  margin-bottom: 0;
}

.prompt .el-icon {
  margin-top: 2px;
  color: #97ab9f;
  flex: none;
}

.prompt:hover {
  border-color: #b9d0c5;
  color: var(--brand);
  transform: translateX(2px);
}

.status-block .cap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 0;
  font-size: 10px;
  border-bottom: 1px solid #edf2ee;
}

.status-block .cap:last-child {
  border-bottom: none;
}

.cap span {
  color: #8b9a92;
}

.cap b {
  color: #4f6659;
  font-weight: 600;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cap b.off {
  color: #b0803c;
}

.side-tip {
  display: flex;
  gap: 8px;
  padding: 13px 15px;
  border-radius: 12px;
  background: #fdf8ef;
  border: 1px solid #f2e6d1;
  color: #a07838;
}

.side-tip .el-icon {
  flex: none;
  margin-top: 2px;
}

.side-tip p {
  font-size: 10px;
  line-height: 1.7;
}

.tool-intro {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.tool-intro h3 {
  font-size: 19px;
  color: #244a3c;
}

.tool-intro p {
  margin-top: 7px;
  max-width: 640px;
  font-size: 12px;
  line-height: 1.8;
  color: #87968e;
}

.search-area {
  display: flex;
  gap: 9px;
}

.search-area :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f9fbf9;
  box-shadow: 0 0 0 1px #dfe8e2 inset;
}

.search-area .el-button {
  height: 40px;
  border-radius: 10px;
  flex: none;
  padding: 0 22px;
}

.example-row {
  margin-top: 11px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 11px;
  color: #98a69f;
}

.example-row button {
  border: none;
  background: #f2f6f3;
  color: #60766a;
  font-size: 11px;
  padding: 4px 9px;
  border-radius: 7px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.example-row button:hover {
  background: #e5efe9;
  color: var(--brand);
}

.search-result,
.plan-result {
  margin-top: 24px;
  min-height: 180px;
}

.search-placeholder {
  padding: 56px 20px;
  text-align: center;
  color: #a1aea7;
  border: 1px dashed #dfe8e2;
  border-radius: 14px;
  background: #fbfcfa;
}

.search-placeholder .el-icon {
  font-size: 30px;
  color: #c0cdc6;
  margin-bottom: 8px;
}

.search-placeholder p {
  font-size: 12px;
}

.empty-block h3 {
  font-size: 15px;
  color: #52665c;
  margin-bottom: 6px;
}

.empty-block p {
  font-size: 12px;
  margin-bottom: 14px;
}

.plan-form {
  padding: 18px;
  border-radius: 15px;
  background: #f8faf8;
  border: 1px solid #e8eee9;
}

.plan-form :deep(.el-textarea__inner) {
  background: #fff;
  box-shadow: 0 0 0 1px #e2eae5 inset;
  border-radius: 10px;
  font-size: 13px;
}

.plan-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.count-select {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  color: #7f8f87;
}

.plan-examples {
  margin-top: 12px;
}

.plan-content {
  padding: 22px;
  border-radius: 16px;
  background: linear-gradient(160deg, #fbfdfb, #f4f8f4);
  border: 1px solid #e4ece7;
}

.plan-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 11px;
}

.plan-text {
  margin-top: 15px;
  color: #4e6258;
  font-size: 13px;
  line-height: 2;
  white-space: pre-wrap;
}

.plan-books {
  margin-top: 24px;
}

.plan-books h4 {
  margin-bottom: 14px;
  font-size: 15px;
  color: #334f42;
}

.ai-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 20px;
  border-radius: 14px;
  background: #eef3ef;
  border: 1px solid #e0e8e3;
  flex-wrap: wrap;
}

.ai-foot > div {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #84948c;
}

.ai-foot p {
  font-size: 11px;
  line-height: 1.6;
}

.ai-foot .el-button {
  font-size: 12px;
  color: var(--brand);
}

@media (max-width: 1000px) {
  .ai-hero {
    grid-template-columns: 1fr;
  }
  .hero-orb {
    display: none;
  }
  .chat-layout {
    grid-template-columns: 1fr;
  }
  .chat-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .side-tip {
    grid-column: 1 / -1;
  }
}

@media (max-width: 700px) {
  .ai-hero {
    padding: 26px;
  }
  .ai-panel {
    padding: 4px 14px 18px;
  }
  .chat-body {
    height: 420px;
    padding: 14px;
  }
  .message {
    max-width: 100%;
  }
  .related-grid {
    grid-template-columns: 1fr;
  }
  .chat-side {
    grid-template-columns: 1fr;
  }
  .search-area {
    flex-direction: column;
  }
  .search-area .el-button {
    width: 100%;
  }
  .tool-intro {
    flex-direction: column;
    gap: 10px;
  }
  .input-bar {
    align-items: flex-end;
  }
  .hint {
    display: none;
  }
  .input-actions {
    margin-left: auto;
  }
}
</style>
