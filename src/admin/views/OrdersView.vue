<!-- 订单管理视图：订单列表 / 状态筛选 / 发货 / 退款审核 -->
<template>
  <div class="view view--on" data-view="orders">
    <div class="od-main">
      <div class="od-toolbar">
        <div class="od-title">
          <b>订单管理</b>
          <span>共 {{ orders.length }} 个订单</span>
        </div>
        <label class="od-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
          <input v-model="q" type="text" placeholder="搜索单号 / 方案 / 用户…">
        </label>
      </div>

      <div class="od-tabs">
        <button
          v-for="t in TABS"
          :key="String(t.v)"
          class="od-tab"
          :class="{ 'od-tab--on': status === t.v }"
          @click="switchTab(t.v)"
        >
          {{ t.label }}
        </button>
      </div>

      <div class="od-list" :key="listKey">
        <div v-for="(o, i) in filtered" :key="o.id" class="od-card" :style="{ '--i': i }">
          <img v-if="thumbs[o.id]" :src="thumbs[o.id]" class="od-thumb" alt="">
          <div v-else class="od-thumb od-thumb--empty"></div>

          <div class="od-body">
            <div class="od-row1">
              <b>{{ o.designName }}</b>
              <i class="od-status" :class="'od-status--' + o.status">{{ o.statusText }}</i>
            </div>
            <p class="od-meta">{{ o.orderNo }} · {{ o.beadCount }} 颗 · <b class="od-price">¥{{ o.totalAmount }}</b></p>
            <p class="od-meta">{{ o.nickname || o.username || '未知用户' }} · {{ fmtTime(o.createTime) }}</p>
            <p v-if="o.receiverName" class="od-meta">收货：{{ o.receiverName }} {{ o.receiverPhone }} {{ o.receiverAddress }}</p>
            <p v-if="o.remark" class="od-meta">备注：{{ o.remark }}</p>
            <p v-if="o.shipNo" class="od-meta">物流：{{ o.shipNo }}</p>
          </div>

          <div class="od-ops">
            <button v-if="o.status === 1" class="od-btn od-btn--go" @click="openShip(o)">发货</button>
            <button v-if="o.status === 5" class="od-btn od-btn--go" @click="refundAction(o, 'approve')">同意退款</button>
            <button v-if="o.status === 5" class="od-btn" @click="refundAction(o, 'reject')">驳回</button>
          </div>
        </div>

        <div v-if="!filtered.length" class="od-empty">没有匹配的订单</div>
      </div>
    </div>

    <!-- 发货弹窗 -->
    <transition name="mup">
      <div v-if="ship.open" class="mup-mask" @click.self="ship.open = false">
        <div class="mup-panel">
          <div class="mup-head"><b>订单发货</b><span @click="ship.open = false">✕</span></div>
          <p class="od-ship-hint">
            为订单 <b>{{ ship.order && ship.order.orderNo }}</b>（{{ ship.order && ship.order.designName }}）填写物流单号。
          </p>
          <div class="mup-form">
            <label>物流单号
              <input v-model="ship.no" maxlength="64" placeholder="如 SF1234567890" @keyup.enter="submitShip">
            </label>
          </div>
          <div class="mup-foot">
            <button class="mup-btn" @click="ship.open = false">取消</button>
            <button class="mup-btn mup-btn--go" :disabled="ship.busy" @click="submitShip">
              {{ ship.busy ? '提交中…' : '确认发货' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="od-toast">
      <div v-if="toast" class="od-toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

/* ================= 数据 ================= */
const orders = ref([])
const q = ref('')
const status = ref(null)
const listKey = ref(0)
const thumbs = ref({})
const beadById = ref(new Map())

const TABS = [
  { v: null, label: '全部' },
  { v: 0, label: '待付款' },
  { v: 1, label: '待发货' },
  { v: 2, label: '待收货' },
  { v: 5, label: '退款中' },
  { v: 3, label: '已完成' },
  { v: 4, label: '已取消' },
  { v: 6, label: '已退款' },
]

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  if (!kw) return orders.value
  return orders.value.filter(o =>
    (o.orderNo || '').toLowerCase().includes(kw) ||
    (o.designName || '').toLowerCase().includes(kw) ||
    (o.username || '').toLowerCase().includes(kw) ||
    (o.nickname || '').toLowerCase().includes(kw) ||
    (o.receiverName || '').toLowerCase().includes(kw))
})

/* ================= 轻提示 ================= */
const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 3600)
}

