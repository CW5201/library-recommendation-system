import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  updateProfile: (data) => http.put('/auth/profile', data),
  changePassword: (data) => http.put('/auth/password', data)
}

export const categoryApi = {
  list: () => http.get('/categories')
}

export const bookApi = {
  page: (params) => http.get('/books', { params }),
  detail: (id) => http.get(`/books/${id}`)
}

export const recommendApi = {
  hot: (limit = 8) => http.get('/recommendations/hot', { params: { limit } }),
  newBooks: (limit = 8) => http.get('/recommendations/new', { params: { limit } }),
  personal: (limit = 12) => http.get('/recommendations/personal', { params: { limit } }),
  similar: (bookId, limit = 6) => http.get(`/recommendations/similar/${bookId}`, { params: { limit } })
}

export const behaviorApi = {
  view: (bookId) => http.post(`/behaviors/view/${bookId}`),
  favorite: (bookId) => http.post(`/behaviors/favorite/${bookId}`),
  rate: (bookId, score) => http.post(`/behaviors/rate/${bookId}`, { score }),
  borrow: (bookId) => http.post(`/behaviors/borrow/${bookId}`),
  returnBook: (recordId) => http.post(`/behaviors/return/${recordId}`)
}

export const meApi = {
  favorites: () => http.get('/me/favorites'),
  history: (limit = 50) => http.get('/me/history', { params: { limit } }),
  borrows: (status) => http.get('/me/borrows', { params: { status } })
}

export const aiApi = {
  status: () => http.get('/ai/status'),
  chat: (data) => http.post('/ai/chat', data),
  semanticSearch: (data) => http.post('/ai/semantic-search', data),
  readingPlan: (data) => http.post('/ai/reading-plan', data),
  recommendReason: (data) => http.post('/ai/recommend-reason', data),
  history: (sessionId) => http.get('/ai/history', { params: { sessionId } })
}

export const adminApi = {
  users: (params) => http.get('/admin/users', { params }),
  updateUserStatus: (id, status) => http.put(`/admin/users/${id}/status`, { status }),
  updateUserRole: (id, role) => http.put(`/admin/users/${id}/role`, { role }),
  deleteUser: (id) => http.delete(`/admin/users/${id}`),

  books: (params) => http.get('/admin/books', { params }),
  createBook: (data) => http.post('/admin/books', data),
  updateBook: (id, data) => http.put(`/admin/books/${id}`, data),
  deleteBook: (id) => http.delete(`/admin/books/${id}`),

  createCategory: (data) => http.post('/admin/categories', data),
  updateCategory: (id, data) => http.put(`/admin/categories/${id}`, data),
  deleteCategory: (id) => http.delete(`/admin/categories/${id}`),

  borrows: (params) => http.get('/admin/borrows', { params }),
  returnBorrow: (id) => http.put(`/admin/borrows/${id}/return`),

  overview: () => http.get('/admin/stats/overview'),
  categoryStats: () => http.get('/admin/stats/categories'),
  borrowTrend: (days = 14) => http.get('/admin/stats/borrow-trend', { params: { days } }),
  behaviorStats: () => http.get('/admin/stats/behaviors'),
  hotBooks: (limit = 10) => http.get('/admin/stats/hot-books', { params: { limit } }),
  roleStats: () => http.get('/admin/stats/roles'),
  borrowStatus: () => http.get('/admin/stats/borrow-status'),
  rebuildIndex: () => http.post('/admin/ai/rebuild-index')
}
