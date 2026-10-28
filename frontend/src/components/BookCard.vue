<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { coverUrl, algorithmText } from '@/utils/format'

const props = defineProps({
  book: { type: Object, required: true },
  rank: { type: Number, default: 0 },
  showReason: { type: Boolean, default: false },
  showAlgorithm: { type: Boolean, default: false },
  showScore: { type: Boolean, default: false },
  showSimilarity: { type: Boolean, default: false }
})

const router = useRouter()
const cover = computed(() => coverUrl(props.book.cover))
const available = computed(() => Number(props.book.availableStock) > 0)
const rating = computed(() => Number(props.book.ratingAvg || 0))

function openDetail() {
  router.push(`/books/${props.book.id}`)
}
</script>

<template>
  <article class="book-card" @click="openDetail">
    <div class="cover-frame">
      <img :src="cover" :alt="book.title" loading="lazy" />
      <span v-if="rank" class="rank" :class="{ top: rank <= 3 }">{{ rank }}</span>
      <span v-if="!available" class="stock-badge">暂无库存</span>
      <span v-else-if="book.borrowed" class="stock-badge borrowed">已借阅</span>
    </div>
    <div class="info">
      <div class="cat-line">
        <span class="chip plain">{{ book.categoryName || '馆藏图书' }}</span>
        <span v-if="showAlgorithm && book.algorithm" class="chip accent">{{ algorithmText(book.algorithm) }}</span>
        <span v-if="showScore && book.recommendationScore" class="chip plain">
          匹配 {{ Math.min(99, Math.round(book.recommendationScore * 100)) }}%
        </span>
        <span v-if="showSimilarity && book.similarity" class="chip plain">
          相关度 {{ Math.round(book.similarity * 100) }}%
        </span>
      </div>
      <h3 :title="book.title">{{ book.title }}</h3>
      <p class="author">{{ book.author }}</p>
      <div class="meta">
        <span class="rating">
          <el-icon><StarFilled /></el-icon>
          {{ rating ? rating.toFixed(1) : '暂无' }}
        </span>
        <span>{{ book.borrowCount || 0 }} 次借阅</span>
      </div>
      <p v-if="showReason && book.reason" class="reason">{{ book.reason }}</p>
    </div>
  </article>
</template>

<style scoped>
.book-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px;
  border-radius: var(--radius);
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  transition: transform 0.25s ease, background 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.book-card:hover {
  transform: translateY(-5px);
  background: #fff;
  border-color: var(--line);
  box-shadow: var(--shadow);
}

.cover-frame {
  box-shadow: 0 10px 24px rgba(21, 63, 50, 0.12);
}

.cover-frame img {
  transition: transform 0.4s ease;
}

.book-card:hover .cover-frame img {
  transform: scale(1.035);
}

.rank {
  position: absolute;
  left: 10px;
  top: 10px;
  min-width: 26px;
  height: 26px;
  padding: 0 7px;
  border-radius: 8px;
  background: rgba(15, 47, 38, 0.82);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(4px);
}

.rank.top {
  background: var(--accent);
}

.stock-badge {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(15, 47, 38, 0.8);
  color: #fff;
  font-size: 11px;
  backdrop-filter: blur(4px);
}

.stock-badge.borrowed {
  background: rgba(153, 96, 32, 0.88);
}

.info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 2px;
}

.cat-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

h3 {
  font-size: 16px;
  line-height: 1.45;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 46px;
}

.author {
  color: var(--muted);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #89968f;
  font-size: 12px;
}

.rating {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #b27324;
  font-weight: 600;
}

.reason {
  margin-top: 2px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #f7faf7;
  color: #64756d;
  font-size: 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