/* ================= 请求封装（带登录态 / 401 踢回登录） ================= */
async function req(url, opts = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const t = localStorage.getItem('bracelet-token')
  if (t) headers.Authorization = 'Bearer ' + t
  const res = await fetch('/api/bracelet' + url, { ...opts, headers })
  let json
  try { json = await res.json() } catch { json = { code: res.status, msg: 'HTTP ' + res.status } }
  if (json && json.code === 401) {
    showToast('登录已失效，请重新登录')
    setTimeout(() => { location.href = '/' }, 800)
    throw new Error(json.msg || '未登录')
  }
  return json
}

/* ================= 图片加载与缩略图 ================= */
const imgCache = new Map()
function loadImg(path) {
  if (!path) return Promise.resolve(null)
  if (!imgCache.has(path)) {
    imgCache.set(path, new Promise(resolve => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => resolve(null)
      img.src = path
    }))
  }
  return imgCache.get(path)
}

async function drawRing(canvas, beads, size) {
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, size, size)

  const tray = await loadImg('/底盘/底盘.png')
  if (tray) ctx.drawImage(tray, 0, 0, size, size)

  const loaded = await Promise.all(beads.map(b => loadImg(b.img).then(img => img ? { ...b, img } : null)))
  const valid = loaded.filter(Boolean)
  if (!valid.length) return

  const cx = size / 2
  const cy = size / 2
  const R = size * 0.30

  ctx.beginPath()
  ctx.arc(cx, cy, R + 2, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(180, 160, 140, 0.35)'
  ctx.lineWidth = Math.max(1, size / 200)
  ctx.stroke()

  const n = valid.length
  // 珠多时按内接正多边形边长收缩，避免叠压
  const maxR = size * 0.11
  const fitR = n > 1 ? (2 * Math.PI * R / n) * 0.42 : maxR

  valid.forEach((b, i) => {
    const angle = Math.PI / 2 + (2 * Math.PI / n) * i
    const bx = cx + R * Math.cos(angle)
    const by = cy + R * Math.sin(angle)
    const br = Math.min(maxR, fitR)

    if (b.beadType === 1) {
      ctx.save()
      ctx.beginPath()
      ctx.arc(bx, by, br, 0, Math.PI * 2)
      ctx.clip()
      ctx.drawImage(b.img, bx - br, by - br, br * 2, br * 2)
      ctx.restore()
      const g = ctx.createRadialGradient(bx - br * 0.35, by - br * 0.35, br * 0.1, bx, by, br)
      g.addColorStop(0, 'rgba(255,255,255,.45)')
      g.addColorStop(0.45, 'rgba(255,255,255,0)')
      g.addColorStop(1, 'rgba(0,0,0,.12)')
      ctx.beginPath()
      ctx.arc(bx, by, br, 0, Math.PI * 2)
      ctx.fillStyle = g
      ctx.fill()
    } else {
      const iw = b.img.width
      const ih = b.img.height
      const scale = (br * 2) / Math.max(iw, ih)
      const w = iw * scale
      const h = ih * scale
      ctx.drawImage(b.img, bx - w / 2, by - h / 2, w, h)
    }

    ctx.beginPath()
    ctx.arc(bx, by, br, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(140, 110, 80, 0.18)'
    ctx.lineWidth = 0.5
    ctx.stroke()
  })
}

async function drawThumbs() {
  const map = {}
  for (const o of orders.value) {
    let parsed
    try {
      parsed = JSON.parse(o.beadListJson || '[]')
    } catch {
      continue
    }
    const items = Array.isArray(parsed) ? parsed : (parsed.beads || [])
    if (!items.length) continue

    const beads = items.slice(0, 14).map(b => {
      const info = beadById.value.get(b.beadId)
      return info ? { img: info.img, size: b.size || 10, beadType: info.beadType } : null
    }).filter(Boolean)
    if (!beads.length) continue

    const canvas = document.createElement('canvas')
    await drawRing(canvas, beads, 76)
    map[o.id] = canvas.toDataURL('image/png')
  }
  thumbs.value = map
}

/* ================= 加载 ================= */
async function loadBeads() {
  const json = await req('/beads')
  if (json.code === 200) {
    const map = new Map()
    ;(json.data || []).forEach(b => map.set(b.id, { img: b.imgUrl, beadType: b.beadType }))
    beadById.value = map
  }
}

async function load() {
  const qs = status.value === null ? '' : `?status=${status.value}`
  const json = await req('/admin/orders' + qs)
  if (json.code === 200) {
    orders.value = json.data || []
    listKey.value++
    await drawThumbs()
  } else {
    showToast(json.msg || '加载订单失败')
  }
}

function switchTab(v) {
  if (status.value === v) return
  status.value = v
  load()
}

function fmtTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 16)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/* ================= 发货 ================= */
const ship = reactive({ open: false, order: null, no: '', busy: false })

function openShip(o) {
  Object.assign(ship, { open: true, order: o, no: '', busy: false })
}

