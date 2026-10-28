<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookCard from '@/components/BookCard.vue'
import MiniBook from '@/components/MiniBook.vue'
import { authApi, behaviorApi, meApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import {
  behaviorText,
  borrowStatusText,
  borrowStatusType,
  formatDate,
  formatDateTime,
  roleText
} from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()

const activeTab = ref('profile')
const favorites = ref([])
const history = ref([])
const borrows = ref([])
const dataLoading = ref(true)
const saving = ref(false)
const changingPassword = ref(false)
const profileRef = ref()
const passwordRef = ref()

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
}

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== passwordForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

const favoriteBooks = computed(() => favorites.value.map((item) => item.book).filter(Boolean))
const activeBorrowCount = computed(
  () => borrows.value.filter((item) => item.status === 'BORROWED' || item.status === 'OVERDUE').length
)

function syncProfileForm() {
  const user = auth.user
  if (!user) return
  profileForm.nickname = user.nickname || ''
  profileForm.email = user.email || ''
  profileForm.phone = user.phone || ''
  profileForm.avatar = user.avatar || ''
}

async function loadProfileData() {
  dataLoading.value = true
  try {
    const [favoriteData, historyData, borrowData] = await Promise.all([
      meApi.favorites().catch(() => []),
      meApi.history(50).catch(() => []),
      meApi.borrows().catch(() => [])
    ])
    favorites.value = favoriteData || []
    history.value = historyData || []
    borrows.value = borrowData || []
  } finally {
    dataLoading.value = false
  }
}

async function saveProfile() {
  await profileRef.value.validate()
  saving.value = true
  try {
    const data = await authApi.updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email || null,
      phone: profileForm.phone || null,
      avatar: profileForm.avatar || null
    })
    auth.updateUser(data)
    ElMessage.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  await passwordRef.value.validate()
  changingPassword.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    auth.clearAuth()
    router.replace('/login')
  } finally {
    changingPassword.value = false
  }
}

