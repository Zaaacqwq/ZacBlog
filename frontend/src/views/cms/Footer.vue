<!-- footer -->
<template>
  <div class="footer">
    <div class="footer-mark">Zac Blog</div>
    <span>Copyright © 2023-2025 Zac. Essays, notes, and code.</span>
  </div>
</template>

<script>
  export default {
    name: 'cmsFooter',
  }
</script>

<style scoped>
.footer {
  width: min(1120px, calc(100vw - 32px));
  margin: 0 auto 24px;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid rgba(23, 32, 51, 0.1);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 16px 44px rgba(18, 27, 43, 0.08);
  color: rgba(29, 36, 51, 0.72);
}

.footer-mark {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: #1d2433;
}

@media screen and (max-width: 768px) {
  .footer {
    width: calc(100vw - 24px);
    padding: 16px 18px;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
