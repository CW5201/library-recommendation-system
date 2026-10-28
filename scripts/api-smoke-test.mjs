const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api'

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

async function request(method, path, options = {}) {
  const headers = { Accept: 'application/json' }
  if (options.token) {
    headers.satoken = options.token
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8'
  }
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  })
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`${method} ${path} returned non-JSON response: ${text.slice(0, 200)}`)
  }
  if (!response.ok || !payload || payload.code !== 200) {
    const reason = payload?.message || text || `HTTP ${response.status}`
    throw new Error(`${method} ${path} failed: ${reason}`)
  }
  return payload.data
}

async function expectFailure(method, path, options = {}) {
  try {
    await request(method, path, options)
    return false
  } catch {
    return true
  }
}

function ok(step) {
  console.log(`[PASS] ${step}`)
}

async function main() {
  const suffix = Date.now().toString().slice(-8)
  console.log(`API smoke test: ${BASE}`)

  const categories = await request('GET', '/categories')
  assert(Array.isArray(categories) && categories.length > 0, 'categories are empty')
  ok('读取图书分类')

  const books = await request('GET', '/books?page=1&size=5')
  assert(books.total > 0 && books.records.length > 0, 'book list is empty')
  ok('图书分页查询')

  const student = await request('POST', '/auth/login', {
    body: { username: 'student', password: 'Student@123' }
  })
  assert(student.token, 'student login did not return token')
  ok('普通用户登录')

  const admin = await request('POST', '/auth/login', {
    body: { username: 'admin', password: 'Admin@123' }
  })
  assert(admin.token && admin.user.role === 'ADMIN', 'admin login failed')
  ok('管理员登录')

  const smokeUsername = `smoke_${suffix}`
  const smokePassword = 'Smoke@123'
  const smoke = await request('POST', '/auth/register', {
    body: {
      username: smokeUsername,
      password: smokePassword,
      nickname: '冒烟测试用户',
      email: `${smokeUsername}@smoke.local`
    }
  })
  assert(smoke.token && smoke.user.username === smokeUsername, 'register failed')
  ok('用户注册')

  const crudBookBody = {
    title: `[SMOKE] CRUD 测试图书 ${suffix}`,
    author: '自动化测试',
    publisher: '测试出版社',
    isbn: `SMOKE${suffix}`,
    categoryId: categories[0].id,
    description: '用于验证图书新增、修改、查询和删除的临时数据。',
    cover: '/covers/book.svg',
    publishDate: '2025-01-01',
    totalStock: 2,
    availableStock: 2,
    status: 1
  }
  const crudBook = await request('POST', '/admin/books', {
    token: admin.token,
    body: crudBookBody
  })
  assert(crudBook.id, 'create book failed')
  ok('管理员新增图书')

  const detail = await request('GET', `/books/${crudBook.id}`)
  assert(detail.title === crudBookBody.title, 'book detail does not match')
  const search = await request('GET', `/books?page=1&size=10&keyword=${encodeURIComponent('[SMOKE]')}`)
  assert(search.records.some((item) => item.id === crudBook.id), 'keyword search did not find test book')
  ok('图书详情与关键词搜索')

  const updatedTitle = `${crudBookBody.title} 已更新`
  await request('PUT', `/admin/books/${crudBook.id}`, {
    token: admin.token,
    body: { ...crudBookBody, title: updatedTitle }
  })
  const updated = await request('GET', `/books/${crudBook.id}`)
  assert(updated.title === updatedTitle, 'update book failed')
  ok('管理员修改图书')

  await request('DELETE', `/admin/books/${crudBook.id}`, { token: admin.token })
  assert(await expectFailure('GET', `/books/${crudBook.id}`), 'deleted book still exists')
  ok('管理员删除图书')

  const behaviorBookBody = {
    title: `[SMOKE] 行为与推荐测试图书 ${suffix}`,
    author: '自动化测试',
    publisher: '测试出版社',
    isbn: `BEHAVIOR${suffix}`.slice(0, 20),
    categoryId: categories[0].id,
    description: '用于验证浏览、收藏、评分、借阅、归还与推荐接口的临时数据。',
    cover: '/covers/computer.svg',
    publishDate: '2025-02-01',
    totalStock: 3,
    availableStock: 3,
    status: 1
  }
  const behaviorBook = await request('POST', '/admin/books', {
    token: admin.token,
    body: behaviorBookBody
  })

  await request('POST', `/behaviors/view/${behaviorBook.id}`, { token: smoke.token })
  const favorite = await request('POST', `/behaviors/favorite/${behaviorBook.id}`, { token: smoke.token })
  assert(favorite.favorited === true, 'favorite failed')
  await request('POST', `/behaviors/rate/${behaviorBook.id}`, {
    token: smoke.token,
    body: { score: 5 }
  })
  const borrow = await request('POST', `/behaviors/borrow/${behaviorBook.id}`, { token: smoke.token })
  assert(borrow.id && borrow.status === 'BORROWED', 'borrow failed')
  ok('浏览、收藏、评分、借阅行为写入')

  const borrows = await request('GET', '/me/borrows', { token: smoke.token })
  assert(borrows.some((item) => item.id === borrow.id), 'user borrow record missing')
  await request('POST', `/behaviors/return/${borrow.id}`, { token: smoke.token })
  const returned = await request('GET', '/me/borrows?status=RETURNED', { token: smoke.token })
  assert(returned.some((item) => item.id === borrow.id), 'return book failed')
  ok('个人借阅记录与归还')

  const favorites = await request('GET', '/me/favorites', { token: smoke.token })
  assert(favorites.some((item) => item.book.id === behaviorBook.id), 'favorite list missing test book')
  const history = await request('GET', '/me/history?limit=20', { token: smoke.token })
  assert(history.some((item) => item.book.id === behaviorBook.id), 'browse history missing test book')
  ok('收藏列表与浏览历史')

  const personal = await request('GET', '/recommendations/personal?limit=5', { token: smoke.token })
  assert(Array.isArray(personal) && personal.length > 0, 'personal recommendation is empty')
  assert(personal.every((item) => item.reason), 'recommendation reason missing')
  ok('个性化推荐与推荐理由')

  const hot = await request('GET', '/recommendations/hot?limit=5')
  const newBooks = await request('GET', '/recommendations/new?limit=5')
  assert(hot.length > 0 && newBooks.length > 0, 'hot or new books are empty')
  ok('热门推荐与新书推荐')

  const aiStatus = await request('GET', '/ai/status')
  assert(typeof aiStatus.llmConfigured === 'boolean', 'AI status is invalid')
  const semantic = await request('POST', '/ai/semantic-search', {
    token: smoke.token,
    body: { query: '想学习 Java 后端开发和数据库', limit: 5 }
  })
  assert(Array.isArray(semantic), 'semantic search failed')
  const chat = await request('POST', '/ai/chat', {
    token: smoke.token,
    body: { message: '我想学习 Java 后端开发，有什么书推荐？', sessionId: `smoke-${suffix}` }
  })
  assert(chat.answer && Array.isArray(chat.relatedBooks), 'AI chat failed')
  const plan = await request('POST', '/ai/reading-plan', {
    token: smoke.token,
    body: { goal: '从零开始学习 Java 后端开发', count: 5 }
  })
  assert(plan.content && Array.isArray(plan.books), 'AI reading plan failed')
  const reason = await request('POST', '/ai/recommend-reason', {
    token: smoke.token,
    body: { bookId: behaviorBook.id }
  })
  assert(reason.reason, 'AI recommendation reason failed')
  ok(`AI 状态、语义搜索、RAG 对话、阅读清单、推荐理由（LLM 已配置=${aiStatus.llmConfigured}）`)

  const adminUsers = await request(
    'GET',
    `/admin/users?page=1&size=10&keyword=${encodeURIComponent(smokeUsername)}`,
    { token: admin.token }
  )
  assert(adminUsers.total >= 1, 'admin user search failed')
  const adminBooks = await request('GET', '/admin/books?page=1&size=10', { token: admin.token })
  assert(adminBooks.total > 0, 'admin book list failed')
  const adminBorrows = await request('GET', '/admin/borrows?page=1&size=10', { token: admin.token })
  assert(adminBorrows.total > 0, 'admin borrow list failed')
  const overview = await request('GET', '/admin/stats/overview', { token: admin.token })
  assert(overview.bookCount > 0 && overview.userCount > 0, 'admin overview failed')
  const stats = await Promise.all([
    request('GET', '/admin/stats/categories', { token: admin.token }),
    request('GET', '/admin/stats/borrow-trend?days=14', { token: admin.token }),
    request('GET', '/admin/stats/behaviors', { token: admin.token }),
    request('GET', '/admin/stats/hot-books?limit=10', { token: admin.token }),
    request('GET', '/admin/stats/roles', { token: admin.token }),
    request('GET', '/admin/stats/borrow-status', { token: admin.token })
  ])
  assert(stats.every((item) => Array.isArray(item)), 'admin statistics failed')
  ok('管理端用户、图书、借阅与统计接口')

  const forbidden = await expectFailure('GET', '/admin/users?page=1&size=5', { token: student.token })
  assert(forbidden, 'ordinary user can access admin API')
  ok('普通用户访问管理端接口被拒绝')

  await request('POST', `/behaviors/favorite/${behaviorBook.id}`, { token: smoke.token }).catch(() => null)
  await request('PUT', `/admin/books/${behaviorBook.id}`, {
    token: admin.token,
    body: { ...behaviorBookBody, status: 0 }
  }).catch(() => null)
  await request('PUT', `/admin/users/${smoke.user.id}/status`, {
    token: admin.token,
    body: { status: 0 }
  }).catch(() => null)

  console.log('[PASS] 冒烟测试全部通过')
  console.log(`测试账号 ${smokeUsername} 已禁用，测试图书 ${behaviorBook.id} 已下架。重新执行 sql/init.sql 可恢复纯演示数据。`)
}

main().catch((error) => {
  console.error('[FAIL]', error.message)
  process.exitCode = 1
})
