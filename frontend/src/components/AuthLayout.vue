<script setup>
defineProps({
  badge: { type: String, default: '智慧图书馆服务' },
  title: { type: String, default: '让每一次阅读，都有方向' },
  subtitle: {
    type: String,
    default: '基于借阅行为、内容特征与 AI 语义理解的个性化图书推荐平台'
  }
})
</script>

<template>
  <div class="auth-page">
    <aside class="side">
      <div class="glow glow-a"></div>
      <div class="glow glow-b"></div>

      <router-link class="brand" to="/home">
        <span class="logo"><el-icon><Reading /></el-icon></span>
        <span>
          <strong>知书</strong>
          <small>LIBRARY RECOMMENDATION</small>
        </span>
      </router-link>

      <div class="side-copy">
        <span class="badge">{{ badge }}</span>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>

      <div class="features">
        <div class="feature">
          <el-icon><MagicStick /></el-icon>
          <div><strong>混合推荐</strong><span>热门 · 内容 · 协同过滤</span></div>
        </div>
        <div class="feature">
          <el-icon><ChatDotRound /></el-icon>
          <div><strong>AI 图书助手</strong><span>RAG 检索增强问答</span></div>
        </div>
        <div class="feature">
          <el-icon><Search /></el-icon>
          <div><strong>语义检索</strong><span>理解你的自然语言需求</span></div>
        </div>
      </div>

      <div class="quote">
        <p>“ 读书是在别人思想的帮助下，建立起自己的思想。 ”</p>
        <span>— 尼古拉·鲁巴金</span>
      </div>

      <div class="shelf">
        <span class="book b1"></span>
        <span class="book b2"></span>
        <span class="book b3"></span>
        <span class="book b4"></span>
        <span class="book b5"></span>
      </div>
    </aside>

    <main class="form-side">
      <router-link class="back" to="/home">
        <el-icon><Back /></el-icon>返回首页
      </router-link>
      <div class="form-wrap">
        <slot />
      </div>
      <p class="copyright">© 2026 知书高校图书馆推荐系统</p>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(420px, 0.95fr);
  background: #f6f8f5;
}

.side {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 44px 56px 40px;
  background: linear-gradient(145deg, #1c5344 0%, #12392d 55%, #0c2a22 100%);
  color: #e8f2ed;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.glow-a {
  width: 460px;
  height: 460px;
  right: -170px;
  top: -140px;
  background: radial-gradient(circle, rgba(200, 137, 60, 0.24), transparent 65%);
}

.glow-b {
  width: 520px;
  height: 520px;
  left: -240px;
  bottom: -260px;
  background: radial-gradient(circle, rgba(100, 168, 143, 0.25), transparent 66%);
}

.brand {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  z-index: 2;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-size: 23px;
  color: #f6ecd8;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.brand span:last-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand strong {
  font-size: 22px;
  letter-spacing: 0.16em;
}

.brand small {
  font-size: 8px;
  letter-spacing: 0.22em;
  color: #89a99b;
}

.side-copy {
  position: relative;
  z-index: 2;
  margin-top: clamp(58px, 11vh, 110px);
  max-width: 520px;
}

.badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #e5d5b3;
  background: rgba(200, 137, 60, 0.16);
  border: 1px solid rgba(200, 137, 60, 0.3);
}

h1 {
  margin-top: 20px;
  font-size: clamp(32px, 3.4vw, 48px);
  line-height: 1.22;
  letter-spacing: 0.03em;
  font-weight: 700;
  color: #f4f8f2;
}

.side-copy p {
  margin-top: 18px;
  max-width: 440px;
  color: #a9c1b6;
  font-size: 14px;
  line-height: 1.9;
}

.features {
  position: relative;
  z-index: 2;
  margin-top: 38px;
  display: grid;
  gap: 12px;
  max-width: 460px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 12px 16px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(4px);
}

.feature .el-icon {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 17px;
  color: #ecd9b4;
  background: rgba(200, 137, 60, 0.16);
}

.feature div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.feature strong {
  font-size: 13px;
  color: #f0f6f2;
  font-weight: 600;
}

.feature span {
  font-size: 11px;
  color: #8faa9e;
}

.quote {
  position: relative;
  z-index: 2;
  margin-top: auto;
  padding-top: 34px;
  max-width: 480px;
}

.quote p {
  font-size: 13px;
  line-height: 1.9;
  color: #b2c8bd;
}

.quote span {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: #7f9b8f;
}

.shelf {
  position: absolute;
  right: 34px;
  bottom: 36px;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  gap: 7px;
  opacity: 0.5;
}

.book {
  width: 17px;
  border-radius: 3px 3px 1px 1px;
  background: linear-gradient(180deg, rgba(230, 220, 195, 0.6), rgba(160, 190, 175, 0.15));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.b1 { height: 88px; }
.b2 { height: 112px; width: 20px; }
.b3 { height: 72px; }
.b4 { height: 100px; width: 15px; }
.b5 { height: 84px; }

.form-side {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 52px 32px 30px;
}

.back {
  position: absolute;
  top: 30px;
  right: 34px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #75847d;
  transition: color 0.2s ease;
}

.back:hover {
  color: var(--brand);
}

.form-wrap {
  width: min(420px, 100%);
}

.copyright {
  position: absolute;
  bottom: 22px;
  font-size: 11px;
  color: #9eaaa4;
}

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .side {
    padding: 32px 30px 28px;
  }
  .side-copy {
    margin-top: 34px;
  }
  h1 {
    font-size: 30px;
  }
  .features,
  .quote,
  .shelf {
    display: none;
  }
  .form-side {
    padding: 42px 22px 48px;
    justify-content: flex-start;
  }
  .back {
    top: 16px;
    right: 20px;
  }
  .copyright {
    display: none;
  }
}
</style>
