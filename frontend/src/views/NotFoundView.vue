<template>
  <div class="not-found">
    <div class="not-found__content">
      <h1 class="not-found__code">404</h1>
      <p class="not-found__message">페이지를 찾을 수 없습니다</p>
      <p class="not-found__sub">요청하신 페이지가 존재하지 않거나 이동되었습니다.</p>
      <router-link to="/" class="not-found__btn">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg, #0f0f13);
  text-align: center;
}

.not-found__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.not-found__code {
  font-size: 6rem;
  font-weight: 700;
  color: var(--color-primary, #a78bfa);
  line-height: 1;
  margin: 0;
}

.not-found__message {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text, #e2e8f0);
  margin: 0;
}

.not-found__sub {
  font-size: 0.875rem;
  color: var(--color-text-muted, #94a3b8);
  margin: 0;
}

.not-found__btn {
  margin-top: 8px;
  padding: 10px 24px;
  border-radius: 8px;
  background: var(--color-primary, #a78bfa);
  color: #fff;
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
  transition: opacity 0.2s;
}

.not-found__btn:hover {
  opacity: 0.85;
}
</style>
