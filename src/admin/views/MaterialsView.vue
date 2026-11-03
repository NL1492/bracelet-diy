<!-- 素材库视图：数据源 = C 端共享目录（src/materials/catalog.js）；支持上传新素材（前端自动裁剪，管理员决定归属） -->
<template>
  <div class="view view--on" data-view="materials">
    <div class="mat-main">
      <div class="mat-toolbar">
        <div class="mat-toolbar__row">
          <div class="mtabs">
            <span class="mtab" :class="{ 'mtab--on': activeCat === 'all' }" @click="setCat('all')">全部</span>
            <span v-for="c in catalog" :key="c.id" class="mtab" :class="{ 'mtab--on': activeCat === c.id }" @click="setCat(c.id)">{{ c.label }}</span>
          </div>
          <label class="msearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
            <input v-model="q" type="text" placeholder="搜索素材名称…">
          </label>
          <button class="mup" @click="openUpload">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>
            上传素材
          </button>
        </div>
        <div v-if="activeCat !== 'all'" class="mat-toolbar__row">
          <span class="mchip" :class="{ 'mchip--on': activeSub === 'all' }" @click="activeSub = 'all'">全部</span>
          <span v-for="s in subList" :key="s.id" class="mchip" :class="{ 'mchip--on': activeSub === s.id }" @click="activeSub = s.id">{{ s.label }}</span>
        </div>
      </div>
      <div class="mat-grid anim" :key="gridKey">
        <div v-for="(it, i) in baseItems" :key="it.catId + '/' + it.subLabel + '/' + it.name" class="mat-card" :style="{ '--i': i }" @click="openDetail(it)">
          <div class="mat-thumb"><img :src="it.path" :alt="it.name" loading="lazy" @error="onImgErr"></div>
          <div class="mat-name">{{ it.name }}</div>
          <div class="mat-meta">
            <span class="mat-size">{{ it.size }}mm</span>
            <span class="mat-total" :class="{ 'mat-total--warn': it.stock <= 12 && it.stock > 5, 'mat-total--danger': it.stock <= 5 }">共 {{ it.stock }} 件</span>
          </div>
        </div>
        <div v-if="!baseItems.length" class="mat-empty">没有匹配的素材</div>
      </div>
    </div>

    <!-- 上传素材弹窗：自动裁剪 + 管理员决定归属 -->
    <transition name="mup">
      <div v-if="up.open" class="mup-mask" @click.self="up.open = false">
        <div class="mup-panel">
          <div class="mup-head"><b>上传素材</b><span @click="up.open = false">✕</span></div>

          <!-- 选图 + 裁剪预览 -->
          <div class="mup-stage" @click="$refs.file.click()" @dragover.prevent @drop.prevent="onDrop">
            <input ref="file" type="file" accept="image/*" hidden @change="onFile">
            <img v-if="up.preview" :src="up.preview" alt="">
            <div v-else class="mup-stage__hint">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M12 16V4m0 0 -4 4m4-4 4 4"></path><path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3"></path></svg>
              点击或拖入图片
            </div>
          </div>

          <!-- 归属（管理员决定放哪） -->
          <div class="mup-form">
            <label>素材名称<input v-model="up.name" type="text" placeholder="如：金发晶"></label>
            <label>大类
              <select v-model="up.catId" @change="resetUpSub">
                <option v-for="c in catalog" :key="c.id" :value="c.id">{{ c.label }}</option>
              </select>
            </label>
            <label>子分类（色系）
              <select v-model="up.subId" :disabled="!upSubList.length">
                <option v-for="s in upSubList" :key="s.id" :value="s.id">{{ s.label }}</option>
                <option v-if="!upSubList.length" value="all">{{ upAllLabel }}</option>
              </select>
            </label>
            <label>长度（mm）
              <select v-model="up.size">
                <option v-for="s in upSizeList" :key="s" :value="s">{{ s }} mm</option>
              </select>
            </label>
            <label>库存数量<input v-model.number="up.stock" type="number" min="0"></label>
          </div>
          <p class="mup-note">自动裁剪：透明边修剪 → 居中补方 → 512×512 PNG。确认后裁剪图自动下载，放入 <b>{{ targetDir }}</b> 目录即完成上架。</p>

          <div class="mup-foot">
            <button class="mup-btn" @click="up.open = false">取消</button>
            <button class="mup-btn mup-btn--go" :disabled="!up.preview" @click="confirmUpload">登记并下载</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 素材详情：按尺寸统计 / 编辑库存（点击卡片弹出） -->
    <transition name="mup">
      <div v-if="det.open && det.item" class="mup-mask" @click.self="det.open = false">
        <div class="det-panel">
          <div class="det-head">
            <span class="det-thumb"><img :src="det.item.path" alt="" @error="$event.target.style.display = 'none'"></span>
            <div class="det-info">
              <b>{{ det.item.name }}</b>
              <span>{{ det.item.catLabel }} · {{ det.item.subLabel }}</span>
            </div>
            <span class="det-close" @click="det.open = false">✕</span>
          </div>
          <div class="det-rows">
            <div v-for="r in detRows" :key="r.size" class="det-row">
              <span class="mat-size">{{ r.size }}mm</span>
              <span class="det-stk">
                <button class="mat-stk-btn" type="button" @click="changeDetStock(r.size, r.qty - 1)">−</button>
                <input class="mat-stk-input" type="number" :value="r.qty" min="0" @change="changeDetStock(r.size, +$event.target.value)">
                <button class="mat-stk-btn" type="button" @click="changeDetStock(r.size, r.qty + 1)">＋</button>
              </span>
              <span class="det-tier" :class="'det-tier--' + tierOf(r.qty)">{{ tierText(r.qty) }}</span>
            </div>
          </div>
          <div class="det-foot">合计 <b>{{ detTotal }}</b> 件 · 尺寸区间 {{ det.cat.sizeMin }}–{{ det.cat.sizeMax }}mm</div>
        </div>
      </div>
    </transition>

    <!-- 轻提示 -->
    <transition name="mup">
      <div v-if="toast" class="mtoast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script>
