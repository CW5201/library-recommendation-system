<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, bookApi, categoryApi } from '@/api'
import { coverUrl, formatDate } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const categories = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref()

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: null,
  status: null,
  sort: 'default'
})

const form = reactive({
  title: '',
  author: '',
  publisher: '',
  isbn: '',
  categoryId: null,
  publishDate: '',
  totalStock: 1,
  availableStock: 1,
  cover: '/covers/book.svg',
  description: '',
  status: 1
})

const dialogTitle = computed(() => (editingId.value ? '编辑图书' : '新增图书'))

const validateStock = (rule, value, callback) => {
  if (value === null || value === undefined || value < 0) return callback(new Error('请输入可借数量'))
  if (Number(value) > Number(form.totalStock)) return callback(new Error('可借数量不能大于馆藏总量'))
  callback()
}

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [{ required: true, message: '请输入 ISBN', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  totalStock: [{ required: true, message: '请输入馆藏总量', trigger: 'blur' }],
  availableStock: [{ validator: validateStock, trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.books({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      status: query.status === '' ? undefined : query.status,
      sort: query.sort
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
  query.categoryId = null
  query.status = null
  query.sort = 'default'
  query.page = 1
  load()
}

function resetForm() {
  Object.assign(form, {
    title: '',
    author: '',
    publisher: '',
    isbn: '',
    categoryId: null,
    publishDate: '',
    totalStock: 1,
    availableStock: 1,
    cover: '/covers/book.svg',
    description: '',
    status: 1
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title || '',
    author: row.author || '',
    publisher: row.publisher || '',
    isbn: row.isbn || '',
    categoryId: row.categoryId || null,
    publishDate: row.publishDate || '',
    totalStock: row.totalStock ?? 1,
    availableStock: row.availableStock ?? 1,
    cover: row.cover || '/covers/book.svg',
    description: row.description || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      title: form.title,
      author: form.author,
      publisher: form.publisher || null,
      isbn: form.isbn,
      categoryId: form.categoryId,
      publishDate: form.publishDate || null,
      totalStock: Number(form.totalStock),
      availableStock: Number(form.availableStock),
      cover: form.cover || '/covers/book.svg',
      description: form.description || null,
      status: Number(form.status)
    }
    if (editingId.value) {
      await adminApi.updateBook(editingId.value, payload)
      ElMessage.success('图书已更新')
    } else {
      await adminApi.createBook(payload)
      ElMessage.success('图书已新增')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row, enabled) {
  const status = enabled ? 1 : 0
  await adminApi.updateBook(row.id, {
    title: row.title,
    author: row.author,
    publisher: row.publisher || null,
    isbn: row.isbn,
    categoryId: row.categoryId,
    publishDate: row.publishDate || null,
    totalStock: row.totalStock,
    availableStock: row.availableStock,
    cover: row.cover,
    description: row.description,
    status
  })
  ElMessage.success(enabled ? '图书已上架' : '图书已下架')
  await load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除《${row.title}》吗？已有借阅记录的图书无法删除，可改为下架。`,
      '删除图书',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }
  await adminApi.deleteBook(row.id)
  ElMessage.success('图书已删除')
  if (rows.value.length === 1 && query.page > 1) query.page -= 1
  await load()
}

onMounted(async () => {
  categories.value = await categoryApi.list().catch(() => [])
  await load()
})
</script>

<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <span class="eyebrow">BOOK MANAGEMENT</span>
        <h1>图书管理</h1>
        <p>维护馆藏信息、库存数量与上下架状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>新增图书
      </el-button>
    </section>

    <section class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="书名 / 作者 / ISBN / 出版社"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 140px" @change="search">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="search">
          <el-option label="在架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-select v-model="query.sort" style="width: 130px" @change="search">
          <el-option label="最新上架" value="default" />
          <el-option label="借阅热度" value="hot" />
          <el-option label="评分最高" value="rating" />
          <el-option label="出版时间" value="new" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column label="图书" min-width="240">
          <template #default="{ row }">
            <div class="book-cell">
              <img :src="coverUrl(row.cover)" :alt="row.title" />
              <div>
                <strong>{{ row.title }}</strong>
                <span>{{ row.author }}</span>
                <small>{{ row.isbn }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <span class="chip plain">{{ row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <div class="stock-cell">
              <strong :class="{ out: row.availableStock === 0 }">{{ row.availableStock }}</strong>
              <span>/ {{ row.totalStock }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="90">
          <template #default="{ row }">
            <span class="rating">{{ Number(row.ratingAvg || 0).toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="借阅" width="80">
          <template #default="{ row }">{{ row.borrowCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="出版日期" width="110">
          <template #default="{ row }">{{ formatDate(row.publishDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="在架"
              inactive-text="下架"
              @change="(value) => toggleStatus(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" align-center destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="书名" prop="title" class="span-2">
            <el-input v-model="form.title" maxlength="200" />
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="form.author" maxlength="120" />
          </el-form-item>
          <el-form-item label="ISBN" prop="isbn">
            <el-input v-model="form.isbn" maxlength="20" />
          </el-form-item>
          <el-form-item label="出版社">
            <el-input v-model="form.publisher" maxlength="120" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="出版日期">
            <el-date-picker
              v-model="form.publishDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">在架</el-radio-button>
              <el-radio-button :value="0">下架</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="馆藏总量" prop="totalStock">
            <el-input-number v-model="form.totalStock" :min="0" :max="99999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="可借数量" prop="availableStock">
            <el-input-number v-model="form.availableStock" :min="0" :max="99999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="封面地址" class="span-2">
            <el-input v-model="form.cover" maxlength="255" placeholder="/covers/computer.svg" />
          </el-form-item>
          <el-form-item label="图书简介" class="span-2">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" show-word-limit />
          </el-form-item>
        </div>
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

.book-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.book-cell img {
  width: 42px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #e8efeb;
  flex: none;
}

.book-cell div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.book-cell strong {
  font-size: 12px;
  color: #3a5145;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.book-cell span {
  font-size: 10px;
  color: #8a998f;
}

.book-cell small {
  font-size: 9px;
  color: #aab6af;
}

.stock-cell {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.stock-cell strong {
  font-size: 14px;
  color: #2e6b54;
}

.stock-cell strong.out {
  color: #c05e42;
}

.stock-cell span {
  font-size: 10px;
  color: #9aa8a0;
}

.rating {
  font-size: 12px;
  color: #a3742d;
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.span-2 {
  grid-column: 1 / -1;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: auto;
  }
}
</style>
