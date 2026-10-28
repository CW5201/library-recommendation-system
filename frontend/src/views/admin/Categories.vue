<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, categoryApi } from '@/api'
import { formatDateTime } from '@/utils/format'

const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  name: '',
  description: '',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    categories.value = await categoryApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', description: '', sortOrder: categories.value.length })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    sortOrder: row.sortOrder ?? 0
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      description: form.description || null,
      sortOrder: Number(form.sortOrder) || 0
    }
    if (editingId.value) {
      await adminApi.updateCategory(editingId.value, payload)
      ElMessage.success('分类已更新')
    } else {
      await adminApi.createCategory(payload)
      ElMessage.success('分类已新增')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」吗？分类下仍有图书时无法删除。`,
      '删除分类',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }
  await adminApi.deleteCategory(row.id)
  ElMessage.success('分类已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <span class="eyebrow">CATEGORY MANAGEMENT</span>
        <h1>分类管理</h1>
        <p>维护馆藏学科分类与展示顺序。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增分类
      </el-button>
    </section>

    <section class="category-overview">
      <div class="overview-copy">
        <span class="chip"><el-icon><Files /></el-icon>分类体系</span>
        <strong>{{ categories.length }}</strong>
        <small>个学科分类，排序值越小越靠前</small>
      </div>
      <div class="overview-tip">
        <el-icon><InfoFilled /></el-icon>
        分类是内容推荐的重要特征。合理的分类结构能提升推荐结果的解释性与准确度。
      </div>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="categories" stripe style="width: 100%">
        <el-table-column label="排序" width="90">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类名称" width="180">
          <template #default="{ row }">
            <strong class="cat-name">{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="分类说明" min-width="300">
          <template #default="{ row }">
            <span class="desc">{{ row.description || '暂无说明' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="460px" align-center>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="例如：计算机技术" />
        </el-form-item>
        <el-form-item label="分类说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="简要描述该分类涵盖的图书主题"
          />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 160px" />
          <span class="form-hint">数值越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
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

.category-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 24px;
  border-radius: 16px;
  background: linear-gradient(120deg, #1d5545, #123a2e);
  color: #e8f2ed;
  flex-wrap: wrap;
}

.overview-copy {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.overview-copy .chip {
  background: rgba(255, 255, 255, 0.12);
  color: #d8e7df;
  margin-right: 4px;
}

.overview-copy strong {
  font-size: 32px;
  color: #f0e2c8;
}

.overview-copy small {
  font-size: 11px;
  color: #9eb9ac;
}

.overview-tip {
  display: flex;
  align-items: center;
  gap: 7px;
  max-width: 470px;
  font-size: 11px;
  line-height: 1.7;
  color: #a9c3b7;
}

.overview-tip .el-icon {
  flex: none;
  color: #d7b87f;
}

.table-panel {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--line);
  overflow: hidden;
}

.sort-badge {
  display: inline-grid;
  place-items: center;
  min-width: 26px;
  height: 24px;
  padding: 0 7px;
  border-radius: 7px;
  background: #f1f5f2;
  color: #6e8277;
  font-size: 11px;
  font-weight: 600;
}

.cat-name {
  font-size: 12px;
  color: #3a5145;
}

.desc {
  font-size: 11px;
  color: #7c8e84;
  line-height: 1.6;
}

.form-hint {
  margin-left: 10px;
  font-size: 10px;
  color: #9aa8a0;
}
</style>
