<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const menus = [
  { path: '/admin/dashboard', label: '数据概览', icon: 'DataLine' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/books', label: '图书管理', icon: 'Reading' },
  { path: '/admin/categories', label: '分类管理', icon: 'Files' },
  { path: '/admin/borrows', label: '借阅管理', icon: 'Tickets' },
  { path: '/admin/stats', label: '统计分析', icon: 'PieChart' }
]

const title = computed(() => route.meta.title || '管理后台')

async function logout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <span class="logo"><el-icon><Reading /></el-icon></span>
        <span v-if="!collapsed" class="brand-text">
          <strong>知书后台</strong>
          <small>ADMIN CONSOLE</small>
        </span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="menu"
        background-color="transparent"
        text-color="#b9cec5"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>

      <div v-if="!collapsed" class="side-foot">
        <router-link to="/home"><el-icon><Back /></el-icon>返回前台</router-link>
        <p>Library Recommendation v1.0</p>
      </div>
    </aside>

    <section class="admin-main">
      <header class="admin-top">
        <div class="left">
          <el-button text circle @click="collapsed = !collapsed">
            <el-icon :size="18"><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <div class="crumb">
            <span>管理后台</span>
            <el-icon><ArrowRight /></el-icon>
            <strong>{{ title }}</strong>
          </div>
        </div>
        <div class="right">
          <el-tooltip content="返回前台" placement="bottom">
            <el-button text circle @click="router.push('/home')">
              <el-icon :size="18"><House /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown trigger="click">
            <div class="admin-user">
              <el-avatar :size="32" :src="auth.user?.avatar || undefined">{{ auth.avatarText }}</el-avatar>
              <span>{{ auth.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #f3f6f4;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 30;
  width: 238px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #153f32 0%, #0f2f26 100%);
  color: #dbe8e2;
  transition: width 0.22s ease;
  overflow: hidden;
}

.collapsed .sidebar {
  width: 74px;
}

.brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex: none;
}

.logo {
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.12);
  color: #f2e6cc;
  font-size: 20px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
}

.brand-text strong {
  font-size: 17px;
  letter-spacing: 0.08em;
}

.brand-text small {
  font-size: 7px;
  letter-spacing: 0.18em;
  color: #7d9a8e;
  margin-top: 4px;
}

.menu {
  border-right: none;
  padding: 16px 10px;
  flex: 1;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  border-radius: 11px;
  margin-bottom: 4px;
  font-size: 14px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(100deg, #2e7a63, #236351) !important;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.18);
}

.side-foot {
  padding: 16px 20px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex: none;
}

.side-foot a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #b9cec5;
}

.side-foot a:hover {
  color: #fff;
}

.side-foot p {
  margin-top: 12px;
  font-size: 10px;
  color: #6d8a7e;
}

.admin-main {
  flex: 1;
  min-width: 0;
  margin-left: 238px;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.22s ease;
}

.collapsed .admin-main {
  margin-left: 74px;
}

.admin-top {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 64px;
  padding: 0 26px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8a998f;
}

.crumb strong {
  color: var(--ink);
  font-size: 14px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  outline: none;
  border: 1px solid transparent;
}

.admin-user:hover {
  background: #f2f6f3;
  border-color: var(--line);
}

.admin-user span {
  font-size: 13px;
  font-weight: 600;
}

.admin-content {
  padding: 26px;
  min-width: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .sidebar {
    width: 74px;
  }
  .sidebar .brand-text,
  .side-foot {
    display: none;
  }
  .admin-main {
    margin-left: 74px;
  }
  .admin-top {
    padding: 0 14px;
  }
  .admin-content {
    padding: 16px;
  }
}
</style>
