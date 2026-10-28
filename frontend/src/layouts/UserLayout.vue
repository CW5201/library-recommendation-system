<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navs = [
  { path: '/home', label: '首页' },
  { path: '/books', label: '馆藏图书' },
  { path: '/recommendations', label: '为你推荐' },
  { path: '/ai', label: 'AI 图书助手' }
]

const activePath = computed(() => {
  if (route.path.startsWith('/books')) return '/books'
  return route.path
})

async function logout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/home')
}
</script>

<template>
  <div class="user-shell">
    <header class="topbar">
      <div class="page-shell inner">
        <router-link class="brand" to="/home">
          <span class="logo">
            <el-icon><Reading /></el-icon>
          </span>
          <span class="brand-text">
            <strong>知书</strong>
            <small>LIBRARY</small>
          </span>
        </router-link>

        <nav class="nav">
          <router-link
            v-for="item in navs"
            :key="item.path"
            :to="item.path"
            :class="{ active: activePath === item.path }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions">
          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click">
              <div class="user-chip">
                <el-avatar :size="34" :src="auth.user?.avatar || undefined">
                  {{ auth.avatarText }}
                </el-avatar>
                <span class="name">{{ auth.displayName }}</span>
                <el-icon class="down"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" @click="router.push('/admin/dashboard')">
                    <el-icon><Setting /></el-icon>管理后台
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      <div class="page-shell footer-inner">
        <div>
          <strong>知书 · 高校图书馆图书推荐系统</strong>
          <p>融合个性化推荐、语义检索与大模型问答的智慧图书馆服务平台</p>
        </div>
        <div class="tech">
          <span>Spring Boot</span>
          <span>Vue 3</span>
          <span>MySQL</span>
          <span>RAG</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.user-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(244, 246, 242, 0.88);
  border-bottom: 1px solid rgba(226, 233, 228, 0.9);
  backdrop-filter: blur(14px);
}

.inner {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 36px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, var(--brand), var(--brand-dark));
  color: #f7f3e8;
  font-size: 21px;
  box-shadow: 0 8px 18px rgba(31, 92, 74, 0.22);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-text strong {
  font-size: 20px;
  letter-spacing: 0.12em;
}

.brand-text small {
  margin-top: 4px;
  font-size: 8px;
  letter-spacing: 0.28em;
  color: #8a9a92;
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.nav a {
  padding: 8px 14px;
  border-radius: 999px;
  color: #51635b;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease;
}

.nav a:hover {
  color: var(--brand);
  background: #e9f1ed;
}

.nav a.active {
  color: var(--brand);
  background: #dfece6;
  font-weight: 700;
}

.actions {
  margin-left: auto;
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--line);
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.user-chip:hover {
  border-color: #bcd0c7;
  box-shadow: var(--shadow-sm);
}

.name {
  font-size: 13px;
  font-weight: 600;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.down {
  font-size: 12px;
  color: #8a9a92;
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--line);
  background: #eef2ee;
  padding: 34px 0;
  color: #62746b;
}

.footer-inner {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.footer strong {
  color: #33473e;
  font-size: 15px;
}

.footer p {
  margin-top: 8px;
  font-size: 12px;
}

.tech {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tech span {
  padding: 4px 10px;
  border: 1px solid #d6e0da;
  border-radius: 999px;
  font-size: 11px;
  background: #f7faf8;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 860px) {
  .inner {
    height: auto;
    min-height: 64px;
    flex-wrap: wrap;
    gap: 10px 16px;
    padding: 10px 0;
  }
  .brand-text small {
    display: none;
  }
  .nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
    padding-bottom: 2px;
  }
  .nav::-webkit-scrollbar {
    display: none;
  }
  .nav a {
    padding: 7px 11px;
    font-size: 13px;
  }
  .name {
    display: none;
  }
  .user-chip {
    padding: 3px;
  }
}
</style>
