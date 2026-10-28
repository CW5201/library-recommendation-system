import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', guestOnly: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'books',
        name: 'books',
        component: () => import('@/views/user/BookList.vue'),
        meta: { title: '馆藏图书' }
      },
      {
        path: 'books/:id',
        name: 'book-detail',
        component: () => import('@/views/user/BookDetail.vue'),
        meta: { title: '图书详情' }
      },
      {
        path: 'recommendations',
        name: 'recommendations',
        component: () => import('@/views/user/Recommendations.vue'),
        meta: { title: '个性化推荐', requiresAuth: true }
      },
      {
        path: 'ai',
        name: 'ai',
        component: () => import('@/views/user/AiAssistant.vue'),
        meta: { title: 'AI 图书助手', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据概览', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'books',
        name: 'admin-books',
        component: () => import('@/views/admin/Books.vue'),
        meta: { title: '图书管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/Categories.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'borrows',
        name: 'admin-borrows',
        component: () => import('@/views/admin/Borrows.vue'),
        meta: { title: '借阅管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'stats',
        name: 'admin-stats',
        component: () => import('@/views/admin/Stats.vue'),
        meta: { title: '统计分析', requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 知书图书馆` : '知书 · 高校图书馆图书推荐系统'
  const auth = useAuthStore()
  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clearAuth()
    }
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return auth.isLoggedIn ? { path: '/home' } : { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return auth.isAdmin ? { path: '/admin/dashboard' } : { path: '/home' }
  }
  return true
})

export default router