async function submitShip() {
  if (ship.busy) return
  if (!ship.no.trim()) {
    showToast('请填写物流单号')
    return
  }
  ship.busy = true
  const json = await req(`/admin/orders/${ship.order.id}/ship`, {
    method: 'POST',
    body: JSON.stringify({ shipNo: ship.no.trim() }),
  })
  ship.busy = false
  if (json.code === 200) {
    showToast('已发货')
    ship.open = false
    load()
  } else {
    showToast(json.msg || '发货失败')
  }
}

/* ================= 退款审核 ================= */
async function refundAction(o, action) {
  const isApprove = action === 'approve'
  if (!window.confirm(isApprove ? `确认同意订单 ${o.orderNo} 的退款？` : `确认驳回订单 ${o.orderNo} 的退款申请？`)) return

  const json = await req(`/admin/orders/${o.id}/refund/${action}`, { method: 'POST' })
  if (json.code === 200) {
    showToast(json.msg || '操作成功')
    load()
  } else {
    showToast(json.msg || '操作失败')
  }
}

onMounted(async () => {
  await loadBeads()
  await load()
})
</script>

<style>
/* 覆盖壳层 .view--on 的总览双列网格：行高自适应 + 内容横跨全部列，占满面板 */
.view--on[data-view="orders"] { grid-template-rows: auto; }

.od-main {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
  min-height: 0;
}

.od-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.od-title b {
  font-size: 16px;
  color: var(--ink);
}

.od-title span {
  margin-left: 8px;
  font-size: 12px;
  color: var(--dim);
}

.od-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--soft);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 8px 12px;
}

.od-search svg {
  width: 13px;
  height: 13px;
  color: var(--dim);
  flex-shrink: 0;
}

.od-search input {
  border: none;
  background: transparent;
  outline: none;
  color: var(--ink);
  font-size: 13px;
  font-family: inherit;
  width: 190px;
}

.od-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.od-tab {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--dim);
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.od-tab:hover {
  background: var(--soft);
}

.od-tab--on {
  background: var(--ink);
  color: var(--page-bg);
  border-color: var(--ink);
}

.od-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.od-card {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 14px;
  background: var(--card-solid);
  border: 1px solid var(--line);
  border-radius: 14px;
  animation: odIn 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--i) * 45ms);
}

@keyframes odIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}

.od-thumb {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  flex-shrink: 0;
  background: var(--soft);
  object-fit: cover;
}

.od-thumb--empty {
  background: radial-gradient(circle at 50% 45%, rgba(200, 190, 175, 0.2), rgba(160, 150, 135, 0.1));
}

.od-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.od-row1 {
  display: flex;
  align-items: center;
  gap: 10px;
}

.od-row1 b {
  font-size: 14px;
  color: var(--ink);
}

.od-status {
  font-style: normal;
  font-size: 11.5px;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--soft);
  color: var(--dim);
  flex-shrink: 0;
}

.od-status--0 { background: rgba(214, 120, 72, 0.16); color: #b4623a; }
.od-status--1 { background: rgba(200, 169, 110, 0.18); color: #9a7d3f; }
.od-status--2 { background: rgba(90, 140, 180, 0.16); color: #4a7a9e; }
.od-status--3 { background: rgba(100, 150, 110, 0.16); color: #4d8159; }
.od-status--4,
.od-status--6 { background: var(--soft); color: var(--dim); }
.od-status--5 { background: rgba(214, 150, 72, 0.18); color: #a8742f; }

.od-meta {
  font-size: 12px;
  color: var(--dim);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.od-price {
  color: var(--ink);
  font-size: 12.5px;
}

.od-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.od-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--ink);
  font-size: 12.5px;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease;
}

.od-btn:hover {
  background: var(--soft);
}

.od-btn--go {
  background: var(--blue);
  color: #fff;
  border-color: var(--blue);
}

.od-btn--go:hover {
  background: var(--blue-deep);
}

.od-empty {
  text-align: center;
  color: var(--dim);
  font-size: 13px;
  padding: 50px 0;
}

.od-ship-hint {
  font-size: 13px;
  color: var(--dim);
  margin: 0 0 12px;
  line-height: 1.6;
}

.od-toast {
  position: fixed;
  left: 50%;
  bottom: 48px;
  transform: translateX(-50%);
  background: rgba(26, 28, 32, 0.92);
  color: #fff;
  font-size: 13px;
  padding: 10px 20px;
  border-radius: 999px;
  z-index: 9999;
}

.od-toast-enter-active,
.od-toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.od-toast-enter-from,
.od-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

@media (max-width: 720px) {
  .od-card {
    flex-wrap: wrap;
  }

  .od-ops {
    width: 100%;
    justify-content: flex-end;
  }

  .od-search input {
    width: 130px;
  }
}
</style>