import { reactive, ref, computed, onMounted } from 'vue'
import { initCatalog, getStock, getStockDetail, setStockSize, getSize } from '../../materials/catalog'

/* 大类 id → image 目录名（与共享目录里的路径约定一致） */
const DIR = { 'main-bead': '水晶', 'accessories': '配饰', 'spacer': '隔片', 'wenwan': '文玩' }

export default {
  name: 'MaterialsView',
  setup() {
    /* 进入页面：卡片级联入场（与其他视图同语言） */
    onMounted(() => {
      const root = document.querySelector('[data-view="materials"]')
      if (!root) return
      Array.prototype.forEach.call(root.children, (c, i) => c.style.setProperty('--i', i))
      root.classList.add('view--in')
      setTimeout(() => root.classList.remove('view--in'), 1200)
    })

    /* 共享目录（reactive：上传登记 push 后网格自动更新） */
    const catalog = reactive(initCatalog())
    const activeCat = ref('all')
    const activeSub = ref('all')
    const q = ref('')
    const gridKey = ref(0)

    /* 当前大类下的可选子分类（排除 正在使用 / 所有xx 聚合项） */
    const subList = computed(() => {
      const c = catalog.find(x => x.id === activeCat.value)
      return c ? c.subCategories.filter(s => !s.id.endsWith('-in-use') && !s.id.endsWith('-all')) : []
    })
    const allLabel = computed(() => {
      const c = catalog.find(x => x.id === activeCat.value)
      const all = c && c.subCategories.find(s => s.id.endsWith('-all'))
      return all ? all.label : '全部分类'
    })

    /* 扁平化条目：附带 长度 / 实时库存（stockRev 撬动重算），供卡片展示与编辑 */
    const stockRev = ref(0)
    /* 长度不再作为筛选类别：尺寸库存的查看/编辑收敛到详情面板（点卡片弹出） */
    const baseItems = computed(() => {
      stockRev.value   // 库存变更依赖：编辑后重算
      const kw = q.value.trim()
      const out = []
      catalog.forEach(c => {
        if (activeCat.value !== 'all' && c.id !== activeCat.value) return
        c.subCategories.forEach(s => {
          if (s.id.endsWith('-in-use')) return
          if (activeSub.value !== 'all' && s.id !== activeSub.value) return
          s.items.forEach(it => {
            if (kw && it.name.indexOf(kw) === -1) return
            out.push({ ...it, catId: c.id, catLabel: c.label, subLabel: s.label, size: getSize(it, c), stock: getStock(it.name) })
          })
        })
      })
      return out
    })

    /* ================= 素材详情面板：按尺寸统计 / 编辑库存 ================= */
    const det = reactive({ open: false, item: null, cat: null })
    const detRows = ref([])
    const detTotal = computed(() => detRows.value.reduce((a, r) => a + r.qty, 0))
    /* 状态阈值与经营总览「库存预警」同口径：≤5 紧缺 / ≤12 偏低 / 其余正常 */
    function tierOf(q) { return q <= 5 ? 'danger' : (q <= 12 ? 'warn' : 'ok') }
    function tierText(q) { return q <= 5 ? '紧缺' : (q <= 12 ? '偏低' : '正常') }
    function openDetail(it) {
      const c = catalog.find(x => x.id === it.catId)
      if (!c) return
      det.item = it; det.cat = c; det.open = true
      detRows.value = getStockDetail(it.name, c)
    }
    function changeDetStock(size, qty) {
      setStockSize(det.item.name, size, qty, det.cat)
      detRows.value = getStockDetail(det.item.name, det.cat)
      stockRev.value++   // 撬动卡片合计重算
    }

    function setCat(id) { activeCat.value = id; activeSub.value = 'all'; gridKey.value++ }
    function onImgErr(e) { e.target.style.display = 'none' }

    /* ================= 上传：自动裁剪（透明边修剪 → 居中补方 → 512×512） ================= */
    const up = reactive({ open: false, preview: '', name: '', catId: 'main-bead', subId: 'all', size: 10, stock: 40 })
    const toast = ref('')
    let toastTimer = null
    function showToast(msg) {
      toast.value = msg
      clearTimeout(toastTimer)
      toastTimer = setTimeout(() => (toast.value = ''), 3800)
    }

    /* 弹窗内：跟随弹窗大类（而非视图筛选）的子分类列表 */
    const upSubList = computed(() => {
      const c = catalog.find(x => x.id === up.catId)
      return c ? c.subCategories.filter(s => !s.id.endsWith('-in-use') && !s.id.endsWith('-all')) : []
    })
    const upAllLabel = computed(() => {
      const c = catalog.find(x => x.id === up.catId)
      const all = c && c.subCategories.find(s => s.id.endsWith('-all'))
      return all ? all.label : '全部分类'
    })
    /* 长度可选项：所选大类的 sizeMin..sizeMax 区间 */
    const upSizeList = computed(() => {
      const c = catalog.find(x => x.id === up.catId)
      if (!c) return [10]
      const out = []
      for (let s = c.sizeMin; s <= c.sizeMax; s++) out.push(s)
      return out
    })
    function resetUpSub() {
      up.subId = upSubList.value.length ? upSubList.value[0].id : 'all'
      const c = catalog.find(x => x.id === up.catId)
      up.size = c ? c.defaultSize : 10
    }

    function openUpload() {
      up.open = true
      up.preview = ''
      up.name = ''
      up.catId = activeCat.value === 'all' ? 'main-bead' : activeCat.value
      up.stock = 40
      resetUpSub()
    }

    /* 裁剪管线：读取 alpha 包围盒 → 裁剪 → 长边贴边等比缩放 → 透明居中成方图 */
    function autoCrop(img) {
      const S = 512
      const t = document.createElement('canvas')
      t.width = img.naturalWidth; t.height = img.naturalHeight
      const tx = t.getContext('2d')
      tx.drawImage(img, 0, 0)
      const d = tx.getImageData(0, 0, t.width, t.height).data
      let minX = t.width, minY = t.height, maxX = -1, maxY = -1
      for (let y = 0; y < t.height; y++) {
        for (let px = 0; px < t.width; px++) {
          if (d[(y * t.width + px) * 4 + 3] > 12) {
            if (px < minX) minX = px; if (px > maxX) maxX = px
            if (y < minY) minY = y; if (y > maxY) maxY = y
          }
        }
      }
      if (maxX < 0) { minX = 0; minY = 0; maxX = t.width - 1; maxY = t.height - 1 }
      const bw = maxX - minX + 1, bh = maxY - minY + 1
      const k = Math.min(S / bw, S / bh)
      const w = bw * k, h = bh * k
      const c = document.createElement('canvas')
      c.width = S; c.height = S
      const x = c.getContext('2d')
      x.imageSmoothingQuality = 'high'
      x.drawImage(t, minX, minY, bw, bh, (S - w) / 2, (S - h) / 2, w, h)
      return c.toDataURL('image/png')
    }

    function handleFile(file) {
      if (!file || !file.type.startsWith('image/')) return
      const reader = new FileReader()
      reader.onload = () => {
        const img = new Image()
        img.onload = () => {
          up.preview = autoCrop(img)
          if (!up.name) up.name = file.name.replace(/\.[^.]+$/, '')
        }
        img.src = reader.result
      }
      reader.readAsDataURL(file)
    }
    function onFile(e) { handleFile(e.target.files[0]); e.target.value = '' }
    function onDrop(e) { handleFile(e.dataTransfer.files[0]) }

    /* 目标路径 / 目录（确认后自动下载裁剪图，管理员放入对应 image 目录） */
    const targetDir = computed(() => {
      const dir = DIR[up.catId] || '水晶'
      const folder = up.catId === 'wenwan' ? '文玩'
        : (upSubList.value.find(s => s.id === up.subId) || upSubList.value[0] || { label: '文玩' }).label
      return 'image/' + dir + '/' + folder + '/'
    })

    function confirmUpload() {
      const name = up.name.trim()
      if (!name || !up.preview) { showToast('请先选图并填写素材名称'); return }
      const c = catalog.find(x => x.id === up.catId)
      const dir = DIR[up.catId]
      let target, folder
      if (up.catId === 'wenwan') {
        target = c.subCategories.find(x => x.id.endsWith('-all'))   // 文玩无静态子分类 → 登记进「所有文玩」
        folder = '文玩'
      } else {
        if (up.subId === 'all') { showToast('请选择子分类（色系）'); return }
        target = c.subCategories.find(x => x.id === up.subId)
        folder = target.label
      }
      if (!target) { showToast('该大类暂无可用子分类'); return }
      if (target.items.some(it => it.name === name)) { showToast('已存在同名素材'); return }

      const path = '/' + dir + '/' + folder + '/' + name + '.png'
      target.items.push({ name, path, size: up.size })
      setStockSize(name, up.size, up.stock, c)

      /* 自动下载裁剪图（后端上传接口就绪后替换为 POST /upload） */
      const a = document.createElement('a')
      a.href = up.preview
      a.download = name + '.png'
      a.click()

      up.open = false
      gridKey.value++
      showToast('已登记「' + name + '」' + up.size + 'mm → ' + path + ' ；把下载的图片放入 ' + targetDir.value
        + '，并在 catalog.js 该子分类补一条记录即可 C 端生效')
    }

    return {
      catalog, activeCat, activeSub, q, gridKey, subList, baseItems,
      setCat, onImgErr,
      det, detRows, detTotal, openDetail, changeDetStock, tierOf, tierText,
      up, toast, openUpload, onFile, onDrop, confirmUpload, targetDir, upSubList, upAllLabel, upSizeList, resetUpSub,
    }
  },
}
</script>

