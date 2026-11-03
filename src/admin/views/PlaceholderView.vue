<!-- 占位视图：订单管理 / 用户中心（模块待建） -->
<template>
  <div class="view view--on" data-view="placeholder">
    <div class="ph">
      <div class="ph__ic">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="8" height="8" rx="2"></rect><rect x="13" y="3" width="8" height="8" rx="2"></rect><rect x="3" y="13" width="8" height="8" rx="2"></rect><rect x="13" y="13" width="8" height="8" rx="2"></rect></svg>
      </div>
      <b>{{ title }}</b>
      <span>模块建设中 · 敬请期待</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PlaceholderView',
  props: { title: { type: String, default: '模块建设中' } },
  mounted() {
    /* 进入页面：卡片级联入场 */
    const root = this.$el;
    Array.prototype.forEach.call(root.children, function (c, i) { c.style.setProperty('--i', i); });
    root.classList.add('view--in');
    setTimeout(function () { root.classList.remove('view--in'); }, 1200);
  }
}
</script>

<style>
  .ph {
    grid-column: 1 / -1; align-self: center; justify-self: center;
    display: flex; flex-direction: column; align-items: center; gap: 10px;
    padding: 60px 80px; border-radius: 20px;
    background: var(--panel-bg); border: 1px dashed var(--line);
  }
  .ph__ic {
    width: 52px; height: 52px; border-radius: 16px;
    display: grid; place-items: center;
    background: var(--soft); color: var(--ic);
  }
  .ph__ic svg { width: 24px; height: 24px; }
  .ph b { font-size: 16px; font-weight: 800; color: var(--ink); }
  .ph span { font-size: 12px; color: var(--dim); }
</style>
