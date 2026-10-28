export function formatDate(value) {
  if (!value) return '—'
  const text = String(value)
  return text.includes('T') ? text.slice(0, 10) : text
}

export function formatDateTime(value) {
  if (!value) return '—'
  const text = String(value).replace('T', ' ')
  return text.slice(0, 16)
}

export function coverUrl(path) {
  return path || '/covers/book.svg'
}

export function borrowStatusText(status) {
  return {
    BORROWED: '借阅中',
    RETURNED: '已归还',
    OVERDUE: '已逾期'
  }[status] || status || '—'
}

export function borrowStatusType(status) {
  return {
    BORROWED: 'primary',
    RETURNED: 'info',
    OVERDUE: 'danger'
  }[status] || 'info'
}

export function roleText(role) {
  return role === 'ADMIN' ? '管理员' : '读者'
}

export function algorithmText(algorithm) {
  return {
    HOT: '热门推荐',
    CONTENT: '内容推荐',
    ITEM_CF: '协同过滤',
    HYBRID: '混合推荐',
    SIMILAR: '相似图书',
    COLD_START: '新用户推荐'
  }[algorithm] || '智能推荐'
}

export function behaviorText(type) {
  return {
    VIEW: '浏览',
    FAVORITE: '收藏',
    UNFAVORITE: '取消收藏',
    RATE: '评分',
    BORROW: '借阅',
    RETURN: '归还'
  }[type] || type
}

export function percent(value) {
  if (value === null || value === undefined) return 0
  return Math.max(0, Math.min(100, Math.round(Number(value) * 100)))
}

export function truncate(text, length = 80) {
  if (!text) return ''
  return text.length > length ? `${text.slice(0, length)}…` : text
}