async function returnBook(record) {
  try {
    await ElMessageBox.confirm(`确认归还《${record.bookTitle}》吗？`, '归还确认', {
      confirmButtonText: '确认归还',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  await behaviorApi.returnBook(record.id)
  ElMessage.success('归还成功')
  await loadProfileData()
}

watch(
  () => auth.user,
  () => syncProfileForm(),
  { immediate: true }
)

onMounted(loadProfileData)
</script>

<template>
  <div class="page-shell page-stack">
    <section class="profile-hero">
      <div class="profile-main">
        <el-avatar :size="76" :src="auth.user?.avatar || undefined" class="avatar">
          {{ auth.avatarText }}
        </el-avatar>
        <div class="profile-info">
          <div class="name-line">
            <h1>{{ auth.displayName }}</h1>
            <span class="chip" :class="{ accent: auth.isAdmin }">{{ roleText(auth.user?.role) }}</span>
          </div>
          <p class="username">@{{ auth.user?.username }}</p>
          <p class="join">
            <el-icon><Calendar /></el-icon>
            加入于 {{ formatDate(auth.user?.createTime) }}
            <span v-if="auth.user?.email">· {{ auth.user.email }}</span>
          </p>
        </div>
      </div>
      <div class="profile-stats">
        <div>
          <strong>{{ favorites.length }}</strong>
          <span>收藏图书</span>
        </div>
        <div>
          <strong>{{ history.length }}</strong>
          <span>浏览记录</span>
        </div>
        <div>
          <strong>{{ activeBorrowCount }}</strong>
          <span>当前借阅</span>
        </div>
        <div>
          <strong>{{ borrows.length }}</strong>
          <span>累计借阅</span>
        </div>
      </div>
    </section>

    <section class="profile-panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="profile">
          <template #label><span class="tab-label"><el-icon><User /></el-icon>个人资料</span></template>
          <div class="form-grid">
            <div class="form-card">
              <div class="form-head">
                <h3>基本信息</h3>
                <p>完善资料，方便接收借阅与推荐服务。</p>
              </div>
              <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-position="top">
                <div class="two-col">
                  <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="profileForm.nickname" maxlength="50" />
                  </el-form-item>
                  <el-form-item label="邮箱" prop="email">
                    <el-input v-model="profileForm.email" placeholder="选填" />
                  </el-form-item>
                </div>
                <div class="two-col">
                  <el-form-item label="手机号">
                    <el-input v-model="profileForm.phone" placeholder="选填" maxlength="20" />
                  </el-form-item>
                  <el-form-item label="头像地址">
                    <el-input v-model="profileForm.avatar" placeholder="选填，图片 URL" maxlength="255" />
                  </el-form-item>
                </div>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
              </el-form>
            </div>

            <div class="form-card">
              <div class="form-head">
                <h3>账号安全</h3>
                <p>修改密码后需要重新登录。</p>
              </div>
              <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <div class="two-col">
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                  </el-form-item>
                </div>
                <el-button type="primary" plain :loading="changingPassword" @click="changePassword">
                  修改密码
                </el-button>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="favorites">
          <template #label>
            <span class="tab-label"><el-icon><Star /></el-icon>我的收藏 <em>{{ favorites.length }}</em></span>
          </template>
          <div v-loading="dataLoading" class="tab-content">
            <div v-if="favoriteBooks.length" class="book-grid">
              <BookCard v-for="book in favoriteBooks" :key="book.id" :book="book" />
            </div>
            <div v-else class="empty-block">
              <el-icon class="icon"><Star /></el-icon>
              <h3>还没有收藏图书</h3>
              <p>在图书详情页点击收藏，喜欢的书会出现在这里。</p>
              <el-button type="primary" plain @click="router.push('/books')">去发现图书</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="history">
          <template #label>
            <span class="tab-label"><el-icon><Clock /></el-icon>浏览历史 <em>{{ history.length }}</em></span>
          </template>
          <div v-loading="dataLoading" class="tab-content">
            <div v-if="history.length" class="history-list">
              <div v-for="item in history" :key="item.id" class="history-row">
                <MiniBook v-if="item.book" :book="item.book" />
                <div class="history-meta">
                  <el-tag size="small" effect="plain" type="info">{{ behaviorText(item.behaviorType) }}</el-tag>
                  <span>{{ item.remark }}</span>
                  <small>{{ formatDateTime(item.createTime) }}</small>
                </div>
              </div>
            </div>
            <div v-else class="empty-block">
              <el-icon class="icon"><Clock /></el-icon>
              <h3>暂无浏览记录</h3>
              <p>浏览图书详情后，系统会记录你的阅读兴趣。</p>
              <el-button type="primary" plain @click="router.push('/books')">浏览馆藏</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="borrows">
          <template #label>
            <span class="tab-label"><el-icon><Tickets /></el-icon>借阅记录 <em>{{ borrows.length }}</em></span>
          </template>
          <div v-loading="dataLoading" class="tab-content">
            <el-table v-if="borrows.length" :data="borrows" stripe style="width: 100%">
              <el-table-column label="图书" min-width="260">
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
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="borrowStatusType(row.status)" effect="light" size="small">
                    {{ borrowStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'BORROWED' || row.status === 'OVERDUE'"
                    type="primary"
                    link
                    @click="returnBook(row)"
                  >
                    归还
                  </el-button>
                  <span v-else class="muted">已完成</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="empty-block">
              <el-icon class="icon"><Tickets /></el-icon>
              <h3>暂无借阅记录</h3>
              <p>从馆藏中选择感兴趣的图书，开始你的阅读计划。</p>
              <el-button type="primary" plain @click="router.push('/books')">浏览馆藏</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  padding: 30px 34px;
  border-radius: 24px;
  background:
    radial-gradient(540px 300px at 90% 10%, rgba(200, 137, 60, 0.16), transparent 64%),
    linear-gradient(140deg, #1d5545, #123a2e 65%, #0d2e24);
  color: #e9f3ee;
  box-shadow: 0 22px 46px rgba(18, 54, 42, 0.18);
  flex-wrap: wrap;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar {
  border: 3px solid rgba(255, 255, 255, 0.18);
  background: #2b6a56;
  color: #f1e1c3;
  font-size: 26px;
  font-weight: 600;
  flex: none;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.name-line h1 {
  font-size: 26px;
  letter-spacing: 0.02em;
}

.name-line .chip {
  background: rgba(255, 255, 255, 0.12);
  color: #d7e7df;
}

.name-line .chip.accent {
  background: rgba(200, 137, 60, 0.2);
  color: #e8ca96;
}

.username {
  margin-top: 5px;
  font-size: 12px;
  color: #8fae9f;
}

.join {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #9db8ab;
  flex-wrap: wrap;
}

.profile-stats {
  display: flex;
  gap: 30px;
  padding-left: 30px;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
}

.profile-stats div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-stats strong {
  font-size: 24px;
  color: #f0e3c9;
}

.profile-stats span {
  font-size: 10px;
  color: #93afa2;
}

.profile-panel {
  padding: 10px 26px 26px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.tab-label em {
  font-style: normal;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #eef3ef;
  color: #7b8e84;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-card {
  padding: 22px;
  border-radius: 16px;
  border: 1px solid #e9efeb;
  background: #fbfcfa;
}

.form-head {
  margin-bottom: 18px;
}

.form-head h3 {
  font-size: 16px;
  color: #2e4f41;
}

.form-head p {
  margin-top: 5px;
  font-size: 11px;
  color: #96a49d;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.tab-content {
  min-height: 240px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px;
  border: 1px solid #e9efeb;
  border-radius: 12px;
  background: #fbfcfa;
}

.history-row :deep(.mini-book) {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 0;
}

.history-row :deep(.mini-book img) {
  width: 42px;
  height: 56px;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
  color: #809188;
  font-size: 11px;
}

.history-meta small {
  color: #a3b0a9;
}

.book-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.book-cell img {
  width: 38px;
  height: 50px;
  border-radius: 5px;
  object-fit: cover;
  background: #e8efeb;
}

.book-cell div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.book-cell strong {
  font-size: 12px;
  color: #3b5146;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-cell span {
  font-size: 10px;
  color: #93a29a;
}

.overdue {
  color: #c45f42;
  font-weight: 600;
}

.empty-block h3 {
  font-size: 15px;
  color: #52665c;
  margin-bottom: 6px;
}

.empty-block p {
  font-size: 12px;
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .profile-hero {
    padding: 24px;
  }
  .profile-stats {
    width: 100%;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    padding-top: 18px;
    justify-content: space-between;
    gap: 12px;
  }
  .history-meta {
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
  }
}

@media (max-width: 600px) {
  .profile-panel {
    padding: 6px 14px 18px;
  }
  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .history-row {
    flex-direction: column;
    align-items: stretch;
  }
  .history-meta {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
  }
}
</style>
