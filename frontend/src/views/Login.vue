<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  username: 'student',
  password: 'Student@123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demos = [
  { label: '学生读者', username: 'student', password: 'Student@123', icon: 'User' },
  { label: '系统管理员', username: 'admin', password: 'Admin@123', icon: 'Setting' }
]

function fill(account) {
  form.username = account.username
  form.password = account.password
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const data = await auth.login({ ...form })
    ElMessage.success(`欢迎回来，${data.user.nickname || data.user.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect) {
      router.replace(redirect)
    } else {
      router.replace(data.user.role === 'ADMIN' ? '/admin/dashboard' : '/home')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <div class="login-head">
      <span class="eyebrow">WELCOME BACK</span>
      <h2>登录你的阅读空间</h2>
      <p>登录后可获得个性化推荐、借阅记录与 AI 图书助手服务。</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" clearable>
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-button class="submit" type="primary" size="large" :loading="submitting" @click="submit">
        登录
      </el-button>
    </el-form>

    <div class="demo-box">
      <div class="demo-title">
        <span>演示账号</span>
        <small>点击快速填充</small>
      </div>
      <div class="demo-list">
        <button v-for="item in demos" :key="item.username" type="button" class="demo" @click="fill(item)">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.username }}</small>
          </span>
          <el-icon class="go"><Right /></el-icon>
        </button>
      </div>
      <p class="demo-pass">密码：学生 <b>Student@123</b> · 管理员 <b>Admin@123</b></p>
    </div>

    <p class="switch">
      还没有账号？
      <router-link to="/register">立即注册</router-link>
      <span class="dot">·</span>
      <router-link to="/books">先逛逛馆藏</router-link>
    </p>
  </AuthLayout>
</template>

<style scoped>
.login-head {
  margin-bottom: 28px;
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 0.22em;
  color: var(--accent);
  font-weight: 700;
}

h2 {
  margin-top: 10px;
  font-size: 28px;
  letter-spacing: 0.02em;
}

.login-head p {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.submit {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.demo-box {
  margin-top: 26px;
  padding: 16px;
  border-radius: 14px;
  background: #f2f6f2;
  border: 1px dashed #d5e1da;
}

.demo-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: #53655c;
}

.demo-title small {
  font-weight: 400;
  color: #93a29a;
  font-size: 11px;
}

.demo-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.demo {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #dfe8e2;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.demo:hover {
  border-color: #a9c6b9;
  transform: translateY(-1px);
}

.demo .el-icon {
  color: var(--brand);
  font-size: 16px;
}

.demo span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.demo strong {
  font-size: 12px;
  color: #33473e;
}

.demo small {
  font-size: 10px;
  color: #93a29a;
  margin-top: 2px;
}

.demo .go {
  margin-left: auto;
  color: #b3c1ba;
  font-size: 11px;
}

.demo-pass {
  margin-top: 10px;
  font-size: 10px;
  color: #8b9a92;
}

.demo-pass b {
  color: #5e756a;
  font-weight: 600;
}

.switch {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: #7b8a83;
}

.switch a {
  color: var(--brand);
  font-weight: 600;
}

.switch .dot {
  margin: 0 8px;
  color: #b5c1ba;
}

@media (max-width: 420px) {
  .demo-list {
    grid-template-columns: 1fr;
  }
}
</style>
