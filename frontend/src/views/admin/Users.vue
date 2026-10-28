<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, roleText } from '@/utils/format'

const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  role: '',
  status: null
})

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      status: query.status === '' ? undefined : query.status
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.role = ''
  query.status = null
  query.page = 1
  load()
}

async function changeStatus(row, enabled) {
  const target = enabled ? 1 : 0
  try {
    await ElMessageBox.confirm(
      `确认${enabled ? '启用' : '禁用'}用户「${row.nickname || row.username}」吗？`,
      '状态变更',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await adminApi.updateUserStatus(row.id, target)
    ElMessage.success(`已${enabled ? '启用' : '禁用'}该用户`)
    await load()
  } catch {
    await load()
  }
}

async function changeRole(row, role) {
  if (role === row.role) return
  try {
    await ElMessageBox.confirm(
      `确认将「${row.nickname || row.username}」的角色调整为${roleText(role)}吗？`,
      '角色变更',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await adminApi.updateUserRole(row.id, role)
  ElMessage.success('用户角色已更新')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除用户「${row.nickname || row.username}」后不可恢复，确认继续吗？`,
      '删除用户',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }
  await adminApi.deleteUser(row.id)
  ElMessage.success('用户已删除')
  if (rows.value.length === 1 && query.page > 1) query.page -= 1
  await load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <span class="eyebrow">USER MANAGEMENT</span>
        <h1>用户管理</h1>
        <p>维护读者账号、角色权限与启用状态。</p>
      </div>
      <div class="summary">
        <span>共 <strong>{{ total }}</strong> 个账号</span>
      </div>
    </section>

    <section class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 130px" @change="search">
          <el-option label="读者" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="正常" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatar || undefined">{{ (row.nickname || row.username || '读').slice(0, 1) }}</el-avatar>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <span>@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="190">
          <template #default="{ row }">
            <div class="contact">
              <span>{{ row.email || '未填写邮箱' }}</span>
              <small>{{ row.phone || '未填写手机号' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-select
              :model-value="row.role"
              size="small"
              :disabled="row.id === auth.user?.id"
              @change="(value) => changeRole(row, value)"
            >
              <el-option label="读者" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.id === auth.user?.id"
              inline-prompt
              active-text="正常"
              inactive-text="禁用"
              @change="(value) => changeStatus(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              :disabled="row.id === auth.user?.id"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.eyebrow {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--accent);
  font-weight: 700;
}

.page-head h1 {
  margin-top: 6px;
  font-size: 25px;
  color: #1c3d31;
}

.page-head p {
  margin-top: 6px;
  font-size: 12px;
  color: #8a998f;
}

.summary {
  font-size: 11px;
  color: #87968e;
  padding: 7px 12px;
  border-radius: 9px;
  background: #fff;
  border: 1px solid var(--line);
}

.summary strong {
  color: var(--brand);
  font-size: 14px;
}

.toolbar {
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--line);
}

.filters {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  align-items: center;
}

.table-panel {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  overflow: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-cell strong {
  font-size: 12px;
  color: #3d5549;
}

.user-cell span {
  font-size: 10px;
  color: #95a39b;
}

.contact {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11px;
  color: #5f7368;
}

.contact small {
  font-size: 10px;
  color: #9aa8a0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
