<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { coverUrl } from '@/utils/format'

const props = defineProps({
  book: { type: Object, required: true },
  similarity: { type: Boolean, default: false }
})

const router = useRouter()
const cover = computed(() => coverUrl(props.book.cover))

function open() {
  router.push(`/books/${props.book.id}`)
}
</script>

<template>
  <div class="mini-book" @click="open">
    <img :src="cover" :alt="book.title" />
    <div class="txt">
      <strong>{{ book.title }}</strong>
      <span>{{ book.author }}</span>
      <span class="sub">
        {{ book.categoryName }}
        <template v-if="similarity && book.similarity">
          · 相关度 {{ Math.round(book.similarity * 100) }}%
        </template>
      </span>
    </div>
    <el-icon class="arrow"><ArrowRight /></el-icon>
  </div>
</template>

<style scoped>
.mini-book {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.mini-book:hover {
  border-color: #b9d0c6;
  transform: translateX(2px);
}

img {
  width: 46px;
  height: 62px;
  border-radius: 6px;
  object-fit: cover;
  flex: none;
  background: #e8efeb;
}

.txt {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

strong {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

span {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub {
  color: #90a098;
}

.arrow {
  color: #a7b6af;
  font-size: 13px;
}
</style>
