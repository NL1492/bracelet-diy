<!-- 灵感管理视图：设计新款（设计器产出）+ 优质客订（分享码审核入库） -->
<template>
  <div class="view view--on" data-view="inspirations">
    <div class="ins-main">
      <div class="ins-toolbar">
        <div class="ins-title">
          <b>灵感管理</b>
          <span>设计新款 {{ designCount }} · 客订 {{ guestCount }}</span>
        </div>
        <div class="ins-tabs">
          <span class="ins-tab" :class="{ 'ins-tab--on': tab === 'design' }" @click="setTab('design')">设计新款</span>
          <span class="ins-tab" :class="{ 'ins-tab--on': tab === 'guest' }" @click="setTab('guest')">优质客订</span>
          <span class="ins-tab" :class="{ 'ins-tab--on': tab === 'review' }" @click="setTab('review')">
            待审核<i v-if="reviewCount" class="ins-badge">{{ reviewCount }}</i>
          </span>
        </div>
        <button v-if="tab === 'design'" class="ins-add" @click="openDesigner()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>
          新建灵感
        </button>
      </div>

      <!-- 设计新款 / 优质客订：卡片网格 -->
      <div v-if="tab !== 'review'" class="ins-grid anim" :key="gridKey">
        <div v-for="(it, i) in listCards" :key="it.id" class="ins-card" :style="{ '--i': i }" :class="{ 'ins-card--off': it.status !== 1 }">
          <div class="ins-thumb">
            <img v-if="thumbs[it.id]" :src="thumbs[it.id]" alt="">
            <span v-else class="ins-thumb--skeleton"></span>
          </div>
          <div class="ins-name">{{ it.name }}</div>
          <div class="ins-meta">¥{{ it.price }} · {{ it.author }}</div>
          <div class="ins-meta ins-meta--dim">{{ it.beadCount }} 颗 · 应用 {{ it.useCount }} 次</div>
          <div class="ins-ops">
            <i class="ins-state" :class="it.status === 1 ? 'ins-state--on' : 'ins-state--off'">{{ it.status === 1 ? '上架中' : '已下架' }}</i>
            <span class="ins-ops__btns">
              <button @click="toggleStatus(it)">{{ it.status === 1 ? '下架' : '上架' }}</button>
              <button @click="openDesigner(it)">编辑</button>
              <button class="ins-del" @click="openDelete(it)">删除</button>
            </span>
          </div>
        </div>
        <div v-if="!listCards.length" class="ins-empty">{{ tab === 'design' ? '还没有设计新款，点右上角「新建灵感」开始创作' : '暂无已入库的优质客订' }}</div>
      </div>

      <!-- 待审核客订：行列表 -->
      <div v-else class="ins-reviews anim" :key="gridKey">
        <div v-for="(rv, i) in reviews" :key="rv.designId" class="ins-review" :style="{ '--i': i }">
          <div class="ins-review__thumb">
            <img v-if="rvThumbs[rv.designId]" :src="rvThumbs[rv.designId]" alt="">
            <span v-else class="ins-thumb--skeleton"></span>
          </div>
          <div class="ins-review__info">
            <b>{{ rv.designName }}</b>
            <span>客订用户：{{ rv.ownerNickname }}</span>
            <span>{{ rv.beadCount }} 颗 · 估价 ¥{{ rv.totalPrice }}</span>
          </div>
          <div class="ins-review__count">
            <b>{{ rv.shareUseCount }}</b>
            <span>次被采用</span>
          </div>
          <div class="ins-review__ops">
            <button class="ins-review__go" :disabled="rv.busy" @click="approveReview(rv)">通过入库</button>
            <button class="ins-review__no" :disabled="rv.busy" @click="rejectReview(rv)">驳回</button>
          </div>
        </div>
        <div v-if="!reviews.length" class="ins-empty">暂无待审核客订 · 用户方案被采用 {{ REVIEW_THRESHOLD }} 次后进入此处</div>
      </div>
    </div>

    <!-- 灵感设计器：左选素材 / 右环形预览 + 表单 -->
    <transition name="mup">
      <div v-if="ed.open" class="mup-mask" @click.self="ed.open = false">
        <div class="ins-editor">
          <div class="mup-head"><b>{{ ed.id ? '编辑灵感' : '新建灵感' }}</b><span @click="ed.open = false">✕</span></div>

          <div class="ins-editor__body">
            <!-- 左：素材选择 -->
            <div class="ins-pick">
              <div class="ins-pick__cats">
                <span v-for="c in catalog" :key="c.id" class="ins-pick__cat" :class="{ 'ins-pick__cat--on': pickCat === c.id }" @click="setPickCat(c.id)">{{ c.label }}</span>
              </div>
              <div class="ins-pick__subs">
                <span v-for="s in pickSubs" :key="s.id" class="ins-pick__sub" :class="{ 'ins-pick__sub--on': pickSub === s.id }" @click="pickSub = s.id">{{ s.label }}</span>
              </div>
              <div class="ins-pick__grid">
                <button v-for="it in pickItems" :key="it.path" class="ins-pick__item" @click="addBead(it)">
                  <img :src="it.path" :alt="it.name" loading="lazy" @error="$event.target.style.visibility = 'hidden'">
                  <span>{{ it.name }}</span>
                </button>
              </div>
            </div>

            <!-- 右：预览 + 表单 -->
            <div class="ins-form">
              <div class="ins-form__preview">
                <canvas ref="previewCv" width="300" height="300"></canvas>
              </div>
              <div class="ins-form__beadbar">
                <span>已串 <b>{{ ed.beads.length }}</b> / 25 颗</span>
                <i v-if="ed.beads.length && ed.beads.length < MIN_BEADS" class="ins-form__need">再添 {{ MIN_BEADS - ed.beads.length }} 颗即可成串</i>
                <span class="ins-form__spacer"></span>
                <button type="button" :disabled="!ed.beads.length" @click="removeLastBead">撤销一颗</button>
                <button type="button" :disabled="!ed.beads.length" @click="clearBeads">清空</button>
              </div>
              <div class="mup-form ins-form__fields">
                <label>灵感名称<input v-model="ed.name" maxlength="30" placeholder="如：清雅白玉"></label>
                <label>作者署名<input v-model="ed.author" maxlength="20" placeholder="默认：官方设计师"></label>
                <label>演示视频（C 端灵感页实况展示）
                  <div class="ins-video-row">
                    <input v-model="ed.videoUrl" type="text" placeholder="视频地址，可上传或留空">
                    <button type="button" class="ins-video-btn" :disabled="ed.uploading" @click="videoInput && videoInput.click()">{{ ed.uploading ? '上传中…' : '上传' }}</button>
                  </div>
                  <input ref="videoInput" type="file" accept="video/mp4,video/webm,.mp4,.webm,.mov,.avi,.mkv" hidden @change="onVideoChosen">
                </label>
              </div>
            </div>
          </div>

          <div class="mup-foot">
            <button class="mup-btn" @click="ed.open = false">取消</button>
            <button class="mup-btn mup-btn--go" :disabled="ed.busy || ed.beads.length < MIN_BEADS" @click="saveDesigner">{{ ed.busy ? '保存中…' : '保存灵感' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除确认 -->
    <transition name="mup">
      <div v-if="del.open" class="mup-mask" @click.self="del.open = false">
        <div class="mup-panel">
          <div class="mup-head"><b>删除灵感</b><span @click="del.open = false">✕</span></div>
          <p class="ins-del-hint">确定删除灵感「<b>{{ del.item && del.item.name }}</b>」？删除后 C 端灵感页将不再展示，此操作不可恢复。</p>
          <div class="mup-foot">
            <button class="mup-btn" @click="del.open = false">取消</button>
            <button class="mup-btn ins-btn-danger" :disabled="del.busy" @click="submitDelete">{{ del.busy ? '删除中…' : '确认删除' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="us-toast">
      <div v-if="toast" class="us-toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { initCatalog } from '../../materials/catalog'

/* 审核阈值（与后端 InspirationServiceImpl.SHARE_REVIEW_THRESHOLD 一致，仅文案用） */
const REVIEW_THRESHOLD = 5

/* 成串最少珠数（与 C 端 useBeadEngine.MIN_BEADS_FOR_BRACELET 一致） */
const MIN_BEADS = 8

/* ==================== 数据 ====================
   list = 全部灵感（含下架）；beadById / pathToBead = bead_info 索引（设计器素材 → 商品库 ID） */
const list = ref([])
const reviews = ref([])
const thumbs = ref({})     // inspirationId → dataURL
const rvThumbs = ref({})   // designId → dataURL
const tab = ref('design')  // design | guest | review
const gridKey = ref(0)

const beadById = new Map()
const pathToBead = new Map()

const designCount = computed(() => list.value.filter(it => it.type === 'design').length)
const guestCount = computed(() => list.value.filter(it => it.type === 'guest').length)
const reviewCount = computed(() => reviews.value.length)
const listCards = computed(() =>
  list.value.filter(it => (tab.value === 'design' ? it.type === 'design' : it.type === 'guest')))

/* ==================== 轻提示 ==================== */
const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 3800)
}

/* ==================== 请求封装（带登录态 / 401 踢回登录） ==================== */
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

/* ==================== 图片加载（模块级缓存） ==================== */
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

/* ==================== 环形排布绘制（卡片缩略图 / 设计器预览共用） ==================== */
/* beads: [{ img, size(mm), beadType }]；主珠圆裁 + 球面高光，非主珠按原始宽高比绘制 */
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

  const cx = size / 2, cy = size / 2
  // 环径固定：所有缩略图/预览的珠环大小一致，多珠时靠珠径收缩容纳（视觉均衡）
  const R = size * 0.30

  ctx.beginPath()
  ctx.arc(cx, cy, R + 2, 0, Math.PI * 2)
  ctx.strokeStyle = 'rgba(180, 160, 140, 0.35)'
  ctx.lineWidth = Math.max(1, size / 200)
  ctx.stroke()

  valid.forEach((b, i) => {
    const angle = Math.PI / 2 + (2 * Math.PI / valid.length) * i
    const bx = cx + R * Math.cos(angle)
    const by = cy + R * Math.sin(angle)
    // 标称半径（旧实现 /115 偏大，缩至 /145）；珠多时按内接正多边形边长收缩，避免相邻珠重叠
    const nominal = (b.size || 10) * size / 145
    const fit = R * Math.sin(Math.PI / valid.length) * 0.95
    const r = Math.min(nominal, fit)
    const isMain = b.beadType === 1

    if (isMain) {
      // 投影
      ctx.save()
      ctx.beginPath()
      ctx.ellipse(bx, by + r * 0.35, r * 0.95, r * 0.6, 0, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(90, 70, 50, 0.16)'
      ctx.fill()
      ctx.restore()
      // 本体（圆裁 + 高光）
      ctx.save()
      ctx.beginPath()
      ctx.arc(bx, by, r, 0, Math.PI * 2)
      ctx.clip()
      ctx.drawImage(b.img, bx - r, by - r, r * 2, r * 2)
      ctx.restore()
      const overlay = ctx.createRadialGradient(bx - r * 0.3, by - r * 0.35, r * 0.05, bx, by, r)
      overlay.addColorStop(0, 'rgba(255,255,255,0.16)')
      overlay.addColorStop(0.6, 'rgba(0,0,0,0)')
      overlay.addColorStop(1, 'rgba(0,0,0,0.1)')
      ctx.beginPath()
      ctx.arc(bx, by, r, 0, Math.PI * 2)
      ctx.fillStyle = overlay
      ctx.fill()
      ctx.beginPath()
      ctx.arc(bx, by, r, 0, Math.PI * 2)
      ctx.strokeStyle = 'rgba(140, 110, 80, 0.18)'
      ctx.lineWidth = Math.max(0.6, size / 300)
      ctx.stroke()
    } else {
      // 隔片/配饰/文玩：保留原始宽高比
      const iw = b.img.naturalWidth || 100, ih = b.img.naturalHeight || 100
      const aspect = iw / ih
      let dw = r * 2, dh = r * 2
      if (aspect >= 1) dh = r * 2 / aspect
      else dw = r * 2 * aspect
      ctx.drawImage(b.img, bx - dw / 2, by - dh / 2, dw, dh)
    }
  })
}

/* beadsJson → 绘制条目 [{ img, size, beadType }] */
function parseBeads(json) {
  let parsed = {}
  try { parsed = JSON.parse(json || '{}') } catch {}
  const arr = Array.isArray(parsed.beads) ? parsed.beads : []
  return arr.map(b => {
    const info = beadById.get(b.beadId)
    return info ? { img: info.imgUrl, size: b.size || 10, beadType: info.beadType } : null
  }).filter(Boolean)
}

async function renderThumbs() {
  const out = {}
  for (const it of list.value) {
    const beads = parseBeads(it.beadsJson)
    if (!beads.length) continue
    const cv = document.createElement('canvas')
    await drawRing(cv, beads, 220)
    out[it.id] = cv.toDataURL('image/png')
  }
  thumbs.value = out
}

async function renderRvThumbs() {
  const out = {}
  for (const rv of reviews.value) {
    const beads = parseBeads(rv.beadListJson)
    if (!beads.length) continue
    const cv = document.createElement('canvas')
    await drawRing(cv, beads, 160)
    out[rv.designId] = cv.toDataURL('image/png')
  }
  rvThumbs.value = out
}

/* ==================== 数据加载 ==================== */
async function loadBeads() {
  try {
    const json = await req('/beads')
    if (json.code === 200 && Array.isArray(json.data)) {
      json.data.forEach(b => {
        beadById.set(b.id, b)
        if (b.imgUrl) pathToBead.set(b.imgUrl, b)
      })
    }
  } catch (e) {
    showToast('商品库加载失败，设计器暂不可用')
  }
}

async function loadList() {
  const json = await req('/admin/inspirations')
  if (json.code === 200) {
    list.value = json.data || []
    renderThumbs()
  } else {
    showToast(json.msg || '加载灵感列表失败')
  }
}

async function loadReviews() {
  const json = await req('/admin/reviews')
  if (json.code === 200) {
    reviews.value = json.data || []
    renderRvThumbs()
  }
}

function setTab(t) {
  tab.value = t
  gridKey.value++
  if (t === 'review') loadReviews()
}

/* ==================== 上下架 / 删除 ==================== */
async function toggleStatus(it) {
  const enable = it.status !== 1
  const json = await req(`/admin/inspirations/${it.id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ enable }),
  })
  if (json.code === 200) {
    showToast(enable ? `「${it.name}」已上架` : `「${it.name}」已下架`)
    loadList()
  } else {
    showToast(json.msg || '操作失败')
  }
}

const del = reactive({ open: false, item: null, busy: false })
function openDelete(it) {
  Object.assign(del, { open: true, item: it, busy: false })
}
async function submitDelete() {
  if (del.busy || !del.item) return
  del.busy = true
  const json = await req('/admin/inspirations/' + del.item.id, { method: 'DELETE' })
  del.busy = false
  if (json.code === 200) {
    showToast(`灵感「${del.item.name}」已删除`)
    del.open = false
    loadList()
  } else {
    showToast(json.msg || '删除失败')
  }
}

/* ==================== 客订审核 ==================== */
async function approveReview(rv) {
  const json = await req(`/admin/reviews/${rv.designId}/approve`, { method: 'POST' })
  if (json.code === 200) {
    showToast(`「${rv.designName}」已入库灵感集录（优质客订）`)
    loadReviews()
    loadList()
  } else {
    showToast(json.msg || '操作失败')
  }
}

async function rejectReview(rv) {
  const json = await req(`/admin/reviews/${rv.designId}/reject`, { method: 'POST' })
  if (json.code === 200) {
    showToast(`「${rv.designName}」已驳回`)
    loadReviews()
  } else {
    showToast(json.msg || '操作失败')
  }
}

/* ==================== 灵感设计器 ==================== */
const catalog = initCatalog()
const pickCat = ref('main-bead')
const pickSub = ref('')
const previewCv = ref(null)

const ed = reactive({ open: false, id: null, name: '', author: '', videoUrl: '', uploading: false, beads: [], busy: false })
const videoInput = ref(null)

/* 演示视频上传 → POST /admin/upload/video（存储于 image/uploads/video/，dev 模式经 vite publicDir 直接可访问） */
async function onVideoChosen(e) {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!f) return
  if (f.size > 500 * 1024 * 1024) { showToast('视频不能超过 500MB'); return }
  ed.uploading = true
  try {
    const fd = new FormData()
    fd.append('file', f)
    const headers = {}
    const t = localStorage.getItem('bracelet-token')
    if (t) headers.Authorization = 'Bearer ' + t
    const res = await fetch('/api/bracelet/admin/upload/video', { method: 'POST', headers, body: fd })
    const json = await res.json()
    if (json.code === 200 && json.data && json.data.url) {
      ed.videoUrl = json.data.url
      showToast('视频已上传')
    } else {
      showToast(json.msg || '上传失败')
    }
  } catch (err) {
    showToast('上传失败，请重试')
  }
  ed.uploading = false
}

const pickSubs = computed(() => {
  const c = catalog.find(x => x.id === pickCat.value)
  return c ? c.subCategories.filter(s => !s.id.endsWith('-in-use')) : []
})
const pickItems = computed(() => {
  const c = catalog.find(x => x.id === pickCat.value)
  const s = pickSubs.value.find(x => x.id === pickSub.value)
  return s ? s.items : []
})

function setPickCat(id) {
  pickCat.value = id
  const subs = pickSubs.value
  pickSub.value = subs.length ? subs[0].id : ''
}

function addBead(item) {
  if (ed.beads.length >= 25) {
    showToast('最多 25 颗珠子')
    return
  }
  const cat = catalog.find(c => c.id === pickCat.value)
  ed.beads.push({ path: item.path, size: item.size || (cat && cat.defaultSize) || 10, name: item.name })
}

function removeLastBead() { ed.beads.pop() }
function clearBeads() { ed.beads.splice(0) }

/* 预览画布：beads 变化即重绘 */
watch(() => ed.beads.length, () => {
  if (!ed.open) return
  nextTick(() => {
    if (!previewCv.value) return
    drawRing(previewCv.value, ed.beads.map(b => ({
      img: b.path,
      size: b.size,
      beadType: pathToBead.get(b.path)?.beadType ?? 1,
    })), 300)
  })
})

function openDesigner(ins) {
  if (ins) {
    ed.id = ins.id
    ed.name = ins.name
    ed.author = ins.author || ''
    ed.videoUrl = ins.videoUrl || ''
    ed.uploading = false
    const raw = parseBeads(ins.beadsJson)
    ed.beads = raw.map(b => ({ path: b.img, size: b.size, name: beadById.size ? '' : '' }))
  } else {
    ed.id = null
    ed.name = ''
    ed.author = ''
    ed.videoUrl = ''
    ed.uploading = false
    ed.beads = []
  }
  ed.open = true
  nextTick(() => {
    if (previewCv.value && ed.beads.length) {
      drawRing(previewCv.value, ed.beads.map(b => ({
        img: b.path, size: b.size, beadType: pathToBead.get(b.path)?.beadType ?? 1,
      })), 300)
    }
  })
}

async function saveDesigner() {
  if (ed.busy) return
  if (!ed.name.trim()) { showToast('请输入灵感名称'); return }
  if (ed.beads.length < MIN_BEADS) {
    showToast(`再添点珠子吧，至少需要 ${MIN_BEADS} 颗才能串成串！`)
    return
  }
  const beads = []
  let skipped = 0
  ed.beads.forEach(b => {
    const info = pathToBead.get(b.path)
    if (info) beads.push({ beadId: info.id, size: b.size })
    else skipped++
  })
  if (!beads.length) { showToast('所选素材未关联商品库，无法保存'); return }
  if (skipped) showToast('部分素材不在商品库中，已自动跳过')

  const body = {
    name: ed.name.trim(),
    author: ed.author.trim(),
    videoUrl: ed.videoUrl.trim() || null,
    braceleted: true,
    beads,
  }
  // 不传 price：后端自动按珠子单价 × 数量汇总成本价（与 C 端口径一致）

  ed.busy = true
  const json = ed.id
    ? await req('/admin/inspirations/' + ed.id, { method: 'PUT', body: JSON.stringify(body) })
    : await req('/admin/inspirations', { method: 'POST', body: JSON.stringify(body) })
  ed.busy = false
  if (json.code === 200) {
    showToast(ed.id ? '灵感已更新' : '灵感已创建')
    ed.open = false
    loadList()
  } else {
    showToast(json.msg || '保存失败')
  }
}

/* ==================== 入场级联 ==================== */
onMounted(() => {
  const root = document.querySelector('[data-view="inspirations"]')
  if (!root) return
  Array.prototype.forEach.call(root.children, (c, i) => c.style.setProperty('--i', i))
  root.classList.add('view--in')
  setTimeout(() => root.classList.remove('view--in'), 1200)

  loadBeads().then(() => {
    loadList()
    loadReviews()
  })
})
</script>

<style>
/* ===== 灵感管理视图（ins- 前缀；复用 mup-mask/mup-panel 弹窗体系与 us-toast） ===== */
.view--on[data-view="inspirations"] { grid-template-rows: auto; }
.ins-main { grid-column: 1 / -1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.ins-toolbar { display: flex; align-items: center; gap: 12px; }
.ins-title { display: flex; flex-direction: column; gap: 2px; }
.ins-title b { font-size: 17px; font-weight: 800; color: var(--ink); }
.ins-title span { font-size: 12px; color: var(--dim); }
.ins-tabs { margin-left: auto; display: flex; gap: 4px; background: var(--soft); border: 1px solid var(--line); border-radius: 12px; padding: 3px; }
.ins-tab {
  padding: 6px 14px; border-radius: 9px; font-size: 12.5px; font-weight: 600;
  color: var(--dim); cursor: pointer; transition: all .2s; display: inline-flex; align-items: center; gap: 5px;
}
.ins-tab:hover { color: var(--ink); }
.ins-tab--on { background: var(--card-solid); color: var(--ink); box-shadow: 0 2px 8px rgba(17,17,26,.08); }
.ins-badge {
  font-style: normal; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 999px;
  background: #c2543e; color: #fff; font-size: 10px; font-weight: 700;
  display: inline-grid; place-items: center;
}
.ins-add {
  display: flex; align-items: center; gap: 6px;
  border: 0; border-radius: 12px; padding: 9px 16px;
  background: var(--blue); color: #fff; font: inherit; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: filter .2s;
}
.ins-add svg { width: 15px; height: 15px; }
.ins-add:hover { filter: brightness(1.08); }

/* 卡片网格 */
.ins-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.ins-card {
  background: var(--card-solid); border: 1px solid var(--line); border-radius: 16px;
  padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 3px;
  transition: transform .2s, box-shadow .2s;
}
.ins-card:hover { transform: translateY(-3px); box-shadow: 0 12px 28px rgba(17,17,26,.1); }
.ins-card--off { opacity: .55; }
.ins-grid.anim .ins-card { animation: insCardIn .45s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i, 0) * 45ms); }
@keyframes insCardIn { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: none; } }
.ins-thumb {
  width: 100%; aspect-ratio: 1; border-radius: 12px; overflow: hidden;
  background: linear-gradient(160deg, rgba(0,0,0,.03), rgba(0,0,0,.07));
  display: grid; place-items: center;
}
.ins-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.ins-thumb--skeleton { width: 100%; height: 100%; background: var(--soft); animation: insPulse 1.2s ease-in-out infinite; }
@keyframes insPulse { 50% { opacity: .5; } }
.ins-name { font-size: 13.5px; font-weight: 800; color: var(--ink); margin-top: 3px; }
.ins-meta { font-size: 11.5px; color: var(--ink); font-weight: 600; }
.ins-meta--dim { color: var(--dim); font-weight: 400; }
.ins-ops { display: flex; align-items: center; margin-top: 7px; gap: 6px; }
.ins-state {
  font-style: normal; font-size: 10.5px; font-weight: 700; padding: 2.5px 9px; border-radius: 999px;
}
.ins-state--on { background: rgba(74,124,232,.12); color: var(--blue); }
.ins-state--off { background: var(--soft); color: var(--dim); }
.ins-ops__btns { margin-left: auto; display: flex; gap: 5px; }
.ins-ops__btns button {
  border: 1px solid var(--line); background: var(--card-solid); color: var(--ink);
  border-radius: 8px; padding: 4px 9px; font: inherit; font-size: 11.5px; cursor: pointer; transition: all .15s;
}
.ins-ops__btns button:hover { border-color: var(--blue); color: var(--blue); }
.ins-ops .ins-del { color: #c2543e; }
.ins-ops .ins-del:hover { border-color: #c2543e; color: #c2543e; background: rgba(194,84,62,.08); }
.ins-empty { grid-column: 1 / -1; padding: 70px 0; text-align: center; color: var(--dim); font-size: 13px; }

/* 审核列表 */
.ins-reviews { display: flex; flex-direction: column; gap: 10px; }
.ins-review {
  display: grid; grid-template-columns: 88px 1fr auto auto; gap: 14px; align-items: center;
  background: var(--card-solid); border: 1px solid var(--line); border-radius: 16px; padding: 12px 16px;
}
.ins-reviews.anim .ins-review { animation: insCardIn .45s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i, 0) * 45ms); }
.ins-review__thumb {
  width: 88px; height: 88px; border-radius: 12px; overflow: hidden;
  background: linear-gradient(160deg, rgba(0,0,0,.03), rgba(0,0,0,.07));
  display: grid; place-items: center;
}
.ins-review__thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.ins-review__info { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.ins-review__info b { font-size: 14px; font-weight: 800; color: var(--ink); }
.ins-review__info span { font-size: 11.5px; color: var(--dim); }
.ins-review__count { text-align: center; padding: 0 10px; }
.ins-review__count b { display: block; font-size: 22px; font-weight: 800; color: var(--blue); line-height: 1.1; }
.ins-review__count span { font-size: 10.5px; color: var(--dim); }
.ins-review__ops { display: flex; gap: 8px; }
.ins-review__ops button {
  border: 0; border-radius: 10px; padding: 8px 15px; font: inherit; font-size: 12.5px; font-weight: 700;
  cursor: pointer; transition: filter .2s, opacity .2s;
}
.ins-review__ops button:disabled { opacity: .5; cursor: not-allowed; }
.ins-review__go { background: var(--blue); color: #fff; }
.ins-review__go:hover { filter: brightness(1.08); }
.ins-review__no { background: var(--soft); color: var(--dim); border: 1px solid var(--line); }
.ins-review__no:hover { color: #c2543e; border-color: rgba(194,84,62,.4); }

/* 设计器弹窗 */
.ins-editor {
  width: 860px; max-width: 94vw; max-height: 88vh; overflow: hidden;
  background: var(--card-solid); border-radius: 18px; padding: 18px 20px 16px;
  display: flex; flex-direction: column; gap: 12px;
  box-shadow: 0 24px 64px rgba(10,14,24,.25);
}
.ins-editor__body { display: grid; grid-template-columns: 1fr 340px; gap: 16px; min-height: 0; }
.ins-pick { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.ins-pick__cats { display: flex; gap: 4px; background: var(--soft); border-radius: 10px; padding: 3px; }
.ins-pick__cat { flex: 1; text-align: center; padding: 6px 0; border-radius: 8px; font-size: 12px; font-weight: 600; color: var(--dim); cursor: pointer; transition: all .15s; }
.ins-pick__cat--on { background: var(--card-solid); color: var(--ink); box-shadow: 0 2px 6px rgba(17,17,26,.08); }
.ins-pick__subs { display: flex; flex-wrap: wrap; gap: 5px; }
.ins-pick__sub { padding: 4px 11px; border-radius: 999px; font-size: 11px; color: var(--dim); background: var(--soft); cursor: pointer; transition: all .15s; }
.ins-pick__sub--on { background: var(--blue); color: #fff; }
.ins-pick__grid {
  flex: 1; min-height: 0; overflow-y: auto;
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; align-content: start;
  max-height: 330px; padding-right: 2px;
}
.ins-pick__grid::-webkit-scrollbar { width: 5px; }
.ins-pick__grid::-webkit-scrollbar-thumb { background: var(--line); border-radius: 3px; }
.ins-pick__item {
  border: 1px solid var(--line); background: var(--card-solid); border-radius: 10px;
  padding: 7px 4px 5px; cursor: pointer; transition: all .15s; font: inherit;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.ins-pick__item:hover { border-color: var(--blue); transform: translateY(-2px); }
.ins-pick__item img { width: 44px; height: 44px; object-fit: contain; }
.ins-pick__item span { font-size: 10.5px; color: var(--ink); max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.ins-form { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.ins-form__preview {
  border-radius: 14px; overflow: hidden; background: var(--soft);
  display: grid; place-items: center; flex: none;
}
.ins-form__preview canvas { width: 100%; height: auto; display: block; }
.ins-form__beadbar { display: flex; align-items: center; gap: 8px; font-size: 11.5px; color: var(--dim); }
.ins-form__beadbar b { color: var(--ink); }
.ins-form__spacer { flex: 1; }
.ins-form__beadbar button {
  border: 1px solid var(--line); background: var(--card-solid); color: var(--ink);
  border-radius: 8px; padding: 4px 10px; font: inherit; font-size: 11px; cursor: pointer; transition: all .15s;
}
.ins-form__beadbar button:hover:not(:disabled) { border-color: var(--blue); color: var(--blue); }
.ins-form__beadbar button:disabled { opacity: .4; cursor: not-allowed; }
.ins-form__fields label { font-size: 11.5px; color: var(--dim); }
.ins-form__fields input { margin-top: 3px; }
.ins-form__need { font-style: normal; font-size: 11px; font-weight: 700; color: #c2543e; }
.ins-video-row { display: flex; align-items: center; gap: 6px; margin-top: 3px; }
.ins-video-row input { margin-top: 0; flex: 1; min-width: 0; }
.ins-video-btn {
  flex: none; border: 1px solid var(--line); background: var(--soft); color: var(--ink);
  border-radius: 8px; padding: 6px 12px; font: inherit; font-size: 11.5px; font-weight: 600;
  cursor: pointer; white-space: nowrap; transition: all .15s;
}
.ins-video-btn:hover:not(:disabled) { border-color: var(--blue); color: var(--blue); }
.ins-video-btn:disabled { opacity: .5; cursor: not-allowed; }

/* 删除确认 */
.ins-del-hint { font-size: 12.5px; line-height: 1.8; color: var(--dim); margin: 0; }
.ins-del-hint b { color: #c2543e; }
.ins-btn-danger { background: #c2543e !important; border-color: transparent !important; color: #fff !important; font-weight: 700; }
.ins-btn-danger:disabled { opacity: .4; cursor: not-allowed; }

@media (max-width: 1020px) {
  .view--on[data-view="inspirations"] { grid-template-columns: 1fr; }
  .ins-grid { grid-template-columns: repeat(2, 1fr); }
  .ins-toolbar { flex-wrap: wrap; }
  .ins-tabs { margin-left: 0; }
  .ins-editor__body { grid-template-columns: 1fr; }
}
</style>
