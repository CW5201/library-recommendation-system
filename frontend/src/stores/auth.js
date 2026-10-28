import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_KEY } from '@/api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    displayName: (state) => state.user?.nickname || state.user?.username || '读者',
    avatarText: (state) => (state.user?.nickname || state.user?.username || '读').slice(0, 1)
  },
  actions: {
    setSession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
    },
    async login(payload) {
      this.loading = true
      try {
        const data = await authApi.login(payload)
        this.setSession(data)
        return data
      } finally {
        this.loading = false
      }
    },
    async register(payload) {
      this.loading = true
      try {
        const data = await authApi.register(payload)
        this.setSession(data)
        return data
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      if (!this.token) return null
      this.user = await authApi.me()
      return this.user
    },
    updateUser(user) {
      this.user = { ...this.user, ...user }
    },
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
    async logout() {
      try {
        if (this.token) await authApi.logout()
      } catch {
        // ignore network errors on logout
      } finally {
        this.clearAuth()
      }
    }
  }
})
