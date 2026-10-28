<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入密码'))
  if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await auth.register({
      username: form.username,
      nickname: form.nickname,
      email: form.email || null,
      password: form.password
    })
    ElMessage.success('注册成功，已自动登录')
    router.replace('/home')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout
    badge="加入知书"
    title="从一本好书开始新的旅程"
    subtitle="创建读者账号，记录阅读轨迹，让推荐系统越来越懂你"
  >
    <div class="register-head">
      <span class="eyebrow">CREATE ACCOUNT</span>
      <h2>注册读者账号</h2>
      <p>仅需几秒钟，开启属于你的智慧阅读空间。</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
      <div class="two-col">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母 / 数字 / 下划线" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示名称" clearable>
            <template #prefix><el-icon><Avatar /></el-icon></template>
          </el-input>
        </el-form-item>
      </div>

      <el-form-item label="邮箱（选填）" prop="email">
        <el-input v-model="form.email" placeholder="用于接收借阅提醒" clearable>
          <template #prefix><el-icon><Message /></el-icon></template>
        </el-input>
      </el-form-item>

      <div class="two-col">
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-32 位" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </div>

      <el-button class="submit" type="primary" size="large" :loading="submitting" @click="submit">
        创建账号
      </el-button>
    </el-form>

    <p class="switch">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </p>
  </AuthLayout>
</template>

<style scoped>
.register-head {
  margin-bottom: 24px;
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
}

.register-head p {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.switch {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #7b8a83;
}

.switch a {
  color: var(--brand);
  font-weight: 600;
}

@media (max-width: 520px) {
  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