<style>
  /* ===== 素材库视图（数据源：共享目录 catalog.js） ===== */
  .view--on[data-view="materials"] { grid-template-rows: auto; }
  .mat-main { grid-column: 1 / -1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
  .mat-toolbar { display: flex; flex-direction: column; gap: 10px;
    /* 吸顶：滚动素材网格时筛选工具栏固定在 .view 顶部，随主题换底色 */
    position: sticky; top: 0; z-index: 6;
    background: var(--card-solid);
    border-radius: 14px;
    padding: 6px 10px 10px;
    box-shadow: 0 10px 18px -12px rgba(31,42,64,.28);
  }
  /* 吸顶条下缘的渐隐过渡：盖住 gap 缝隙，卡片滑过时自然淡入，不用负 margin 防横向滚动条 */
  .mat-toolbar::after { content: ''; position: absolute; left: -10px; right: -10px; top: 100%;
    height: 14px; background: linear-gradient(var(--card-solid), transparent); pointer-events: none; }
  .mat-toolbar__row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  .mtab { padding: 8px 18px; border-radius: 999px; background: var(--panel-bg); border: 1px solid var(--line);
          font-size: 13px; color: var(--dim); cursor: pointer; user-select: none; transition: all .2s; }
  .mtab:hover { color: var(--ink); }
  .mtab--on { background: var(--blue); border-color: transparent; color: #fff; font-weight: 700;
              box-shadow: 0 8px 20px rgba(74,124,232,.35); }
  .mchip { padding: 5px 13px; border-radius: 999px; font-size: 12px; background: var(--soft); color: var(--dim);
           cursor: pointer; user-select: none; transition: all .2s; }
  .mchip--on { background: var(--soft-2); color: var(--ink); font-weight: 600; box-shadow: inset 0 0 0 1.5px var(--ink); }
  .msearch { margin-left: auto; display: flex; align-items: center; gap: 6px; background: var(--panel-bg);
             border: 1px solid var(--line); border-radius: 999px; padding: 7px 14px; width: 210px; box-sizing: border-box; }
  .msearch svg { width: 14px; height: 14px; color: var(--dim); flex: none; }
  .msearch input { border: 0; outline: 0; background: transparent; font: inherit; font-size: 13px; color: var(--ink); width: 100%; }
  /* 上传按钮 */
  .mup {
    display: flex; align-items: center; gap: 6px;
    padding: 8px 16px; border-radius: 999px; border: 0; cursor: pointer;
    background: var(--blue); color: #fff; font-size: 13px; font-weight: 700; font-family: inherit;
    box-shadow: 0 8px 20px rgba(74,124,232,.35); transition: filter .2s;
  }
  .mup:hover { filter: brightness(1.06); }
  .mup svg { width: 13px; height: 13px; }
  .mat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(124px, 1fr)); gap: 12px; }
  .mat-card { background: var(--panel-bg); border: 1px solid var(--line); border-radius: 16px; padding: 14px 10px;
              display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer;
              transition: transform .25s, box-shadow .25s; }
  .mat-card:hover { transform: translateY(-4px); box-shadow: 0 14px 30px rgba(31,42,64,.12); }
  .mat-thumb {
    width: 72px; height: 72px; border-radius: 50%; display: grid; place-items: center;
    background:
      radial-gradient(circle at 50% 42%, rgba(0,0,0,.06), transparent 72%),
      var(--soft);
    overflow: hidden;
  }
  .mat-thumb img { width: 82%; height: 82%; object-fit: contain; display: block; }
  .mat-name { font-size: 12.5px; font-weight: 700; color: var(--ink); max-width: 100%;
              white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  /* 长度徽章 + 合计件数（同一行两端对齐；合计色与总览预警同口径） */
  .mat-meta { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 4px; }
  .mat-size { flex: none; font-size: 10px; font-weight: 700; color: var(--dim);
              background: var(--soft); border-radius: 999px; padding: 2px 6px; }
  .mat-total { flex: none; font-size: 10.5px; font-weight: 700; color: var(--dim); }
  .mat-total--warn { color: #F59E0B; }
  .mat-total--danger { color: #EF4444; }
  .mat-stk-btn { width: 16px; height: 16px; border: 0; border-radius: 50%; background: transparent;
                 color: var(--dim); font-size: 11px; line-height: 1; cursor: pointer; padding: 0;
                 display: grid; place-items: center; font-family: inherit; transition: all .15s; }
  .mat-stk-btn:hover { background: var(--panel-bg); color: var(--ink); }
  .mat-stk-input { width: 26px; border: 0; outline: 0; background: transparent; text-align: center;
                   font: inherit; font-size: 11px; font-weight: 700; color: var(--ink); padding: 2px 0;
                   -moz-appearance: textfield; appearance: textfield; }
  .mat-stk-input::-webkit-outer-spin-button,
  .mat-stk-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
  .mat-empty { grid-column: 1 / -1; padding: 60px 0; text-align: center; color: var(--dim); font-size: 13px; }
  .mat-grid.anim .mat-card { animation: cardIn .5s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i, 0) * 22ms); }

  /* ===== 上传素材弹窗 ===== */
  .mup-mask {
    position: fixed; inset: 0; z-index: 90;
    background: rgba(15,18,26,.42);
    backdrop-filter: blur(6px); -webkit-backdrop-filter: blur(6px);
    display: grid; place-items: center;
  }
  .mup-panel {
    width: 400px; max-height: 86vh; overflow-y: auto;
    border-radius: 22px; padding: 20px 22px;
    background: var(--card-solid);
    border: 1px solid var(--line);
    box-shadow: 0 30px 80px rgba(10,14,24,.35);
    display: flex; flex-direction: column; gap: 14px;
  }
  .mup-head { display: flex; align-items: center; }
  .mup-head b { font-size: 16px; font-weight: 800; color: var(--ink); }
  .mup-head span { margin-left: auto; cursor: pointer; color: var(--dim); font-size: 14px; padding: 4px 8px; border-radius: 8px; }
  .mup-head span:hover { background: var(--soft); color: var(--ink); }
  .mup-stage {
    height: 170px; border-radius: 16px; cursor: pointer;
    border: 1.5px dashed var(--line);
    background: var(--soft);
    display: grid; place-items: center; overflow: hidden;
  }
  .mup-stage__hint { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--dim); font-size: 12.5px; }
  .mup-stage__hint svg { width: 26px; height: 26px; }
  .mup-stage img {
    width: 132px; height: 132px; object-fit: contain;
    background: repeating-conic-gradient(#e9e9ec 0 25%, #ffffff 0 50%) 0 0 / 18px 18px;   /* 棋盘格衬透明区 */
    border-radius: 50%;
    box-shadow: 0 10px 24px rgba(31,42,64,.14);
  }
  .mup-form { display: flex; flex-direction: column; gap: 10px; }
  .mup-form label { display: flex; align-items: center; gap: 10px; font-size: 12.5px; color: var(--dim); }
  .mup-form input, .mup-form select {
    flex: 1; min-width: 0;
    border: 1px solid var(--line); border-radius: 10px; outline: 0;
    background: var(--page-bg); color: var(--ink);
    font: inherit; font-size: 13px; padding: 8px 12px;
  }
  .mup-form select:disabled { opacity: .55; }
  .mup-note { font-size: 11px; line-height: 1.7; color: var(--dim); }
  .mup-note b { color: var(--ink); }
  .mup-foot { display: flex; justify-content: flex-end; gap: 10px; }
  .mup-btn {
    border: 1px solid var(--line); background: var(--soft); color: var(--ink);
    border-radius: 10px; padding: 9px 18px; font: inherit; font-size: 13px; cursor: pointer;
  }
  .mup-btn--go { background: var(--blue); border-color: transparent; color: #fff; font-weight: 700; }
  .mup-btn--go:disabled { opacity: .45; cursor: not-allowed; }
  .mup-btn:hover:not(:disabled) { filter: brightness(1.05); }
  /* 弹窗 / 轻提示过渡 */
  .mup-enter-active, .mup-leave-active { transition: opacity .25s ease; }
  .mup-enter-active .mup-panel, .mup-enter-active .det-panel { transition: transform .3s cubic-bezier(.22, 1, .36, 1); }
  .mup-enter-from, .mup-leave-to { opacity: 0; }
  .mup-enter-from .mup-panel, .mup-enter-from .det-panel { transform: translateY(18px) scale(.97); }

  /* 轻提示 */
  .mtoast {
    position: fixed; left: 50%; bottom: 92px; transform: translateX(-50%);
    z-index: 95; max-width: 70vw;
    background: var(--card-solid); color: var(--ink);
    border: 1px solid var(--line); border-radius: 999px;
    padding: 10px 20px; font-size: 12.5px;
    box-shadow: 0 14px 34px rgba(10,14,24,.22);
  }

  /* ===== 素材详情面板（按尺寸统计 / 编辑库存） ===== */
  .det-panel {
    width: 340px; max-height: 86vh; overflow-y: auto;
    border-radius: 22px; padding: 18px 20px;
    background: var(--card-solid);
    border: 1px solid var(--line);
    box-shadow: 0 30px 80px rgba(10,14,24,.35);
    display: flex; flex-direction: column; gap: 14px;
  }
  .det-head { display: flex; align-items: center; gap: 12px; }
  .det-thumb { width: 52px; height: 52px; border-radius: 50%; background: var(--soft);
               display: grid; place-items: center; overflow: hidden; flex: none; }
  .det-thumb img { width: 84%; height: 84%; object-fit: contain; display: block; }
  .det-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .det-info b { font-size: 15px; font-weight: 800; color: var(--ink);
                white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .det-info span { font-size: 11px; color: var(--dim); }
  .det-close { margin-left: auto; cursor: pointer; color: var(--dim); font-size: 14px;
               padding: 4px 8px; border-radius: 8px; flex: none; }
  .det-close:hover { background: var(--soft); color: var(--ink); }
  .det-rows { display: flex; flex-direction: column; gap: 8px; }
  .det-row { display: flex; align-items: center; gap: 10px;
             background: var(--soft); border-radius: 12px; padding: 8px 10px; }
  .det-row .mat-size { background: var(--panel-bg); }
  .det-stk { margin-left: auto; display: flex; align-items: center; gap: 2px;
             background: var(--panel-bg); border-radius: 999px; padding: 1px 2px; }
  .det-tier { flex: none; width: 40px; text-align: right; font-size: 10.5px; font-weight: 700; }
  .det-tier--ok { color: #10B981; }
  .det-tier--warn { color: #F59E0B; }
  .det-tier--danger { color: #EF4444; }
  .det-foot { font-size: 11.5px; color: var(--dim); }
  .det-foot b { color: var(--ink); }

  @media (max-width: 1020px) {
    .view--on[data-view="materials"] { grid-template-columns: 1fr; }
  }
</style>
