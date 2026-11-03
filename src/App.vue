<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import gsap from 'gsap'
import { useBeadEngine } from './composables/useBeadEngine.js'
import { braceletApi, getToken, setToken } from './api/index.js'

// ==================== 底盘背景 ====================
const trays = [
  { name: '大理石', path: '/底盘/大理石.png' },
  { name: '实木底座', path: '/底盘/实木底座.png' },
  { name: '胡桃木底座', path: '/底盘/胡桃木底座.png' },
  { name: '胡桃木雕刻盘', path: '/底盘/胡桃木雕刻盘.png' },
]
const currentTrayIndex = ref(0)
const currentTray = computed(() => trays[currentTrayIndex.value].path)

function switchBackground() {
  currentTrayIndex.value = (currentTrayIndex.value + 1) % trays.length
}

// ==================== 展示效果模式 ====================
const isShowcasing = ref(false)
let showcaseTimeline = null

async function enterShowcase() {
  if (!canvasContainerRef.value || !canvasRef.value) return

  if (showcaseTimeline) showcaseTimeline.kill()

  const container = canvasContainerRef.value
  const canvas = canvasRef.value
  const trayOffset = window.innerHeight * 0.35

  container.style.overflow = 'visible'
  gsap.set(canvas, { transformOrigin: 'center center' })
  gsap.set(container, { transformOrigin: 'center center' })

  isShowcasing.value = true
  engine.setShowcasing(true)

  showcaseTimeline = gsap.timeline({
    defaults: { overwrite: 'auto' },
  })

  showcaseTimeline
    .to(container, {
      y: trayOffset,
      duration: 0.85,
      ease: 'power3.inOut',
    }, 0)
    .to(canvas, {
      y: -trayOffset - 18,
      scale: 1.28,
      duration: 0.85,
      ease: 'power2.out',
    }, 0)
}

function exitShowcase() {
  if (!canvasContainerRef.value || !canvasRef.value) return

  if (showcaseTimeline) showcaseTimeline.kill()

  const container = canvasContainerRef.value
  const canvas = canvasRef.value

  engine.setShowcasing(false)

  showcaseTimeline = gsap.timeline({
    defaults: { overwrite: 'auto' },
    onComplete: () => {
      isShowcasing.value = false
      gsap.set([container, canvas], { clearProps: 'transform' })
      // 保持 overflow: visible，挂件超出底盘也能显示
      container.style.overflow = 'visible'
    },
  })

  showcaseTimeline
    .to([container, canvas], {
      y: 0,
      scale: 1,
      duration: 0.7,
      ease: 'power3.inOut',
    }, 0)
}

function toggleShowcase() {
  if (!isShowcasing.value && !engine.isBracelet.value) {
    engine.showToast('请先串成手串再进入展示模式！')
    return
  }
  if (isShowcasing.value) {
    exitShowcase()
  } else {
    enterShowcase()
  }
}

// ==================== 珠子引擎 ====================
const engine = useBeadEngine()
const canvasRef = ref(null)
const canvasContainerRef = ref(null)

// 引擎是否已挂载（canvas 在 v-if 内，onMounted 时可能还未渲染）
const engineMounted = ref(false)

onMounted(() => {
  window.addEventListener('resize', handleResize)
  window.addEventListener('auth:required', handleAuthRequired)
  window.addEventListener('demo:mode', onDemoMode)
  // 初始化时静默加载购物车数量用于导航角标
  loadCartBadge()
  // 启动时校验本地 token，恢复登录态
  restoreSession()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('auth:required', handleAuthRequired)
  window.removeEventListener('demo:mode', onDemoMode)
  engine.destroy()
})

/* 游客演示模式：接口不可达时由 api 层降级，这里只负责提示 */
const isDemoMode = ref(false)
function onDemoMode() {
  isDemoMode.value = true
}

function handleResize() {
  engine.resize()
}

// ==================== 收拢成串 / 解除手串 ====================
function toggleBracelet() {
  if (engine.isStringing.value) return
  if (engine.isBracelet.value) {
    engine.dissolveBracelet()
  } else {
    if (engine.beadCount.value === 0) {
      engine.showToast('请先添加珠子再收拢成串')
      return
    }
    engine.stringToBracelet()
  }
}

// ==================== 素材分类数据（共享目录：src/materials/catalog.js，C 端作坊与 admin 素材库共用） ====================
import { initCatalog } from './materials/catalog'
const categories = initCatalog()

// ==================== 顶部导航 ====================
const navTabs = [
  { id: 'workshop', label: '作坊' },
  { id: 'inspiration', label: '灵感' },
  { id: 'plan', label: '方案' },
  { id: 'order', label: '订单' },
  { id: 'mine', label: '我的' },
]
const activeNavTab = ref('workshop')

// ==================== 设计步骤侧边栏 ====================
const steps = [
  { id: 1, title: '挑选主石', desc: '从珠子库里挑选入容器' },
  { id: 2, title: '选择配饰', desc: '搭配各类饰品衬托手串' },
  { id: 3, title: '调整尺寸', desc: '修改确认您的手围大小' },
  { id: 4, title: '保存下单', desc: '生成您的专属手串' },
]
const activeStep = ref(1)
const hasAdjustedSize = ref(false)

// ==================== 盲盒方案 ====================
const showBlindBoxModal = ref(false)
const deleteMode = ref(false)

const blindBoxPlans = [
  {
    name: '清雅白玉',
    desc: '白色系纯净搭配，温润如玉',
    beads: [
      { path: '/水晶/白色系/和田白玉.png', size: 10 },
      { path: '/水晶/白色系/白月光.png', size: 10 },
      { path: '/水晶/白色系/白珍珠.png', size: 8 },
      { path: '/水晶/白色系/白水晶.png', size: 10 },
      { path: '/水晶/白色系/白蝶贝.png', size: 8 },
      { path: '/水晶/白色系/白玉髓.png', size: 10 },
      { path: '/水晶/白色系/白阿塞.png', size: 10 },
      { path: '/水晶/白色系/白幽灵.png', size: 9 },
    ],
  },
  {
    name: '朱砂红韵',
    desc: '红色系热烈浓情，吉祥如意',
    beads: [
      { path: '/水晶/红色系/朱砂.png', size: 10 },
      { path: '/水晶/红色系/红玛瑙.png', size: 10 },
      { path: '/水晶/红色系/南红玛瑙.png', size: 10 },
      { path: '/水晶/红色系/石榴石.png', size: 8 },
      { path: '/水晶/红色系/红纹石.png', size: 10 },
      { path: '/水晶/红色系/草莓晶.png', size: 9 },
      { path: '/水晶/红色系/红碧玺.png', size: 10 },
      { path: '/水晶/红色系/红兔毛.png', size: 9 },
    ],
  },
  {
    name: '碧海蓝天',
    desc: '蓝色系清新自然，宁静致远',
    beads: [
      { path: '/水晶/蓝色系/海蓝宝.png', size: 10 },
      { path: '/水晶/蓝色系/蓝月光.png', size: 10 },
      { path: '/水晶/蓝色系/蓝晶石.png', size: 10 },
      { path: '/水晶/蓝色系/青金石.png', size: 9 },
      { path: '/水晶/蓝色系/天河石.png', size: 8 },
      { path: '/水晶/蓝色系/蓝纹玛瑙.png', size: 10 },
      { path: '/水晶/蓝色系/蓝砂石.png', size: 9 },
      { path: '/水晶/蓝色系/坦桑石.png', size: 10 },
    ],
  },
  {
    name: '金玉满堂',
    desc: '黄色系富贵吉祥，招财纳福',
    beads: [
      { path: '/水晶/黄色系/黄水晶.png', size: 10 },
      { path: '/水晶/黄色系/金发晶.png', size: 10 },
      { path: '/水晶/黄色系/黄虎眼.png', size: 10 },
      { path: '/水晶/黄色系/黄玛瑙.png', size: 9 },
      { path: '/水晶/黄色系/蜜蜡.png', size: 8 },
      { path: '/水晶/黄色系/太阳石.png', size: 10 },
      { path: '/水晶/黄色系/柠檬黄晶.png', size: 9 },
      { path: '/水晶/黄色系/黄碧玺.png', size: 10 },
    ],
  },
]

function openBlindBoxPlan() {
  showBlindBoxModal.value = true
}

function closeBlindBoxModal() {
  showBlindBoxModal.value = false
}

function applyBlindBoxPlan(plan) {
  engine.reset()
  // 按顺序发射每颗珠子
  plan.beads.forEach((b, i) => {
    setTimeout(() => {
      const colorIndex = [...b.path].reduce((sum, c) => sum + c.charCodeAt(0), 0)
      const BEAD_COLORS = [
        '#E85D5D', '#F4A460', '#F0C75E', '#6BCB77',
        '#4D96FF', '#9B72CF', '#E97EB2', '#45B7D1',
      ]
      const meta = beadMetaMap.get(b.path) || {}
      engine.launchBead(
        BEAD_COLORS[colorIndex % BEAD_COLORS.length],
        b.path,
        b.size,
        meta.categoryId || 'main-bead',
        meta.subCategoryId || null
      )
      // 最后一颗发射后延迟串成手串
      if (i === plan.beads.length - 1) {
        setTimeout(() => {
          engine.stringToBracelet()
        }, 800)
      }
    }, i * 350)
  })
  closeBlindBoxModal()
  engine.showToast(`已应用「${plan.name}」方案`)
}

function clearAllBeads() {
  if (engine.beadCount.value === 0) {
    engine.showToast('画布已为空，无需清除')
    return
  }
  engine.reset()
  engine.showToast('已清除全部珠子')
}

function toggleDeleteMode() {
  deleteMode.value = !deleteMode.value
  if (deleteMode.value) {
    engine.showToast('删除模式已开启，点击珠子即可删除')
  }
}

function deleteBeadFromCanvas(bead) {
  engine.removeBead(bead)
}

// ==================== 删除模式：点击画布删除珠子 ====================
function onCanvasClick(e) {
  if (!deleteMode.value) return
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const list = engine.beads.value
  for (let i = list.length - 1; i >= 0; i--) {
    const b = list[i]
    const dx = b.x - x
    const dy = b.y - y
    if (Math.sqrt(dx * dx + dy * dy) < 26) {
      engine.removeBead(b)
      engine.showToast('已删除一颗珠子')
      return
    }
  }
}

// ==================== 素材选择栏状态 ====================
const activeCategory = ref(categories[0].id)
const activeSubCategory = ref(
  categories[0].subCategories?.[0]?.id ?? null
)
const selectedBead = ref(null)
const clickedBeadPath = ref(null)  // 点击动画状态

// 分类切换 → 自动激活设计步骤
watch(activeCategory, (val) => {
  if (val === 'main-bead') activeStep.value = 1
  else if (val === 'accessories') activeStep.value = 2
})

// 手串完成 → 自动激活步骤 4
watch(() => engine.isBracelet.value, (val) => {
  if (val) activeStep.value = 4
})

// ==================== 珠子→分类 尺寸/DB-ID 元数据查找表 ====================
const beadMetaMap = new Map() // path → { defaultSize, sizeMin, sizeMax, categoryId, subCategoryId, dbId }
;(function buildBeadMetaMap() {
  let dbIdCounter = 0

  // 按 seed.sql 插入顺序遍历：主珠 → 隔片 → 配饰(含文玩)
  const seedOrder = ['main-bead', 'spacer', 'accessories', 'wenwan']

  seedOrder.forEach(catId => {
    const cat = categories.find(c => c.id === catId)
    if (!cat) return
    const baseMeta = { defaultSize: cat.defaultSize, sizeMin: cat.sizeMin, sizeMax: cat.sizeMax, categoryId: cat.id }

    if (cat.subCategories) {
      // 过滤真实子分类（跳过 -all / -in-use 聚合项），避免珠子重复编号
      const realSubs = cat.subCategories.filter(s =>
        !s.id.endsWith('-all') && !s.id.endsWith('-in-use')
      )
      // 若无真实子分类（如文玩只挂 -all），则回退到 -all
      const subsToProcess = realSubs.length > 0
        ? realSubs
        : cat.subCategories.filter(s => s.id.endsWith('-all'))

      subsToProcess.forEach(sub => {
        sub.items.forEach(item => {
          dbIdCounter++
          const meta = {
            ...baseMeta,
            subCategoryId: sub.id,
            dbId: dbIdCounter,
            defaultSize: item.size || cat.defaultSize,
            sizeMin: item.size ? Math.max(4, item.size - 4) : cat.sizeMin,
            sizeMax: item.size ? item.size + 8 : cat.sizeMax,
          }
          beadMetaMap.set(item.path, meta)
        })
      })
    } else if (cat.items) {
      cat.items.forEach(item => {
        dbIdCounter++
        const meta = {
          ...baseMeta,
          dbId: dbIdCounter,
          defaultSize: item.size || cat.defaultSize,
          sizeMin: item.size ? Math.max(4, item.size - 4) : cat.sizeMin,
          sizeMax: item.size ? item.size + 8 : cat.sizeMax,
        }
        beadMetaMap.set(item.path, meta)
      })
    }
  })
})()
// dbId → 珠子信息反向索引（用于设计回显时查找图片路径）
const dbIdToBeadMap = new Map()
beadMetaMap.forEach((meta, path) => {
  if (meta.dbId) {
    const parts = path.split('/')
    dbIdToBeadMap.set(meta.dbId, {
      imagePath: path,
      name: parts[parts.length - 1]?.replace('.png', '') || '',
    })
  }
})

// ==================== 设计方案历史 ====================
const designs = ref([])
const designsLoading = ref(false)
const planConfirmId = ref(null)   // 卡片内联删除确认的方案 ID
const planDeleting = ref(false)
const currentDesignId = ref(null)
const designThumbnails = ref({})  // designId → dataURL 手串缩略图

/** 创作数据：所有方案被应用次数合计（他人通过分享码载入即计一次） */
const creatorApplied = computed(() =>
  (designs.value || []).reduce((s, d) => s + (Number(d.shareUseCount) || 0), 0)
)

// 命名弹窗
const showNameModal = ref(false)
const nameModalInput = ref('')
const nameModalMode = ref('new')

/** 页面加载时拉取已保存方案 */
async function loadDesigns() {
  if (!getToken()) {
    designs.value = []
    return
  }
  designsLoading.value = true
  try {
    const res = await braceletApi.listDesigns()
    if (res.code === 200) {
      designs.value = res.data || []
      await generateDesignThumbnails()
    }
  } catch (e) {
    console.error('加载方案列表失败:', e)
  } finally {
    designsLoading.value = false
  }
}

/** 卡片右上角 × → 打开卡片内联确认（不跳浏览器 confirm） */
function askDeletePlan(design) {
  planConfirmId.value = design.id
}

/** 确认删除方案：逻辑删除后本地移除，避免整页重载 */
async function runDeletePlan(design) {
  if (planDeleting.value) return
  planDeleting.value = true
  try {
    const res = await braceletApi.deleteDesign(design.id)
    if (res.code === 200) {
      planConfirmId.value = null
      designs.value = designs.value.filter(d => d.id !== design.id)
      const next = { ...designThumbnails.value }
      delete next[design.id]
      designThumbnails.value = next
      // 若当前正在编辑该方案，解除绑定，避免后续保存写回已删除记录
      if (currentDesignId.value === design.id) currentDesignId.value = null
    } else {
      engine.showToast(res.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除方案失败:', e)
    engine.showToast('删除失败，请稍后重试')
  } finally {
    planDeleting.value = false
  }
}

/** 为所有已加载方案生成手串缩略图 */
async function generateDesignThumbnails() {
  const thumbnails = {}
  const THUMB_SIZE = 68  // 缩略图画布尺寸 (px)，2x 清晰度
  const BRACELET_R = 22  // 手串环半径

  for (const design of designs.value) {
    let parsed
    try {
      parsed = JSON.parse(design.beadListJson)
    } catch {
      continue
    }
    const beadItems = parsed.beads || []
    if (!beadItems.length) continue
    const isBraceleted = parsed.braceleted

    // 最多取前 12 颗用于缩略图（太多会糊成一团）
    const displayBeads = beadItems.slice(0, 12)

    // 映射 beadId → imagePath
    const beadInfos = displayBeads.map(b => {
      const info = dbIdToBeadMap.get(b.beadId)
      return info ? { imagePath: info.imagePath, size: b.size || 10 } : null
    }).filter(Boolean)

    if (!beadInfos.length) continue

    // 并行加载所有珠子图片
    const loaded = await Promise.all(
      beadInfos.map(info => {
        return new Promise((resolve) => {
          const img = new Image()
          img.crossOrigin = 'anonymous'
          img.src = info.imagePath
          img.onload = () => resolve({ img, size: info.size, path: info.imagePath })
          img.onerror = () => resolve(null)
        })
      })
    )
    const validBeads = loaded.filter(Boolean)
    if (!validBeads.length) continue

    // 创建离屏 Canvas 绘制手串缩略图
    const canvas = document.createElement('canvas')
    canvas.width = THUMB_SIZE
    canvas.height = THUMB_SIZE
    const ctx = canvas.getContext('2d')

    const cx = THUMB_SIZE / 2
    const cy = THUMB_SIZE / 2
    const n = validBeads.length

    if (isBraceleted && n >= 2) {
      // ── 手串态：珠子绕环排列（主珠圆裁球面；隔片/配饰/文玩按原始宽高比；珠多时收缩防重叠） ──
      // 画浅色环底
      ctx.beginPath()
      ctx.arc(cx, cy, BRACELET_R + 1, 0, Math.PI * 2)
      ctx.strokeStyle = 'rgba(180, 160, 140, 0.3)'
      ctx.lineWidth = 1
      ctx.stroke()

      validBeads.forEach((b, i) => {
        const angle = Math.PI / 2 + (2 * Math.PI / n) * i
        const bx = cx + BRACELET_R * Math.cos(angle)
        const by = cy + BRACELET_R * Math.sin(angle)
        const br0 = 5 + (b.size / 10) * 3                     // 标称珠子半径
        const fit = BRACELET_R * Math.sin(Math.PI / n) * 0.95 // 相邻珠不重叠上限
        const br = Math.min(br0, fit)
        const isMain = beadMetaMap.get(b.path)?.categoryId === 'main-bead'

        if (isMain) {
          // 本体（圆裁）+ 球面高光 + 微细描边模拟串线感
          ctx.save()
          ctx.beginPath()
          ctx.arc(bx, by, br, 0, Math.PI * 2)
          ctx.clip()
          ctx.drawImage(b.img, bx - br, by - br, br * 2, br * 2)
          ctx.restore()
          const overlay = ctx.createRadialGradient(bx - br * 0.3, by - br * 0.35, br * 0.05, bx, by, br)
          overlay.addColorStop(0, 'rgba(255,255,255,0.16)')
          overlay.addColorStop(0.6, 'rgba(0,0,0,0)')
          overlay.addColorStop(1, 'rgba(0,0,0,0.1)')
          ctx.beginPath()
          ctx.arc(bx, by, br, 0, Math.PI * 2)
          ctx.fillStyle = overlay
          ctx.fill()
          ctx.beginPath()
          ctx.arc(bx, by, br, 0, Math.PI * 2)
          ctx.strokeStyle = 'rgba(140, 110, 80, 0.18)'
          ctx.lineWidth = 0.5
          ctx.stroke()
        } else {
          // 隔片/配饰/文玩：保留原始宽高比，避免圆裁变形
          const iw = b.img.naturalWidth || 100, ih = b.img.naturalHeight || 100
          const aspect = iw / ih
          let dw = br * 2, dh = br * 2
          if (aspect >= 1) dh = br * 2 / aspect
          else dw = br * 2 * aspect
          ctx.drawImage(b.img, bx - dw / 2, by - dh / 2, dw, dh)
        }
      })
    } else {
      // ── 散落态：珠子错落摆放（同样区分主珠/异形素材） ──
      validBeads.forEach((b, i) => {
        const angle = (2 * Math.PI / n) * i + 0.3
        const dist = 6 + (i % 3) * 5
        const bx = cx + dist * Math.cos(angle)
        const by = cy + dist * Math.sin(angle)
        const br = 4 + (b.size / 10) * 2.5
        const isMain = beadMetaMap.get(b.path)?.categoryId === 'main-bead'

        if (isMain) {
          ctx.save()
          ctx.beginPath()
          ctx.arc(bx, by, br, 0, Math.PI * 2)
          ctx.clip()
          ctx.drawImage(b.img, bx - br, by - br, br * 2, br * 2)
          ctx.restore()
        } else {
          const iw = b.img.naturalWidth || 100, ih = b.img.naturalHeight || 100
          const aspect = iw / ih
          let dw = br * 2, dh = br * 2
          if (aspect >= 1) dh = br * 2 / aspect
          else dw = br * 2 * aspect
          ctx.drawImage(b.img, bx - dw / 2, by - dh / 2, dw, dh)
        }
      })
    }

    thumbnails[design.id] = canvas.toDataURL('image/png')
  }

  designThumbnails.value = thumbnails
}

/** 弹出命名弹窗 */
function promptSaveDesign() {
  if (!user.value) {
    engine.showToast('请先登录后再保存方案')
    openAuth('login')
    return
  }
  if (engine.beadCount.value === 0) {
    engine.showToast('请先添加珠子再保存')
    return
  }
  if (currentDesignId.value) {
    nameModalMode.value = 'overwrite'
  } else {
    nameModalMode.value = 'new'
  }
  nameModalInput.value = ''
  showNameModal.value = true
  nextTick(() => {
    const input = document.querySelector('.name-modal__input')
    if (input) input.focus()
  })
}

/** 确认保存 */
async function confirmSave(action) {
  const name = nameModalInput.value.trim()
  if (!name) {
    engine.showToast('请输入方案名称')
    return
  }
  let designId = null
  if (action === 'overwrite') {
    designId = currentDesignId.value
  }
  try {
    const payload = buildApiBeadPayload()
    const res = await braceletApi.saveDesign({
      designId,
      designName: name,
      braceleted: engine.isBracelet.value,
      beads: payload,
    })
    if (res.code === 200) {
      currentDesignId.value = res.data
      engine.showToast(action === 'overwrite' ? '方案已更新' : '方案已保存')
      showNameModal.value = false
      await loadDesigns()
    } else {
      engine.showToast(res.msg || '保存失败')
    }
  } catch (e) {
    console.error('保存异常:', e)
    engine.showToast('网络异常，保存失败')
  }
}

/** 回显设计方案到画布 */
async function restoreDesign(design) {
  if (!design || !design.beadListJson) return
  let parsed
  try {
    parsed = JSON.parse(design.beadListJson)
  } catch {
    engine.showToast('方案数据解析失败')
    return
  }
  const beads = parsed.beads || []
  if (!beads.length) {
    engine.showToast('方案内无珠子数据')
    return
  }
  engine.reset()
  const delay = 280
  beads.forEach((item, i) => {
    setTimeout(() => {
      const info = dbIdToBeadMap.get(item.beadId)
      if (!info) return
      const colorIndex = info.name.split('').reduce((s, c) => s + c.charCodeAt(0), 0)
      const colors = ['#E85D5D', '#F4A460', '#F0C75E', '#6BCB77', '#4D96FF', '#9B72CF', '#E97EB2', '#45B7D1']
      const meta = beadMetaMap.get(info.imagePath) || {}
      engine.launchBead(
        colors[colorIndex % colors.length],
        info.imagePath,
        item.size || meta.defaultSize || 10,
        meta.categoryId || 'main-bead',
        meta.subCategoryId || null,
      )
      if (i === beads.length - 1 && parsed.braceleted) {
        setTimeout(() => { engine.stringToBracelet() }, 900)
      }
    }, i * delay)
  })
  currentDesignId.value = design.id
  engine.showToast(`已恢复方案「${design.designName}」`)
}

/** 从 beadListJson 解析第一颗珠子的图片路径（用于历史列表缩略图） */
function getDesignPreviewBead(beadListJson) {
  if (!beadListJson) return null
  try {
    const parsed = JSON.parse(beadListJson)
    const beads = parsed.beads || []
    if (!beads.length) return null
    const info = dbIdToBeadMap.get(beads[0].beadId)
    return info ? info.imagePath : null
  } catch {
    return null
  }
}

// ==================== 方案分享码 ====================
const showShareModal = ref(false)
const shareModalMode = ref('gen')     // 'gen' = 展示已生成码 / 'load' = 输入码载入
const shareCodeResult = ref('')
const shareLoadInput = ref('')
const shareBusy = ref(false)

/** 生成方案分享码（同一方案复用同一码） */
async function openShareGen(design) {
  if (!user.value) {
    engine.showToast('请先登录后再分享方案')
    openAuth('login')
    return
  }
  try {
    const res = await braceletApi.createShareCode(design.id)
    if (res.code === 200) {
      shareCodeResult.value = res.data
      shareModalMode.value = 'gen'
      showShareModal.value = true
    } else {
      engine.showToast(res.msg || '分享码生成失败')
    }
  } catch (e) {
    console.error('分享码生成异常:', e)
    engine.showToast('网络异常，分享码生成失败')
  }
}

/** 复制分享码 */
function copyShareCode() {
  const code = shareCodeResult.value
  if (!code) return
  navigator.clipboard?.writeText(code).then(
    () => engine.showToast('分享码已复制'),
    () => engine.showToast('复制失败，请长按手动复制')
  )
}

/** 打开分享码载入弹窗 */
function openShareLoad() {
  if (!user.value) {
    engine.showToast('请先登录后再载入分享方案')
    openAuth('login')
    return
  }
  shareLoadInput.value = ''
  shareModalMode.value = 'load'
  showShareModal.value = true
  nextTick(() => {
    const input = document.querySelector('.share-modal__input')
    if (input) input.focus()
  })
}

/** 凭分享码载入好友方案 → 还原到工作台（对方方案载入次数 +1） */
async function confirmShareLoad() {
  if (shareBusy.value) return
  const code = shareLoadInput.value.trim().toUpperCase()
  if (!code) {
    engine.showToast('请输入分享码')
    return
  }
  shareBusy.value = true
  try {
    const res = await braceletApi.loadShareCode(code)
    if (res.code !== 200) {
      engine.showToast(res.msg || '分享码无效')
      return
    }
    const vo = res.data
    showShareModal.value = false
    await restoreDesign({
      id: null,   // 非本人方案：不接管 currentDesignId
      designName: vo.designName || '好友方案',
      beadListJson: vo.beadListJson,
    })
  } catch (e) {
    console.error('分享码载入异常:', e)
    engine.showToast('网络异常，载入失败')
  } finally {
    shareBusy.value = false
  }
}

// ==================== "正在使用" 动态子分类 ====================
const inUseSubCategories = computed(() => {
  const result = {}
  const canvasBeads = engine.beads.value || []

  // 为每个主分类收集正在使用的珠子
  categories.forEach(cat => {
    const inUsePaths = new Set()
    canvasBeads.forEach(b => {
      if (b.imagePath) {
        const meta = beadMetaMap.get(b.imagePath)
        if (meta && meta.categoryId === cat.id) {
          inUsePaths.add(b.imagePath)
        }
      }
    })
    const inUseSub = cat.subCategories?.find(s => s.id.endsWith('-in-use'))
    if (inUseSub) {
      result[cat.id] = [...inUsePaths].map(path => {
        // 从原数据中找到 name
        for (const sub of cat.subCategories) {
          const found = sub.items.find(item => item.path === path)
          if (found) return found
        }
        // fallback: 从 path 提取 name
        const parts = path.split('/')
        const fileName = parts[parts.length - 1]
        return { name: fileName.replace('.png', ''), path }
      })
    }
  })
  return result
})

// ==================== 珠子尺寸调节 ====================
const beadSizeMap = reactive({})

function getBeadSize(path) {
  if (beadSizeMap[path] === undefined) {
    const catMeta = beadMetaMap.get(path)
    beadSizeMap[path] = catMeta?.defaultSize ?? 10
  }
  return beadSizeMap[path]
}

function adjustBeadSize(path, delta, event) {
  event.stopPropagation()
  const cur = getBeadSize(path)
  const catMeta = beadMetaMap.get(path)
  const min = catMeta?.sizeMin ?? 8
  const max = catMeta?.sizeMax ?? 12
  beadSizeMap[path] = Math.max(min, Math.min(max, cur + delta))
  activeStep.value = 3
}

function selectCategory(catId) {
  activeCategory.value = catId
  const cat = categories.find(c => c.id === catId)
  if (cat?.subCategories?.length) {
    activeSubCategory.value = cat.subCategories[0].id
  } else {
    activeSubCategory.value = null
  }
}

function addBead(bead, event) {
  selectedBead.value = bead

  // 点击放大动画
  clickedBeadPath.value = bead.path
  setTimeout(() => { clickedBeadPath.value = null }, 300)

  const colorIndex = [...bead.name].reduce((sum, c) => sum + c.charCodeAt(0), 0)
  const BEAD_COLORS = [
    '#E85D5D', '#F4A460', '#F0C75E', '#6BCB77',
    '#4D96FF', '#9B72CF', '#E97EB2', '#45B7D1',
    '#FF6B6B', '#FFD93D', '#6BCB77', '#4ECDC4',
    '#FF8C42', '#C9B1FF', '#FFB3BA', '#7EC8E3',
    '#A0E7E5', '#FFAEBC', '#B4F8C8', '#FBE7C6',
    '#55C1E9', '#E8A0BF', '#BAE1C8', '#F8C471',
    '#D7BDE2',
  ]
  const color = BEAD_COLORS[colorIndex % BEAD_COLORS.length]

  const size = getBeadSize(bead.path)
  const meta = beadMetaMap.get(bead.path) || {}
  engine.launchBead(color, bead.path, size, meta.categoryId || 'main-bead', meta.subCategoryId || null)
}

const currentCategory = computed(() =>
  categories.find(c => c.id === activeCategory.value)
)

const currentSubCategory = computed(() => {
  const cat = currentCategory.value
  if (!cat) return null
  if (cat.subCategories) {
    const targetId = activeSubCategory.value
    let sub = cat.subCategories.find(s => s.id === targetId)
    if (!sub) {
      // fallback to first
      sub = cat.subCategories[0]
    }
    // 如果是 "正在使用" 子分类，使用动态数据
    if (sub && sub.id.endsWith('-in-use')) {
      const dynamicItems = inUseSubCategories.value[cat.id] || []
      return { ...sub, items: dynamicItems }
    }
    return sub
  }
  return cat
})

function getCategoryTotalCount(cat) {
  if (cat.subCategories) {
    return cat.subCategories.reduce((sum, sub) => {
      if (sub.id.endsWith('-in-use')) return sum
      return sum + sub.items.length
    }, 0)
  }
  return cat.items?.length ?? 0
}

// ==================== 页面视图 ====================
const currentView = ref('home')  // 'home' | 'workshop' | 'order'
const homePageRef = ref(null)
const workshopBodyRef = ref(null)
const transitionLeaving = ref(null) // 转场期间保留的旧视图（'home' | 'workshop' | null）

// 视图转场：旧页缩小后从上方划出；新页以相同缩小态从下方划入，再放大铺满，全程等速对称
let viewTransitionActive = false

function runViewTransition(leavingEl, enteringEl) {
  if (!leavingEl || !enteringEl) return
  // 中断上一次未完成的转场，清理残留行内样式
  if (viewTransitionActive) {
    const els = [homePageRef.value, workshopBodyRef.value].filter(Boolean)
    gsap.killTweensOf(els)
    els.forEach((el) => el.removeAttribute('style'))
    transitionLeaving.value = null
  }
  viewTransitionActive = true
  // 旧页脱离文档流并置于低层
  gsap.set(leavingEl, {
    position: 'absolute', inset: 0, width: '100%', height: '100%',
    zIndex: 5, borderRadius: '0px',
    boxShadow: '0 30px 80px rgba(60, 40, 25, 0)',
  })
  // 新页初始：以与旧页缩小后相同的形态藏在屏幕下方（scale 0.84 + 圆角 + 投影）
  gsap.set(enteringEl, {
    zIndex: 10, yPercent: 115, scale: 0.84, borderRadius: '28px',
    boxShadow: '0 40px 90px rgba(60, 40, 25, 0.3)',
  })
  const tl = gsap.timeline({
    onComplete: () => {
      enteringEl.removeAttribute('style')
      transitionLeaving.value = null
      viewTransitionActive = false
    },
  })
  // 阶段一：旧页缩小（1 → 0.84，圆角与投影浮现）
  tl.to(leavingEl, {
    duration: 0.35, ease: 'power2.inOut',
    scale: 0.84, borderRadius: '28px',
    boxShadow: '0 40px 90px rgba(60, 40, 25, 0.3)',
  }, 0)
  // 阶段二：垂直交换 — 旧页向上划出，新页等速从下方划入
  tl.to(leavingEl, { duration: 0.65, ease: 'power2.inOut', yPercent: -115 }, 0.35)
  tl.to(enteringEl, { duration: 0.65, ease: 'power2.inOut', yPercent: 0 }, 0.35)
  // 阶段三：新页放大铺满（0.84 → 1，圆角与投影收起）
  tl.to(enteringEl, {
    duration: 0.4, ease: 'power2.inOut',
    scale: 1, borderRadius: '0px',
    boxShadow: '0 30px 80px rgba(60, 40, 25, 0)',
  }, 1.0)
}

// 监听视图切换（离开作坊时销毁引擎，回来时重新挂载）
watch(currentView, async (view, oldView) => {
  // 首页 ↔ 作坊 双页并行转场
  const isHomeWorkshopSwap = (oldView === 'home' && view === 'workshop') || (oldView === 'workshop' && view === 'home')
  if (isHomeWorkshopSwap) {
    transitionLeaving.value = oldView
    await nextTick()
    const leavingEl = oldView === 'home' ? homePageRef.value : workshopBodyRef.value
    const enteringEl = view === 'home' ? homePageRef.value : workshopBodyRef.value
    runViewTransition(leavingEl, enteringEl)
    // 离开作坊时引擎销毁推迟到转场结束，转场期间画面保持渲染
    if (oldView === 'workshop') {
      setTimeout(() => {
        if (currentView.value !== 'workshop' && transitionLeaving.value !== 'workshop') {
          engine.destroy()
          engineMounted.value = false
        }
      }, 1000)
    }
  } else if (oldView === 'workshop' && view !== 'workshop') {
    engine.destroy()
    engineMounted.value = false
  }
  if (view === 'workshop') {
    if (!engineMounted.value) {
      await nextTick()
      engine.mount(canvasRef.value)
      engineMounted.value = true
    }
    // 每次进入作坊视图拉取已保存方案列表 & 刷新购物车角标
    loadDesigns()
    loadCartBadge()
  }
  if (view === 'order') {
    // 每次进入订单视图拉取购物车列表
    loadCart()
  }
})

function goToWorkshop() {
  currentView.value = 'workshop'
  activeNavTab.value = 'workshop'
}

function goToOrder() {
  if (!user.value) {
    engine.showToast('请先登录后查看购物车')
    openAuth('login')
    return
  }
  currentView.value = 'order'
  activeNavTab.value = 'order'
}

// ==================== 灵感页 ====================
const inspirationTab = ref('design-new') // 'design-new' | 'guest-order'
const inspirationThumbnails = ref({}) // index → dataURL

// 灵感页用的底盘背景
const inspirationTray = '/底盘/底盘.png'

// Mock 灵感数据 — 10 款手串
const inspirationItems = [
  {
    name: '清雅白玉', sizeMin: '14.5', sizeMax: '15.5', price: 168,
    author: '串珠匠人·墨白', tray: inspirationTray,
    beads: [
      '/水晶/白色系/和田白玉.png','/水晶/白色系/白月光.png','/水晶/白色系/白珍珠.png',
      '/水晶/白色系/白水晶.png','/水晶/白色系/白蝶贝.png','/水晶/白色系/白玉髓.png',
      '/水晶/白色系/白阿塞.png','/水晶/白色系/白幽灵.png',
    ],
  },
  {
    name: '朱砂红韵', sizeMin: '14.0', sizeMax: '15.0', price: 188,
    author: '串珠匠人·朱颜', tray: inspirationTray,
    beads: [
      '/水晶/红色系/朱砂.png','/水晶/红色系/红玛瑙.png','/水晶/红色系/南红玛瑙.png',
      '/水晶/红色系/石榴石.png','/水晶/红色系/红纹石.png','/水晶/红色系/草莓晶.png',
      '/水晶/红色系/红碧玺.png','/水晶/红色系/红兔毛.png',
    ],
  },
  {
    name: '碧海蓝天', sizeMin: '14.5', sizeMax: '15.5', price: 198,
    author: '串珠匠人·海韵', tray: inspirationTray,
    beads: [
      '/水晶/蓝色系/海蓝宝.png','/水晶/蓝色系/蓝月光.png','/水晶/蓝色系/蓝晶石.png',
      '/水晶/蓝色系/青金石.png','/水晶/蓝色系/天河石.png','/水晶/蓝色系/蓝纹玛瑙.png',
      '/水晶/蓝色系/蓝砂石.png','/水晶/蓝色系/坦桑石.png',
    ],
  },
  {
    name: '金玉满堂', sizeMin: '14.0', sizeMax: '15.0', price: 176,
    author: '串珠匠人·金羽', tray: inspirationTray,
    beads: [
      '/水晶/黄色系/黄水晶.png','/水晶/黄色系/金发晶.png','/水晶/黄色系/黄虎眼.png',
      '/水晶/黄色系/黄玛瑙.png','/水晶/黄色系/蜜蜡.png','/水晶/黄色系/太阳石.png',
      '/水晶/黄色系/柠檬黄晶.png','/水晶/黄色系/黄碧玺.png',
    ],
  },
  {
    name: '翠影流光', sizeMin: '14.5', sizeMax: '15.5', price: 218,
    author: '串珠匠人·青岚', tray: inspirationTray,
    beads: [
      '/水晶/绿色系/翡翠.png','/水晶/绿色系/东陵玉.png','/水晶/绿色系/绿幽灵.png',
      '/水晶/绿色系/绿发晶.png','/水晶/绿色系/橄榄石.png','/水晶/绿色系/葡萄石.png',
      '/水晶/绿色系/绿玉髓.png','/水晶/绿色系/孔雀石.png',
    ],
  },
  {
    name: '墨韵沉香', sizeMin: '15.0', sizeMax: '16.0', price: 258,
    author: '串珠匠人·沉木', tray: inspirationTray,
    beads: [
      '/水晶/黑色系/黑曜石.png','/水晶/黑色系/黑发晶.png','/水晶/黑色系/黑玛瑙.png',
      '/水晶/黑色系/茶晶.png','/水晶/黑色系/黑碧玺.png','/水晶/黑色系/黑月光.png',
      '/水晶/黑色系/金曜石.png','/水晶/黑色系/黑水晶.png',
    ],
  },
  {
    name: '粉黛佳人', sizeMin: '13.5', sizeMax: '14.5', price: 158,
    author: '串珠匠人·桃夭', tray: inspirationTray,
    beads: [
      '/水晶/红色系/粉玉髓.png','/水晶/红色系/冰粉晶.png','/水晶/红色系/粉月光.png',
      '/水晶/红色系/莫粉.png','/水晶/红色系/草莓晶.png','/水晶/红色系/马粉.png',
      '/水晶/红色系/星光粉.png','/水晶/红色系/粉欧珀.png',
    ],
  },
  {
    name: '紫气东来', sizeMin: '14.0', sizeMax: '15.0', price: 208,
    author: '串珠匠人·紫微', tray: inspirationTray,
    beads: [
      '/水晶/蓝色系/蓝晶石.png','/水晶/蓝色系/蓝月光.png','/水晶/蓝色系/堇青石.png',
      '/水晶/蓝色系/蓝纹玛瑙.png','/水晶/蓝色系/坦桑石.png','/水晶/蓝色系/蓝萤石.png',
      '/水晶/蓝色系/苏打石.png','/水晶/蓝色系/蓝磷辉.png',
    ],
  },
  {
    name: '暖阳橙光', sizeMin: '14.0', sizeMax: '15.0', price: 182,
    author: '串珠匠人·橙意', tray: inspirationTray,
    beads: [
      '/水晶/黄色系/橙月光.png','/水晶/黄色系/太阳石.png','/水晶/黄色系/黄水晶.png',
      '/水晶/黄色系/蜜蜡.png','/水晶/黄色系/橙红石榴.png','/水晶/黄色系/黄虎眼.png',
      '/水晶/黄色系/金发晶.png','/水晶/黄色系/柠檬黄晶.png',
    ],
  },
  {
    name: '春意盎然', sizeMin: '14.5', sizeMax: '15.5', price: 228,
    author: '串珠匠人·青绿', tray: inspirationTray,
    beads: [
      '/水晶/绿色系/绿幽灵.png','/水晶/绿色系/葡萄石.png','/水晶/绿色系/孔雀石.png',
      '/水晶/绿色系/橄榄石.png','/水晶/绿色系/祖母绿.png','/水晶/绿色系/绿发晶.png',
      '/水晶/绿色系/东陵玉.png','/水晶/绿色系/绿玉髓.png',
    ],
  },
  // 第二行
  {
    name: '冰晶雪韵', sizeMin: '14.0', sizeMax: '15.0', price: 192,
    author: '串珠匠人·冰雪', tray: inspirationTray,
    beads: [
      '/水晶/白色系/白水晶.png','/水晶/白色系/白月光.png','/水晶/白色系/白兔毛.png',
      '/水晶/白色系/和田白玉.png','/水晶/白色系/白阿塞.png','/水晶/白色系/白幽灵.png',
      '/水晶/白色系/白蝶贝.png','/水晶/白色系/白玉髓.png',
    ],
  },
  {
    name: '赤霞鎏金', sizeMin: '14.5', sizeMax: '15.5', price: 216,
    author: '串珠匠人·霞光', tray: inspirationTray,
    beads: [
      '/水晶/红色系/红玛瑙.png','/水晶/红色系/石榴石.png','/水晶/红色系/红发晶.png',
      '/水晶/红色系/红碧玺.png','/水晶/红色系/朱砂.png','/水晶/红色系/红幽灵.png',
      '/水晶/红色系/红胶花.png','/水晶/红色系/南红玛瑙.png',
    ],
  },
  {
    name: '深海幽蓝', sizeMin: '14.0', sizeMax: '15.0', price: 205,
    author: '串珠匠人·深海', tray: inspirationTray,
    beads: [
      '/水晶/蓝色系/蓝晶石.png','/水晶/蓝色系/海蓝宝.png','/水晶/蓝色系/蓝月光.png',
      '/水晶/蓝色系/蓝萤石.png','/水晶/蓝色系/蓝纹玛瑙.png','/水晶/蓝色系/蓝砂石.png',
      '/水晶/蓝色系/蓝虎眼.png','/水晶/蓝色系/坦桑石.png',
    ],
  },
  {
    name: '琥珀流光', sizeMin: '14.5', sizeMax: '15.5', price: 188,
    author: '串珠匠人·琥珀', tray: inspirationTray,
    beads: [
      '/水晶/黄色系/蜜蜡.png','/水晶/黄色系/黄水晶.png','/水晶/黄色系/太阳石.png',
      '/水晶/黄色系/金发晶.png','/水晶/黄色系/黄虎眼.png','/水晶/黄色系/柠檬黄晶.png',
      '/水晶/黄色系/黄玛瑙.png','/水晶/黄色系/黄碧玺.png',
    ],
  },
  {
    name: '翠竹清风', sizeMin: '14.0', sizeMax: '15.0', price: 198,
    author: '串珠匠人·竹韵', tray: inspirationTray,
    beads: [
      '/水晶/绿色系/翡翠.png','/水晶/绿色系/绿幽灵.png','/水晶/绿色系/东陵玉.png',
      '/水晶/绿色系/绿发晶.png','/水晶/绿色系/葡萄石.png','/水晶/绿色系/橄榄石.png',
      '/水晶/绿色系/孔雀石.png','/水晶/绿色系/绿玉髓.png',
    ],
  },
  {
    name: '墨夜星辰', sizeMin: '15.0', sizeMax: '16.0', price: 232,
    author: '串珠匠人·星辰', tray: inspirationTray,
    beads: [
      '/水晶/黑色系/黑曜石.png','/水晶/黑色系/黑发晶.png','/水晶/黑色系/金曜石.png',
      '/水晶/黑色系/黑水晶.png','/水晶/黑色系/茶晶.png','/水晶/黑色系/黑碧玺.png',
      '/水晶/黑色系/黑玛瑙.png','/水晶/黑色系/黑月光.png',
    ],
  },
  {
    name: '桃色芳华', sizeMin: '13.5', sizeMax: '14.5', price: 175,
    author: '串珠匠人·芳华', tray: inspirationTray,
    beads: [
      '/水晶/红色系/粉玉髓.png','/水晶/红色系/莫粉.png','/水晶/红色系/冰粉晶.png',
      '/水晶/红色系/马粉.png','/水晶/红色系/星光粉.png','/水晶/红色系/粉月光.png',
      '/水晶/红色系/草莓晶.png','/水晶/红色系/蔷薇石.png',
    ],
  },
  {
    name: '紫穹星河', sizeMin: '14.0', sizeMax: '15.0', price: 220,
    author: '串珠匠人·星河', tray: inspirationTray,
    beads: [
      '/水晶/蓝色系/堇青石.png','/水晶/蓝色系/坦桑石.png','/水晶/蓝色系/蓝月光.png',
      '/水晶/蓝色系/苏打石.png','/水晶/蓝色系/蓝晶石.png','/水晶/蓝色系/蓝萤石.png',
      '/水晶/蓝色系/海蓝宝.png','/水晶/蓝色系/蓝纹玛瑙.png',
    ],
  },
  {
    name: '金秋硕果', sizeMin: '14.5', sizeMax: '15.5', price: 186,
    author: '串珠匠人·秋实', tray: inspirationTray,
    beads: [
      '/水晶/黄色系/橙月光.png','/水晶/黄色系/太阳石.png','/水晶/黄色系/黄水晶.png',
      '/水晶/黄色系/金发晶.png','/水晶/黄色系/橙红石榴.png','/水晶/黄色系/黄虎眼.png',
      '/水晶/黄色系/蜜蜡.png','/水晶/黄色系/柠檬黄晶.png',
    ],
  },
  {
    name: '碧波清影', sizeMin: '14.0', sizeMax: '15.0', price: 210,
    author: '串珠匠人·清波', tray: inspirationTray,
    beads: [
      '/水晶/绿色系/绿幽灵.png','/水晶/绿色系/葡萄石.png','/水晶/绿色系/绿发晶.png',
      '/水晶/绿色系/东陵玉.png','/水晶/绿色系/橄榄石.png','/水晶/绿色系/翡翠.png',
      '/水晶/绿色系/绿玉髓.png','/水晶/绿色系/孔雀石.png',
    ],
  },
]

// ==================== 灵感成品搭配编排 ====================
// 每款构成：主珠(beads[0]) + 点缀珠(beads[1]) + 隔珠 + 挂件
// 隔珠/挂件使用 bead_info 表自增 ID（与 seed.sql 插入顺序一致）：
//   151 白水晶跑环 / 152 紫水晶跑环 / 153 蓝水晶跑环 / 154 叶纹筒 / 155 珍珠隔
//   156 白色金隔片 / 158 红色金隔片 / 161 金色隔片
//   166 好事发生(24mm) / 167 繁花(24) / 169 银色树叶(24) / 170 银色流苏(24)
//   171 玛瑙福袋(16) / 172 祥云坠(16) / 173 紫晶福袋(16) / 174 莲蓬(16) / 175 金圆坠(16)
//   176 玛瑙铃兰(12) / 177 金花(12)
const inspirationRecipes = [
  { mainCount: 12, mainSize: 12, spacerId: 153, spacerCount: 2, charmId: 169, charmSize: 24, date: '2026-04-08' }, // 清雅白玉
  { mainCount: 14, mainSize: 12, spacerId: 161, spacerCount: 2, charmId: 166, charmSize: 24, date: '2026-03-22' }, // 朱砂红韵
  { mainCount: 12, mainSize: 12, spacerId: 151, spacerCount: 2, charmId: 172, charmSize: 16, date: '2026-04-15' }, // 碧海蓝天
  { mainCount: 12, mainSize: 12, spacerId: 161, spacerCount: 2, charmId: 175, charmSize: 16, date: '2026-03-30' }, // 金玉满堂
  { mainCount: 12, mainSize: 12, spacerId: 154, spacerCount: 2, charmId: 174, charmSize: 16, date: '2026-04-20' }, // 翠影流光
  { mainCount: 14, mainSize: 12, spacerId: 156, spacerCount: 2, charmId: 170, charmSize: 24, date: '2026-03-12' }, // 墨韵沉香
  { mainCount: 12, mainSize: 12, spacerId: 155, spacerCount: 2, charmId: 176, charmSize: 12, date: '2026-05-06' }, // 粉黛佳人
  { mainCount: 12, mainSize: 12, spacerId: 152, spacerCount: 2, charmId: 173, charmSize: 16, date: '2026-04-02' }, // 紫气东来
  { mainCount: 12, mainSize: 12, spacerId: 161, spacerCount: 2, charmId: 177, charmSize: 12, date: '2026-05-01' }, // 暖阳橙光
  { mainCount: 12, mainSize: 12, spacerId: 154, spacerCount: 2, charmId: 167, charmSize: 24, date: '2026-04-25' }, // 春意盎然
  { mainCount: 12, mainSize: 10, spacerId: 151, spacerCount: 2, charmId: 171, charmSize: 16, date: '2026-05-09' }, // 冰晶雪韵
  { mainCount: 14, mainSize: 12, spacerId: 158, spacerCount: 2, charmId: 166, charmSize: 24, date: '2026-03-18' }, // 赤霞鎏金
  { mainCount: 12, mainSize: 10, spacerId: 153, spacerCount: 2, charmId: 172, charmSize: 16, date: '2026-04-28' }, // 深海幽蓝
  { mainCount: 12, mainSize: 12, spacerId: 161, spacerCount: 2, charmId: 175, charmSize: 16, date: '2026-05-02' }, // 琥珀流光
  { mainCount: 12, mainSize: 10, spacerId: 154, spacerCount: 2, charmId: 174, charmSize: 16, date: '2026-04-10' }, // 翠竹清风
  { mainCount: 14, mainSize: 12, spacerId: 156, spacerCount: 2, charmId: 170, charmSize: 24, date: '2026-03-25' }, // 墨夜星辰
  { mainCount: 12, mainSize: 10, spacerId: 155, spacerCount: 2, charmId: 167, charmSize: 24, date: '2026-05-13' }, // 桃色芳华
  { mainCount: 12, mainSize: 10, spacerId: 152, spacerCount: 2, charmId: 173, charmSize: 16, date: '2026-04-05' }, // 紫穹星河
  { mainCount: 12, mainSize: 10, spacerId: 161, spacerCount: 2, charmId: 177, charmSize: 12, date: '2026-04-30' }, // 金秋硕果
  { mainCount: 12, mainSize: 10, spacerId: 154, spacerCount: 2, charmId: 167, charmSize: 24, date: '2026-05-11' }, // 碧波清影
]

// 实况图路径约定：image/实况/{两位序号}/1.jpg ~ 3.jpg（用户提供后自动显示）
const pad2 = n => String(n).padStart(2, '0')

inspirationItems.forEach((item, i) => {
  const recipe = inspirationRecipes[i]
  item.globalIdx = i
  const mainId = beadMetaMap.get(item.beads[0])?.dbId
  const accentId = beadMetaMap.get(item.beads[1])?.dbId
  item.components = [
    { beadId: mainId, count: recipe.mainCount, size: recipe.mainSize },
    { beadId: accentId, count: 1, size: recipe.mainSize },
    { beadId: recipe.spacerId, count: recipe.spacerCount, size: 8 },
    { beadId: recipe.charmId, count: 1, size: recipe.charmSize },
  ]
  item.date = recipe.date
  item.livePhotos = [1, 2, 3].map(n => `/实况/${pad2(i + 1)}/${n}.jpg`)
})

// 按分类筛选：优先展示后端上架灵感（DB 条目带 id），无数据时回退内置样例（设计新款 0-9 / 优秀客订 10-19）
const dbInspirationRev = ref(0)   // DB 条目追加信号（inspirationItems 为普通数组，手动撬动重算）
const dbDesignItems = computed(() => {
  dbInspirationRev.value
  return inspirationItems.filter(it => it.id && it.type === 'design')
})
const dbGuestItems = computed(() => {
  dbInspirationRev.value
  return inspirationItems.filter(it => it.id && it.type === 'guest')
})
const filteredInspiration = computed(() => {
  if (inspirationTab.value === 'design-new') {
    return dbDesignItems.value.length ? dbDesignItems.value : inspirationItems.slice(0, 10)
  }
  return dbGuestItems.value.length ? dbGuestItems.value : inspirationItems.slice(10, 20)
})

/** 拉取后端灵感集录 → 归一化后追加到列表尾部（globalIdx 续接内置样例） */
async function loadInspirations() {
  try {
    const res = await braceletApi.listInspirations()
    if (res.code !== 200 || !res.data) return
    inspirationItems.splice(20)   // 清掉旧 DB 条目，保留内置样例 0-19
    let nextIdx = 20
    ;[...(res.data.design || []), ...(res.data.guest || [])].forEach(rec => {
      const item = normalizeInspiration(rec)
      item.globalIdx = nextIdx++
      inspirationItems.push(item)
    })
    dbInspirationRev.value++
    // 缩略图已生成过（DB 条目晚于首绘）→ 重建全部缩略图
    if (Object.keys(inspirationThumbnails.value).length > 0) {
      inspirationThumbnails.value = {}
      generateInspirationThumbnails()
    }
  } catch (e) {
    console.warn('灵感列表接口不可用，使用内置样例:', e?.message)
  }
}

/** 后端灵感记录 → 页面条目（components 分组 / beads 路径序列 / 日期） */
function normalizeInspiration(rec) {
  let parsed = {}
  try { parsed = JSON.parse(rec.beadsJson || '{}') } catch {}
  const beads = Array.isArray(parsed.beads) ? parsed.beads : []
  const grouped = new Map()
  const components = []
  beads.forEach(b => {
    const key = b.beadId + '-' + b.size
    if (grouped.has(key)) grouped.get(key).count++
    else {
      const c = { beadId: b.beadId, size: b.size, count: 1 }
      grouped.set(key, c)
      components.push(c)
    }
  })
  return {
    id: rec.id,
    type: rec.type,
    name: rec.name,
    price: Number(rec.price) || 0,
    author: rec.author || '官方设计师',
    tray: inspirationTray,
    sizeMin: rec.wristMin != null ? Number(rec.wristMin).toFixed(1) : '—',
    sizeMax: rec.wristMax != null ? Number(rec.wristMax).toFixed(1) : '—',
    beads: beads.map(b => dbIdToBeadMap.get(b.beadId)?.imagePath).filter(Boolean),
    components,
    date: String(rec.createTime || '').slice(0, 10),
    livePhotos: [],
  }
}

// 进入灵感页时生成缩略图
async function generateInspirationThumbnails() {
  const thumbnails = {}
  const THUMB_SIZE = 284        // 等比例放大历史面板的 68px → 适配 240px 底盘
  const BRACELET_R = 92         // 22 × (284/68) ≈ 92
  const SCALE = 284 / 68        // ≈ 4.176

  for (let idx = 0; idx < inspirationItems.length; idx++) {
    const item = inspirationItems[idx]
    const beadInfos = item.beads.map(path => {
      const meta = beadMetaMap.get(path)
      return { imagePath: path, size: meta?.defaultSize || 10 }
    })

    const loaded = await Promise.all(
      beadInfos.map(info => {
        return new Promise((resolve) => {
          const img = new Image()
          img.crossOrigin = 'anonymous'
          img.src = info.imagePath
          img.onload = () => resolve({ img, size: info.size, path: info.imagePath })
          img.onerror = () => resolve(null)
        })
      })
    )
    const validBeads = loaded.filter(Boolean)
    if (!validBeads.length) continue

    const canvas = document.createElement('canvas')
    canvas.width = THUMB_SIZE
    canvas.height = THUMB_SIZE
    const ctx = canvas.getContext('2d')

    const cx = THUMB_SIZE / 2
    const cy = THUMB_SIZE / 2
    const n = validBeads.length

    // 画浅色环底（与历史面板一致，等比例放大）
    ctx.beginPath()
    ctx.arc(cx, cy, BRACELET_R + 2, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(180, 160, 140, 0.3)'
    ctx.lineWidth = 1 * SCALE
    ctx.stroke()

    // 珠子绕环排列（主珠圆裁球面渲染；隔片/配饰/文玩按 PNG 原始宽高比；珠多时自动收缩防重叠）
    validBeads.forEach((b, i) => {
      const angle = Math.PI / 2 + (2 * Math.PI / n) * i
      const bx = cx + BRACELET_R * Math.cos(angle)
      const by = cy + BRACELET_R * Math.sin(angle)
      const br0 = (5 + (b.size / 10) * 3) * SCALE               // 标称珠径（与历史面板同口径）
      const fit = BRACELET_R * Math.sin(Math.PI / n) * 0.95     // 相邻珠不重叠上限
      const br = Math.min(br0, fit)
      const isMain = beadMetaMap.get(b.path)?.categoryId === 'main-bead'

      if (isMain) {
        // 投影
        ctx.save()
        ctx.beginPath()
        ctx.ellipse(bx, by + br * 0.35, br * 0.95, br * 0.6, 0, 0, Math.PI * 2)
        ctx.fillStyle = 'rgba(90, 70, 50, 0.14)'
        ctx.fill()
        ctx.restore()
        // 本体（圆裁）
        ctx.save()
        ctx.beginPath()
        ctx.arc(bx, by, br, 0, Math.PI * 2)
        ctx.clip()
        ctx.drawImage(b.img, bx - br, by - br, br * 2, br * 2)
        ctx.restore()
        // 球面高光
        const overlay = ctx.createRadialGradient(bx - br * 0.3, by - br * 0.35, br * 0.05, bx, by, br)
        overlay.addColorStop(0, 'rgba(255,255,255,0.16)')
        overlay.addColorStop(0.6, 'rgba(0,0,0,0)')
        overlay.addColorStop(1, 'rgba(0,0,0,0.1)')
        ctx.beginPath()
        ctx.arc(bx, by, br, 0, Math.PI * 2)
        ctx.fillStyle = overlay
        ctx.fill()
        // 微细描边模拟串线感
        ctx.beginPath()
        ctx.arc(bx, by, br, 0, Math.PI * 2)
        ctx.strokeStyle = 'rgba(140, 110, 80, 0.18)'
        ctx.lineWidth = 0.5 * SCALE
        ctx.stroke()
      } else {
        // 隔片/配饰/文玩：保留原始宽高比，避免圆裁变形
        const iw = b.img.naturalWidth || 100, ih = b.img.naturalHeight || 100
        const aspect = iw / ih
        let dw = br * 2, dh = br * 2
        if (aspect >= 1) dh = br * 2 / aspect
        else dw = br * 2 * aspect
        ctx.drawImage(b.img, bx - dw / 2, by - dh / 2, dw, dh)
      }
    })

    thumbnails[idx] = canvas.toDataURL('image/png')
  }
  inspirationThumbnails.value = thumbnails
}

// ==================== 灵感详情页 ====================
const beadInfoMap = ref(new Map())   // dbId → bead_info 记录 { id, beadName, price, imgUrl, ... }
const beadInfoLoaded = ref(false)
const inspirationDetail = ref(null)  // 当前查看的灵感成品
const inspirationDetailIdx = ref(-1) // 全局索引（0~19）
const detailStats = ref(null)        // 后端统计 { beadCount, totalCircumference, wristMin, wristMax, totalPrice }
const detailBigPhoto = ref('')       // Canvas 合成成品大图
const detailPhotoLoading = ref(false)
const detailLiveDir = computed(() => pad2(inspirationDetailIdx.value + 1))

/** 拉取珠子信息，构建 dbId → 信息映射（名称/单价来自数据库 bead_info） */
async function ensureBeadInfoLoaded() {
  if (beadInfoLoaded.value) return
  try {
    const res = await braceletApi.listBeads()
    if (res.code === 200 && Array.isArray(res.data)) {
      const map = new Map()
      res.data.forEach(b => map.set(b.id, b))
      beadInfoMap.value = map
      beadInfoLoaded.value = true
    }
  } catch (e) {
    console.error('加载珠子信息失败:', e)
  }
}

/** 展开构成明细为逐颗珠子数组 [{beadId, size}] */
function expandComponents(components) {
  const list = []
  components.forEach(c => {
    for (let i = 0; i < c.count; i++) list.push({ beadId: c.beadId, size: c.size })
  })
  return list
}

/** 构成解析行：合并数据库名称/单价与本地明细 */
const detailComponents = computed(() => {
  const item = inspirationDetail.value
  if (!item) return []
  return item.components.map(c => {
    const info = beadInfoMap.value.get(c.beadId)
    const fallback = dbIdToBeadMap.get(c.beadId)
    const unit = info ? Number(info.price) : 0
    return {
      beadId: c.beadId,
      name: info?.beadName || fallback?.name || '未知组件',
      imgUrl: info?.imgUrl || fallback?.imagePath || '',
      size: c.size,
      count: c.count,
      unitPrice: unit,
      subtotal: +(unit * c.count).toFixed(1),
    }
  })
})

const detailBeadCount = computed(() => {
  if (detailStats.value?.beadCount != null) return detailStats.value.beadCount
  return detailComponents.value.reduce((s, c) => s + c.count, 0)
})

/** 建议手围：优先后端统计，后端不可用时按周长本地兜底 */
const detailWrist = computed(() => {
  const s = detailStats.value
  if (s?.wristMin != null && s?.wristMax != null) {
    return { min: Number(s.wristMin), max: Number(s.wristMax) }
  }
  const circ = detailComponents.value.reduce((sum, c) => sum + c.size * c.count, 0) / 10
  return { min: +(circ * 0.95).toFixed(1), max: +(circ * 1.02).toFixed(1) }
})

const detailTotalPrice = computed(() => {
  // 灵感自带展示售价（admin 端定价）优先；未定价(0)或 mock 无价时回退按珠成本
  const cur = inspirationDetail.value
  if (cur && Number(cur.price) > 0) return parseFloat(Number(cur.price).toFixed(1))
  const v = detailStats.value?.totalPrice != null
    ? Number(detailStats.value.totalPrice)
    : detailComponents.value.reduce((s, c) => s + c.subtotal, 0)
  return parseFloat(v.toFixed(1))
})

/** 手围范围条高亮段（轨道 13~20cm） */
const detailRangeStyle = computed(() => {
  const lo = Math.max(0, Math.min(100, ((detailWrist.value.min - 13) / 7) * 100))
  const hi = Math.max(0, Math.min(100, ((detailWrist.value.max - 13) / 7) * 100))
  return { left: lo + '%', width: Math.max(2, hi - lo) + '%' }
})

/** 打开灵感详情 */
async function openInspirationDetail(globalIdx) {
  const item = inspirationItems[globalIdx]
  if (!item) return
  inspirationDetailIdx.value = globalIdx
  inspirationDetail.value = item
  currentView.value = 'inspiration-detail'
  detailStats.value = null
  detailBigPhoto.value = ''

  await ensureBeadInfoLoaded()

  // 统计数据（后端不可用时静默降级为本地计算）
  try {
    const res = await braceletApi.getCountData(expandComponents(item.components))
    if (res.code === 200) detailStats.value = res.data
  } catch (e) {
    console.warn('灵感统计接口不可用，使用本地兜底:', e?.message)
  }

  // Canvas 合成成品大图
  detailPhotoLoading.value = true
  detailBigPhoto.value = await renderInspirationDetailPhoto(item)
  detailPhotoLoading.value = false
}

/** 返回灵感集录 */
function closeInspirationDetail() {
  currentView.value = 'inspiration'
}

/** 切换到上一款（循环） */
function prevInspiration() {
  const total = inspirationItems.length
  openInspirationDetail((inspirationDetailIdx.value - 1 + total) % total)
}

/**
 * Canvas 合成详情页成品大图：底盘 + 珠子按构成明细绕环排列
 * 渲染逻辑与工作台引擎一致：
 *   主珠 → 圆形裁剪 + 球形光照
 *   隔片/配饰/文玩 → 按图片原始宽高比绘制（保留实物形状），随环上角度旋转
 *   挂件 → 顶部连接环对齐串线，整体向外垂坠
 * 比例 6px/mm（12mm 主珠 → 半径 36px），画布 640×640
 */
async function renderInspirationDetailPhoto(item) {
  const SIZE = 640
  const PX_PER_MM = 6

  // 并行加载珠子图（路径来自前端素材索引，与 dbId 对应）
  const flat = expandComponents(item.components)
  const loaded = await Promise.all(flat.map(b => {
    const path = dbIdToBeadMap.get(b.beadId)?.imagePath
    if (!path) return Promise.resolve(null)
    return new Promise(resolve => {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.src = path
      img.onload = () => resolve({ img, size: b.size, path })
      img.onerror = () => resolve(null)
    })
  }))
  const beadsRaw = loaded.filter(Boolean)
  if (!beadsRaw.length) return ''

  // 附加类别元数据与绘制尺寸（与引擎渲染规则一致）
  const beads = beadsRaw.map(b => {
    const meta = beadMetaMap.get(b.path) || {}
    const r = (b.size * PX_PER_MM) / 2
    const isNonBead = meta.categoryId && meta.categoryId !== 'main-bead'
    let dw = r * 2, dh = r * 2
    if (isNonBead) {
      const iw = b.img.naturalWidth || b.img.width || 100
      const ih = b.img.naturalHeight || b.img.height || 100
      const aspect = iw / ih
      if (aspect >= 1) {
        dw = r * 2
        dh = r * 2 / aspect
      } else {
        dh = r * 2
        dw = r * 2 * aspect
      }
    }
    return { ...b, r, isNonBead, isPendant: meta.subCategoryId === 'acc-pendant', dw, dh }
  })

  // 底盘背景
  const trayImg = await new Promise(resolve => {
    const img = new Image()
    img.src = item.tray
    img.onload = () => resolve(img)
    img.onerror = () => resolve(null)
  })

  const canvas = document.createElement('canvas')
  canvas.width = SIZE
  canvas.height = SIZE
  const ctx = canvas.getContext('2d')

  if (trayImg) {
    ctx.drawImage(trayImg, 0, 0, SIZE, SIZE)
  } else {
    // 底盘加载失败时绘制米色圆盘兜底
    ctx.fillStyle = '#EFE7DA'
    ctx.beginPath()
    ctx.arc(SIZE / 2, SIZE / 2, SIZE / 2 - 12, 0, Math.PI * 2)
    ctx.fill()
  }

  const cx = SIZE / 2
  const cy = SIZE / 2

  // 环半径：按珠子占用周长计算；若挂件外垂导致溢出则整串等比缩小
  const periphery = beads.reduce((s, b) => s + b.r * 2, 0)
  const idealR = periphery / (2 * Math.PI)
  const maxOuter = Math.max(...beads.map(b =>
    b.isPendant ? b.dh : Math.max(b.dw, b.dh) / 2
  ))
  let R = Math.min(idealR, SIZE / 2 - 26 - maxOuter)
  const fitScale = R / idealR
  if (fitScale < 1) {
    beads.forEach(b => {
      b.r *= fitScale
      b.dw *= fitScale
      b.dh *= fitScale
    })
  }
  R = Math.max(120, R)

  beads.forEach((b, i) => {
    const angle = Math.PI / 2 + (2 * Math.PI / beads.length) * i
    const bx = cx + R * Math.cos(angle)
    const by = cy + R * Math.sin(angle)
    // 珠子在环上的角度，图片"顶部"始终指向圆心（与引擎一致）
    const beadAngle = Math.atan2(by - cy, bx - cx)
    const spin = beadAngle - Math.PI / 2

    if (b.isNonBead && b.img) {
      // ── 隔片/配饰/文玩：按真实形状绘制，不圆形裁剪 ──
      let px = bx, py = by
      if (b.isPendant) {
        // 挂件：中心向外偏移半高，顶部连接环贴合串线
        px = bx + (b.dh / 2) * Math.cos(beadAngle)
        py = by + (b.dh / 2) * Math.sin(beadAngle)
      }

      // 底部投影
      ctx.save()
      ctx.translate(px, py)
      ctx.rotate(spin)
      ctx.shadowColor = 'rgba(0, 0, 0, 0.28)'
      ctx.shadowBlur = b.r * 0.3
      ctx.shadowOffsetX = 1.5
      ctx.shadowOffsetY = 2
      ctx.drawImage(b.img, -b.dw / 2, -b.dh / 2, b.dw, b.dh)
      ctx.restore()

      // 表面微光
      ctx.save()
      ctx.translate(px, py)
      ctx.rotate(spin)
      ctx.globalCompositeOperation = 'source-atop'
      const hl = ctx.createLinearGradient(-b.dw / 2, -b.dh / 2, -b.dw / 2, b.dh / 2)
      hl.addColorStop(0, 'rgba(255,255,255,0.12)')
      hl.addColorStop(0.4, 'rgba(255,255,255,0.03)')
      hl.addColorStop(1, 'rgba(0,0,0,0.05)')
      ctx.fillStyle = hl
      ctx.fillRect(-b.dw / 2, -b.dh / 2, b.dw, b.dh)
      ctx.restore()
    } else {
      // ── 主珠：圆形裁剪 + 纹理旋转 + 球形光照 ──
      // 投影
      ctx.save()
      ctx.beginPath()
      ctx.ellipse(bx, by + b.r * 0.35, b.r * 0.95, b.r * 0.6, 0, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(90, 70, 50, 0.18)'
      ctx.filter = 'blur(4px)'
      ctx.fill()
      ctx.restore()

      // 本体
      ctx.save()
      ctx.beginPath()
      ctx.arc(bx, by, b.r, 0, Math.PI * 2)
      ctx.clip()
      ctx.translate(bx, by)
      ctx.rotate(spin)
      ctx.drawImage(b.img, -b.r, -b.r, b.r * 2, b.r * 2)
      ctx.restore()

      // 球形光照（固定光源方向，左上亮右下暗）
      ctx.save()
      const overlay = ctx.createRadialGradient(
        bx - b.r * 0.3, by - b.r * 0.35, b.r * 0.05,
        bx, by, b.r
      )
      overlay.addColorStop(0, 'rgba(255,255,255,0.18)')
      overlay.addColorStop(0.6, 'rgba(0,0,0,0)')
      overlay.addColorStop(1, 'rgba(0,0,0,0.12)')
      ctx.beginPath()
      ctx.arc(bx, by, b.r, 0, Math.PI * 2)
      ctx.fillStyle = overlay
      ctx.fill()
      ctx.restore()
    }
  })

  return canvas.toDataURL('image/png')
}

/** 应用灵感设计 → 载入工作台，逐颗发射后收拢成环 */
async function applyInspirationDesign() {
  const item = inspirationDetail.value
  if (!item) return
  currentView.value = 'workshop'
  activeNavTab.value = 'workshop'
  await nextTick()
  engine.reset()
  const flat = expandComponents(item.components)
  const delay = 260
  flat.forEach((b, i) => {
    setTimeout(() => {
      const info = dbIdToBeadMap.get(b.beadId)
      if (!info) return
      const colorIndex = info.name.split('').reduce((s, c) => s + c.charCodeAt(0), 0)
      const colors = ['#E85D5D', '#F4A460', '#F0C75E', '#6BCB77', '#4D96FF', '#9B72CF', '#E97EB2', '#45B7D1']
      const meta = beadMetaMap.get(info.imagePath) || {}
      engine.launchBead(
        colors[colorIndex % colors.length],
        info.imagePath,
        b.size,
        meta.categoryId || 'main-bead',
        meta.subCategoryId || null,
      )
      if (i === flat.length - 1) {
        setTimeout(() => { engine.stringToBracelet() }, 900)
      }
    }, i * delay)
  })
  engine.showToast(`已应用灵感「${item.name}」，可继续编辑`)
  if (item.id) {
    braceletApi.useInspiration(item.id).catch(() => {})   // 被应用计数（静默）
  }
}

function goToInspiration() {
  currentView.value = 'inspiration'
  activeNavTab.value = 'inspiration'
  nextTick(() => {
    loadInspirations()
    if (Object.keys(inspirationThumbnails.value).length === 0) {
      generateInspirationThumbnails()
    }
  })
}

function goToPlan() {
  if (!user.value) {
    engine.showToast('请先登录后查看我的方案')
    openAuth('login')
    return
  }
  currentView.value = 'plan'
  activeNavTab.value = 'plan'
  nextTick(() => {
    loadDesigns()
  })
}

function goToMine() {
  currentView.value = 'mine'
  activeNavTab.value = 'mine'
  if (user.value) {
    loadAddresses()
    loadDesigns()   // 创作数据：方案列表含 shareUseCount
  }
}

// ==================== 用户登录 ====================
const user = ref(null)
try {
  user.value = JSON.parse(localStorage.getItem('bracelet-user') || 'null')
} catch {
  user.value = null
}

const showAuthModal = ref(false)
const authMode = ref('login') // 'login' | 'register'
const authSubmitting = ref(false)
const authForm = reactive({ username: '', password: '', nickname: '' })

function openAuth(mode = 'login') {
  authMode.value = mode
  showAuthModal.value = true
}

function closeAuthModal() {
  showAuthModal.value = false
  authForm.password = ''
}

function switchAuthMode(mode) {
  authMode.value = mode
  authForm.password = ''
}

/** 登录 / 注册提交 */
async function submitAuth() {
  if (authSubmitting.value) return
  const username = authForm.username.trim()
  if (!username) {
    engine.showToast('请输入用户名')
    return
  }
  if (!authForm.password) {
    engine.showToast('请输入密码')
    return
  }
  if (authMode.value === 'register' && authForm.password.length < 6) {
    engine.showToast('密码至少 6 位')
    return
  }
  authSubmitting.value = true
  try {
    const dto = { username, password: authForm.password }
    if (authMode.value === 'register' && authForm.nickname.trim()) {
      dto.nickname = authForm.nickname.trim()
    }
    const res = authMode.value === 'login'
      ? await braceletApi.login(dto)
      : await braceletApi.register(dto)
    if (res.code === 200 && res.data) {
      setToken(res.data.token)
      const info = { userId: res.data.userId, username: res.data.username, nickname: res.data.nickname, avatar: res.data.avatar, role: res.data.role || 'user' }
      user.value = info
      localStorage.setItem('bracelet-user', JSON.stringify(info))
      // 管理员 → 跳转管理面板
      if (info.role === 'admin') {
        engine.showToast('管理员身份验证通过，正在进入管理面板…')
        setTimeout(() => { window.location.href = '/admin.html#/overview' }, 600)
        return
      }
      engine.showToast(authMode.value === 'login' ? `欢迎回来，${info.nickname}` : '注册成功，已自动登录')
      closeAuthModal()
      loadCartBadge()
      loadAddresses()
    } else {
      engine.showToast(res.msg || (authMode.value === 'login' ? '登录失败' : '注册失败'))
    }
  } catch (e) {
    console.error('登录/注册异常:', e)
    engine.showToast('网络异常，请稍后再试')
  } finally {
    authSubmitting.value = false
  }
}

/** 退出登录 */
async function logoutUser() {
  try {
    await braceletApi.logout()
  } catch {
    // JWT 无状态，忽略
  }
  setToken('')
  user.value = null
  localStorage.removeItem('bracelet-user')
  cartItems.value = []
  addresses.value = []
  checkoutAddressId.value = null
  engine.showToast('已退出登录')
}

/** token 失效时由 api 层广播 → 弹出登录框 */
function handleAuthRequired() {
  user.value = null
  cartItems.value = []
  openAuth('login')
}

/** 启动时校验本地 token，恢复登录态 */
async function restoreSession() {
  if (!getToken()) return
  try {
    const res = await braceletApi.getUserInfo()
    if (res.code === 200 && res.data) {
      const info = { userId: res.data.userId, username: res.data.username, nickname: res.data.nickname, avatar: res.data.avatar, role: res.data.role || 'user' }
      user.value = info
      localStorage.setItem('bracelet-user', JSON.stringify(info))
      loadCartBadge()
      loadAddresses()
    }
  } catch {
    // 401 时 api 层已广播 auth:required
  }
}

function syncLocalPlans() {
  engine.showToast('正在同步本地方案…')
  // TODO: 对接后端同步接口
  setTimeout(() => {
    engine.showToast('本地方案已同步')
  }, 800)
}

// ==================== 购物车 ====================
const cartItems = ref([])
const cartLoading = ref(false)
const selectedCartIds = ref(new Set())
const cartThumbnails = ref({})  // cartId → dataURL

const cartSelectedCount = computed(() => selectedCartIds.value.size)
const cartSelectedTotal = computed(() => {
  let total = 0
  cartItems.value.forEach(item => {
    if (selectedCartIds.value.has(item.id)) {
      total += Number(item.totalPrice || 0)
    }
  })
  return total
})
const isCartAllSelected = computed(() => {
  return cartItems.value.length > 0 && selectedCartIds.value.size === cartItems.value.length
})
const cartBadgeCount = computed(() => cartItems.value.length)

async function loadCart() {
  if (!getToken()) {
    cartItems.value = []
    return
  }
  cartLoading.value = true
  try {
    const res = await braceletApi.listCart()
    if (res.code === 200) {
      cartItems.value = res.data || []
      console.log('[购物车] 已加载', cartItems.value.length, '条记录:', cartItems.value.map(i => ({ id: i.id, name: i.designName })))
      // 默认全部勾选
      selectedCartIds.value = new Set(cartItems.value.map(item => item.id))
      await generateCartThumbnails()
    }
  } catch (e) {
    console.error('加载购物车失败:', e)
  } finally {
    cartLoading.value = false
  }
}

// 环形缩略图公用渲染：beadListJson → dataURL（2 倍图，CSS 缩小显示保证清晰）
const ringImgCache = new Map()

function loadRingImage(path) {
  if (!path) return Promise.resolve(null)
  if (!ringImgCache.has(path)) {
    ringImgCache.set(path, new Promise(resolve => {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.onload = () => resolve(img)
      img.onerror = () => resolve(null)
      img.src = path
    }))
  }
  return ringImgCache.get(path)
}

async function renderRingThumb(beadListJson, size, baseRadius, maxBeads) {
  let parsed
  try {
    parsed = JSON.parse(beadListJson || '[]')
  } catch {
    return null
  }
  const beadItems = Array.isArray(parsed) ? parsed : (parsed.beads || [])
  if (!beadItems.length) return null

  const beadInfos = beadItems.slice(0, maxBeads).map(b => {
    const meta = dbIdToBeadMap.get(b.beadId)
    return meta ? { imagePath: meta.imagePath, size: b.size || 10 } : null
  }).filter(Boolean)
  if (!beadInfos.length) return null

  const loaded = (await Promise.all(
    beadInfos.map(info => loadRingImage(info.imagePath).then(img => img ? { img, size: info.size } : null))
  )).filter(Boolean)
  if (!loaded.length) return null

  const scale = size / 68
  // baseRadius 由调用方按输出像素给出（≈ size/3），不能再乘 scale，否则环会溢出画布
  const R = baseRadius
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  const cx = size / 2
  const cy = size / 2
  const n = loaded.length
  const stepAngle = (2 * Math.PI) / n
  const spacing = (2 * Math.PI * R) / n

  loaded.forEach((b, i) => {
    const angle = Math.PI / 2 + stepAngle * i
    const bx = cx + R * Math.cos(angle)
    const by = cy + R * Math.sin(angle)
    // 珠多时按环上可用弧长收缩半径，避免相互压叠
    const br = Math.min((4.6 + (b.size / 10) * 3) * scale, spacing * 0.44)

    ctx.save()
    ctx.beginPath()
    ctx.arc(bx, by, br, 0, Math.PI * 2)
    ctx.clip()
    ctx.drawImage(b.img, bx - br, by - br, br * 2, br * 2)
    ctx.restore()

    ctx.beginPath()
    ctx.arc(bx, by, br, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(140, 110, 80, 0.18)'
    ctx.lineWidth = 0.5 * scale
    ctx.stroke()
  })

  return canvas.toDataURL('image/png')
}

async function generateCartThumbnails() {
  const thumbnails = {}
  for (const item of cartItems.value) {
    const url = await renderRingThumb(item.beadListJson, 136, 44, 12)
    if (url) thumbnails[item.id] = url
  }
  cartThumbnails.value = thumbnails
}

function toggleCartSelectAll() {
  if (isCartAllSelected.value) {
    selectedCartIds.value = new Set()
  } else {
    selectedCartIds.value = new Set(cartItems.value.map(item => item.id))
  }
}

function toggleCartSelect(id) {
  const next = new Set(selectedCartIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedCartIds.value = next
}

async function removeCartItem(id) {
  try {
    const res = await braceletApi.deleteCart(id)
    if (res.code === 200) {
      cartItems.value = cartItems.value.filter(item => item.id !== id)
      const next = new Set(selectedCartIds.value)
      next.delete(id)
      selectedCartIds.value = next
      engine.showToast('已移除')
    } else {
      engine.showToast(res.msg || '移除失败')
    }
  } catch (e) {
    console.error('删除购物车失败:', e)
    engine.showToast('网络异常，移除失败')
  }
}

// ==================== 结算与订单 ====================
const showCheckoutModal = ref(false)
const checkoutStep = ref('form')   // form 确认信息 → pay 支付 → done 完成
const checkoutMode = ref('cart')   // cart 购物车结算 / order 单个订单补付
const checkoutForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' })
const checkoutBusy = ref(false)
const checkoutOrders = ref([])     // 本次生成的订单（支付步骤展示）
const checkoutThumbs = ref({})     // orderId → dataURL
const payBusyId = ref(null)
const checkoutErrors = ref({})     // 字段级校验错误（替代 toast，避免弹窗外再弹小提示）

// ==================== 收货地址簿 ====================
const addresses = ref([])           // 我的地址列表（默认地址排在最前）
const showAddressModal = ref(false)
const addressFormOpen = ref(false)  // 面板内切换：列表 ↔ 表单
const addressEditingId = ref(null)
const addressForm = ref({ receiverName: '', receiverPhone: '', region: '', detail: '', isDefault: false })
const addressErrors = ref({})
const addressBusy = ref(false)
const addressConfirmId = ref(null)  // 地址卡内联删除确认
const addressFromCheckout = ref(false)
const checkoutAddressId = ref(null) // 结算面板当前选中的地址（null = 手动填写的新地址）

const defaultAddress = computed(() => addresses.value.find(a => a.isDefault === 1) || addresses.value[0] || null)
const addressSummary = computed(() => {
  const a = defaultAddress.value
  return a ? `${a.region} ${a.detail}` : ''
})

async function loadAddresses() {
  if (!getToken()) {
    addresses.value = []
    return
  }
  try {
    const res = await braceletApi.listAddresses()
    if (res.code === 200) addresses.value = res.data || []
  } catch (e) {
    console.error('加载收货地址失败:', e)
  }
}

/** 打开地址簿；fromCheckout=true 时保存成功会把新地址回填到结算表单 */
function openAddressBook(fromCheckout = false) {
  if (!user.value) {
    engine.showToast('请先登录后管理收货地址')
    openAuth('login')
    return
  }
  addressFromCheckout.value = fromCheckout === true
  addressFormOpen.value = false
  addressEditingId.value = null
  addressErrors.value = {}
  addressConfirmId.value = null
  showAddressModal.value = true
  loadAddresses()
}

function closeAddressBook() {
  if (addressBusy.value) return
  showAddressModal.value = false
  addressFormOpen.value = false
  addressEditingId.value = null
  addressErrors.value = {}
  addressConfirmId.value = null
  addressFromCheckout.value = false
}

function openAddressForm(addr) {
  addressConfirmId.value = null
  addressErrors.value = {}
  addressEditingId.value = addr ? addr.id : null
  addressForm.value = addr
    ? {
        receiverName: addr.receiverName,
        receiverPhone: addr.receiverPhone,
        region: addr.region,
        detail: addr.detail,
        isDefault: addr.isDefault === 1,
      }
    : {
        receiverName: user.value ? user.value.nickname || '' : '',
        receiverPhone: '',
        region: '',
        detail: '',
        isDefault: addresses.value.length === 0,
      }
  addressFormOpen.value = true
}

function backToAddressList() {
  addressFormOpen.value = false
  addressEditingId.value = null
  addressErrors.value = {}
}

function clearAddressError(key) {
  if (!addressErrors.value[key]) return
  const next = { ...addressErrors.value }
  delete next[key]
  addressErrors.value = next
}

async function saveAddress() {
  if (addressBusy.value) return
  const f = addressForm.value
  const errors = {}
  if (!f.receiverName.trim()) errors.receiverName = '请填写收货人'
  if (!f.receiverPhone.trim()) errors.receiverPhone = '请填写联系电话'
  else if (!/^1[3-9]\d{9}$/.test(f.receiverPhone.trim())) errors.receiverPhone = '手机号需为 11 位，且以 1 开头'
  if (!f.region.trim()) errors.region = '请填写省 / 市 / 区'
  if (!f.detail.trim()) errors.detail = '请填写详细地址'
  addressErrors.value = errors
  if (Object.keys(errors).length) return

  addressBusy.value = true
  const dto = {
    receiverName: f.receiverName.trim(),
    receiverPhone: f.receiverPhone.trim(),
    region: f.region.trim(),
    detail: f.detail.trim(),
    isDefault: f.isDefault ? 1 : 0,
  }
  try {
    const res = addressEditingId.value
      ? await braceletApi.updateAddress(addressEditingId.value, dto)
      : await braceletApi.createAddress(dto)
    if (res.code === 200) {
      await loadAddresses()
      engine.showToast(addressEditingId.value ? '地址已更新' : '地址已保存')
      // 从结算面板进来的：保存后直接回填并返回结算
      if (addressFromCheckout.value && res.data) {
        applyAddressToForm(res.data)
        checkoutAddressId.value = res.data.id
        closeAddressBook()
        return
      }
      addressFormOpen.value = false
      addressEditingId.value = null
    } else {
      engine.showToast(res.msg || '保存失败')
    }
  } catch (e) {
    console.error('保存收货地址失败:', e)
    engine.showToast(e.message || '网络异常，保存失败')
  } finally {
    addressBusy.value = false
  }
}

async function setDefaultAddress(addr) {
  if (addr.isDefault === 1) return
  try {
    const res = await braceletApi.setDefaultAddress(addr.id)
    if (res.code === 200) {
      await loadAddresses()
      engine.showToast('已设为默认地址')
    } else {
      engine.showToast(res.msg || '设置失败')
    }
  } catch (e) {
    console.error('设置默认地址失败:', e)
    engine.showToast(e.message || '网络异常，设置失败')
  }
}

function askDeleteAddress(addr) {
  addressConfirmId.value = addr.id
}

async function confirmDeleteAddress() {
  const id = addressConfirmId.value
  if (!id) return
  try {
    const res = await braceletApi.deleteAddress(id)
    if (res.code === 200) {
      addressConfirmId.value = null
      await loadAddresses()
      if (checkoutAddressId.value === id) {
        const next = defaultAddress.value
        checkoutAddressId.value = next ? next.id : null
        if (next) applyAddressToForm(next)
      }
      engine.showToast('地址已删除')
    } else {
      engine.showToast(res.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除收货地址失败:', e)
    engine.showToast(e.message || '网络异常，删除失败')
  }
}

/** 把地址簿条目回填到结算表单 */
function applyAddressToForm(addr) {
  if (!addr) return
  checkoutForm.value.receiverName = addr.receiverName || ''
  checkoutForm.value.receiverPhone = addr.receiverPhone || ''
  checkoutForm.value.receiverAddress = [addr.region, addr.detail].filter(Boolean).join(' ')
  checkoutErrors.value = {}
}

function pickCheckoutAddress(addr) {
  checkoutAddressId.value = addr.id
  applyAddressToForm(addr)
}


const showOrdersModal = ref(false)
const ordersLoading = ref(false)
const myOrders = ref([])
const orderFilter = ref(null)
const orderThumbnails = ref({})
const orderConfirm = ref(null)     // 订单卡内联二次确认 { id, action, tip }

const ORDER_TABS = [
  { v: null, label: '全部' },
  { v: 0, label: '待付款' },
  { v: 1, label: '待发货' },
  { v: 2, label: '待收货' },
  { v: 5, label: '退款中' },
  { v: 3, label: '已完成' },
  { v: 4, label: '已取消' },
  { v: 6, label: '已退款' },
]

const checkoutItems = computed(() => cartItems.value.filter(it => selectedCartIds.value.has(it.id)))
const checkoutStepIndex = computed(() => ({ form: 0, pay: 1, done: 2 }[checkoutStep.value] ?? 0))
const checkoutTotal = computed(() =>
  checkoutOrders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0))
const checkoutPayLeft = computed(() => checkoutOrders.value.filter(o => o.status === 0))

/* 面板展示模型：购物车方案 / 已生成订单 → { id, name, meta, amount, thumb, paid } */
const checkoutDisplayItems = computed(() => {
  if (checkoutStep.value === 'form') {
    return checkoutItems.value.map(it => ({
      id: it.id,
      name: it.designName,
      meta: `${it.beadCount || 0} 颗 · 手围 ${it.wristMin}–${it.wristMax} cm`,
      amount: Number(it.totalPrice || 0),
      thumb: cartThumbnails.value[it.id],
      paid: false,
    }))
  }
  return checkoutOrders.value.map(o => ({
    id: o.id,
    name: o.designName,
    meta: `单号 ${o.orderNo} · ${o.beadCount} 颗`,
    amount: Number(o.totalAmount || 0),
    thumb: checkoutThumbs.value[o.id],
    paid: o.status !== 0,
  }))
})

async function checkout() {
  if (!user.value) {
    engine.showToast('请先登录后再结算')
    openAuth('login')
    return
  }
  if (cartSelectedCount.value === 0) {
    engine.showToast('请先勾选要结算的方案')
    return
  }
  if (!checkoutForm.value.receiverName) {
    checkoutForm.value.receiverName = user.value.nickname || ''
  }
  checkoutMode.value = 'cart'
  checkoutStep.value = 'form'
  checkoutErrors.value = {}
  addressFromCheckout.value = false
  showCheckoutModal.value = true
  carrySelectedThumbs()

  // 地址簿：有默认地址则自动填充，省去重复输入
  await loadAddresses()
  if (addresses.value.length) {
    const pick = defaultAddress.value
    checkoutAddressId.value = pick.id
    applyAddressToForm(pick)
  } else {
    checkoutAddressId.value = null
  }
}

/* 结算面板打开：勾选方案的缩略图从购物车原位“带”进面板物品栏
   （参考 NightKidz.shop 的 Carried Across 元素：物品在流程中始终可见且有连续感） */
let carryTimer = null
async function carrySelectedThumbs() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  await nextTick()
  await new Promise(r => setTimeout(r, 140))
  if (!showCheckoutModal.value) return

  const layer = document.createElement('div')
  layer.className = 'co-carry-layer'
  document.body.appendChild(layer)

  const list = document.querySelector('.co-list')
  let carryCount = 0
  if (list) {
    list.classList.add('co-list--carry')
    clearTimeout(carryTimer)
    const clear = () => list.classList.remove('co-list--carry')
    carryTimer = setTimeout(clear, 2200)
  }

  for (const it of checkoutItems.value) {
    const src = document.querySelector(`.cart-item[data-cart-id="${it.id}"] .cart-item__thumb img`)
    const dst = document.querySelector(`.co-item[data-cart-id="${it.id}"] .co-item__thumb`)
    if (!src || !dst) continue
    const a = src.getBoundingClientRect()
    const b = dst.getBoundingClientRect()
    if (!a.width || !b.width) continue

    const ghost = document.createElement('img')
    ghost.src = src.currentSrc || src.src
    ghost.className = 'co-carry-item'
    ghost.style.left = `${a.left}px`
    ghost.style.top = `${a.top}px`
    ghost.style.width = `${a.width}px`
    ghost.style.height = `${a.height}px`
    layer.appendChild(ghost)

    const dx = (b.left + b.width / 2) - (a.left + a.width / 2)
    const dy = (b.top + b.height / 2) - (a.top + a.height / 2)
    const scale = b.width / a.width
    const delay = 120 + carryCount * 70
    carryCount++

    ghost.animate(
      [
        { transform: 'translate(0px, 0px) scale(1)', opacity: 1 },
        { transform: `translate(${dx}px, ${dy}px) scale(${scale})`, opacity: 1 },
      ],
      { duration: 640, delay, easing: 'cubic-bezier(.2,.9,.25,1)', fill: 'both' }
    ).finished.catch(() => {}).then(() => ghost.remove())
  }

  setTimeout(() => layer.remove(), 2000 + carryCount * 70)
}

function closeCheckout() {
  if (checkoutBusy.value || payBusyId.value) return
  showCheckoutModal.value = false
  // 从订单列表进来的补付，关掉支付面板就退回订单列表，避免用户被丢回空页面
  if (checkoutMode.value === 'order') showOrdersModal.value = true
}

async function generateCheckoutThumbs() {
  const thumbnails = {}
  for (const o of checkoutOrders.value) {
    const url = await renderRingThumb(o.beadListJson, 168, 56, 14)
    if (url) thumbnails[o.id] = url
  }
  checkoutThumbs.value = thumbnails
}

/* 字段级校验：错误直接落在对应输入框下方，不再用面板外的 toast
   （参考 Tamannaah / NightKidz 的 Address Confidence 模式：地址类信息就地校验） */
function clearCheckoutError(key) {
  if (!checkoutErrors.value[key]) return
  const next = { ...checkoutErrors.value }
  delete next[key]
  checkoutErrors.value = next
}

function validateCheckout() {
  const f = checkoutForm.value
  const errors = {}
  if (!f.receiverName.trim()) errors.receiverName = '请填写收货人姓名'
  if (!f.receiverPhone.trim()) errors.receiverPhone = '请填写联系电话'
  else if (!/^1[3-9]\d{9}$/.test(f.receiverPhone.trim())) errors.receiverPhone = '手机号需为 11 位，且以 1 开头'
  if (!f.receiverAddress.trim()) errors.receiverAddress = '请填写完整收货地址'
  checkoutErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submitCheckout() {
  if (checkoutBusy.value) return
  const f = checkoutForm.value
  if (!validateCheckout()) return

  const cartIds = checkoutItems.value.map(item => item.id)
  if (!cartIds.length) return engine.showToast('请先勾选要结算的方案')

  checkoutBusy.value = true
  try {
    const res = await braceletApi.createOrders({
      cartIds,
      receiverName: f.receiverName.trim(),
      receiverPhone: f.receiverPhone.trim(),
      receiverAddress: f.receiverAddress.trim(),
      remark: f.remark.trim(),
    })
    if (res.code === 200) {
      checkoutOrders.value = res.data || []
      selectedCartIds.value = new Set()
      await Promise.all([generateCheckoutThumbs(), loadCart(), loadCartBadge()])
      checkoutStep.value = 'pay'
    } else {
      engine.showToast(res.msg || '下单失败')
    }
  } catch (e) {
    console.error('下单失败:', e)
    engine.showToast(e.message || '网络异常，下单失败')
  } finally {
    checkoutBusy.value = false
  }
}

/** 单笔支付（mock：直接改状态 0 → 1），返回是否成功 */
async function payCreatedOrder(order) {
  if (payBusyId.value) return false
  payBusyId.value = order.id
  let ok = false
  try {
    const res = await braceletApi.payOrder(order.id)
    if (res.code === 200) {
      ok = true
      order.status = 1
      order.statusText = '待发货'
      const mirrored = myOrders.value.find(o => o.id === order.id)
      if (mirrored) {
        mirrored.status = 1
        mirrored.statusText = '待发货'
      }
      if (!checkoutPayLeft.value.length) checkoutStep.value = 'done'
    } else {
      engine.showToast(res.msg || '支付失败')
    }
  } catch (e) {
    engine.showToast(e.message || '网络异常，支付失败')
  } finally {
    payBusyId.value = null
  }
  return ok
}

async function payAllCreated() {
  for (const o of [...checkoutPayLeft.value]) {
    // 任一失败即停止，避免连续报错
    if (!(await payCreatedOrder(o))) break
  }
}

/** 从订单列表补付：复用支付步骤（先收起订单面板，避免两层 overlay 叠在一起） */
function openPayForOrder(order) {
  showOrdersModal.value = false
  checkoutMode.value = 'order'
  checkoutOrders.value = [order]
  checkoutThumbs.value = {}
  orderConfirm.value = null
  checkoutStep.value = 'pay'
  generateCheckoutThumbs()
  showCheckoutModal.value = true
}

/** 支付完成 → 回到订单列表 */
async function finishCheckout() {
  showCheckoutModal.value = false
  const fromOrders = checkoutMode.value === 'order'
  await loadOrders()
  // 购物车结算：有未付就停在待付款，否则去待发货；订单补付：直接回待发货
  orderFilter.value = fromOrders ? 1 : (checkoutPayLeft.value.length ? 0 : 1)
  showOrdersModal.value = true
}

async function openOrders(status) {
  if (!user.value) {
    engine.showToast('请先登录后查看订单')
    openAuth('login')
    return
  }
  orderFilter.value = status === undefined ? null : status
  showOrdersModal.value = true
  await loadOrders()
}

function switchOrderFilter(v) {
  if (orderFilter.value === v) return
  orderFilter.value = v
  loadOrders()
}

async function loadOrders() {
  if (!getToken()) {
    myOrders.value = []
    return
  }
  ordersLoading.value = true
  try {
    const res = await braceletApi.listOrders(orderFilter.value)
    if (res.code === 200) {
      myOrders.value = res.data || []
      await generateOrderThumbnails()
    }
  } catch (e) {
    console.error('加载订单失败:', e)
  } finally {
    ordersLoading.value = false
  }
}

async function generateOrderThumbnails() {
  const thumbnails = {}
  for (const order of myOrders.value) {
    const url = await renderRingThumb(order.beadListJson, 168, 56, 14)
    if (url) thumbnails[order.id] = url
  }
  orderThumbnails.value = thumbnails
}

const ORDER_ACTION_TIP = {
  cancel: '取消后不可恢复，确认取消这笔订单？',
  refund: '提交后由设计师审核，确认申请退款？',
  confirm: '确认已收到手串？订单将标记为已完成。',
}

/** 订单操作入口：支付走支付面板，其余在卡片内做二次确认 */
function askOrderAction(order, action) {
  if (action === 'pay') {
    openPayForOrder(order)
    return
  }
  orderConfirm.value = { id: order.id, action, tip: ORDER_ACTION_TIP[action] || '确认执行该操作？' }
}

async function runOrderAction(order) {
  const c = orderConfirm.value
  if (!c || c.action === 'pay') return
  const api = { cancel: 'cancelOrder', refund: 'refundOrder', confirm: 'confirmOrder' }[c.action]
  if (!api) return
  try {
    const res = await braceletApi[api](order.id)
    if (res.code === 200) {
      orderConfirm.value = null
      await loadOrders()
    } else {
      engine.showToast(res.msg || '操作失败')
    }
  } catch (e) {
    engine.showToast(e.message || '网络异常')
  }
}

function fmtOrderTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

// ==================== 首页 3D 手串（finished-ring） ====================
// 原理：20 颗珠子按椭圆参数方程分布，translate3d + 深度缩放 + zIndex 遮挡，
// rAF 自转 + pointer 拖拽调整旋转/俯仰/斜切，点击（位移极小）进入工作台。
const HERO_BEAD_COUNT = 20
const heroRingRef = ref(null)
const heroRingDragging = ref(false)
const heroBeads = computed(() =>
  Array.from({ length: HERO_BEAD_COUNT }, (_, i) => crystalSources[i % crystalSources.length])
)

const heroRing = {
  angle: -8,      // 整体旋转角（度）
  squash: 0.72,   // 垂直压扁系数（俯视透视）
  tilt: 0.16,     // 斜切系数
  raf: 0,
  frame: 0,
  px: 0,
  py: 0,
  downX: 0,
  downY: 0,
  downT: 0,
}

function heroRingLayout() {
  const ring = heroRingRef.value
  if (!ring) return
  const beads = ring.querySelectorAll('.finished-bead')
  const mobile = ring.getBoundingClientRect().width < 430
  const R = mobile ? 118 : 152          // 水平半径
  const ZD = mobile ? 30 : 40           // Z 轴深度半径
  beads.forEach((el, g) => {
    const u = (g / HERO_BEAD_COUNT) * Math.PI * 2 - Math.PI / 2 + (heroRing.angle * Math.PI) / 180
    const cosU = Math.cos(u)
    const sinU = Math.sin(u)
    const x = cosU * R
    const y = sinU * R * heroRing.squash - cosU * R * heroRing.tilt
    const depth = sinU
    const scale = 0.94 + (depth + 1) * 0.045
    el.style.transform = `translate3d(${x}px, ${y}px, ${depth * ZD}px) scale(${scale})`
    // zIndex 仅在变化时写入，减少无效样式操作
    const z = String(Math.round(100 + depth * 80))
    if (el._z !== z) { el.style.zIndex = z; el._z = z }
  })
}

function heroRingLoop() {
  if (!heroRingDragging.value) {
    // 每帧布局（60fps 平滑），步进减半保持速度
    heroRing.angle += 0.11
    heroRingLayout()
  }
  heroRing.raf = requestAnimationFrame(heroRingLoop)
}

function onHeroRingDown(e) {
  heroRingDragging.value = true
  heroRing.px = heroRing.downX = e.clientX
  heroRing.py = heroRing.downY = e.clientY
  heroRing.downT = Date.now()
  e.currentTarget.setPointerCapture?.(e.pointerId)
}

function onHeroRingMove(e) {
  if (!heroRingDragging.value) return
  const dx = e.clientX - heroRing.px
  const dy = e.clientY - heroRing.py
  heroRing.px = e.clientX
  heroRing.py = e.clientY
  heroRing.angle += dx * 0.42
  heroRing.squash = Math.max(0.42, Math.min(0.96, heroRing.squash + dy * 0.0026))
  heroRing.tilt = Math.max(-0.18, Math.min(0.28, heroRing.tilt - dx * 8e-4))
  heroRingLayout()
}

function onHeroRingUp(e) {
  if (!heroRingDragging.value) return
  heroRingDragging.value = false
  e.currentTarget.releasePointerCapture?.(e.pointerId)
  // 位移与时长都极小 → 视为点击，进入工作台
  const moved = Math.hypot(e.clientX - heroRing.downX, e.clientY - heroRing.downY)
  if (moved < 6 && Date.now() - heroRing.downT < 400) {
    goToWorkshop()
  }
}

onMounted(() => {
  heroRingLayout()
  heroRingLoop()
})
onUnmounted(() => cancelAnimationFrame(heroRing.raf))

// 首页时暂停工作台引擎渲染循环（canvas 不可见时避免空耗主线程导致旋转卡顿）
watch(currentView, (v) => {
  if (v === 'home') engine.pauseRenderLoop()
  else engine.resumeRenderLoop()
}, { immediate: true })

// ==================== 侧边栏装饰珠子 ====================
const decorBracelet = ref(false)

// 粉色系水晶 + 黄玛瑙 — 真实水晶纹理装饰珠
const crystalSources = [
  '/水晶/红色系/粉玉髓.png',
  '/水晶/红色系/冰粉晶.png',
  '/水晶/红色系/粉月光.png',
  '/水晶/红色系/莫粉.png',
  '/水晶/黄色系/黄玛瑙.png',
  '/水晶/红色系/草莓晶.png',
  '/水晶/红色系/马粉.png',
  '/水晶/红色系/星光粉.png',
  '/水晶/黄色系/黄玛瑙.png',
  '/水晶/红色系/粉欧珀.png',
  '/水晶/红色系/粉白幽.png',
  '/水晶/红色系/粉兔毛.png',
]

const DECOR_COUNT = 12

// ================================================================
//  散落态 & 手串态 — 共享"横放手串"椭圆环模型
//
//  视觉意图：手串平放在桌面上，从轻微仰角斜向俯视
//  - 环呈横宽椭圆（rx > ry），体现"平放"透视
//  - 环心微倾 ~22°，左上→右下斜向，营造斜视角
//  - 散落态 = 松散环（径向随机散开），手串态 = 紧致环
//  - 两态共享同一环心 + 同一倾角，过渡平滑
//  - 全部坐标统一用 px，避免 %/px 混用导致椭圆变形
// ================================================================

const RING_CX = 20       // 环心 right 偏移 (px)
const RING_CY = 340      // 环心 top (px)
const RING_TILT = 0.38   // 环倾斜角 (rad ≈ 22°)
const COS_T = Math.cos(RING_TILT)
const SIN_T = Math.sin(RING_TILT)

// 将椭圆坐标绕环心倾斜旋转
function tiltPoint(ex, ey) {
  const dx = ex - RING_CX
  const dy = ey - RING_CY
  return {
    x: RING_CX + dx * COS_T - dy * SIN_T,
    y: RING_CY + dx * SIN_T + dy * COS_T,
  }
}

// 散落态：极大幅度散开 — 珠子在环心周围 50~650px 大范围错落分布
function makeScatterPositions(n) {
  const arr = []
  const rx = 90, ry = 50  // 松散环半径 (px)

  for (let i = 0; i < n; i++) {
    const angle = (2 * Math.PI / n) * i - Math.PI / 2

    // 径向偏移：-35~130px
    const r = rx + (Math.random() - 0.25) * 165
    const ryScaled = ry * (r / rx)

    const ex = RING_CX + r * Math.cos(angle)
    const ey = RING_CY + ryScaled * Math.sin(angle)
    const tilted = tiltPoint(ex, ey)

    // 切线扰动 ±40px
    const tj = (Math.random() - 0.5) * 80
    const x = tilted.x + tj * Math.cos(angle + Math.PI / 2)
    const y = tilted.y + tj * Math.sin(angle + Math.PI / 2)

    const size = 14 + Math.random() * 32
    const depth = 0.15 + Math.random() * 0.6

    arr.push({
      x: Math.max(-18, Math.min(100, x)),
      y: Math.max(50, Math.min(650, y)),
      size: Math.max(12, Math.min(48, size)),
      depth,
    })
  }
  return arr
}

// 手串态：大尺寸横宽椭圆环 — 手串平放效果
function makeBraceletPositions(n) {
  const arr = []
  const rx = 65, ry = 38  // 130×76px 醒目手串

  for (let i = 0; i < n; i++) {
    const angle = (2 * Math.PI / n) * i - Math.PI / 2
    const ex = RING_CX + rx * Math.cos(angle)
    const ey = RING_CY + ry * Math.sin(angle)
    const tilted = tiltPoint(ex, ey)
    arr.push({ x: tilted.x, y: tilted.y, size: 28 })
  }
  return arr
}

const scatterPositions = makeScatterPositions(DECOR_COUNT)
const bracelet3DPositions = makeBraceletPositions(DECOR_COUNT)

const decorBeads = reactive(
  scatterPositions.map((pos, i) => {
    const path = crystalSources[i]
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = path
    const bead = {
      id: i + 1,
      ...pos,
      // 深度大气透视：远处稍淡、近处浓郁
      opacity: 0.58 + (pos.depth || 0.5) * 0.38,
      crystalPath: path,
      imgLoaded: false,
    }
    img.onload = () => { bead.imgLoaded = true }
    img.onerror = () => { bead.imgLoaded = false }
    return bead
  })
)

// 渐进收拢：根据 progress (0~1) 在散落态与环形态之间插值
function lerpDecors(progress, instant = false) {
  const dur = instant ? 0.01 : 0.9
  decorBeads.forEach((b, i) => {
    const s = scatterPositions[i]
    const t = bracelet3DPositions[i]
    gsap.to(b, {
      x: s.x + (t.x - s.x) * progress,
      y: s.y + (t.y - s.y) * progress,
      size: s.size + (t.size - s.size) * progress,
      duration: dur,
      ease: 'power2.out',
      overwrite: 'auto',
    })
  })
}

// 监听主界面手串状态 → 装饰珠子完全成环（仅按钮触发）
watch(() => engine.isBracelet.value, (isBraceletNow) => {
  if (isBraceletNow) {
    decorBracelet.value = true
    lerpDecors(1)
  } else {
    // ── 散开：弹回原始散落位置 ──
    decorBracelet.value = false

    decorBeads.forEach((b, i) => {
      const target = scatterPositions[i]
      gsap.to(b, {
        x: target.x,
        y: target.y,
        size: target.size,
        duration: 0.7,
        ease: 'power3.out',
        delay: i * 0.04,
        overwrite: 'auto',
      })
    })
  }
})

// ==================== 状态标签文案 ====================
const statusText = computed(() => {
  if (engine.isDragging.value) return '拖拽珠子到目标位置'
  if (engine.isBracelet.value) return '手环已成 — 可拖拽珠子调整顺序'
  if (engine.isStringing.value) return '串线中...'
  if (engine.isFull.value) return '珠槽已满 (25颗)'
  if (engine.beadCount.value > 0) return `珠子: ${engine.beadCount.value} / 25`
  return '点击下方素材添加珠子'
})

// ==================== 订单信息卡 ====================
/** 获取单颗珠子价格（元）—— 按品类与子分类估价，后续可对接数据库实际价格 */
function getBeadPrice(bead) {
  const subPrices = {
    'crystal-white': 12,   'crystal-black': 15,   'crystal-red': 14,
    'crystal-yellow': 13,  'crystal-blue': 16,    'crystal-green': 18,
    'acc-pendant': 22,     'acc-sugar': 14,       'acc-carve': 18,
    'spacer-flower': 5,    'spacer-ring': 8,      'spacer-sheet': 7,
    'spacer-bead': 10,     'wenwan-all': 10,
  }
  if (bead.subCategoryId && subPrices[bead.subCategoryId] !== undefined) {
    return subPrices[bead.subCategoryId]
  }
  const basePrices = { 'main-bead': 12, 'spacer': 7, 'accessories': 15, 'wenwan': 10 }
  return basePrices[bead.categoryId] || 10
}

const orderBeadCount = computed(() => engine.beadCount.value)
const orderMaxBeads = 25

/** 预估总周长 (cm)：累加所有珠子直径（mm → cm） */
const orderTotalCircumference = computed(() => {
  const totalMm = engine.beads.value.reduce((sum, b) => sum + b.size, 0)
  return (totalMm / 10).toFixed(1)
})

/** 建议手围区间 (cm)：总周长 - 5.0 ~ 总周长 - 4.3（与后端公式一致） */
const orderWristMin = computed(() => {
  const circ = parseFloat(orderTotalCircumference.value)
  return Math.max(0, circ - 5.0).toFixed(1)
})
const orderWristMax = computed(() => {
  const circ = parseFloat(orderTotalCircumference.value)
  return Math.max(0, circ - 4.3).toFixed(1)
})

/** 订单总价 */
const orderTotalPrice = computed(() => {
  const total = engine.beads.value.reduce((sum, b) => sum + getBeadPrice(b), 0)
  return total
})

/** 构建 API 请求的珠子数组：从画布珠子提取 dbId + size */
function buildApiBeadPayload() {
  return engine.beads.value.map(b => {
    const meta = beadMetaMap.get(b.imagePath) || {}
    return {
      beadId: meta.dbId || 0,
      size: b.size,
    }
  })
}

/** 加购下单 → 弹出命名弹窗 */
function addToCart() {
  if (!user.value) {
    engine.showToast('请先登录后再下单')
    openAuth('login')
    return
  }
  if (engine.beadCount.value === 0) {
    engine.showToast('请先添加珠子再下单')
    return
  }
  showCartNameModal.value = true
  cartNameInput.value = ''
  nextTick(() => {
    const input = document.querySelector('.cart-name-modal__input')
    if (input) input.focus()
  })
}

// 购物车命名弹窗
const showCartNameModal = ref(false)
const cartNameInput = ref('')
const cartAdded = ref(false)       // 加购成功后就地确认（不再弹全局 toast）
let cartAddedTimer = null

/** 加购成功后：按钮原地切成「已加入」，1.8s 后复原 */
function flashCartAdded() {
  cartAdded.value = true
  clearTimeout(cartAddedTimer)
  cartAddedTimer = setTimeout(() => { cartAdded.value = false }, 1800)
}

/** 确认加购 */
async function confirmAddToCart() {
  const name = cartNameInput.value.trim()
  if (!name) {
    engine.showToast('请输入方案名称')
    return
  }
  showCartNameModal.value = false
  try {
    const payload = buildApiBeadPayload()
    console.log('[加购] 发送请求: designName=' + name + ', beads=' + payload.length + '颗')
    const res = await braceletApi.addCart({ designName: name, beads: payload })
    console.log('[加购] 响应:', res)
    if (res.code === 200) {
      flashCartAdded()
      loadCartBadge()
    } else {
      engine.showToast(res.msg || '加购失败')
    }
  } catch (e) {
    console.error('加购请求异常:', e)
    engine.showToast('网络异常，加购失败')
  }
}

/** 取消加购 */
function cancelAddToCart() {
  showCartNameModal.value = false
}

/** 仅查询购物车数量用于导航角标（静默，不显示 loading） */
async function loadCartBadge() {
  if (!getToken()) {
    cartItems.value = []
    return
  }
  try {
    const res = await braceletApi.listCart()
    if (res.code === 200) {
      cartItems.value = res.data || []
    }
  } catch {
    // 静默失败
  }
}

/** 保存设计记录 → 弹出命名弹窗 */
function saveDesign() {
  promptSaveDesign()
}
</script>

<template>
  <div class="app-shell">
    <!-- ==================== 顶部导航栏（首页外的视图使用） ==================== -->
    <div v-if="currentView !== 'home'" class="top-nav" :class="{ 'top-nav--hidden': isShowcasing }">
      <div class="top-nav__bar">
        <button
          v-for="tab in navTabs"
          :key="tab.id"
          class="top-nav__tab"
          :class="{ 'top-nav__tab--active': activeNavTab === tab.id }"
          @click="tab.id === 'workshop' ? goToWorkshop() : tab.id === 'order' ? goToOrder() : tab.id === 'inspiration' ? goToInspiration() : tab.id === 'mine' ? goToMine() : tab.id === 'plan' ? goToPlan() : (activeNavTab = tab.id)"
        >
          {{ tab.label }}
          <!-- 订单角标 -->
          <span
            v-if="tab.id === 'order' && cartBadgeCount > 0"
            class="top-nav__badge"
          >{{ cartBadgeCount }}</span>
        </button>
      </div>
    </div>

    
    <!-- ==================== 首页 ==================== -->
    <div
      v-if="currentView === 'home' || transitionLeaving === 'home'"
      ref="homePageRef"
      class="home-page"
    >
      <!-- 首页专属顶栏 -->
      <div class="home-nav">
        <button class="home-nav__brand" @click="activeNavTab = ''">
          <span class="home-nav__brand-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="12" cy="12" r="7.2"/>
              <circle cx="12" cy="3.6" r="2.1"/>
              <circle cx="19.8" cy="16.2" r="2.1"/>
              <circle cx="4.2" cy="16.2" r="2.1"/>
            </svg>
          </span>
          <span class="home-nav__brand-text">串珠定制</span>
        </button>

        <nav class="home-nav__links">
          <button
            v-for="tab in navTabs.slice(0, 3)"
            :key="tab.id"
            class="home-nav__tab"
            :class="{ 'home-nav__tab--active': activeNavTab === tab.id }"
            @click="tab.id === 'workshop' ? goToWorkshop() : tab.id === 'inspiration' ? goToInspiration() : goToPlan()"
          >{{ tab.label }}</button>
        </nav>

        <div class="home-nav__actions">
          <button class="home-nav__icon-btn" title="订单" @click="goToOrder()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 8h12l-1 12a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L6 8z"/>
              <path d="M9 8V6a3 3 0 0 1 6 0v2"/>
            </svg>
            <span v-if="cartBadgeCount > 0" class="home-nav__badge">{{ cartBadgeCount }}</span>
          </button>
          <button class="home-nav__icon-btn" title="我的" @click="goToMine()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="8" r="4"/>
              <path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 背景描边水印 -->
      <span class="home-watermark" aria-hidden="true">Bracelet</span>

      <div class="home-hero">
        <!-- 左侧文案区 -->
        <div class="hero-copy">
          <h1 class="hero-copy__title">串珠<br>定制</h1>
          <p class="hero-copy__subtitle">精选天然水晶 · 自由搭配 · 打造专属手串</p>
          <div class="hero-copy__actions">
            <button class="hero-copy__cta" @click="goToWorkshop">
              <span>开始设计</span>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <span class="hero-copy__hint">点击右侧手串 · 直达工作台</span>
          </div>
        </div>

        <!-- 中央 3D 手串（finished-ring：椭圆分布 + 自转 + 拖拽视角） -->
        <div
          class="hero-ring-visual"
          :class="{ 'is-dragging': heroRingDragging }"
          @pointerdown="onHeroRingDown"
          @pointermove="onHeroRingMove"
          @pointerup="onHeroRingUp"
          @pointercancel="onHeroRingUp"
          @pointerleave="onHeroRingUp"
          @dragstart.prevent
        >
          <div ref="heroRingRef" class="finished-ring">
            <span
              v-for="(src, i) in heroBeads"
              :key="i"
              class="finished-bead"
              :data-index="i"
            >
              <img :src="src" alt="" draggable="false" />
            </span>
          </div>
          <span class="hero-ring-visual__hint">拖动旋转 · 点击进入</span>
        </div>

        <!-- 右侧步骤指示 -->
        <div class="hero-steps" aria-hidden="true">
          <div class="hero-step hero-step--muted">
            <span class="hero-step__num">01</span>
            <span class="hero-step__label">选珠</span>
          </div>
          <div class="hero-step hero-step--active">
            <span class="hero-step__num">02</span>
            <span class="hero-step__label">搭配设计</span>
          </div>
          <div class="hero-step hero-step--muted">
            <span class="hero-step__num">03</span>
            <span class="hero-step__label">收拢成串</span>
          </div>
        </div>
      </div>

      <!-- 右下滚动提示 -->
      <button class="hero-scroll" aria-label="查看更多" @click="goToWorkshop">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 5v14M5 12l7 7 7-7"/>
        </svg>
      </button>
    </div>

    <!-- ==================== 主体区域（侧边栏 + 主内容） ==================== -->
    <div
      v-if="currentView === 'workshop' || transitionLeaving === 'workshop'"
      ref="workshopBodyRef"
      class="app-body"
    >
      <!-- ==================== 设计步骤侧边栏 ==================== -->
      <aside class="design-sidebar" :class="{ 'design-sidebar--hidden': isShowcasing }">
        <h2 class="design-sidebar__title">设计步骤</h2>
        <div class="design-sidebar__steps">
          <div
            v-for="step in steps"
            :key="step.id"
            class="step-item"
            :class="{ 'step-item--active': activeStep === step.id }"
          >
            <div class="step-item__indicator">
              <span class="step-item__number">{{ step.id }}</span>
            </div>
            <div class="step-item__body">
              <p class="step-item__title">{{ step.title }}</p>
              <p class="step-item__desc">{{ step.desc }}</p>
            </div>
          </div>
        </div>
        <!-- 装饰圆珠（松散手串环 + 收拢成环 — 横放斜向俯视） -->
        <div
          class="design-sidebar__decor"
          :class="{ 'design-sidebar__decor--bracelet': decorBracelet }"
        >
          <!-- 手串态：浅米白纯色圆形背景 -->
          <div v-if="decorBracelet" class="decor-bg-ring"></div>

          <span
            v-for="b in decorBeads"
            :key="b.id"
            class="decor-bead"
            :class="`decor-bead--float-${((b.id - 1) % 6) + 1}`"
            :style="{
              width: b.size + 'px',
              height: b.size + 'px',
              top: b.y + 'px',
              right: b.x + 'px',
              zIndex: decorBracelet ? 4 : Math.round(2 + (b.depth || 0.5) * 16),
              opacity: b.opacity,
            }"
          >
            <!-- 水晶纹理图 -->
            <img
              :src="b.crystalPath"
              class="decor-bead__img"
              :class="{ 'decor-bead__img--loaded': b.imgLoaded }"
              alt=""
            />
            <!-- 水晶球面高光 -->
            <span class="decor-bead__highlight"></span>
            <!-- 底部反光 -->
            <span class="decor-bead__bounce"></span>
          </span>
        </div>
      </aside>

      <!-- ==================== 右侧主内容区 ==================== -->
      <div class="app-main">
    <!-- ==================== 中心 Canvas 容器 ==================== -->
    <div class="canvas-area">
      <div class="canvas-area__inner">
        <div
          ref="canvasContainerRef"
          class="canvas-container"
          :style="{ backgroundImage: `url(${currentTray})` }"
        >
          <canvas
            ref="canvasRef"
            class="bead-canvas"
            :class="{ 'bead-canvas--delete-mode': deleteMode }"
            @click.stop="onCanvasClick"
          ></canvas>

          <div
            v-if="engine.beadCount.value === 0 || engine.isBracelet.value"
            class="canvas-status"
            :class="{ 'canvas-status--bracelet': engine.isBracelet.value }"
          >
            {{ statusText }}
          </div>
        </div>

        <!-- 展示模式覆盖层：单击任意处退出展示 -->
        <div
          v-if="isShowcasing"
          class="showcase-overlay"
          @click="exitShowcase"
        >
          <span class="showcase-overlay__hint">单击任意处退出展示</span>
        </div>

        <!-- ==================== 右侧悬浮功能按钮栏 ==================== -->
        <div
          class="floating-bar"
          :class="{ 'floating-bar--hidden': isShowcasing }"
        >
          <!-- 胶囊按钮组 -->
          <div class="floating-bar__capsule">
            <!-- 1. 展示效果 - 放大镜 -->
            <div class="floating-bar__icon-wrap">
              <button
                class="floating-bar__icon-btn"
                :class="{ 'floating-bar__icon-btn--active': isShowcasing }"
                @click="toggleShowcase"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"/>
                  <path d="M21 21l-4.35-4.35"/>
                </svg>
              </button>
              <span class="floating-bar__tooltip">展示效果</span>
            </div>

            <!-- 2. 更换背景 - 手绘画笔 -->
            <div class="floating-bar__icon-wrap">
              <button class="floating-bar__icon-btn" @click="switchBackground">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 19l7-7 3 3-7 7-3-3z"/>
                  <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/>
                  <path d="M2 2l7.586 7.586"/>
                </svg>
              </button>
              <span class="floating-bar__tooltip">更换背景</span>
            </div>

            <!-- 3. 盲盒方案 - 方块盒子 -->
            <div class="floating-bar__icon-wrap">
              <button class="floating-bar__icon-btn" @click="openBlindBoxPlan">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                  <line x1="12" y1="22.08" x2="12" y2="12"/>
                </svg>
              </button>
              <span class="floating-bar__tooltip">盲盒方案</span>
            </div>

            <!-- 4. 清除手串 - 垃圾桶 -->
            <div class="floating-bar__icon-wrap">
              <button class="floating-bar__icon-btn floating-bar__icon-btn--danger" @click="clearAllBeads">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  <line x1="10" y1="11" x2="10" y2="17"/>
                  <line x1="14" y1="11" x2="14" y2="17"/>
                </svg>
              </button>
              <span class="floating-bar__tooltip">清除手串</span>
            </div>
          </div>

          <!-- 底部独立删除珠子按钮 -->
          <div class="floating-bar__delete-wrap">
            <button
              class="floating-bar__delete-btn"
              :class="{ 'floating-bar__delete-btn--active': deleteMode }"
              @click="toggleDeleteMode"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 4v6h6"/>
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
              </svg>
            </button>
            <span class="floating-bar__tooltip">删除模式</span>
          </div>
        </div>

        <!-- ==================== 收拢成串 / 解除手串按钮 ==================== -->
        <button
          class="string-btn"
          :class="{
            'string-btn--active': engine.isBracelet.value,
            'string-btn--loading': engine.isStringing.value,
            'string-btn--hidden': isShowcasing,
          }"
          :disabled="engine.isStringing.value"
          @click="toggleBracelet"
        >
          <!-- 环形转圈加载图标 -->
          <span class="string-btn__spinner">
            <svg viewBox="0 0 24 24" fill="none">
              <circle
                class="string-btn__spinner-track"
                cx="12" cy="12" r="10"
                stroke="currentColor"
                stroke-width="2.5"
              />
              <circle
                class="string-btn__spinner-head"
                cx="12" cy="12" r="10"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-dasharray="16 48"
              />
            </svg>
          </span>
          <span class="string-btn__text">
            {{ engine.isStringing.value ? '串线中…' : (engine.isBracelet.value ? '解除手串' : '收拢成串') }}
          </span>
        </button>
      </div>
    </div>

    <!-- ==================== 订单信息卡（右侧固定悬浮） ==================== -->
    <div
      class="order-card"
      :class="{ 'order-card--hidden': isShowcasing }"
    >
      <!-- 顶部行：ORDER + 颗数 -->
      <div class="order-card__header">
        <span class="order-card__label">ORDER</span>
        <span class="order-card__count">{{ orderBeadCount }} 颗</span>
      </div>

      <!-- 动态总价 -->
      <div class="order-card__price">
        <span class="order-card__price-symbol">¥</span>
        <span class="order-card__price-value">{{ orderTotalPrice }}</span>
      </div>

      <!-- 三组数据行 -->
      <div class="order-card__info">
        <div class="order-card__row">
          <span class="order-card__row-label">材质数量</span>
          <span class="order-card__row-value">{{ orderBeadCount }} / {{ orderMaxBeads }}</span>
        </div>
        <div class="order-card__row">
          <span class="order-card__row-label">预估周长</span>
          <span class="order-card__row-value">{{ orderTotalCircumference }} cm</span>
        </div>
        <div class="order-card__row">
          <span class="order-card__row-label">建议手围</span>
          <span class="order-card__row-value">{{ orderWristMin }} ~ {{ orderWristMax }} cm</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="order-card__actions">
        <button class="order-card__btn" :class="{ 'order-card__btn--done': cartAdded }" @click="addToCart">
          <svg v-if="!cartAdded" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5"/>
          </svg>
          <span>{{ cartAdded ? '已加入购物车' : '加入到购物车' }}</span>
        </button>
        <button class="order-card__btn" @click="saveDesign">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
          </svg>
          <span>保存设计记录</span>
        </button>
      </div>
    </div>

    <!-- ==================== 历史方案面板 ==================== -->
    <div
      class="history-panel"
      :class="{ 'history-panel--hidden': isShowcasing }"
    >
      <!-- 头部（固定，不随列表滚动） -->
      <div class="history-panel__header">
        <span class="history-panel__title">HISTORY</span>
        <span class="history-panel__subtitle">最近方案</span>
        <span class="history-panel__share" @click="openShareLoad" title="凭分享码载入好友方案">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
          分享码
        </span>
      </div>

      <!-- 可滚动列表区域（最多显示 3 个方案） -->
      <div class="history-panel__scroll">
        <!-- 加载中 -->
        <div v-if="designsLoading" class="history-panel__empty">
          加载中...
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="designs.length === 0"
          class="history-panel__empty"
        >
          暂无保存方案
        </div>

        <!-- 方案列表 -->
        <div v-else class="history-panel__list">
          <button
            v-for="d in designs"
            :key="d.id"
            class="history-item"
            :class="{ 'history-item--active': currentDesignId === d.id }"
            @click="restoreDesign(d)"
          >
            <!-- 左侧缩略预览：手串 / 散落珠子缩略图 -->
            <div class="history-item__thumb">
              <div class="history-item__thumb-inner">
                <img
                  v-if="designThumbnails[d.id]"
                  :src="designThumbnails[d.id]"
                  class="history-item__thumb-img"
                  alt=""
                />
                <!-- 缩略图尚未生成时的骨架占位 -->
                <span v-else class="history-item__thumb-placeholder"></span>
              </div>
            </div>

            <!-- 右侧信息 -->
            <div class="history-item__info">
              <span class="history-item__name">{{ d.designName }}</span>
              <span class="history-item__meta">{{ d.beadCount }} 颗 &middot; &yen;{{ d.totalPrice }}</span>
            </div>

            <!-- 生成分享码 -->
            <span
              class="history-item__share"
              title="生成分享码"
              @click.stop="openShareGen(d)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7"/><path d="M12 3v13"/><path d="m7 8 5-5 5 5"/></svg>
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 素材选择面板 ==================== -->
    <div
      class="material-panel"
      :class="{ 'material-panel--hidden': isShowcasing }"
    >
      <!-- 主分类标签 -->
      <div class="material-panel__tabs">
        <button
          v-for="cat in categories"
          :key="cat.id"
          v-memo="[activeCategory === cat.id]"
          class="material-panel__tab"
          :class="{ 'material-panel__tab--active': activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.label }}
        </button>
      </div>

      <!-- 内容区：侧边栏 + 素材网格 -->
      <div class="material-panel__body">
        <!-- 左侧子分类栏 -->
        <div class="material-panel__sidebar">
          <button
            v-for="sub in currentCategory?.subCategories"
            :key="sub.id"
            v-memo="[activeSubCategory === sub.id, (inUseSubCategories[currentCategory?.id] || []).length]"
            class="material-panel__subtab"
            :class="{ 'material-panel__subtab--active': activeSubCategory === sub.id }"
            @click="activeSubCategory = sub.id"
          >
            <span class="material-panel__subtab-label">{{ sub.label }}</span>
            <span
              class="material-panel__subtab-count"
              v-if="sub.id.endsWith('-in-use')"
            >
              {{ (inUseSubCategories[currentCategory?.id] || []).length }}
            </span>
            <span class="material-panel__subtab-count" v-else>{{ sub.items.length }}</span>
          </button>
        </div>

        <!-- 右侧素材展示区 -->
        <div class="material-panel__grid-wrap">
          <div class="material-panel__grid" v-if="currentSubCategory">
            <button
              v-for="bead in currentSubCategory.items"
              :key="bead.path"
              v-memo="[selectedBead?.path === bead.path, clickedBeadPath === bead.path, beadSizeMap[bead.path]]"
              class="bead-item"
              :class="{
                'bead-item--selected': selectedBead?.path === bead.path,
                'bead-item--clicked': clickedBeadPath === bead.path,
              }"
              @click="addBead(bead, $event)"
            >
              <div class="bead-item__img-wrap">
                <img
                  :src="bead.path"
                  :alt="bead.name"
                  class="bead-item__img"
                  loading="lazy"
                />
              </div>
              <span class="bead-item__name">{{ bead.name }}</span>

              <div class="bead-item__size">
                <button
                  class="bead-item__size-btn"
                  @click="adjustBeadSize(bead.path, -1, $event)"
                  :disabled="getBeadSize(bead.path) <= (beadMetaMap.get(bead.path)?.sizeMin ?? 8)"
                >-</button>
                <span class="bead-item__size-val">{{ getBeadSize(bead.path) }}mm</span>
                <button
                  class="bead-item__size-btn"
                  @click="adjustBeadSize(bead.path, +1, $event)"
                  :disabled="getBeadSize(bead.path) >= (beadMetaMap.get(bead.path)?.sizeMax ?? 12)"
                >+</button>
              </div>
            </button>
          </div>

          <!-- 空状态 -->
          <div
            class="material-panel__empty"
            v-if="currentSubCategory && currentSubCategory.items.length === 0"
          >
            暂无素材
          </div>
        </div>
      </div>
    </div>
        </div>
      </div>

    <!-- ==================== 订单/购物车页面 ==================== -->
    <div v-if="currentView === 'order'" class="order-page">
      <div class="order-page__card">
        <!-- 头部 -->
        <div class="order-page__header">
          <h2 class="order-page__title">购物车</h2>
          <p class="order-page__subtitle">已选设计方案</p>
        </div>

        <!-- 加载中 -->
        <div v-if="cartLoading" class="order-page__empty">
          加载中...
        </div>

        <!-- 空购物车 -->
        <div
          v-else-if="cartItems.length === 0"
          class="order-page__empty"
        >
          <span class="order-page__empty-icon">🛒</span>
          <p>购物车是空的，去作坊设计一串手串吧</p>
        </div>

        <!-- 购物车列表 -->
        <div v-else class="order-page__list">
          <div
            v-for="item in cartItems"
            :key="item.id"
            class="cart-item"
            :data-cart-id="item.id"
          >
            <!-- ① 复选框 -->
            <label class="cart-item__check-wrap" @click.stop>
              <input
                type="checkbox"
                class="cart-item__check"
                :checked="selectedCartIds.has(item.id)"
                @change="toggleCartSelect(item.id)"
              />
              <span class="cart-item__check-mark"></span>
            </label>

            <!-- ② 手串缩略图 -->
            <div class="cart-item__thumb">
              <img
                v-if="cartThumbnails[item.id]"
                :src="cartThumbnails[item.id]"
                class="cart-item__thumb-img"
                alt=""
              />
              <span v-else class="cart-item__thumb-skeleton"></span>
            </div>

            <!-- ③ 文字信息 -->
            <div class="cart-item__info">
              <span class="cart-item__name">{{ item.designName }}</span>
              <span class="cart-item__wrist">
                建议手围：{{ item.wristMin }} - {{ item.wristMax }} cm
              </span>
              <span class="cart-item__price">¥{{ item.totalPrice }}</span>
            </div>

            <!-- ④ 删除按钮 -->
            <button
              class="cart-item__remove"
              @click.stop="removeCartItem(item.id)"
              title="移除此方案"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 底部固定结算栏 -->
      <div v-if="cartItems.length > 0" class="order-page__bar">
        <div class="order-page__bar-inner">
          <!-- 左侧：全选 + 合计 -->
          <div class="order-page__bar-left">
            <label class="cart-item__check-wrap" @click.stop>
              <input
                type="checkbox"
                class="cart-item__check"
                :checked="isCartAllSelected"
                @change="toggleCartSelectAll"
              />
              <span class="cart-item__check-mark"></span>
            </label>
            <span class="order-page__bar-label">全选</span>
            <span class="order-page__bar-divider"></span>
            <span class="order-page__bar-label">合计金额</span>
            <span class="order-page__bar-total">¥{{ cartSelectedTotal }}</span>
          </div>

          <!-- 右侧：结算按钮 -->
          <button class="order-page__checkout-btn" @click="checkout">
            合并结算({{ cartSelectedCount }})
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 灵感页 ==================== -->
    <div v-if="currentView === 'inspiration'" class="inspiration-page">
      <!-- 外层大圆角奶油米色容器 -->
      <div class="inspiration-page__container">
        <!-- 1. 顶部标题功能区 -->
        <div class="inspiration-page__header">
          <!-- 左侧标题 + 副标题 -->
          <div class="inspiration-page__title-group">
            <h1 class="inspiration-page__title">灵感集录</h1>
            <p class="inspiration-page__subtitle">精选搭配方案，为您提供设计灵感</p>
          </div>
          <!-- 右侧：切换标签 + 同步按钮 -->
          <div class="inspiration-page__actions">
            <div class="inspiration-page__tabs">
              <button
                class="inspiration-page__tab"
                :class="{ 'inspiration-page__tab--active': inspirationTab === 'design-new' }"
                @click="inspirationTab = 'design-new'"
              >
                设计新款
              </button>
              <button
                class="inspiration-page__tab"
                :class="{ 'inspiration-page__tab--active': inspirationTab === 'guest-order' }"
                @click="inspirationTab = 'guest-order'"
              >
                优秀客订
              </button>
            </div>
            <button class="inspiration-page__sync-btn" @click="syncLocalPlans">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
              </svg>
              <span>同步本地方案</span>
            </button>
          </div>
        </div>

        <!-- 2. 商品网格区（2 行 5 列） -->
        <div class="inspiration-grid">
          <div
            v-for="item in filteredInspiration"
            :key="item.globalIdx"
            class="inspiration-card"
            @click="openInspirationDetail(item.globalIdx)"
          >
            <!-- 底盘背景图 + 手串缩略图叠加 -->
            <div
              class="inspiration-card__plate"
              :style="{ backgroundImage: `url(${item.tray})` }"
            >
              <img
                v-if="inspirationThumbnails[item.globalIdx]"
                :src="inspirationThumbnails[item.globalIdx]"
                class="inspiration-card__thumb"
                alt=""
              />
              <span v-else class="inspiration-card__plate-skeleton"></span>
            </div>

            <!-- 下方圆角长方 UI 文字区 -->
            <div class="inspiration-card__info">
              <h3 class="inspiration-card__name">{{ item.name }}</h3>
              <p class="inspiration-card__size">{{ item.sizeMin }} - {{ item.sizeMax }} cm &nbsp;·&nbsp; ¥{{ item.price }}</p>
              <p class="inspiration-card__author">{{ item.author }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 灵感详情页 ==================== -->
    <div v-if="currentView === 'inspiration-detail' && inspirationDetail" class="insp-detail">
      <div class="insp-detail__container">
        <!-- 返回 -->
        <button class="insp-detail__back" @click="closeInspirationDetail">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 18l-6-6 6-6" />
          </svg>
          <span>返回灵感集录</span>
        </button>

        <div class="insp-detail__body">
          <!-- 左：成品大图（Canvas 合成） -->
          <div class="insp-detail__photo">
            <span v-if="detailPhotoLoading || !detailBigPhoto" class="insp-detail__photo-skeleton"></span>
            <img v-else :src="detailBigPhoto" class="insp-detail__photo-img" alt="" />
          </div>

          <!-- 右：设计信息 -->
          <div class="insp-detail__info">
            <p class="insp-detail__eyebrow">DESIGN DETAIL</p>
            <h1 class="insp-detail__name">{{ inspirationDetail.name }}</h1>
            <p class="insp-detail__meta">
              <span>建议手围 {{ detailWrist.min }} - {{ detailWrist.max }} cm</span>
              <span class="insp-detail__meta-divider">|</span>
              <span class="insp-detail__meta-price">¥ {{ detailTotalPrice }}</span>
              <span class="insp-detail__meta-divider">|</span>
              <span>◎ {{ detailBeadCount }} 颗成串</span>
              <span class="insp-detail__meta-divider">|</span>
              <span>{{ inspirationDetail.date }}</span>
            </p>

            <!-- 构成解析 -->
            <p class="insp-detail__section-title">构成解析 STRUCTURE</p>
            <div class="insp-detail__comps">
              <div v-for="c in detailComponents" :key="c.beadId + '-' + c.size" class="insp-detail__comp">
                <span class="insp-detail__comp-img">
                  <img v-if="c.imgUrl" :src="c.imgUrl" alt="" />
                </span>
                <div class="insp-detail__comp-main">
                  <p class="insp-detail__comp-name">{{ c.name }}</p>
                  <p class="insp-detail__comp-size">{{ c.size }}mm</p>
                </div>
                <div class="insp-detail__comp-price">
                  <p class="insp-detail__comp-unit">单价 ¥{{ c.unitPrice }}</p>
                  <p class="insp-detail__comp-count">x {{ c.count }} <b>¥{{ c.subtotal }}</b></p>
                </div>
              </div>
            </div>

            <!-- 材质总计 -->
            <div class="insp-detail__total">
              <span>材质总计</span>
              <b>¥ {{ detailTotalPrice }}</b>
            </div>

            <!-- 手围范围条 -->
            <div class="insp-detail__range">
              <div class="insp-detail__range-track">
                <div class="insp-detail__range-fill" :style="detailRangeStyle"></div>
              </div>
              <div class="insp-detail__range-labels">
                <span>13 cm</span>
                <span>{{ detailWrist.min }} - {{ detailWrist.max }} cm</span>
                <span>20 cm</span>
              </div>
            </div>

            <button class="insp-detail__apply" @click="applyInspirationDesign">应用此设计</button>
          </div>
        </div>

        <!-- 底部 Live 实况（后端灵感暂无实况图时整块隐藏） -->
        <div class="insp-detail__live" v-if="inspirationDetail.livePhotos && inspirationDetail.livePhotos.length">
          <div class="insp-detail__live-grid">
            <div
              v-for="(photo, pi) in inspirationDetail.livePhotos"
              :key="inspirationDetailIdx + '-' + pi"
              class="insp-detail__live-item"
            >
              <span class="insp-detail__live-badge">◎ 实况</span>
              <div class="insp-detail__live-placeholder">
                <p>实况图待补充</p>
                <p class="insp-detail__live-path">image/实况/{{ detailLiveDir }}/{{ pi + 1 }}.jpg</p>
              </div>
              <img
                :src="photo"
                class="insp-detail__live-img"
                alt=""
                @error="e => { e.target.style.display = 'none' }"
              />
            </div>
          </div>
          <p class="insp-detail__live-note"><b>Live 实况实拍</b>&nbsp;|&nbsp;自然光拍摄&nbsp;|&nbsp;无滤镜处理</p>
          <p class="insp-detail__live-sub">因屏幕显示差异，光感与藏珠可能略暗，实物会有光线与藏珠呈现更明显的层次。</p>
        </div>
      </div>

      <!-- 右侧浮动操作 -->
      <button class="insp-detail__nav" @click="prevInspiration" title="上一款">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <button class="insp-detail__ok" @click="applyInspirationDesign" title="应用此设计">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 6L9 17l-5-5" />
        </svg>
      </button>
    </div>

    <!-- ==================== 方案页面 ==================== -->
    <div v-if="currentView === 'plan'" class="plan-page">
      <!-- 外层大圆角白色容器（与灵感页一致） -->
      <div class="plan-page__container">
        <!-- 1. 顶部标题功能区 -->
        <div class="plan-page__header">
          <div class="plan-page__title-group">
            <h1 class="plan-page__title">我的方案</h1>
            <p class="plan-page__subtitle">保存的设计方案，随时回显编辑</p>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="designsLoading" class="plan-page__empty">
          加载中...
        </div>

        <!-- 空方案 -->
        <div
          v-else-if="designs.length === 0"
          class="plan-page__empty"
        >
          <span class="plan-page__empty-icon">📋</span>
          <p>暂无保存方案，去作坊设计一串手串吧</p>
        </div>

        <!-- 2. 方案网格 -->
        <div v-else class="plan-grid">
          <div
            v-for="(d, idx) in designs"
            :key="d.id"
            class="plan-card"
            :class="{ 'plan-card--asking': planConfirmId === d.id }"
            @click="planConfirmId === d.id ? null : restoreDesign(d)"
          >
            <!-- 删除入口：hover 显示，点击就地二次确认 -->
            <button class="plan-del" type="button" title="删除方案" @click.stop="askDeletePlan(d)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"></path></svg>
            </button>
            <!-- 底盘背景图 + 手串缩略图叠加 -->
            <div
              class="plan-card__plate"
              :style="{ backgroundImage: `url(${inspirationTray})` }"
            >
              <img
                v-if="designThumbnails[d.id]"
                :src="designThumbnails[d.id]"
                class="plan-card__thumb"
                alt=""
              />
              <span v-else class="plan-card__plate-skeleton"></span>
            </div>

            <!-- 下方文字信息区 -->
            <div class="plan-card__info">
              <h3 class="plan-card__name">{{ d.designName }}</h3>
              <p class="plan-card__wrist">
                建议手围：{{ d.wristMin }} - {{ d.wristMax }} cm
              </p>
              <p class="plan-card__price">¥{{ d.totalPrice }}</p>
            </div>

            <!-- 内联删除确认 -->
            <transition name="ord-fade">
              <div v-if="planConfirmId === d.id" class="plan-confirm" @click.stop>
                <b>删除方案「{{ d.designName }}」</b>
                <p>删除后不可恢复，已生成的订单与购物车不受影响</p>
                <div class="plan-confirm__ops">
                  <button type="button" class="plan-confirm__no" :disabled="planDeleting" @click.stop="planConfirmId = null">取消</button>
                  <button type="button" class="plan-confirm__yes" :disabled="planDeleting" @click.stop="runDeletePlan(d)">{{ planDeleting ? '删除中…' : '确认删除' }}</button>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 我的页面 ==================== -->
    <div v-if="currentView === 'mine'" class="mine-page">
      <div class="mine-page__container">

        <!-- ==== 顶部区域：两个通栏卡片 ==== -->
        <div class="mine-page__top">

          <!-- 模块 1：登录/注册 / 用户信息 -->
          <div
            class="mine-card mine-card--login"
            @click="user ? null : openAuth('login')"
          >
            <div class="mine-login">
              <div class="mine-login__avatar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </div>
              <template v-if="user">
                <div class="mine-login__info">
                  <span class="mine-login__text">{{ user.nickname }}</span>
                  <span class="mine-login__sub">ID: {{ user.userId }}</span>
                </div>
                <button class="mine-login__logout" @click.stop="logoutUser">退出登录</button>
              </template>
              <span v-else class="mine-login__text">点击登录 / 注册</span>
            </div>
          </div>

          <!-- 模块 2：说明横幅 -->
          <div class="mine-card mine-card--banner">
            <div class="mine-banner">
              <div class="mine-banner__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <div class="mine-banner__body">
                <h3 class="mine-banner__title">关于手串DIY研究所</h3>
                <p class="mine-banner__subtitle">自由搭配天然水晶，设计专属手串，每一串都是独一无二的作品</p>
                <div class="mine-banner__tags">
                  <span class="mine-banner__tag">天然水晶</span>
                  <span class="mine-banner__tag">自由搭配</span>
                  <span class="mine-banner__tag">专属定制</span>
                  <span class="mine-banner__tag">匠人工艺</span>
                </div>
              </div>
              <div class="mine-banner__arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 18l6-6-6-6"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- ==== 下方：左右两栏布局 ==== -->
        <div class="mine-page__bottom">

          <!-- ===== 左侧栏 60% ===== -->
          <div class="mine-page__left">

            <!-- 模块 3：我的订单 -->
            <div class="mine-card mine-card--orders">
              <div class="mine-orders">
                <div class="mine-orders__header">
                  <h3 class="mine-orders__title">我的订单</h3>
                  <span class="mine-orders__more" @click="openOrders()">查看全部
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mine-orders__more-icon">
                      <path d="M9 18l6-6-6-6"/>
                    </svg>
                  </span>
                </div>
                <div class="mine-orders__grid">
                  <div class="mine-orders__item" @click="openOrders(0)">
                    <div class="mine-orders__item-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="1" y="3" width="15" height="13" rx="2"/>
                        <polyline points="16 8 20 8 23 11 23 16 16 16 16 8"/>
                        <circle cx="5.5" cy="18.5" r="2.5"/>
                        <circle cx="18.5" cy="18.5" r="2.5"/>
                      </svg>
                    </div>
                    <span class="mine-orders__item-label">待付款</span>
                  </div>
                  <div class="mine-orders__item" @click="openOrders(1)">
                    <div class="mine-orders__item-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                      </svg>
                    </div>
                    <span class="mine-orders__item-label">待发货</span>
                  </div>
                  <div class="mine-orders__item" @click="openOrders(2)">
                    <div class="mine-orders__item-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="2" y="3" width="20" height="14" rx="2"/>
                        <line x1="8" y1="21" x2="16" y2="21"/>
                        <line x1="12" y1="17" x2="12" y2="21"/>
                      </svg>
                    </div>
                    <span class="mine-orders__item-label">待收货</span>
                  </div>
                  <div class="mine-orders__item" @click="openOrders(5)">
                    <div class="mine-orders__item-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                      </svg>
                    </div>
                    <span class="mine-orders__item-label">退款/售后</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 模块 4：功能列表 -->
            <div class="mine-card mine-card--list">
              <div class="mine-list">
                <div class="mine-list__item" @click="openAddressBook(false)">
                  <div class="mine-list__item-left">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="mine-list__icon">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                      <circle cx="12" cy="10" r="3"/>
                    </svg>
                    <div class="mine-list__item-body">
                      <span>收货地址</span>
                      <small class="mine-list__item-sub">
                        {{ defaultAddress ? addressSummary : '添加地址，结算时自动填充' }}
                      </small>
                    </div>
                  </div>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mine-list__arrow">
                    <path d="M9 18l6-6-6-6"/>
                  </svg>
                </div>
                <div class="mine-list__divider"></div>
                <div class="mine-list__item">
                  <div class="mine-list__item-left">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="mine-list__icon">
                      <circle cx="12" cy="12" r="10"/>
                      <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                      <line x1="12" y1="17" x2="12.01" y2="17"/>
                    </svg>
                    <span>帮助中心</span>
                  </div>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mine-list__arrow">
                    <path d="M9 18l6-6-6-6"/>
                  </svg>
                </div>
                <div class="mine-list__divider"></div>
                <div class="mine-list__item">
                  <div class="mine-list__item-left">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="mine-list__icon">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                      <line x1="16" y1="13" x2="8" y2="13"/>
                      <line x1="16" y1="17" x2="8" y2="17"/>
                      <polyline points="10 9 9 9 8 9"/>
                    </svg>
                    <span>服务条款</span>
                  </div>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mine-list__arrow">
                    <path d="M9 18l6-6-6-6"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>

          <!-- ===== 右侧栏 40% ===== -->
          <div class="mine-page__right">

            <!-- 模块 5：共同创作者卡片（深色主题） -->
            <div class="mine-card mine-card--creator">
              <div class="mine-creator">
                <h3 class="mine-creator__title">共同创作者</h3>
                <p class="mine-creator__desc">分享您的设计方案，让社区用上你的搭配</p>
                <button class="mine-creator__btn" @click="goToPlan">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mine-creator__btn-icon">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                  <span>去分享我的方案</span>
                </button>
              </div>
            </div>

            <!-- 模块 6：创作数据（真实统计，来自分享码被载入记录） -->
            <div class="mine-card mine-card--status">
              <div class="mine-status">
                <h3 class="mine-status__title">创作数据</h3>
                <div class="mine-status__grid mine-status__grid--single">
                  <div class="mine-status__cell">
                    <span class="mine-status__value">{{ creatorApplied }}</span>
                    <span class="mine-status__label">被应用次数</span>
                  </div>
                  <div class="mine-status__vdivider"></div>
                  <div class="mine-status__cell">
                    <span class="mine-status__value">{{ designs.length }}</span>
                    <span class="mine-status__label">已保存方案</span>
                  </div>
                </div>
                <p class="mine-status__tip">他人通过你的分享码载入方案，被应用次数 +1</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 游客演示模式提示条 ==================== -->
    <Teleport to="body">
      <div v-if="isDemoMode" class="demo-pill">游客演示模式 · 数据仅存于本浏览器</div>
    </Teleport>

    <!-- ==================== Toast 消息 ==================== -->
    <Teleport to="body">
      <Transition name="toast">
        <div
          v-if="engine.toastMessage.value"
          class="toast-overlay"
        >
          <div class="toast-box">
            <span class="toast-box__text">{{ engine.toastMessage.value }}</span>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 盲盒方案弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showBlindBoxModal"
          class="modal-overlay"
          @click.self="closeBlindBoxModal"
        >
          <div class="modal-card">
            <div class="modal-card__header">
              <h3 class="modal-card__title">盲盒搭配方案</h3>
              <button class="modal-card__close" @click="closeBlindBoxModal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            <p class="modal-card__subtitle">选择一款心仪的搭配，自动生成手串</p>
            <div class="modal-card__grid">
              <button
                v-for="plan in blindBoxPlans"
                :key="plan.name"
                class="plan-card"
                @click="applyBlindBoxPlan(plan)"
              >
                <div class="plan-card__icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                  </svg>
                </div>
                <h4 class="plan-card__name">{{ plan.name }}</h4>
                <p class="plan-card__desc">{{ plan.desc }}</p>
                <span class="plan-card__count">{{ plan.beads.length }} 颗珠子</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 方案命名弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showNameModal"
          class="modal-overlay"
          @click.self="showNameModal = false"
        >
          <div class="name-modal">
            <p class="name-modal__title">
              {{ nameModalMode === 'overwrite' ? '修改方案名称' : '为新方案命名' }}
            </p>
            <input
              class="name-modal__input"
              v-model="nameModalInput"
              placeholder="输入方案名称..."
              @keyup.enter="confirmSave(nameModalMode)"
            />
            <div class="name-modal__actions">
              <button
                v-if="nameModalMode === 'overwrite'"
                class="name-modal__btn name-modal__btn--new"
                @click="confirmSave('new')"
              >
                另存为新方案
              </button>
              <button
                class="name-modal__btn name-modal__btn--primary"
                @click="confirmSave(nameModalMode)"
              >
                {{ nameModalMode === 'overwrite' ? '覆盖原方案' : '确认保存' }}
              </button>
            </div>
            <button
              class="name-modal__close"
              @click="showNameModal = false"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 购物车命名弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showCartNameModal"
          class="modal-overlay"
          @click.self="cancelAddToCart"
        >
          <div class="name-modal">
            <p class="name-modal__title">为手链命个名吧</p>
            <input
              class="name-modal__input cart-name-modal__input"
              v-model="cartNameInput"
              placeholder="输入方案名称..."
              @keyup.enter="confirmAddToCart"
            />
            <div class="name-modal__actions">
              <button
                class="name-modal__btn name-modal__btn--primary"
                @click="confirmAddToCart"
              >
                确定该名称
              </button>
              <button
                class="name-modal__btn name-modal__btn--new"
                @click="cancelAddToCart"
              >
                我再想想
              </button>
            </div>
            <button
              class="name-modal__close"
              @click="cancelAddToCart"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 方案分享码弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showShareModal"
          class="modal-overlay"
          @click.self="showShareModal = false"
        >
          <div class="name-modal">
            <!-- 生成态：展示分享码 -->
            <template v-if="shareModalMode === 'gen'">
              <p class="name-modal__title">方案分享码</p>
              <p class="share-modal__hint">把分享码发给好友，好友在作坊右侧「分享码」入口输入即可一键还原此方案。</p>
              <div class="share-modal__code">{{ shareCodeResult }}</div>
              <div class="name-modal__actions">
                <button
                  class="name-modal__btn name-modal__btn--primary"
                  @click="copyShareCode"
                >
                  复制分享码
                </button>
                <button
                  class="name-modal__btn name-modal__btn--new"
                  @click="showShareModal = false"
                >
                  完成
                </button>
              </div>
            </template>
            <!-- 载入态：输入分享码 -->
            <template v-else>
              <p class="name-modal__title">分享码载入</p>
              <p class="share-modal__hint">输入好友分享给你的 16 位分享码，方案将还原到工作台。</p>
              <input
                class="name-modal__input share-modal__input"
                v-model="shareLoadInput"
                placeholder="输入分享码..."
                maxlength="16"
                @keyup.enter="confirmShareLoad"
              />
              <div class="name-modal__actions">
                <button
                  class="name-modal__btn name-modal__btn--primary"
                  :disabled="shareBusy"
                  @click="confirmShareLoad"
                >
                  {{ shareBusy ? '载入中…' : '载入到工作台' }}
                </button>
                <button
                  class="name-modal__btn name-modal__btn--new"
                  @click="showShareModal = false"
                >
                  取消
                </button>
              </div>
            </template>
            <button
              class="name-modal__close"
              @click="showShareModal = false"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 结算 / 支付面板（同层状态机：确认 → 支付 → 完成） ==================== -->
    <Teleport to="body">
      <Transition name="sheet">
        <div
          v-if="showCheckoutModal"
          class="co-overlay"
          @click.self="closeCheckout"
        >
          <div class="co-sheet">
            <!-- 头部：eyebrow + 标题 + 步骤轨 -->
            <header class="co-head">
              <div class="co-head__main">
                <span class="co-eyebrow">
                  {{ checkoutStep === 'form' ? 'CHECKOUT' : checkoutStep === 'pay' ? 'PAYMENT' : 'COMPLETE' }}
                </span>
                <h3 class="co-title">
                  {{ checkoutStep === 'form' ? '确认结算' : checkoutStep === 'pay' ? '完成支付' : '支付成功' }}
                </h3>
              </div>
              <div class="co-track">
                <i
                  v-for="(label, si) in ['确认', '支付', '完成']"
                  :key="label"
                  class="co-track__step"
                  :class="{
                    'co-track__step--on': si <= checkoutStepIndex,
                    'co-track__step--now': si === checkoutStepIndex,
                  }"
                >
                  <span class="co-track__dot"></span>{{ label }}
                </i>
              </div>
              <button class="co-close" aria-label="关闭" @click="closeCheckout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </header>

            <div class="co-body" :class="{ 'co-body--done': checkoutStep === 'done' }">
              <!-- 左：方案 / 订单清单（任何步骤都可见物品） -->
              <section v-if="checkoutStep !== 'done'" class="co-list">
                <div class="co-list__head">
                  <span>{{ checkoutStep === 'form' ? '方案清单' : '新生成订单' }}</span>
                  <span class="co-list__count">{{ checkoutDisplayItems.length }} 件</span>
                </div>
                <div class="co-list__scroll">
                  <article
                    v-for="(it, i) in checkoutDisplayItems"
                    :key="it.id"
                    class="co-item"
                    :data-cart-id="it.id"
                    :style="{ '--i': i }"
                  >
                    <div class="co-item__thumb">
                      <img v-if="it.thumb" :src="it.thumb" alt="">
                      <span v-else class="co-item__thumb--empty"></span>
                    </div>
                    <div class="co-item__info">
                      <b>{{ it.name }}</b>
                      <span>{{ it.meta }}</span>
                    </div>
                    <div class="co-item__right">
                      <span class="co-item__price"><em>¥</em>{{ it.amount }}</span>
                      <span v-if="checkoutStep === 'pay'" class="co-item__state" :class="{ 'co-item__state--paid': it.paid }">
                        {{ it.paid ? '已支付' : '待付款' }}
                      </span>
                    </div>
                  </article>
                </div>
                <div class="co-list__total">
                  <span>合计</span>
                  <div class="co-amount"><em>¥</em><b>{{ checkoutStep === 'form' ? cartSelectedTotal : checkoutTotal }}</b></div>
                </div>
              </section>

              <!-- 右：步骤内容 -->
              <section class="co-side">
                <!-- 01 收货信息 -->
                <template v-if="checkoutStep === 'form'">
                  <p class="co-side__hint">
                    {{ cartSelectedCount }} 个方案将拆分为 {{ cartSelectedCount }} 个独立订单，逐件手工定制
                  </p>
                  <!-- 地址簿：默认地址已自动填充，也可以切换成其他已保存地址 -->
                  <div class="co-addr">
                    <div class="co-addr__head">
                      <span>收货地址簿<i v-if="addresses.length">默认已填充</i></span>
                      <button class="co-addr__manage" @click="openAddressBook(true)">
                        {{ addresses.length ? '管理地址' : '去添加' }}
                      </button>
                    </div>
                    <div v-if="addresses.length" class="co-addr__chips">
                      <button
                        v-for="a in addresses"
                        :key="a.id"
                        class="co-addr__chip"
                        :class="{ 'co-addr__chip--on': checkoutAddressId === a.id }"
                        @click="pickCheckoutAddress(a)"
                      >
                        <span class="co-addr__chip-top">
                          <b>{{ a.receiverName }}</b>
                          <em v-if="a.isDefault === 1">默认</em>
                        </span>
                        <small>{{ a.receiverPhone }}</small>
                        <small class="co-addr__chip-addr">{{ a.region }} {{ a.detail }}</small>
                      </button>
                    </div>
                    <p v-else class="co-addr__empty">还没有保存的地址，可先手动填写，或去地址簿添加</p>
                  </div>
                  <div class="co-form">
                    <label class="co-field" :class="{ 'co-field--bad': checkoutErrors.receiverName }">
                      <span>收货人</span>
                      <input v-model="checkoutForm.receiverName" maxlength="20" placeholder="姓名" @input="clearCheckoutError('receiverName'); checkoutAddressId = null">
                      <small v-if="checkoutErrors.receiverName" class="co-field__err">{{ checkoutErrors.receiverName }}</small>
                    </label>
                    <label class="co-field" :class="{ 'co-field--bad': checkoutErrors.receiverPhone }">
                      <span>联系电话</span>
                      <input v-model="checkoutForm.receiverPhone" maxlength="11" placeholder="11 位手机号" @input="clearCheckoutError('receiverPhone'); checkoutAddressId = null">
                      <small v-if="checkoutErrors.receiverPhone" class="co-field__err">{{ checkoutErrors.receiverPhone }}</small>
                    </label>
                    <label class="co-field" :class="{ 'co-field--bad': checkoutErrors.receiverAddress }">
                      <span>收货地址</span>
                      <textarea v-model="checkoutForm.receiverAddress" maxlength="120" rows="2" placeholder="省 / 市 / 区 / 详细地址" @input="clearCheckoutError('receiverAddress'); checkoutAddressId = null"></textarea>
                      <small v-if="checkoutErrors.receiverAddress" class="co-field__err">{{ checkoutErrors.receiverAddress }}</small>
                    </label>
                    <label class="co-field">
                      <span>备注<i>选填</i></span>
                      <input v-model="checkoutForm.remark" maxlength="60" placeholder="手围、个性化要求">
                    </label>
                  </div>
                  <button class="co-go" :disabled="checkoutBusy" @click="submitCheckout">
                    <span>{{ checkoutBusy ? '正在生成订单…' : '提交订单' }}</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                      <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                    </svg>
                  </button>
                </template>

                <!-- 02 支付 -->
                <template v-else-if="checkoutStep === 'pay'">
                  <div class="co-pay">
                    <span class="co-pay__label">应付金额</span>
                    <div class="co-pay__amount"><em>¥</em><b>{{ checkoutTotal }}</b></div>
                    <p class="co-pay__hint">演示环境 · 点击即视为付款成功（mock 支付），订单立即进入待发货</p>
                  </div>
                  <div class="co-pay__rows">
                    <div v-for="o in checkoutOrders" :key="o.id" class="co-pay__row">
                      <span class="co-pay__row-name">{{ o.designName }}</span>
                      <span class="co-pay__row-amt">¥{{ o.totalAmount }}</span>
                      <button
                        v-if="o.status === 0"
                        class="co-pay__mini"
                        :disabled="!!payBusyId"
                        @click="payCreatedOrder(o)"
                      >
                        {{ payBusyId === o.id ? '处理中…' : '单独支付' }}
                      </button>
                      <span v-else class="co-pay__paid">已支付</span>
                    </div>
                  </div>
                  <button
                    v-if="checkoutPayLeft.length"
                    class="co-go"
                    :disabled="!!payBusyId"
                    @click="payAllCreated"
                  >
                    <span>{{ payBusyId ? '支付处理中…' : `支付 ${checkoutPayLeft.length} 笔订单` }}</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                      <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                    </svg>
                  </button>
                  <button v-else class="co-go" @click="finishCheckout">
                    <span>查看订单</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                      <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                    </svg>
                  </button>
                </template>

                <!-- 03 完成 -->
                <template v-else>
                  <div class="co-done">
                    <svg class="co-done__mark" viewBox="0 0 64 64">
                      <circle class="co-done__circle" cx="32" cy="32" r="27"/>
                      <path class="co-done__check" d="M19.5 33.5l8.5 8.5 16.5-18"/>
                    </svg>
                    <h4 class="co-done__title">支付成功</h4>
                    <p class="co-done__desc">
                      共 {{ checkoutOrders.length }} 个订单已进入待发货，设计师会按手围逐件制作
                    </p>
                    <button class="co-go" @click="finishCheckout">
                      <span>查看我的订单</span>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                        <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                      </svg>
                    </button>
                  </div>
                </template>
              </section>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 我的订单面板 ==================== -->
    <Teleport to="body">
      <Transition name="sheet">
        <div
          v-if="showOrdersModal"
          class="co-overlay"
          @click.self="showOrdersModal = false"
        >
          <div class="ord-sheet">
            <header class="ord-head">
              <div class="ord-head__main">
                <span class="co-eyebrow">MY ORDERS</span>
                <h3 class="co-title">我的订单</h3>
              </div>
              <span class="ord-head__count">{{ myOrders.length }} 笔</span>
              <button class="co-close" aria-label="关闭" @click="showOrdersModal = false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </header>

            <div class="ord-body">
              <aside class="ord-nav">
                <button
                  v-for="t in ORDER_TABS"
                  :key="String(t.v)"
                  class="ord-nav__item"
                  :class="{ 'ord-nav__item--on': orderFilter === t.v }"
                  @click="switchOrderFilter(t.v)"
                >
                  <span class="ord-nav__dot"></span>{{ t.label }}
                </button>
              </aside>

              <main class="ord-main">
                <div v-if="ordersLoading" class="ord-loading">
                  <i class="ord-loading__ring"></i>正在载入订单…
                </div>
                <div v-else-if="!myOrders.length" class="ord-empty">
                  <span class="ord-empty__mark">◌</span>
                  <p>这个状态下还没有订单</p>
                </div>
                <div v-else :key="String(orderFilter)" class="ord-list">
                  <article
                    v-for="(o, i) in myOrders"
                    :key="o.id"
                    class="ord-card"
                    :style="{ '--i': i }"
                  >
                    <div class="ord-card__thumb">
                      <img v-if="orderThumbnails[o.id]" :src="orderThumbnails[o.id]" alt="">
                      <span v-else class="ord-card__thumb--empty"></span>
                    </div>
                    <div class="ord-card__main">
                      <div class="ord-card__top">
                        <b class="ord-card__name">{{ o.designName }}</b>
                        <span class="ord-card__state" :class="'ord-card__state--' + o.status">{{ o.statusText }}</span>
                      </div>
                      <div class="ord-card__chips">
                        <span>{{ o.beadCount }} 颗</span>
                        <span>{{ fmtOrderTime(o.createTime) }}</span>
                        <span class="ord-card__no">{{ o.orderNo }}</span>
                      </div>
                      <p v-if="o.shipNo" class="ord-card__note">物流 {{ o.shipNo }}</p>
                      <p v-if="o.remark" class="ord-card__note">备注 {{ o.remark }}</p>
                      <div class="ord-card__foot">
                        <div class="ord-card__amount"><em>¥</em>{{ o.totalAmount }}</div>
                        <div class="ord-card__ops">
                          <button v-if="o.status === 0" class="ord-op ord-op--go" @click="openPayForOrder(o)">立即支付</button>
                          <button v-if="o.status === 0" class="ord-op" @click="askOrderAction(o, 'cancel')">取消</button>
                          <button v-if="o.status === 1 || o.status === 2" class="ord-op" @click="askOrderAction(o, 'refund')">申请退款</button>
                          <button v-if="o.status === 2" class="ord-op ord-op--go" @click="askOrderAction(o, 'confirm')">确认收货</button>
                        </div>
                      </div>
                      <Transition name="ord-fade">
                        <div v-if="orderConfirm && orderConfirm.id === o.id" class="ord-card__confirm">
                          <span>{{ orderConfirm.tip }}</span>
                          <button class="ord-op ord-op--go" @click="runOrderAction(o)">确定</button>
                          <button class="ord-op" @click="orderConfirm = null">再想想</button>
                        </div>
                      </Transition>
                    </div>
                  </article>
                </div>
              </main>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 收货地址簿面板 ==================== -->
    <Teleport to="body">
      <Transition name="sheet">
        <div
          v-if="showAddressModal"
          class="co-overlay addr-overlay"
          @click.self="closeAddressBook"
        >
          <div class="co-sheet addr-sheet">
            <header class="co-head">
              <div class="co-head__main">
                <span class="co-eyebrow">ADDRESS BOOK</span>
                <h3 class="co-title">{{ addressFormOpen ? (addressEditingId ? '编辑地址' : '新增地址') : '收货地址' }}</h3>
              </div>
              <span class="addr-head__count">{{ addresses.length }} 个</span>
              <button class="co-close" aria-label="关闭" @click="closeAddressBook">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </header>

            <div class="addr-body">
              <!-- 列表 -->
              <template v-if="!addressFormOpen">
                <p class="addr-hint">先保存常用地址，之后结算会自动填充默认地址</p>
                <div v-if="addresses.length" class="addr-list">
                  <article
                    v-for="a in addresses"
                    :key="a.id"
                    class="addr-card"
                    :class="{ 'addr-card--default': a.isDefault === 1 }"
                  >
                    <div class="addr-card__main">
                      <div class="addr-card__line">
                        <b>{{ a.receiverName }}</b>
                        <span class="addr-card__phone">{{ a.receiverPhone }}</span>
                        <span v-if="a.isDefault === 1" class="addr-card__tag">默认</span>
                      </div>
                      <p class="addr-card__text">{{ a.region }} {{ a.detail }}</p>
                      <div class="addr-card__acts">
                        <button v-if="a.isDefault !== 1" class="addr-card__act" @click="setDefaultAddress(a)">设为默认</button>
                        <button class="addr-card__act" @click="openAddressForm(a)">编辑</button>
                        <button class="addr-card__act addr-card__act--del" @click="askDeleteAddress(a)">删除</button>
                      </div>
                    </div>
                    <Transition name="ord-fade">
                      <div v-if="addressConfirmId === a.id" class="addr-confirm">
                        <span>删除后不可恢复</span>
                        <button class="addr-confirm__ok" @click="confirmDeleteAddress">确认删除</button>
                        <button class="addr-confirm__no" @click="addressConfirmId = null">再想想</button>
                      </div>
                    </Transition>
                  </article>
                </div>
                <div v-else class="addr-empty">
                  <span class="addr-empty__mark">◌</span>
                  <p>还没有收货地址，添加一条后结算会自动填充</p>
                </div>
                <button class="co-go" @click="openAddressForm(null)">
                  <span>新增收货地址</span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                    <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                  </svg>
                </button>
              </template>

              <!-- 表单 -->
              <template v-else>
                <div class="co-form addr-form">
                  <div class="addr-form__row">
                    <label class="co-field" :class="{ 'co-field--bad': addressErrors.receiverName }">
                      <span>收货人</span>
                      <input v-model="addressForm.receiverName" maxlength="20" placeholder="姓名" @input="clearAddressError('receiverName')">
                      <small v-if="addressErrors.receiverName" class="co-field__err">{{ addressErrors.receiverName }}</small>
                    </label>
                    <label class="co-field" :class="{ 'co-field--bad': addressErrors.receiverPhone }">
                      <span>联系电话</span>
                      <input v-model="addressForm.receiverPhone" maxlength="11" placeholder="11 位手机号" @input="clearAddressError('receiverPhone')">
                      <small v-if="addressErrors.receiverPhone" class="co-field__err">{{ addressErrors.receiverPhone }}</small>
                    </label>
                  </div>
                  <label class="co-field" :class="{ 'co-field--bad': addressErrors.region }">
                    <span>所在地区</span>
                    <input v-model="addressForm.region" maxlength="60" placeholder="省 / 市 / 区" @input="clearAddressError('region')">
                    <small v-if="addressErrors.region" class="co-field__err">{{ addressErrors.region }}</small>
                  </label>
                  <label class="co-field" :class="{ 'co-field--bad': addressErrors.detail }">
                    <span>详细地址</span>
                    <textarea v-model="addressForm.detail" maxlength="120" rows="2" placeholder="街道 / 门牌号 / 小区楼栋" @input="clearAddressError('detail')"></textarea>
                    <small v-if="addressErrors.detail" class="co-field__err">{{ addressErrors.detail }}</small>
                  </label>
                  <label class="addr-form__check">
                    <input type="checkbox" v-model="addressForm.isDefault">
                    <span>设为默认地址（结算时自动填充）</span>
                  </label>
                </div>
                <div class="addr-form__ops">
                  <button class="addr-form__cancel" :disabled="addressBusy" @click="backToAddressList">返回列表</button>
                  <button class="co-go addr-form__save" :disabled="addressBusy" @click="saveAddress">
                    <span>{{ addressBusy ? '保存中…' : '保存地址' }}</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                      <path d="M4 12h15"/><path d="M13 6l6 6-6 6"/>
                    </svg>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== 登录/注册弹窗 ==================== -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showAuthModal"
          class="modal-overlay"
          @click.self="closeAuthModal"
        >
          <div class="name-modal auth-modal">
            <p class="name-modal__title">{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</p>
            <p class="auth-modal__subtitle">
              {{ authMode === 'login' ? '登录后同步你的方案与购物车' : '注册即自动登录，开始你的手串设计' }}
            </p>
            <input
              class="name-modal__input"
              v-model="authForm.username"
              placeholder="用户名（3-20 位）"
              maxlength="20"
              @keyup.enter="submitAuth"
            />
            <input
              v-if="authMode === 'register'"
              class="name-modal__input"
              v-model="authForm.nickname"
              placeholder="昵称（可选，默认同用户名）"
              maxlength="20"
              @keyup.enter="submitAuth"
            />
            <input
              class="name-modal__input"
              type="password"
              v-model="authForm.password"
              :placeholder="authMode === 'login' ? '密码' : '密码（至少 6 位）'"
              maxlength="32"
              @keyup.enter="submitAuth"
            />
            <button class="auth-modal__btn" :disabled="authSubmitting" @click="submitAuth">
              {{ authSubmitting ? '请稍候…' : (authMode === 'login' ? '登 录' : '注册并登录') }}
            </button>
            <p class="auth-modal__switch">
              {{ authMode === 'login' ? '还没有账号？' : '已有账号？' }}
              <button @click="switchAuthMode(authMode === 'login' ? 'register' : 'login')">
                {{ authMode === 'login' ? '去注册' : '去登录' }}
              </button>
            </p>
            <button class="name-modal__close" @click="closeAuthModal">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== Shell / Layout ==================== */
.app-shell {
  position: relative;
  width: 100vw;
  height: 100vh;
  background-color: #FBFAF6;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== Top Navigation Bar ==================== */
.top-nav {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 12px 20px 0;
  z-index: 60;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.top-nav__bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 5px 40px;
  background: #c7cdc0;
  border-radius: 9999px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.top-nav__tab {
  padding: 5px 22px;
  border: none;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.35);
  color: #333;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.04em;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.top-nav__tab:hover {
  background: rgba(255, 255, 255, 0.5);
}

.top-nav__tab--active {
  background: #657166;
  color: #fff;
  font-weight: 600;
}

.top-nav__tab--active:hover {
  background: #657166;
}

.top-nav--hidden {
  transform: translateY(calc(-100% - 20px));
  opacity: 0;
  pointer-events: none;
}

/* ==================== App Body (Sidebar + Main) ==================== */
.app-body {
  flex: 1;
  display: flex;
  flex-direction: row;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
}

/* ==================== Design Steps Sidebar ==================== */
.design-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 12px 20px 20px;
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  overflow: visible;
  z-index: 45;
  background: linear-gradient(to right, rgba(251, 250, 246, 0.95) 60%, rgba(251, 250, 246, 0) 100%);
  pointer-events: auto;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.design-sidebar--hidden {
  transform: translateX(calc(-100% - 20px));
  opacity: 0;
  pointer-events: none;
}

.design-sidebar__title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 28px 4px;
  letter-spacing: 0.06em;
}

/* Steps container */
.design-sidebar__steps {
  display: flex;
  flex-direction: column;
  position: relative;
  gap: 4px;
}

/* Vertical connecting line */
.design-sidebar__steps::before {
  content: '';
  position: absolute;
  left: 19px;
  top: 6px;
  bottom: 6px;
  width: 1.5px;
  background: #d5cec2;
  z-index: 0;
  border-radius: 1px;
}

/* Step Item */
.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
  cursor: default;
}

.step-item--active {
  background: #1a1a1a;
}

/* Step indicator circle */
.step-item__indicator {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
}

.step-item--active .step-item__indicator {
  background: #fff;
}

.step-item:not(.step-item--active) .step-item__indicator {
  background: #e8e3da;
}

.step-item__number {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  transition: color 0.3s ease;
}

.step-item--active .step-item__number {
  color: #1a1a1a;
}

.step-item:not(.step-item--active) .step-item__number {
  color: #9b8e82;
}

/* Step text body */
.step-item__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-top: 2px;
}

.step-item__title {
  font-size: 13px;
  font-weight: 600;
  margin: 0;
  line-height: 1.3;
  transition: color 0.3s ease;
  letter-spacing: 0.03em;
}

.step-item--active .step-item__title {
  color: #fff;
}

.step-item:not(.step-item--active) .step-item__title {
  color: #1a1a1a;
}

.step-item__desc {
  font-size: 10.5px;
  margin: 0;
  line-height: 1.4;
  transition: color 0.3s ease;
  letter-spacing: 0.02em;
}

.step-item--active .step-item__desc {
  color: rgba(255, 255, 255, 0.6);
}

.step-item:not(.step-item--active) .step-item__desc {
  color: #b8a58d;
}

/* ==================== Decorative Beads ==================== */
.design-sidebar__decor {
  position: absolute;
  right: -60px;
  top: 40px;
  bottom: 60px;
  width: 120px;
  pointer-events: none;
  z-index: 0;
}

/* 手串态：浅米白纯色圆形背景 */
.decor-bg-ring {
  position: absolute;
  right: -18px;
  top: 340px;
  transform: translateY(-50%);
  width: 78px;
  height: 78px;
  border-radius: 50%;
  background: rgba(251, 250, 246, 0.92);
  box-shadow:
    0 2px 12px rgba(180, 160, 140, 0.12),
    0 0 0 1px rgba(180, 160, 140, 0.06);
  opacity: 0;
  animation: bgRingAppear 0.5s ease-out 0.25s forwards;
}

@keyframes bgRingAppear {
  0%   { opacity: 0; transform: translateY(-50%) scale(0.7); }
  100% { opacity: 1; transform: translateY(-50%) scale(1); }
}

.decor-bead {
  position: absolute;
  border-radius: 50%;
  transition: width 0.6s ease, height 0.6s ease;
  will-change: top, right, opacity;
  /* 底色兜底（图片加载前可见） */
  background: radial-gradient(circle at 50% 50%,
    rgba(245, 210, 200, 0.25),
    rgba(220, 185, 170, 0.15)
  );
  /* 内外发光 → 玻璃边缘光 + 环境光遮蔽，深度由 JS 动态补充 */
  box-shadow:
    inset 0 -1px 3px rgba(180, 140, 130, 0.22),
    inset 0 1px 2px rgba(255, 255, 255, 0.22),
    0 1px 4px rgba(180, 140, 130, 0.22),
    0 2px 8px rgba(160, 120, 110, 0.12);
  /* 微透视：近珠底部阴影更重，由 inline filter 增强 */
  overflow: hidden;
  transition: box-shadow 0.5s ease;
}

/* ── 水晶纹理图：圆形裁剪 + 柔和显示 ── */
.decor-bead__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.45s ease;
  pointer-events: none;
}

.decor-bead__img--loaded {
  opacity: 0.92;
}

/* ── 高光斑点：玻璃球面镜面反射 ── */
.decor-bead__highlight {
  position: absolute;
  top: 12%;
  left: 18%;
  width: 32%;
  height: 28%;
  border-radius: 50%;
  background: radial-gradient(ellipse at center,
    rgba(255, 255, 255, 0.85) 0%,
    rgba(255, 255, 255, 0.4) 35%,
    rgba(255, 255, 255, 0) 100%
  );
  transform: rotate(-15deg);
  pointer-events: none;
}

/* ── 底部反光：玻璃球底部微弱二次反射 ── */
.decor-bead__bounce {
  position: absolute;
  bottom: 8%;
  left: 25%;
  width: 40%;
  height: 16%;
  border-radius: 50%;
  background: radial-gradient(ellipse at center,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.06) 60%,
    rgba(255, 255, 255, 0) 100%
  );
  pointer-events: none;
}

/* ── 悬浮动画：6 组不同节奏错落起伏 ── */
@keyframes floatA { 0%,100% { margin-top: 0; } 50% { margin-top: -8px; } }
@keyframes floatB { 0%,100% { margin-top: 0; } 50% { margin-top: -6px; } }
@keyframes floatC { 0%,100% { margin-top: 0; } 50% { margin-top: -10px; } }
@keyframes floatD { 0%,100% { margin-top: 0; } 50% { margin-top: -5px; } }
@keyframes floatE { 0%,100% { margin-top: 0; } 50% { margin-top: -9px; } }
@keyframes floatF { 0%,100% { margin-top: 0; } 50% { margin-top: -7px; } }

.decor-bead--float-1 { animation: floatA 3.0s ease-in-out infinite; }
.decor-bead--float-2 { animation: floatB 3.3s ease-in-out infinite 0.35s; }
.decor-bead--float-3 { animation: floatC 2.7s ease-in-out infinite 0.7s; }
.decor-bead--float-4 { animation: floatD 3.1s ease-in-out infinite 1.05s; }
.decor-bead--float-5 { animation: floatE 2.9s ease-in-out infinite 1.4s; }
.decor-bead--float-6 { animation: floatF 3.4s ease-in-out infinite 0.55s; }

/* 手串/收拢状态：暂停悬浮动画 */
.design-sidebar__decor--bracelet .decor-bead {
  animation-play-state: paused;
}

/* ==================== Canvas Area ==================== */
.canvas-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px 20px;
  min-height: 0;
}

.canvas-area__inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.canvas-container {
  --canvas-size: min(
    calc(100vw - 200px),
    calc(100vh - 430px)
  );
  /* 挂件渲染外拓边距：pendant 向外偏移时需要额外画布空间 */
  --canvas-overflow: calc(var(--canvas-size) * 0.18);
  width: var(--canvas-size);
  height: var(--canvas-size);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: 16px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(0, 0, 0, 0.04),
    inset 0 0 0 1px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: visible;
  transition: box-shadow 0.6s ease, border-radius 0.6s ease;
}

.bead-canvas {
  position: absolute;
  /* 外拓画布：超出底盘边界的挂件也能绘制 */
  inset: calc(-1 * var(--canvas-overflow));
  width: calc(100% + 2 * var(--canvas-overflow));
  height: calc(100% + 2 * var(--canvas-overflow));
  touch-action: none;
}

.bead-canvas--delete-mode {
  cursor: crosshair;
}

.canvas-status {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(139, 111, 78, 0.35);
  font-size: 15px;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.1em;
  pointer-events: none;
  user-select: none;
  transition: all 0.5s ease;
}
.canvas-status--bracelet {
  color: rgba(139, 111, 78, 0.6);
  font-size: 20px;
  font-weight: 500;
  animation: statusPulse 2s ease-in-out infinite;
}
@keyframes statusPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ==================== Right Floating Action Bar ==================== */
.floating-bar {
  position: absolute;
  left: calc(100% + 20px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.floating-bar--hidden {
  transform: translateY(-50%) translateX(40px);
  opacity: 0;
  pointer-events: none;
}

/* ---- 展示模式覆盖层：单击任意处退出 ---- */
.showcase-overlay {
  position: fixed;
  inset: 0;
  z-index: 7900;
  cursor: pointer;
  background: transparent;
  animation: showcaseFade .45s ease both;
}

.showcase-overlay__hint {
  position: fixed;
  left: 50%;
  bottom: 44px;
  transform: translateX(-50%);
  padding: 9px 22px;
  border-radius: 999px;
  background: rgba(26, 26, 26, .58);
  color: #FBFAF6;
  font-size: 13px;
  letter-spacing: 2px;
  backdrop-filter: blur(8px);
  animation: showcaseFade .55s ease both;
}

@keyframes showcaseFade {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ---- 游客演示模式提示条 ---- */
.demo-pill {
  position: fixed;
  left: 16px;
  bottom: 16px;
  z-index: 9600;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(200, 169, 110, .92);
  color: #241D15;
  font-size: 12px;
  letter-spacing: 1px;
  box-shadow: 0 4px 14px rgba(36, 29, 21, .18);
  pointer-events: none;
  animation: showcaseFade .5s ease both;
}

/* ---- 胶囊容器 ---- */
.floating-bar__capsule {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  background: #ffffff;
  border-radius: 9999px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(0, 0, 0, 0.04);
}

/* ---- 图标按钮容器 ---- */
.floating-bar__icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

/* ---- 图标按钮 ---- */
.floating-bar__icon-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #4a3a2e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 8px;
  flex-shrink: 0;
}

.floating-bar__icon-btn svg {
  width: 20px;
  height: 20px;
}

.floating-bar__icon-btn:hover {
  background: rgba(200, 169, 110, 0.1);
  color: #C8A96E;
  transform: scale(1.08);
}

.floating-bar__icon-btn:active {
  transform: scale(0.95);
}

.floating-bar__icon-btn--active {
  color: #C8A96E;
  background: rgba(200, 169, 110, 0.12);
}

.floating-bar__icon-btn--danger:hover {
  background: rgba(220, 100, 80, 0.08);
  color: #d4654a;
}

/* ---- 黑色气泡提示 ---- */
.floating-bar__tooltip {
  position: absolute;
  right: calc(100% + 14px);
  top: 50%;
  transform: translateY(-50%);
  padding: 6px 14px;
  background: #1a1a1a;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  border-radius: 8px;
  letter-spacing: 0.04em;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
  transform-origin: right center;
  transform: translateY(-50%) scale(0.9);
}

/* 气泡右侧尖角箭头 */
.floating-bar__tooltip::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  border-left: 6px solid #1a1a1a;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
}

.floating-bar__icon-wrap:hover .floating-bar__tooltip,
.floating-bar__delete-wrap:hover .floating-bar__tooltip {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

/* ---- 底部独立删除珠子按钮 ---- */
.floating-bar__delete-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.floating-bar__delete-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: #8B6F4E;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  box-shadow:
    0 3px 14px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
}

.floating-bar__delete-btn svg {
  width: 20px;
  height: 20px;
}

.floating-bar__delete-btn:hover {
  background: rgba(220, 100, 80, 0.06);
  color: #d4654a;
  box-shadow:
    0 4px 18px rgba(0, 0, 0, 0.1),
    0 1px 6px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.floating-bar__delete-btn:active {
  transform: scale(0.93);
}

.floating-bar__delete-btn--active {
  background: #fdf2f0;
  color: #d4654a;
  box-shadow:
    0 0 0 3px rgba(220, 100, 80, 0.15),
    0 4px 18px rgba(0, 0, 0, 0.08);
  animation: deletePulse 1.8s ease-in-out infinite;
}

@keyframes deletePulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(220, 100, 80, 0.15), 0 4px 18px rgba(0, 0, 0, 0.08); }
  50% { box-shadow: 0 0 0 8px rgba(220, 100, 80, 0.05), 0 4px 20px rgba(0, 0, 0, 0.12); }
}

/* ==================== 收拢成串 / 解除手串按钮 ==================== */
.string-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 28px;
  border: none;
  border-radius: 9999px;
  background: #abd1dc;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.06em;
  cursor: pointer;
  box-shadow:
    0 3px 14px rgba(200, 220, 230, 0.35),
    0 1px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  position: absolute;
  left: calc(100% + 16px);
  bottom: 0;
  white-space: nowrap;
  z-index: 35;
}

.string-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 6px 22px rgba(200, 220, 230, 0.45),
    0 2px 8px rgba(0, 0, 0, 0.06);
}

.string-btn:active:not(:disabled) {
  transform: scale(0.96);
}

.string-btn:disabled {
  cursor: not-allowed;
  opacity: 0.85;
}

.string-btn--hidden {
  opacity: 0;
  pointer-events: none;
  transform: translateY(12px);
}

/* 激活态（解除手串） */
.string-btn--active {
  background: rgba(220, 100, 80, 0.08);
  color: #d4654a;
  box-shadow:
    0 3px 14px rgba(220, 100, 80, 0.12),
    0 1px 4px rgba(0, 0, 0, 0.04);
}

.string-btn--active:hover:not(:disabled) {
  background: rgba(220, 100, 80, 0.13);
  box-shadow:
    0 6px 22px rgba(220, 100, 80, 0.18),
    0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 加载态 */
.string-btn--loading {
  background: #96bdc9;
}

/* ---- 环形转圈加载图标 ---- */
.string-btn__spinner {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.string-btn__spinner svg {
  width: 100%;
  height: 100%;
}

.string-btn__spinner-track {
  opacity: 0.25;
}

.string-btn__spinner-head {
  opacity: 0.85;
  transform-origin: center;
  animation: stringSpinner 0.9s linear infinite;
}

.string-btn--active .string-btn__spinner-head {
  animation-play-state: paused;
  stroke-dasharray: 63 0;
  opacity: 0.55;
}

.string-btn--loading .string-btn__spinner-head {
  animation-duration: 0.6s;
}

@keyframes stringSpinner {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ---- 按钮文字 ---- */
.string-btn__text {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.06em;
}

.string-btn--active .string-btn__text {
  color: #d4654a;
}

/* ---- Responsive ---- */
@media (max-width: 640px) {
  .string-btn {
    padding: 10px 22px;
    gap: 8px;
    bottom: -44px;
    font-size: 13px;
  }
  .string-btn__spinner {
    width: 17px;
    height: 17px;
  }
  .string-btn__text {
    font-size: 13px;
  }
}

/* ==================== Blind Box Modal ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 8000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(6px);
  padding: 24px;
}

.modal-card {
  background: #FBFAF6;
  border-radius: 20px;
  padding: 32px;
  max-width: 520px;
  width: 100%;
  box-shadow:
    0 16px 48px rgba(0, 0, 0, 0.12),
    0 4px 12px rgba(0, 0, 0, 0.06);
}

.modal-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.modal-card__title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.05em;
  margin: 0;
}

.modal-card__close {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.04);
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  transition: all 0.2s ease;
}
.modal-card__close svg { width: 18px; height: 18px; }
.modal-card__close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #333;
}

.modal-card__subtitle {
  font-size: 13px;
  color: #9B8E82;
  margin: 0 0 20px;
  letter-spacing: 0.03em;
}

.modal-card__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.plan-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 16px;
  border: 1.5px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.25s ease;
  font-family: inherit;
  text-align: center;
}

.plan-card:hover {
  border-color: #C8A96E;
  background: rgba(200, 169, 110, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.plan-card:active {
  transform: scale(0.97);
}

/* ---- 方案卡删除：右上角 × + 卡片内联确认 ---- */
.plan-del {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.86);
  color: #9B8E82;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.85);
  transition: opacity 0.2s ease, transform 0.2s ease, color 0.2s ease, background 0.2s ease;
  z-index: 2;
}
.plan-del svg { width: 12px; height: 12px; }
.plan-card:hover .plan-del { opacity: 1; transform: scale(1); }
.plan-del:hover { background: #1a1a1a; color: #fff; }

.plan-card--asking { border-color: #C8A96E; }
.plan-card--asking:hover { transform: none; }

.plan-confirm {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 6px;
  padding: 14px 12px;
  border-radius: 14px;
  background: rgba(251, 250, 246, 0.96);
  backdrop-filter: blur(6px) saturate(1.1);
  cursor: default;
  text-align: center;
}
.plan-confirm b {
  font-size: 12.5px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.02em;
}
.plan-confirm p {
  margin: 0;
  font-size: 11px;
  line-height: 1.45;
  color: #9B8E82;
}
.plan-confirm__ops { display: flex; gap: 8px; margin-top: 4px; }
.plan-confirm__ops button {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  font-family: inherit;
  font-size: 11.5px;
  color: #6b6257;
  cursor: pointer;
  transition: all 0.2s ease;
}
.plan-confirm__no:hover { border-color: #C8A96E; color: #1a1a1a; }
.plan-confirm__yes {
  border-color: transparent !important;
  background: linear-gradient(135deg, #241D15, #3a3128) !important;
  color: #F5EFE3 !important;
}
.plan-confirm__yes:hover { box-shadow: 0 4px 12px rgba(36, 29, 21, 0.28); }
.plan-confirm__ops button:disabled { opacity: 0.55; cursor: default; }

.plan-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(200, 169, 110, 0.15), rgba(200, 169, 110, 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #C8A96E;
  margin-bottom: 2px;
}
.plan-card__icon svg { width: 24px; height: 24px; }

.plan-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: 0.04em;
}

.plan-card__desc {
  font-size: 11px;
  color: #9B8E82;
  margin: 0;
  line-height: 1.4;
}

.plan-card__count {
  font-size: 10px;
  color: #C8A96E;
  font-weight: 500;
  background: rgba(200, 169, 110, 0.08);
  padding: 3px 10px;
  border-radius: 9999px;
}

/* Modal transitions */
.modal-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.modal-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.modal-enter-from {
  opacity: 0;
}
.modal-enter-from .modal-card {
  transform: scale(0.92) translateY(12px);
  opacity: 0;
}
.modal-leave-to {
  opacity: 0;
}
.modal-leave-to .modal-card {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}

/* ==================== Material Panel (居中面板) ==================== */
.material-panel {
  width: 62vw;
  max-width: 960px;
  min-width: 520px;
  height: 280px;
  flex-shrink: 0;
  align-self: center;
  margin-bottom: 24px;
  background: rgba(242, 238, 232, 0.96);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.06),
    inset 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 40;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.material-panel--hidden {
  transform: translateY(calc(100% + 32px));
  opacity: 0;
  pointer-events: none;
}

/* 主分类标签行 */
.material-panel__tabs {
  display: flex;
  justify-content: center;
  gap: 0;
  padding: 14px 20px 0;
  flex-shrink: 0;
}

.material-panel__tab {
  position: relative;
  padding: 8px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #9B8E82;
  font-family: inherit;
  letter-spacing: 0.04em;
  transition: color 0.2s ease;
}

.material-panel__tab::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 60%;
  height: 2px;
  background: #6B4F3E;
  border-radius: 1px;
  transition: transform 0.25s ease;
}

.material-panel__tab:hover {
  color: #6B4F3E;
}

.material-panel__tab--active {
  color: #4a3a2e;
}

.material-panel__tab--active::after {
  transform: translateX(-50%) scaleX(1);
}

/* 内容区：侧边栏 + 网格 */
.material-panel__body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 左侧子分类栏 */
.material-panel__sidebar {
  width: 30%;
  min-width: 90px;
  max-width: 120px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 6px 10px 12px;
  overflow-y: auto;
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.material-panel__subtab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px 7px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  color: #8B6F4E;
  white-space: nowrap;
  transition: all 0.15s ease;
  font-family: inherit;
  text-align: left;
}

.material-panel__subtab:hover {
  background: rgba(200, 169, 110, 0.08);
  color: #6B4F3E;
}

.material-panel__subtab--active {
  background: rgba(200, 169, 110, 0.12);
  color: #4a3a2e;
  font-weight: 600;
}

.material-panel__subtab-label {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.material-panel__subtab-count {
  font-size: 9px;
  color: #B8A58D;
  margin-left: 4px;
  flex-shrink: 0;
}

.material-panel__subtab--active .material-panel__subtab-count {
  color: #8B6F4E;
}

/* 右侧素材网格 */
.material-panel__grid-wrap {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 12px 12px;
  min-height: 0;
}

.material-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(74px, 1fr));
  gap: 8px;
}

.material-panel__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #B8A58D;
  font-size: 13px;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.04em;
}

/* ==================== Bead Item ==================== */
.bead-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px 4px;
  border: 1.5px solid transparent;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}
.bead-item:hover {
  background: rgba(200, 169, 110, 0.08);
  border-color: rgba(200, 169, 110, 0.3);
}
.bead-item--selected {
  background: rgba(200, 169, 110, 0.12);
  border-color: #C8A96E;
}

/* 点击放大动画 */
.bead-item--clicked .bead-item__img-wrap {
  animation: beadClickPulse 0.3s ease-out;
}

@keyframes beadClickPulse {
  0% { transform: scale(1); }
  40% { transform: scale(1.18); }
  100% { transform: scale(1); }
}

.bead-item__img-wrap {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f2ed;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.bead-item__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.25s ease;
}
.bead-item:hover .bead-item__img {
  transform: scale(1.1);
}

.bead-item__name {
  font-size: 9px;
  color: #8B6F4E;
  text-align: center;
  line-height: 1.2;
  max-width: 68px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bead-item__size {
  display: flex;
  align-items: center;
  gap: 4px;
}

.bead-item__size-btn {
  width: 20px;
  height: 20px;
  border: 1px solid rgba(139, 111, 78, 0.25);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  color: #6B4F3E;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.15s ease;
  font-family: inherit;
  flex-shrink: 0;
}

.bead-item__size-btn:hover:not(:disabled) {
  background: #C8A96E;
  color: #fff;
  border-color: #C8A96E;
}

.bead-item__size-btn:active:not(:disabled) {
  transform: scale(0.9);
}

.bead-item__size-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.bead-item__size-val {
  font-size: 9px;
  color: #C8A96E;
  font-weight: 500;
  min-width: 26px;
  text-align: center;
  user-select: none;
}

/* ==================== Scrollbar ==================== */
.material-panel__sidebar,
.material-panel__grid-wrap {
  scrollbar-width: thin;
  scrollbar-color: #d4c5b2 transparent;
}
.material-panel__sidebar::-webkit-scrollbar,
.material-panel__grid-wrap::-webkit-scrollbar {
  width: 3px;
}
.material-panel__sidebar::-webkit-scrollbar-track,
.material-panel__grid-wrap::-webkit-scrollbar-track {
  background: transparent;
}
.material-panel__sidebar::-webkit-scrollbar-thumb,
.material-panel__grid-wrap::-webkit-scrollbar-thumb {
  background: #d4c5b2;
  border-radius: 2px;
}

/* ==================== Nav Badge ==================== */
.top-nav__tab {
  position: relative;
}

.top-nav__badge {
  position: absolute;
  top: -6px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #E85D5D;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  letter-spacing: 0;
  box-shadow: 0 1px 4px rgba(232, 93, 93, 0.4);
  pointer-events: none;
}

/* ==================== Order / Cart Page ==================== */
.order-page {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px 28px 32px;
  min-height: 0;
  overflow: hidden;
}

/* ---- 外层大圆角白色容器（与灵感页一致） ---- */
.order-page__card {
  width: 100%;
  max-width: 1120px;
  max-height: 100%;
  background: #ffffff;
  border-radius: 32px;
  padding: 20px 28px 80px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.05),
    0 1px 4px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: visible;
  scrollbar-width: none;
}
.order-page__card::-webkit-scrollbar {
  display: none;
}

/* Header */
.order-page__header {
  margin-bottom: 20px;
}

.order-page__title {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Noto Serif SC', serif;
  color: #3a2e24;
  letter-spacing: 0.08em;
  margin: 0;
  line-height: 1.2;
}

.order-page__subtitle {
  font-size: 12px;
  color: #9B8E82;
  margin: 0;
  letter-spacing: 0.03em;
}

/* Empty */
.order-page__empty {
  text-align: center;
  padding: 48px 0;
  color: #B8A58D;
  font-size: 13px;
  letter-spacing: 0.04em;
}

.order-page__empty-icon {
  display: block;
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.order-page__empty p {
  margin: 0;
}

/* List */
.order-page__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Cart Item */
.cart-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 16px;
  background: #FBFAF6;
  position: relative;
  transition: all 0.2s ease;
}

.cart-item:hover {
  border-color: rgba(200, 169, 110, 0.2);
  background: #FDFCFA;
}

/* ① 复选框 */
.cart-item__check-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}

.cart-item__check {
  display: none;
}

.cart-item__check-mark {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #d5cec2;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.cart-item__check:checked + .cart-item__check-mark {
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.cart-item__check:checked + .cart-item__check-mark::after {
  content: '';
  display: block;
  width: 6px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) translateY(-1px);
}

/* ② 缩略图 */
.cart-item__thumb {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0ede8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.cart-item__thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.cart-item__thumb-skeleton {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 180, 160, 0.2), rgba(180, 160, 140, 0.08));
  animation: thumbPulse 1.5s ease-in-out infinite;
}

/* ③ 文字信息 */
.cart-item__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cart-item__name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item__wrist {
  font-size: 11px;
  color: #9B8E82;
  letter-spacing: 0.02em;
}

.cart-item__price {
  font-size: 13px;
  font-weight: 600;
  color: #C8A96E;
}

/* ④ 删除按钮 */
.cart-item__remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #c5c0b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.cart-item__remove svg {
  width: 14px;
  height: 14px;
}

.cart-item__remove:hover {
  background: rgba(220, 100, 80, 0.08);
  color: #d4654a;
}

/* Bottom Bar — 固定在页面最底端 */
.order-page__bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1120px;
  padding: 0 28px 20px;
  z-index: 70;
}

.order-page__bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-radius: 16px;
  padding: 14px 18px;
  box-shadow:
    0 -4px 24px rgba(0, 0, 0, 0.08),
    0 -1px 4px rgba(0, 0, 0, 0.04);
}

.order-page__bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-page__bar-label {
  font-size: 13px;
  color: #5a4a3a;
  letter-spacing: 0.04em;
}

.order-page__bar-divider {
  width: 1px;
  height: 14px;
  background: #e5e0d8;
}

.order-page__bar-total {
  font-size: 16px;
  font-weight: 700;
  color: #C8A96E;
  letter-spacing: 0.03em;
}

.order-page__checkout-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 9999px;
  background: #1a1a1a;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.05em;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.order-page__checkout-btn:hover {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.order-page__checkout-btn:active {
  transform: scale(0.97);
}

/* ==================== Responsive: Order Page ==================== */
@media (max-width: 1120px) {
  .order-page {
    padding: 12px 20px 24px;
  }
  .order-page__card {
    border-radius: 24px;
    padding: 20px 24px 80px;
  }
  .order-page__bar {
    max-width: calc(100% - 40px);
    padding: 0 20px 16px;
  }
}

@media (max-width: 640px) {
  .order-page {
    padding: 8px 12px 20px;
  }

  .order-page__card {
    padding: 20px 14px 70px;
    border-radius: 20px;
  }

  .cart-item {
    padding: 12px 10px;
    gap: 8px;
  }

  .cart-item__thumb {
    width: 36px;
    height: 36px;
  }

  .cart-item__name {
    font-size: 12px;
  }

  .order-page__bar-inner {
    padding: 12px 14px;
    gap: 8px;
    border-radius: 12px;
  }
  .order-page__bar {
    padding: 0 12px 12px;
  }
}

/* ==================== Toast ==================== */
.toast-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.toast-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: rgba(74, 58, 46, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  font-size: 15px;
  color: #fff;
  letter-spacing: 0.04em;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  max-width: 85vw;
}

.toast-box__text {
  line-height: 1.4;
}

.toast-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

/* ==================== Order Info Card ==================== */
.order-card {
  position: absolute;
  right: 16px;
  top: 12px;
  width: 200px;
  background: #ffffff;
  border-radius: 20px;
  padding: 22px 18px 18px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 55;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.4s ease;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  user-select: none;
}

.order-card:hover {
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.10),
    0 3px 12px rgba(0, 0, 0, 0.06);
}

.order-card--hidden {
  transform: translateX(calc(100% + 40px));
  opacity: 0;
  pointer-events: none;
}

/* ---- Header: ORDER + 颗数 ---- */
.order-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.order-card__label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #c5c0b8;
  text-transform: uppercase;
}

.order-card__count {
  font-size: 13px;
  font-weight: 500;
  color: #8B6F4E;
  letter-spacing: 0.04em;
}

/* ---- Price ---- */
.order-card__price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.order-card__price-symbol {
  font-size: 18px;
  font-weight: 600;
  color: #3a2e24;
  line-height: 1;
}

.order-card__price-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
  letter-spacing: -0.02em;
}

/* ---- Info Rows ---- */
.order-card__info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__row-label {
  font-size: 11px;
  color: #9B8E82;
  letter-spacing: 0.03em;
}

.order-card__row-value {
  font-size: 11px;
  font-weight: 600;
  color: #4a3a2e;
  letter-spacing: 0.03em;
  text-align: right;
}

/* ---- Action Buttons ---- */
.order-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 2px;
}

.order-card__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  border-radius: 9999px;
  background: #1a1a1a;
  color: #ffffff;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.order-card__btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.order-card__btn:hover {
  background: #333333;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
}

.order-card__btn:active {
  transform: scale(0.96);
  background: #0d0d0d;
}

/* 加购成功：按钮原地确认，替代会飘在页面上的全局提示 */
.order-card__btn--done,
.order-card__btn--done:hover,
.order-card__btn--done:active {
  background: #7C9070;
  color: #ffffff;
  transform: none;
  box-shadow: 0 6px 18px -8px rgba(93, 118, 84, 0.7);
  pointer-events: none;
  animation: cartBtnPop 0.4s cubic-bezier(0.2, 0.9, 0.25, 1);
}

@keyframes cartBtnPop {
  0%   { transform: scale(1); }
  45%  { transform: scale(1.045); }
  100% { transform: scale(1); }
}

/* ==================== Responsive ==================== */
@media (max-width: 640px) {
  .top-nav {
    padding: 8px 10px 0;
  }

  .top-nav__bar {
    gap: 6px;
    padding: 5px 20px;
    border-radius: 9999px;
  }

  .top-nav__tab {
    padding: 5px 14px;
    font-size: 12px;
    border-radius: 9999px;
  }

  .design-sidebar {
    width: 52px;
    padding: 12px 6px 12px 8px;
  }

  .design-sidebar__title {
    display: none;
  }

  .design-sidebar__steps::before {
    left: 13px;
  }

  .step-item {
    padding: 10px 6px;
    gap: 0;
    justify-content: center;
  }

  .step-item__body {
    display: none;
  }

  .step-item__indicator {
    width: 24px;
    height: 24px;
  }

  .step-item__number {
    font-size: 11px;
  }

  .design-sidebar__decor {
    display: none;
  }

  .canvas-container {
    --canvas-size: min(calc(100vw - 48px), calc(100vh - 380px));
    border-radius: 12px;
  }

  .floating-bar { gap: 14px; }
  .floating-bar__capsule { padding: 10px 7px; gap: 4px; }
  .floating-bar__icon-btn { width: 34px; height: 34px; padding: 6px; }
  .floating-bar__icon-btn svg { width: 17px; height: 17px; }
  .floating-bar__delete-btn { width: 38px; height: 38px; padding: 8px; }
  .floating-bar__delete-btn svg { width: 17px; height: 17px; }
  .floating-bar__tooltip { font-size: 10px; padding: 5px 10px; right: calc(100% + 10px); }
  .modal-card { padding: 24px 20px; }
  .modal-card__title { font-size: 17px; }
  .modal-card__grid { grid-template-columns: 1fr 1fr; gap: 8px; }
  .plan-card { padding: 14px 10px; }

  .material-panel {
    width: calc(100vw - 16px);
    min-width: unset;
    max-width: unset;
    height: 260px;
    margin-bottom: 8px;
    border-radius: 14px;
  }

  .material-panel__tab {
    padding: 6px 14px;
    font-size: 13px;
  }

  .material-panel__sidebar {
    width: 28%;
    min-width: 70px;
    max-width: 100px;
    padding: 8px 4px 8px 8px;
  }

  .material-panel__subtab {
    padding: 6px 6px;
    font-size: 10px;
  }

  .material-panel__grid {
    grid-template-columns: repeat(auto-fill, minmax(64px, 1fr));
    gap: 6px;
  }

  .bead-item__img-wrap { width: 38px; height: 38px; }
  .bead-item__name { font-size: 8px; max-width: 56px; }

  /* Order card — 缩小并靠右 */
  .order-card {
    width: 172px;
    right: 6px;
    padding: 16px 14px 14px;
    gap: 10px;
    border-radius: 16px;
  }
  .order-card__price-value { font-size: 26px; }
  .order-card__price-symbol { font-size: 15px; }
  .order-card__btn { padding: 8px 10px; font-size: 11px; }
  .order-card__btn svg { width: 14px; height: 14px; }
}

/* ==================== Home Page（参考 3D Studio 落地页布局） ==================== */
.home-page {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 48px 32px;
  overflow: hidden;
  min-height: 0;
  background:
    radial-gradient(120% 90% at 70% 10%, rgba(255, 214, 186, 0.55), transparent 60%),
    linear-gradient(155deg, #f8ddc9 0%, #fbeadd 45%, #fdf3ea 100%);
}

/* ── 首页专属顶栏 ── */
.home-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 0 0;
  position: relative;
  z-index: 5;
}

.home-nav__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  font-family: inherit;
}

.home-nav__brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #211a13;
  color: #fdf3ea;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.home-nav__brand-mark svg { width: 22px; height: 22px; }

.home-nav__brand-text {
  font-size: 19px;
  font-weight: 800;
  color: #211a13;
  letter-spacing: 0.08em;
}

.home-nav__links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.home-nav__tab {
  padding: 8px 20px;
  border: none;
  border-radius: 9999px;
  background: transparent;
  color: #4a3f33;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.06em;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.home-nav__tab:hover {
  background: rgba(255, 252, 248, 0.72);
  color: #211a13;
}

.home-nav__tab--active {
  background: #211a13;
  color: #fdf3ea;
}

.home-nav__tab--active:hover {
  background: #211a13;
  color: #fdf3ea;
}

.home-nav__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.home-nav__icon-btn {
  position: relative;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(33, 26, 19, 0.3);
  background: rgba(255, 252, 248, 0.55);
  color: #211a13;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.home-nav__icon-btn:hover {
  background: #211a13;
  color: #fdf3ea;
  border-color: #211a13;
}

.home-nav__icon-btn svg { width: 20px; height: 20px; }

.home-nav__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9999px;
  background: #c0392b;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 背景描边水印 */
.home-watermark {
  position: absolute;
  left: 50%;
  top: 44%;
  transform: translate(-50%, -50%);
  font-size: clamp(120px, 17vw, 240px);
  font-weight: 800;
  letter-spacing: 0.04em;
  color: transparent;
  -webkit-text-stroke: 1.5px rgba(58, 46, 36, 0.2);
  white-space: nowrap;
  pointer-events: none;
  user-select: none;
}

/* ── Hero 三列布局：文案 | 手串 | 步骤 ── */
.home-hero {
  position: relative;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto minmax(160px, 0.7fr);
  align-items: center;
  gap: 24px;
  min-height: 0;
}

/* ── 左侧文案 ── */
.hero-copy {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-copy__title {
  font-size: clamp(56px, 7vw, 96px);
  font-weight: 800;
  font-family: 'Noto Serif SC', serif;
  color: #211a13;
  line-height: 1.08;
  letter-spacing: 0.06em;
  margin: 0;
}

.hero-copy__subtitle {
  font-size: 15px;
  color: #7d6c5d;
  letter-spacing: 0.12em;
  margin: 0;
  font-weight: 500;
}

.hero-copy__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}

.hero-copy__cta {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 34px;
  border: none;
  border-radius: 9999px;
  background: #26201a;
  color: #fdf3ea;
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.08em;
  cursor: pointer;
  box-shadow: 0 10px 26px rgba(38, 32, 26, 0.24);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.hero-copy__cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(38, 32, 26, 0.32);
}

.hero-copy__cta:active { transform: scale(0.96); }

.hero-copy__cta svg { width: 18px; height: 18px; transition: transform 0.3s ease; }
.hero-copy__cta:hover svg { transform: translateX(3px); }

.hero-copy__hint {
  font-size: 12px;
  color: #a08d7c;
  letter-spacing: 0.06em;
}

/* ── 中央 3D 手串（finished-ring） ── */
.hero-ring-visual {
  position: relative;
  z-index: 2;
  width: min(92vw, 460px);
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
}

.hero-ring-visual.is-dragging { cursor: grabbing; }

.finished-ring {
  position: relative;
  width: min(92vw, 460px);
  aspect-ratio: 1;
  transform-style: preserve-3d;
}

.finished-bead {
  --bead-size: clamp(46px, 5.4vw, 62px);
  position: absolute;
  left: 50%;
  top: 50%;
  width: var(--bead-size);
  height: var(--bead-size);
  margin: calc(var(--bead-size) * -0.5);
  border-radius: 50%;
  overflow: hidden;
  background: #fff;
  transform: translateZ(0) scale(1);
  will-change: transform;
  -webkit-user-drag: none;
  user-select: none;
  box-shadow:
    0 16px 26px rgba(58, 40, 28, 0.24),
    inset -10px -12px 18px rgba(0, 0, 0, 0.28),
    inset 7px 8px 14px rgba(255, 255, 255, 0.72);
}

/* 球面高光覆盖层 */
.finished-bead::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    radial-gradient(circle at 28% 20%, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.34) 22%, transparent 43%),
    radial-gradient(circle at 68% 76%, rgba(0, 0, 0, 0.24), transparent 46%);
  pointer-events: none;
}

.finished-bead img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.26);
  pointer-events: none;
  -webkit-user-drag: none;
  user-select: none;
}

.hero-ring-visual__hint {
  position: absolute;
  bottom: 2%;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: rgba(125, 108, 93, 0.75);
  white-space: nowrap;
  pointer-events: none;
}

/* ── 右侧步骤指示 ── */
.hero-steps {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 34px;
  padding-right: 8px;
}

.hero-step {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.hero-step__num {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
  color: rgba(33, 26, 19, 0.22);
  font-family: Georgia, 'Times New Roman', serif;
}

.hero-step__label {
  font-size: 12px;
  letter-spacing: 0.1em;
  color: #8d7b6a;
}

.hero-step--active .hero-step__num {
  font-size: 52px;
  color: #211a13;
}

.hero-step--active .hero-step__label {
  font-size: 14px;
  font-weight: 600;
  color: #211a13;
}

/* ── 右下滚动按钮 ── */
.hero-scroll {
  position: absolute;
  right: 44px;
  bottom: 30px;
  z-index: 4;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid rgba(33, 26, 19, 0.35);
  background: rgba(255, 252, 248, 0.5);
  color: #211a13;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.hero-scroll:hover {
  background: #26201a;
  color: #fdf3ea;
  border-color: #26201a;
}

.hero-scroll svg { width: 20px; height: 20px; }

/* ── Responsive ── */
@media (max-width: 960px) {
  .home-hero {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
    gap: 30px;
  }
  .hero-copy { align-items: center; }
  .hero-copy__title { font-size: clamp(44px, 10vw, 64px); }
  .hero-copy__cta { align-self: center; }
  .hero-steps { flex-direction: row; align-items: center; gap: 28px; }
  .hero-step { align-items: center; }
  .home-page { padding: 20px 24px 28px; overflow-y: auto; }
  .hero-scroll { display: none; }
}

@media (max-width: 640px) {
  .history-panel {
    width: 172px;
    right: 4px;
    padding: 14px 12px 12px;
  }
  .history-panel__scroll {
    max-height: 148px;
  }
  .history-item { padding: 8px; gap: 8px; }
  .history-item__thumb { width: 28px; height: 28px; }
  .history-item__name { font-size: 11px; }
  .history-item__meta { font-size: 9px; }
}

/* ==================== History Panel ==================== */
.history-panel {
  position: absolute;
  right: 16px;
  top: 340px;
  width: 200px;
  background: #ffffff;
  border-radius: 20px;
  padding: 18px 14px 14px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.03);
  z-index: 54;
  display: flex;
  flex-direction: column;
  gap: 0;
  transition:
    transform 0.5s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.history-panel--hidden {
  transform: translateX(calc(100% + 40px));
  opacity: 0;
  pointer-events: none;
}

/* Header — 固定在顶部，不随列表滚动 */
.history-panel__header {
  display: flex;
  align-items: baseline;
  justify-content: flex-start;
  gap: 8px;
  flex-shrink: 0;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.history-panel__title {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #c5c0b8;
}

.history-panel__subtitle {
  font-size: 10px;
  color: #9B8E82;
  letter-spacing: 0.04em;
}

/* Scrollable area — 固定高度容纳恰好 3 个方案，超出可滚动 */
.history-panel__scroll {
  overflow-y: auto;
  max-height: 180px;
  margin-left: -6px;
  margin-right: -6px;
  padding-left: 6px;
  padding-right: 6px;
}

/* 美化滚动条 */
.history-panel__scroll::-webkit-scrollbar {
  width: 4px;
}

.history-panel__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.history-panel__scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 2px;
}

.history-panel__scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

/* Empty */
.history-panel__empty {
  text-align: center;
  font-size: 11px;
  color: #B8A58D;
  padding: 16px 0;
  letter-spacing: 0.03em;
}

/* List */
.history-panel__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* Item */
.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  font-family: inherit;
  width: 100%;
}

.history-item:hover {
  background: rgba(200, 169, 110, 0.06);
  border-color: rgba(200, 169, 110, 0.25);
}

.history-item--active {
  background: rgba(200, 169, 110, 0.1);
  border-color: rgba(200, 169, 110, 0.4);
}

/* Thumbnail */
.history-item__thumb {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #f8f6f2;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.history-item__thumb-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-item__thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.history-item__thumb-placeholder {
  width: 60%;
  height: 60%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 180, 160, 0.2), rgba(180, 160, 140, 0.08));
  animation: thumbPulse 1.5s ease-in-out infinite;
}

@keyframes thumbPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.7; }
}

/* Info */
.history-item__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.history-item__name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.03em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item__meta {
  font-size: 10px;
  color: #8B6F4E;
  letter-spacing: 0.03em;
}

/* 生成分享码入口（头部「载入」+ 行内「生成」） */
.history-panel__share {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  color: #8B6F4E;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 8px;
  transition: background 0.2s, color 0.2s;
  flex-shrink: 0;
}
.history-panel__share svg { width: 11px; height: 11px; }
.history-panel__share:hover { background: rgba(139, 111, 78, 0.1); color: #6d563c; }

.history-item__share {
  flex-shrink: 0;
  align-self: center;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: #b0a89e;
  cursor: pointer;
  transition: all 0.2s;
}
.history-item__share svg { width: 13px; height: 13px; }
.history-item__share:hover {
  background: rgba(139, 111, 78, 0.12);
  color: #8B6F4E;
}

/* ==================== Naming Modal ==================== */
.name-modal {
  position: relative;
  background: #FBFAF6;
  border-radius: 18px;
  padding: 28px 24px 20px;
  width: 340px;
  max-width: 90vw;
  box-shadow:
    0 16px 48px rgba(0, 0, 0, 0.12),
    0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.name-modal__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.04em;
  margin: 0;
  text-align: center;
}

.name-modal__input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  color: #1a1a1a;
  background: #fff;
  outline: none;
  transition: border-color 0.2s ease;
}

.name-modal__input:focus {
  border-color: #C8A96E;
}

.name-modal__input::placeholder {
  color: #c5c0b8;
}

/* 分享码弹窗 */
.share-modal__hint {
  font-size: 12px;
  color: #b0a89e;
  line-height: 1.7;
  text-align: center;
  margin: -6px 0 0;
}
.share-modal__code {
  padding: 14px 10px;
  border-radius: 12px;
  background: #f6f1e8;
  border: 1.5px dashed #cbbfa8;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: #4a3f30;
  text-align: center;
  user-select: all;
  word-break: break-all;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.share-modal__input {
  text-transform: uppercase;
  letter-spacing: 2px;
}

/* ==================== Auth Modal（登录/注册） ==================== */
.auth-modal {
  gap: 12px;
}
.auth-modal__subtitle {
  font-size: 12px;
  color: #b0a89e;
  text-align: center;
  margin: -6px 0 2px;
  letter-spacing: 0.03em;
}
.auth-modal__btn {
  width: 100%;
  padding: 11px 16px;
  border: none;
  border-radius: 9999px;
  background: linear-gradient(135deg, #8B6F4E 0%, #C8A96E 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.12em;
  font-family: inherit;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}
.auth-modal__btn:hover {
  opacity: 0.9;
}
.auth-modal__btn:active {
  transform: scale(0.98);
}
.auth-modal__btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.auth-modal__switch {
  font-size: 12px;
  color: #b0a89e;
  text-align: center;
  margin: 0;
}
.auth-modal__switch button {
  border: none;
  background: none;
  color: #8B6F4E;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 2px;
}
.auth-modal__switch button:hover {
  text-decoration: underline;
}

.name-modal__actions {
  display: flex;
  gap: 8px;
}

.name-modal__btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.2s ease;
}

.name-modal__btn--primary {
  background: #1a1a1a;
  color: #fff;
}

.name-modal__btn--primary:hover {
  background: #333;
}

.name-modal__btn--new {
  background: rgba(200, 169, 110, 0.12);
  color: #8B6F4E;
}

.name-modal__btn--new:hover {
  background: rgba(200, 169, 110, 0.2);
}

.name-modal__close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  transition: all 0.15s ease;
}

.name-modal__close svg { width: 16px; height: 16px; }

.name-modal__close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

/* ==================== Inspiration Page ==================== */
.inspiration-page {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px 28px 32px;
  min-height: 0;
  overflow: hidden;
}

/* ---- 外层大圆角白色容器 ---- */
.inspiration-page__container {
  width: 100%;
  max-width: 1120px;
  max-height: 100%;
  background: #ffffff;
  border-radius: 32px;
  padding: 20px 28px 20px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.05),
    0 1px 4px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  overflow-x: visible;
  scrollbar-width: none;
}
.inspiration-page__container::-webkit-scrollbar {
  display: none;
}

/* ---- 1. 顶部标题功能区 ---- */
.inspiration-page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 2;
  background: transparent;
  padding-bottom: 4px;
}

/* 左侧标题组 */
.inspiration-page__title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.inspiration-page__title {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Noto Serif SC', serif;
  color: #3a2e24;
  letter-spacing: 0.08em;
  margin: 0;
  line-height: 1.2;
}

.inspiration-page__subtitle {
  font-size: 11px;
  color: #B8A58D;
  letter-spacing: 0.05em;
  margin: 0;
  font-weight: 400;
}

/* 右侧操作区 */
.inspiration-page__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ---- 分类切换标签 ---- */
.inspiration-page__tabs {
  display: flex;
  gap: 2px;
  padding: 4px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 9999px;
}

.inspiration-page__tab {
  padding: 6px 18px;
  border: none;
  border-radius: 9999px;
  background: transparent;
  color: #9B8E82;
  font-size: 11px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.inspiration-page__tab:hover {
  color: #5a4a3a;
}

.inspiration-page__tab--active {
  background: #ffffff;
  color: #3a2e24;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

/* ---- 同步按钮 ---- */
.inspiration-page__sync-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border: 1.5px solid rgba(139, 111, 78, 0.25);
  border-radius: 9999px;
  background: transparent;
  color: #8B6F4E;
  font-size: 10px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.inspiration-page__sync-btn svg {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.inspiration-page__sync-btn:hover {
  border-color: #6B4F3E;
  color: #4a3a2e;
  background: rgba(139, 111, 78, 0.04);
}

.inspiration-page__sync-btn:active {
  transform: scale(0.97);
}

/* ---- 2. 商品网格区（恰 2 行 × 5 列） ---- */
.inspiration-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(2, auto);
  gap: 10px 28px;
  overflow: visible;
}

/* ---- 3. 单格容器（透明，仅负责排版对齐） ---- */
.inspiration-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

/* ---- 底盘背景图 — 圆形陶瓷盘 ---- */
.inspiration-card__plate {
  position: relative;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 盘子表面微光 */
.inspiration-card__plate::after {
  content: '';
  position: absolute;
  inset: 8%;
  border-radius: 50%;
  background: radial-gradient(ellipse at 40% 35%,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0) 60%
  );
  pointer-events: none;
  z-index: 2;
}

.inspiration-card__thumb {
  width: 78%;
  height: 78%;
  object-fit: contain;
  border-radius: 50%;
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}

.inspiration-card__plate-skeleton {
  width: 70%;
  height: 70%;
  border-radius: 50%;
  background: radial-gradient(circle,
    rgba(200, 180, 160, 0.2),
    rgba(180, 160, 140, 0.08)
  );
  animation: platePulse 1.8s ease-in-out infinite;
}

@keyframes platePulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.85; }
}

/* ---- 下方圆角长方 UI 文字区（独立白色卡片） ---- */
.inspiration-card__info {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  text-align: center;
  padding: 10px 10px 10px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.03),
    0 1px 2px rgba(0, 0, 0, 0.02);
}

/* ① 第一行：手串名称 — 字号最大、加粗优雅宋体 */
.inspiration-card__name {
  font-size: 12px;
  font-weight: 700;
  color: #3a2e24;
  margin: 0;
  letter-spacing: 0.06em;
  font-family: 'Noto Serif SC', serif;
  line-height: 1.3;
}

/* ② 第二行：尺寸区间 · 售价 — 中等字号、浅灰色 */
.inspiration-card__size {
  font-size: 9px;
  color: #A09888;
  margin: 0;
  letter-spacing: 0.04em;
  font-weight: 400;
  line-height: 1.4;
}

/* ③ 第三行：作者署名 — 颜色最浅、字号最小 */
.inspiration-card__author {
  font-size: 8px;
  color: #C5BFB2;
  margin: 0;
  letter-spacing: 0.05em;
  font-weight: 400;
  line-height: 1.3;
}

/* ---- Responsive ---- */
@media (max-width: 1120px) {
  .inspiration-page {
    padding: 20px 16px 28px;
  }

  .inspiration-page__container {
    padding: 28px 24px 28px;
    border-radius: 24px;
    gap: 28px;
  }

  .inspiration-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
  }

  .inspiration-page__actions {
    width: 100%;
    justify-content: space-between;
  }

  .inspiration-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px 20px;
  }

  .inspiration-card__plate {
    width: 206px;
    height: 206px;
  }

  .inspiration-card__name {
    font-size: 11px;
  }

  .inspiration-card__size {
    font-size: 8px;
  }

  .inspiration-card__author {
    font-size: 7px;
  }
}

@media (max-width: 640px) {
  .inspiration-page__container {
    padding: 20px 14px 18px;
    border-radius: 20px;
  }

  .inspiration-page__title {
    font-size: 26px;
  }

  .inspiration-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 16px;
  }

  .inspiration-card__plate {
    width: 190px;
    height: 190px;
  }

  .inspiration-card__info {
    padding: 10px 8px 10px;
    border-radius: 12px;
  }

  .inspiration-card__name {
    font-size: 10.5px;
  }

  .inspiration-card__size {
    font-size: 8px;
  }

  .inspiration-card__author {
    font-size: 7px;
  }

  .inspiration-page__tab {
    padding: 7px 16px;
    font-size: 12px;
  }

  .inspiration-page__sync-btn {
    padding: 8px 16px;
    font-size: 12px;
  }
}

/* ================================================================
   "方案" 页面 —— 外层大圆角白色容器（与灵感页一致）
   ================================================================ */
.plan-page {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px 28px 32px;
  min-height: 0;
  overflow: hidden;
}

/* ---- 外层大圆角白色容器（与灵感页一致） ---- */
.plan-page__container {
  width: 100%;
  max-width: 1120px;
  max-height: 100%;
  background: #ffffff;
  border-radius: 32px;
  padding: 20px 28px 20px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.05),
    0 1px 4px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  overflow-x: visible;
  scrollbar-width: none;
}
.plan-page__container::-webkit-scrollbar {
  display: none;
}

/* ---- 1. 顶部标题功能区 ---- */
.plan-page__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 2;
  background: #ffffff;
  padding-bottom: 4px;
}

.plan-page__title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plan-page__title {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Noto Serif SC', serif;
  color: #3a2e24;
  letter-spacing: 0.08em;
  margin: 0;
  line-height: 1.2;
}

.plan-page__subtitle {
  font-size: 11px;
  color: #B8A58D;
  letter-spacing: 0.05em;
  margin: 0;
  font-weight: 400;
}

/* Empty state */
.plan-page__empty {
  text-align: center;
  padding: 80px 0;
  color: #B8A58D;
  font-size: 13px;
  letter-spacing: 0.04em;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.plan-page__empty-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.plan-page__empty p {
  margin: 0;
}

/* ---- 2. 方案网格 ---- */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px 16px;
  padding-top: 4px;
}

.plan-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.plan-card:hover {
  transform: translateY(-2px);
}
.plan-card:active {
  transform: translateY(0);
}

/* 底盘背景 */
.plan-card__plate {
  width: 100%;
  aspect-ratio: 1;
  max-width: 200px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-color: #FBFAF6;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

/* 手串缩略图 */
.plan-card__thumb {
  width: 88%;
  height: 88%;
  object-fit: contain;
  pointer-events: none;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.12));
}

.plan-card__plate-skeleton {
  width: 88%;
  height: 88%;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0,0,0,0.03), rgba(0,0,0,0.06));
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 下方文字信息 */
.plan-card__info {
  width: 100%;
  max-width: 200px;
  margin-top: 8px;
  background: #ffffff;
  border-radius: 14px;
  padding: 8px 10px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.plan-card__name {
  font-size: 13px;
  font-weight: 600;
  color: #3a2e24;
  margin: 0 0 2px 0;
  letter-spacing: 0.04em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.plan-card__wrist {
  font-size: 10px;
  color: #B8A58D;
  margin: 0 0 2px 0;
  letter-spacing: 0.03em;
}

.plan-card__price {
  font-size: 13px;
  font-weight: 600;
  color: #c8a96e;
  margin: 0;
}

/* ---- 响应式 ---- */
@media (max-width: 1120px) {
  .plan-page {
    padding: 12px 20px 24px;
  }
  .plan-page__container {
    border-radius: 24px;
    padding: 20px 24px 20px;
  }
  .plan-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px 14px;
  }
}

@media (max-width: 640px) {
  .plan-page {
    padding: 8px 12px 20px;
  }
  .plan-page__container {
    border-radius: 20px;
    padding: 20px 14px 20px;
  }
  .plan-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px 10px;
  }
  .plan-page__title {
    font-size: 20px;
  }
}

/* ================================================================
   "我的" 页面
   ================================================================ */
.mine-page {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px 28px 32px;
  min-height: 0;
  overflow: hidden;
}

/* ---- 外层大圆角白色容器（与灵感页一致） ---- */
.mine-page__container {
  width: 100%;
  max-width: 1120px;
  max-height: 100%;
  background: #ffffff;
  border-radius: 32px;
  padding: 20px 28px 20px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.05),
    0 1px 4px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  overflow-x: visible;
  scrollbar-width: none;
}
.mine-page__container::-webkit-scrollbar {
  display: none;
}

/* ---- 顶部区域 ---- */
.mine-page__top {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

/* ---- 底部左右两栏 ---- */
.mine-page__bottom {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.mine-page__left {
  flex: 6;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.mine-page__right {
  flex: 4;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ================================================================
   通用卡片基底
   ================================================================ */
.mine-card {
  background: #fdfdfc;
  border-radius: 16px;
  padding: 20px 22px;
  border: 1px solid #f0ece6;
}

/* ---- 模块 1：登录/注册 ---- */
.mine-login {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
  padding: 8px 0;
  cursor: pointer;
}
.mine-login__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #e8e4e0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0a89e;
  flex-shrink: 0;
}
.mine-login__avatar svg {
  width: 26px;
  height: 26px;
}
.mine-login__text {
  font-size: 16px;
  font-weight: 700;
  color: #3a2e24;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: color 0.2s;
}
.mine-login__text:hover {
  color: #8B6F4E;
}
.mine-login__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.mine-login__sub {
  font-size: 12px;
  color: #b0a89e;
  letter-spacing: 0.04em;
}
.mine-login__logout {
  margin-left: auto;
  padding: 6px 14px;
  border: 1px solid #e0d8cc;
  border-radius: 9999px;
  background: #fff;
  font-size: 12px;
  color: #8B6F4E;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.mine-login__logout:hover {
  background: #8B6F4E;
  border-color: #8B6F4E;
  color: #fff;
}

/* ---- 模块 2：说明横幅 ---- */
.mine-banner {
  display: flex;
  align-items: center;
  gap: 16px;
}
.mine-banner__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f5ede0 0%, #e8dcc8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8B6F4E;
  flex-shrink: 0;
}
.mine-banner__icon svg {
  width: 22px;
  height: 22px;
}
.mine-banner__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.mine-banner__title {
  font-size: 15px;
  font-weight: 700;
  color: #3a2e24;
  margin: 0;
  letter-spacing: 0.04em;
}
.mine-banner__subtitle {
  font-size: 12px;
  color: #9B8E82;
  margin: 0;
  line-height: 1.5;
}
.mine-banner__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.mine-banner__tag {
  font-size: 10px;
  padding: 4px 12px;
  border-radius: 9999px;
  background: #f3f0ec;
  color: #8B6F4E;
  font-weight: 500;
  letter-spacing: 0.03em;
}
.mine-banner__arrow {
  color: #c0b8ac;
  flex-shrink: 0;
}
.mine-banner__arrow svg {
  width: 20px;
  height: 20px;
}

/* ---- 模块 3：我的订单 ---- */
.mine-orders {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.mine-orders__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mine-orders__title {
  font-size: 15px;
  font-weight: 700;
  color: #3a2e24;
  margin: 0;
  letter-spacing: 0.04em;
}
.mine-orders__more {
  font-size: 12px;
  color: #9B8E82;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.2s;
}
.mine-orders__more:hover {
  color: #5a4a3a;
}
.mine-orders__more-icon {
  width: 14px;
  height: 14px;
}
.mine-orders__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.mine-orders__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.mine-orders__item:hover {
  background: #faf8f5;
}
.mine-orders__item-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5a4a3a;
}
.mine-orders__item-icon svg {
  width: 24px;
  height: 24px;
}
.mine-orders__item-label {
  font-size: 11px;
  color: #5a4a3a;
  font-weight: 500;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

/* ---- 模块 4：功能列表 ---- */
.mine-list {
  display: flex;
  flex-direction: column;
}
.mine-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  cursor: pointer;
  transition: background 0.2s;
}
.mine-list__item:hover {
  background: #faf8f5;
  margin: 0 -8px;
  padding: 14px 8px;
  border-radius: 8px;
}
.mine-list__item-left {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #3a2e24;
  font-weight: 500;
  letter-spacing: 0.03em;
}
.mine-list__icon {
  width: 20px;
  height: 20px;
  color: #8B6F4E;
  flex-shrink: 0;
}
.mine-list__item-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.mine-list__item-sub {
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.02em;
  color: #9B8E82;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mine-list__arrow {
  width: 16px;
  height: 16px;
  color: #c0b8ac;
  flex-shrink: 0;
}
.mine-list__divider {
  height: 1px;
  background: #f0ece6;
  margin: 0;
}

/* ---- 模块 5：共同创作者（深色主题） ---- */
.mine-card--creator {
  background: linear-gradient(160deg, #2a2520 0%, #1e1b18 100%);
  border: none;
  border-radius: 16px;
  padding: 22px 22px 24px;
}
.mine-creator {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mine-creator__title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #e2c88c 0%, #c9a45c 50%, #b8943e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.06em;
}
.mine-creator__desc {
  font-size: 11px;
  color: #9b8e82;
  margin: 0;
  line-height: 1.5;
}
.mine-creator__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 11px 0;
  border: none;
  border-radius: 9999px;
  background: linear-gradient(135deg, #d4a853 0%, #c4913d 50%, #b07d2e 100%);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
}
.mine-creator__btn:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}
.mine-creator__btn:active {
  transform: scale(0.98);
}
.mine-creator__btn-icon {
  width: 16px;
  height: 16px;
}

/* ---- 模块 6：创作者状态面板 ---- */
.mine-status {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.mine-status__title {
  font-size: 15px;
  font-weight: 700;
  color: #3a2e24;
  margin: 0;
  letter-spacing: 0.04em;
}
.mine-status__grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  grid-template-rows: 1fr auto 1fr;
  gap: 0;
}
.mine-status__grid--single {
  grid-template-rows: 1fr;
}
.mine-status__tip {
  margin-top: 10px;
  font-size: 10px;
  color: #9B8E82;
  letter-spacing: 0.03em;
  text-align: center;
}
.mine-status__cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
}
.mine-status__value {
  font-size: 20px;
  font-weight: 700;
  color: #3a2e24;
  letter-spacing: 0.04em;
}
.mine-status__label {
  font-size: 10px;
  color: #9B8E82;
  letter-spacing: 0.03em;
}
.mine-status__vdivider {
  width: 1px;
  background: #f0ece6;
  grid-row: 1 / -1;
}

/* ---- Responsive ---- */
@media (max-width: 1120px) {
  .mine-page {
    padding: 20px 16px 28px;
  }
  .mine-page__container {
    padding: 22px 20px 22px;
    border-radius: 24px;
    gap: 14px;
  }
  .mine-page__bottom {
    flex-direction: column;
  }
  .mine-page__left,
  .mine-page__right {
    flex: auto;
  }
}

@media (max-width: 640px) {
  .mine-page__container {
    padding: 18px 14px 18px;
    border-radius: 20px;
    gap: 12px;
  }
  .mine-page__top {
    gap: 12px;
  }
  .mine-page__bottom {
    gap: 12px;
  }
  .mine-page__left,
  .mine-page__right {
    gap: 12px;
  }

  .mine-card {
    padding: 16px 14px;
  }

  .mine-banner {
    flex-wrap: wrap;
    gap: 12px;
  }
  .mine-banner__arrow {
    display: none;
  }

  .mine-orders__grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
  }
  .mine-orders__item {
    padding: 10px 4px;
    gap: 6px;
  }
  .mine-orders__item-icon {
    width: 28px;
    height: 28px;
  }
  .mine-orders__item-icon svg {
    width: 20px;
    height: 20px;
  }
  .mine-orders__item-label {
    font-size: 10px;
  }

  .mine-status__value {
    font-size: 16px;
  }
}

/* ============================================================ */
/* ==================== 灵感详情页 ==================== */
/* ============================================================ */
.insp-detail {
  position: relative;
  flex: 1;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 14px 28px 20px;
  min-height: 0;
  overflow: hidden;
}

.insp-detail__container {
  width: 100%;
  max-width: 1120px;
  background: #ffffff;
  border-radius: 32px;
  padding: 18px 30px 22px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.05),
    0 1px 4px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
}

.insp-detail__container::-webkit-scrollbar {
  display: none;
}

/* ---- 返回按钮 ---- */
.insp-detail__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  align-self: flex-start;
  padding: 4px 10px 4px 4px;
  border: none;
  border-radius: 9999px;
  background: transparent;
  color: #8B6F4E;
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.insp-detail__back svg {
  width: 15px;
  height: 15px;
}

.insp-detail__back:hover {
  color: #4a3a2e;
  background: rgba(139, 111, 78, 0.06);
}

/* ---- 主体两栏 ---- */
.insp-detail__body {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  margin-top: 6px;
}

/* 左侧成品大图 */
.insp-detail__photo {
  position: relative;
  width: 44%;
  max-width: 470px;
  aspect-ratio: 1 / 1;
  flex-shrink: 0;
  border-radius: 24px;
  overflow: hidden;
  background: #f6f1e8;
}

.insp-detail__photo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.insp-detail__photo-skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, #f2ece1 30%, #faf6ef 45%, #f2ece1 60%);
  background-size: 200% 100%;
  animation: insp-detail-shimmer 1.4s ease-in-out infinite;
}

@keyframes insp-detail-shimmer {
  to { background-position-x: -200%; }
}

/* 右侧信息区 */
.insp-detail__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.insp-detail__eyebrow {
  font-size: 11px;
  font-weight: 600;
  color: #B8A58D;
  letter-spacing: 0.22em;
  margin: 0 0 6px;
}

.insp-detail__name {
  font-size: 32px;
  font-weight: 700;
  font-family: 'Noto Serif SC', serif;
  color: #3a2e24;
  letter-spacing: 0.06em;
  line-height: 1.2;
  margin: 0 0 8px;
}

.insp-detail__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #8B6F4E;
  letter-spacing: 0.03em;
  margin: 0 0 22px;
}

.insp-detail__meta-price {
  font-weight: 700;
  color: #5a4a3a;
}

.insp-detail__meta-divider {
  color: rgba(139, 111, 78, 0.35);
}

.insp-detail__section-title {
  font-size: 11px;
  font-weight: 600;
  color: #B8A58D;
  letter-spacing: 0.18em;
  margin: 0 0 4px;
}

/* 构成解析行 */
.insp-detail__comps {
  display: flex;
  flex-direction: column;
}

.insp-detail__comp {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(58, 46, 36, 0.06);
}

.insp-detail__comp-img {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #f2ece1;
}

.insp-detail__comp-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.insp-detail__comp-main {
  flex: 1;
  min-width: 0;
}

.insp-detail__comp-name {
  font-size: 13px;
  font-weight: 600;
  color: #3a2e24;
  margin: 0;
  line-height: 1.3;
}

.insp-detail__comp-size {
  font-size: 10px;
  color: #B8A58D;
  margin: 1px 0 0;
}

.insp-detail__comp-price {
  text-align: right;
  flex-shrink: 0;
}

.insp-detail__comp-unit {
  font-size: 10px;
  color: #B8A58D;
  margin: 0;
}

.insp-detail__comp-count {
  font-size: 12px;
  color: #8B6F4E;
  margin: 1px 0 0;
}

.insp-detail__comp-count b {
  color: #3a2e24;
  font-weight: 600;
  margin-left: 4px;
}

/* 材质总计 */
.insp-detail__total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 12px 0 10px;
  border-bottom: 1px solid rgba(58, 46, 36, 0.06);
}

.insp-detail__total span {
  font-size: 11px;
  color: #B8A58D;
  letter-spacing: 0.06em;
}

.insp-detail__total b {
  font-size: 18px;
  font-weight: 700;
  color: #3a2e24;
}

/* 手围范围条 */
.insp-detail__range {
  margin-top: 14px;
}

.insp-detail__range-track {
  position: relative;
  height: 5px;
  border-radius: 9999px;
  background: rgba(58, 46, 36, 0.08);
}

.insp-detail__range-fill {
  position: absolute;
  top: 0;
  height: 100%;
  border-radius: 9999px;
  background: #7C9070;
}

.insp-detail__range-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 10px;
  color: #B8A58D;
}

.insp-detail__range-labels span:nth-child(2) {
  color: #7C9070;
  font-weight: 600;
}

/* 应用按钮 */
.insp-detail__apply {
  margin-top: 20px;
  width: 100%;
  padding: 15px 0;
  border: none;
  border-radius: 9999px;
  background: #7C9070;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  letter-spacing: 0.24em;
  cursor: pointer;
  transition: all 0.25s ease;
}

.insp-detail__apply:hover {
  background: #6c8060;
  box-shadow: 0 6px 16px rgba(124, 144, 112, 0.35);
}

.insp-detail__apply:active {
  transform: scale(0.985);
}

/* ---- 底部 Live 实况 ---- */
.insp-detail__live {
  margin-top: 26px;
  flex-shrink: 0;
}

.insp-detail__live-grid {
  display: grid;
  grid-template-columns: repeat(3, 200px);
  gap: 12px;
}

.insp-detail__live-item {
  position: relative;
  aspect-ratio: 4 / 3.4;
  border-radius: 14px;
  overflow: hidden;
  background: #f2ece1;
}

.insp-detail__live-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 3;
  padding: 3px 9px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.92);
  color: #5a4a3a;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.insp-detail__live-placeholder {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed rgba(139, 111, 78, 0.3);
  border-radius: 14px;
  color: #B8A58D;
  font-size: 11px;
}

.insp-detail__live-placeholder p {
  margin: 0;
}

.insp-detail__live-path {
  font-size: 9px;
  opacity: 0.75;
}

.insp-detail__live-img {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.insp-detail__live-note {
  margin: 12px 0 0;
  text-align: center;
  font-size: 11px;
  color: #8B6F4E;
  letter-spacing: 0.05em;
}

.insp-detail__live-note b {
  color: #3a2e24;
  font-weight: 600;
}

.insp-detail__live-sub {
  margin: 4px 0 0;
  text-align: center;
  font-size: 10px;
  color: #B8A58D;
}

/* ---- 右侧浮动操作 ---- */
.insp-detail__nav,
.insp-detail__ok {
  position: absolute;
  right: 34px;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  z-index: 5;
}

.insp-detail__nav {
  top: 42%;
  background: #ffffff;
  color: #5a4a3a;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
}

.insp-detail__nav:hover {
  transform: scale(1.08);
}

.insp-detail__nav svg {
  width: 18px;
  height: 18px;
}

.insp-detail__ok {
  top: calc(42% + 54px);
  background: #7C9070;
  color: #ffffff;
  box-shadow: 0 3px 12px rgba(124, 144, 112, 0.45);
}

.insp-detail__ok:hover {
  background: #6c8060;
  transform: scale(1.08);
}

.insp-detail__ok svg {
  width: 17px;
  height: 17px;
}

/* ==================== 结算 / 支付面板（确认 → 支付 → 完成 同层状态机） ==================== */
.co-overlay {
  position: fixed;
  inset: 0;
  z-index: 8200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 26px;
  background: rgba(24, 18, 12, 0.34);
  backdrop-filter: blur(18px) saturate(1.12);
  -webkit-backdrop-filter: blur(18px) saturate(1.12);
}

.co-sheet {
  width: 920px;
  max-width: 96vw;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  border-radius: 26px;
  overflow: hidden;
  background: linear-gradient(180deg, #FDFCF9 0%, #F6F1E7 100%);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow:
    0 42px 110px -28px rgba(46, 32, 16, 0.45),
    0 12px 32px rgba(0, 0, 0, 0.08);
}

.co-head {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px 30px 18px;
  border-bottom: 1px solid rgba(120, 96, 62, 0.1);
}

.co-head__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.co-eyebrow {
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.34em;
  color: #B79A63;
  text-transform: uppercase;
}

.co-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #241D15;
}

.co-track {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-left: auto;
}

.co-track__step {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-style: normal;
  letter-spacing: 0.14em;
  color: #C0B5A2;
  transition: color 0.35s ease;
}

.co-track__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.4;
  transition: all 0.35s ease;
}

.co-track__step--on {
  color: #7E6845;
}

.co-track__step--on .co-track__dot {
  opacity: 1;
}

.co-track__step--now {
  color: #241D15;
}

.co-track__step--now .co-track__dot {
  background: #C8A96E;
  opacity: 1;
  box-shadow: 0 0 0 3px rgba(200, 169, 110, 0.22);
}

.co-close {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  background: rgba(120, 96, 62, 0.08);
  color: #7E7059;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: background 0.25s ease, transform 0.35s cubic-bezier(0.2, 0.9, 0.25, 1);
}

.co-close svg { width: 14px; height: 14px; }

.co-close:hover {
  background: rgba(120, 96, 62, 0.16);
  transform: rotate(90deg);
}

.co-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1.04fr 0.96fr;
}

.co-body--done {
  grid-template-columns: 1fr;
}

/* 左：清单（任何步骤都能看到物品） */
.co-list {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 22px 26px 24px;
  border-right: 1px solid rgba(120, 96, 62, 0.1);
}

.co-list__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 11px;
  letter-spacing: 0.16em;
  color: #8C7C64;
}

.co-list__count {
  color: #B79A63;
}

.co-list__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
  scrollbar-width: thin;
}

.co-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 13px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(120, 96, 62, 0.08);
  animation: coRise 0.55s cubic-bezier(0.2, 0.9, 0.25, 1) both;
  animation-delay: calc(var(--i) * 55ms + 80ms);
  transition: transform 0.3s ease, box-shadow 0.3s ease, background 0.3s ease;
}

.co-item:hover {
  transform: translateX(3px);
  background: #fff;
  box-shadow: 0 10px 26px -14px rgba(70, 50, 24, 0.4);
}

.co-item__thumb {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: 13px;
  overflow: hidden;
  background: radial-gradient(circle at 50% 42%, #F8F3E9, #E9E1D2);
  display: grid;
  place-items: center;
}

.co-item__thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.co-item__thumb--empty {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1.5px dashed rgba(120, 96, 62, 0.28);
}

.co-item__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.co-item__info b {
  font-size: 13.5px;
  font-weight: 600;
  color: #2C241B;
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.co-item__info span {
  font-size: 10.5px;
  color: #A1947F;
  letter-spacing: 0.04em;
}

.co-item__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
  flex-shrink: 0;
}

.co-item__price {
  font-size: 14px;
  font-weight: 600;
  color: #2C241B;
}

.co-item__price em,
.co-amount em,
.co-pay__amount em,
.ord-card__amount em {
  font-style: normal;
  font-size: 10.5px;
  color: #9A8B71;
  margin-right: 1px;
}

.co-item__state {
  font-size: 9.5px;
  letter-spacing: 0.1em;
  color: #B4623A;
  background: rgba(214, 120, 72, 0.12);
  padding: 2px 8px;
  border-radius: 999px;
  transition: all 0.3s ease;
}

.co-item__state--paid {
  color: #4D8159;
  background: rgba(100, 150, 110, 0.14);
}

.co-list__total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed rgba(120, 96, 62, 0.24);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: #8C7C64;
}

.co-amount b {
  font-family: 'Noto Serif SC', serif;
  font-size: 27px;
  font-weight: 600;
  color: #241D15;
  letter-spacing: 0.02em;
}

/* 右：步骤内容 */
.co-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  padding: 24px 28px 26px;
  overflow-y: auto;
}

.co-side__hint {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.8;
  letter-spacing: 0.03em;
  color: #9A8B74;
}

.co-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ---- 结算面板：地址簿选择 ---- */
.co-addr {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 13px 14px;
  border: 1px solid rgba(120, 96, 62, 0.14);
  border-radius: 14px;
  background: rgba(200, 169, 110, 0.06);
}

.co-addr__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: #8C7C64;
}

.co-addr__head i {
  font-style: normal;
  color: #B79A63;
  margin-left: 6px;
  letter-spacing: 0.04em;
}

.co-addr__manage {
  border: none;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #8B6F4E;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.co-addr__chips {
  display: flex;
  gap: 9px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.co-addr__chip {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 158px;
  max-width: 230px;
  padding: 9px 12px;
  text-align: left;
  border: 1.5px solid rgba(120, 96, 62, 0.14);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
}

.co-addr__chip:hover {
  border-color: rgba(200, 169, 110, 0.6);
}

.co-addr__chip--on {
  border-color: #C8A96E;
  background: #FFFDF8;
  box-shadow: 0 0 0 3px rgba(200, 169, 110, 0.16);
}

.co-addr__chip-top {
  display: flex;
  align-items: center;
  gap: 6px;
}

.co-addr__chip-top b {
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #241D15;
}

.co-addr__chip-top em {
  font-style: normal;
  font-size: 9px;
  letter-spacing: 0.08em;
  color: #A8894F;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(200, 169, 110, 0.18);
}

.co-addr__chip small {
  font-size: 10.5px;
  letter-spacing: 0.02em;
  color: #9B8E82;
}

.co-addr__chip-addr {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.co-addr__empty {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.03em;
  color: #A99B85;
}

.co-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: #8C7C64;
}

.co-field i {
  font-style: normal;
  color: #C0B5A2;
  margin-left: 4px;
  letter-spacing: 0.04em;
}

.co-field input,
.co-field textarea {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid rgba(120, 96, 62, 0.14);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 0.02em;
  color: #241D15;
  outline: none;
  resize: none;
  transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
}

.co-field input:focus,
.co-field textarea:focus {
  border-color: #C8A96E;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(200, 169, 110, 0.14);
}

.co-field input::placeholder,
.co-field textarea::placeholder {
  color: #C6BBAA;
}

/* 字段级校验：错误就地呈现，面板外不再弹 toast */
.co-field--bad input,
.co-field--bad textarea {
  border-color: rgba(176, 84, 64, 0.5);
  background: rgba(255, 249, 246, 0.94);
}

.co-field--bad input:focus,
.co-field--bad textarea:focus {
  border-color: rgba(176, 84, 64, 0.7);
  box-shadow: 0 0 0 4px rgba(176, 84, 64, 0.12);
}

.co-field__err {
  font-size: 10.5px;
  letter-spacing: 0.05em;
  color: #B05440;
  animation: coErrIn 0.3s cubic-bezier(0.2, 0.9, 0.25, 1) both;
}

@keyframes coErrIn {
  from { opacity: 0; transform: translateY(-3px); }
  to   { opacity: 1; transform: none; }
}

/* 结算面板打开：购物车缩略图逐个飞入物品栏（Carried Across） */
.co-carry-layer {
  position: fixed;
  inset: 0;
  z-index: 8400;
  pointer-events: none;
}

.co-carry-item {
  position: fixed;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 20px 36px -18px rgba(42, 28, 14, 0.42);
  will-change: transform;
}

/* 物品栏缩略图让位给飞行缩略图，落位后再淡入 */
.co-list--carry .co-item__thumb img {
  opacity: 0;
  animation: coThumbLand 0.46s cubic-bezier(0.2, 0.9, 0.25, 1) forwards;
  animation-delay: calc(var(--i) * 70ms + 700ms);
}

@keyframes coThumbLand {
  from { opacity: 0; transform: scale(0.88); }
  to   { opacity: 1; transform: none; }
}

.co-go {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 15px 20px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(118deg, #241D15 0%, #463629 100%);
  color: #F7F0E4;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.18em;
  cursor: pointer;
  transition: transform 0.28s cubic-bezier(0.2, 0.9, 0.25, 1), box-shadow 0.28s ease, opacity 0.2s ease;
}

.co-go svg {
  width: 15px;
  height: 15px;
  transition: transform 0.28s cubic-bezier(0.2, 0.9, 0.25, 1);
}

.co-go:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px -18px rgba(40, 28, 14, 0.8);
}

.co-go:hover:not(:disabled) svg { transform: translateX(4px); }

.co-go:active:not(:disabled) { transform: scale(0.985); }

.co-go:disabled { opacity: 0.55; cursor: default; }

/* 支付步骤 */
.co-pay {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.co-pay__label {
  font-size: 9.5px;
  letter-spacing: 0.22em;
  color: #B79A63;
  text-transform: uppercase;
}

.co-pay__amount {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.co-pay__amount b {
  font-family: 'Noto Serif SC', serif;
  font-size: 42px;
  font-weight: 600;
  line-height: 1.1;
  color: #241D15;
  letter-spacing: 0.01em;
}

.co-pay__hint {
  margin: 2px 0 0;
  font-size: 10.5px;
  line-height: 1.7;
  letter-spacing: 0.03em;
  color: #A1947F;
}

.co-pay__rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 6px;
  max-height: 168px;
  overflow-y: auto;
}

.co-pay__row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(120, 96, 62, 0.08);
  font-size: 11.5px;
  color: #6C604E;
}

.co-pay__row-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.co-pay__row-amt {
  font-weight: 600;
  color: #2C241B;
}

.co-pay__mini {
  border: 1px solid rgba(120, 96, 62, 0.24);
  background: transparent;
  border-radius: 999px;
  padding: 4px 11px;
  font-family: inherit;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: #6C5B42;
  cursor: pointer;
  transition: all 0.24s ease;
}

.co-pay__mini:hover:not(:disabled) {
  background: rgba(200, 169, 110, 0.16);
  border-color: rgba(200, 169, 110, 0.5);
}

.co-pay__mini:disabled { opacity: 0.5; cursor: default; }

.co-pay__paid {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #4D8159;
}

/* 完成步骤 */
.co-done {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 34px 16px;
  text-align: center;
  animation: coRise 0.6s cubic-bezier(0.2, 0.9, 0.25, 1) both;
}

.co-done__mark {
  width: 88px;
  height: 88px;
}

.co-done__circle {
  fill: none;
  stroke: #4D8159;
  stroke-width: 2;
  stroke-dasharray: 172;
  stroke-dashoffset: 172;
  animation: coDraw 0.7s cubic-bezier(0.3, 0.8, 0.3, 1) forwards;
}

.co-done__check {
  fill: none;
  stroke: #4D8159;
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-dasharray: 40;
  stroke-dashoffset: 40;
  animation: coDraw 0.45s 0.5s cubic-bezier(0.3, 0.8, 0.3, 1) forwards;
}

.co-done__title {
  margin: 6px 0 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: #241D15;
}

.co-done__desc {
  margin: 0;
  max-width: 280px;
  font-size: 11.5px;
  line-height: 1.9;
  letter-spacing: 0.03em;
  color: #9A8B74;
}

.co-done .co-go {
  margin-top: 14px;
  width: auto;
  min-width: 220px;
}

@keyframes coRise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes coDraw {
  to { stroke-dashoffset: 0; }
}

@keyframes coSpin {
  to { transform: rotate(360deg); }
}

/* ==================== 收货地址簿面板 ==================== */
.addr-overlay {
  z-index: 8600;
}

.addr-sheet {
  width: 744px;
}

.addr-head__count {
  margin-left: auto;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: #9B8E82;
}

.addr-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 30px 26px;
  overflow-y: auto;
}

.addr-hint {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.8;
  letter-spacing: 0.03em;
  color: #9A8B74;
}

.addr-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.addr-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid rgba(120, 96, 62, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.addr-card--default {
  border-color: rgba(200, 169, 110, 0.55);
  background: linear-gradient(180deg, #FFFDF8 0%, #FBF5E9 100%);
}

.addr-card__main {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.addr-card__line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.addr-card__line b {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #241D15;
}

.addr-card__phone {
  font-size: 12px;
  letter-spacing: 0.04em;
  color: #8C7C64;
}

.addr-card__tag {
  font-size: 9px;
  letter-spacing: 0.1em;
  color: #A8894F;
  padding: 2px 7px;
  border-radius: 5px;
  background: rgba(200, 169, 110, 0.2);
}

.addr-card__text {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  letter-spacing: 0.02em;
  color: #6E6152;
}

.addr-card__acts {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.addr-card__act {
  padding: 5px 12px;
  border: 1px solid rgba(120, 96, 62, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  font-family: inherit;
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: #7E6845;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.addr-card__act:hover {
  border-color: rgba(200, 169, 110, 0.6);
  background: #fff;
}

.addr-card__act--del:hover {
  border-color: rgba(176, 84, 64, 0.45);
  color: #B05440;
}

.addr-confirm {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(176, 84, 64, 0.07);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: #8E5647;
}

.addr-confirm > span {
  margin-right: auto;
}

.addr-confirm__ok,
.addr-confirm__no {
  border: none;
  border-radius: 8px;
  padding: 5px 11px;
  font-family: inherit;
  font-size: 10.5px;
  letter-spacing: 0.05em;
  cursor: pointer;
}

.addr-confirm__ok {
  background: #B05440;
  color: #FFF7F2;
}

.addr-confirm__no {
  background: rgba(255, 255, 255, 0.85);
  color: #8E5647;
}

.addr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 34px 0 26px;
  color: #A99B85;
}

.addr-empty__mark {
  font-size: 24px;
  color: #D8CBB4;
}

.addr-empty p {
  margin: 0;
  font-size: 11.5px;
  letter-spacing: 0.04em;
}

.addr-form {
  gap: 13px;
}

.addr-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.addr-form__check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  letter-spacing: 0.03em;
  color: #6E6152;
  cursor: pointer;
}

.addr-form__check input {
  width: 14px;
  height: 14px;
  accent-color: #C8A96E;
  cursor: pointer;
}

.addr-form__ops {
  display: flex;
  gap: 10px;
  margin-top: auto;
  padding-top: 4px;
}

.addr-form__save {
  margin-top: 0;
  flex: 1;
}

.addr-form__cancel {
  padding: 15px 20px;
  border: 1px solid rgba(120, 96, 62, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.8);
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: 0.1em;
  color: #7E6845;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.addr-form__cancel:hover {
  background: #fff;
  border-color: rgba(200, 169, 110, 0.6);
}

.addr-form__cancel:disabled {
  opacity: 0.55;
  cursor: default;
}

/* ==================== 我的订单面板 ==================== */
.ord-sheet {
  width: 880px;
  max-width: 96vw;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: 26px;
  overflow: hidden;
  background: #FBFAF6;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow:
    0 42px 110px -28px rgba(46, 32, 16, 0.45),
    0 12px 32px rgba(0, 0, 0, 0.08);
}

.ord-head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 26px 30px 18px;
  border-bottom: 1px solid rgba(120, 96, 62, 0.1);
}

.ord-head__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ord-head__count {
  margin-left: auto;
  padding-bottom: 4px;
  font-size: 10px;
  letter-spacing: 0.2em;
  color: #B79A63;
}

.ord-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 172px 1fr;
}

.ord-nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 18px 12px;
  border-right: 1px solid rgba(120, 96, 62, 0.1);
  background: rgba(246, 241, 231, 0.6);
  overflow-y: auto;
}

.ord-nav__item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  background: transparent;
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: 0.06em;
  color: #7E7362;
  text-align: left;
  cursor: pointer;
  transition: background 0.26s ease, color 0.26s ease, transform 0.26s cubic-bezier(0.2, 0.9, 0.25, 1);
}

.ord-nav__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.25;
  transition: opacity 0.26s ease, background 0.26s ease;
}

.ord-nav__item:hover {
  background: rgba(200, 169, 110, 0.14);
  transform: translateX(2px);
}

.ord-nav__item--on {
  background: #2A231B;
  color: #F6EFE3;
  box-shadow: 0 10px 22px -14px rgba(42, 35, 27, 0.9);
}

.ord-nav__item--on .ord-nav__dot {
  background: #C8A96E;
  opacity: 1;
}

.ord-main {
  min-height: 0;
  overflow-y: auto;
  padding: 18px 24px 24px;
}

.ord-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #9A8B74;
}

.ord-loading__ring {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1.5px solid rgba(120, 96, 62, 0.2);
  border-top-color: #C8A96E;
  animation: coSpin 0.8s linear infinite;
}

.ord-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 64px 0;
  animation: coRise 0.5s ease both;
}

.ord-empty__mark {
  font-size: 26px;
  line-height: 1;
  color: #C3B9A8;
}

.ord-empty p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #A1947F;
}

.ord-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ord-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(120, 96, 62, 0.09);
  animation: coRise 0.5s cubic-bezier(0.2, 0.9, 0.25, 1) both;
  animation-delay: calc(var(--i) * 45ms);
  transition: transform 0.3s cubic-bezier(0.2, 0.9, 0.25, 1), box-shadow 0.3s ease, border-color 0.3s ease;
}

.ord-card:hover {
  transform: translateY(-2px);
  border-color: rgba(200, 169, 110, 0.45);
  box-shadow: 0 16px 34px -20px rgba(60, 42, 20, 0.55);
}

.ord-card__thumb {
  width: 84px;
  height: 84px;
  flex-shrink: 0;
  border-radius: 16px;
  overflow: hidden;
  background: radial-gradient(circle at 50% 42%, #F8F3E9, #E9E1D2);
  display: grid;
  place-items: center;
}

.ord-card__thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.ord-card__thumb--empty {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1.5px dashed rgba(120, 96, 62, 0.28);
}

.ord-card__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ord-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ord-card__name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: #241D15;
}

.ord-card__state {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 10.5px;
  letter-spacing: 0.08em;
  background: rgba(200, 169, 110, 0.14);
  color: #9A7D3F;
}

.ord-card__state--0 { background: rgba(214, 120, 72, 0.14); color: #B4623A; }
.ord-card__state--1 { background: rgba(200, 169, 110, 0.18); color: #94722F; }
.ord-card__state--2 { background: rgba(90, 140, 180, 0.14); color: #4A7A9E; }
.ord-card__state--3 { background: rgba(100, 150, 110, 0.16); color: #47795A; }
.ord-card__state--4,
.ord-card__state--6 { background: rgba(0, 0, 0, 0.06); color: #8A8177; }
.ord-card__state--5 { background: rgba(214, 150, 72, 0.18); color: #A8742F; }

.ord-card__chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.ord-card__chips span {
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 10px;
  letter-spacing: 0.06em;
  color: #9A8E7A;
  background: rgba(200, 169, 110, 0.1);
}

.ord-card__chips .ord-card__no {
  padding: 0;
  background: transparent;
  font-family: 'SFMono-Regular', Consolas, monospace;
  letter-spacing: 0.04em;
  color: #B0A48C;
}

.ord-card__note {
  margin: 0;
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: #A1947F;
}

.ord-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding-top: 11px;
  border-top: 1px dashed rgba(120, 96, 62, 0.16);
}

.ord-card__amount {
  font-family: 'Noto Serif SC', serif;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #241D15;
}

.ord-card__ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ord-op {
  padding: 7px 16px;
  border: 1px solid rgba(120, 96, 62, 0.22);
  border-radius: 999px;
  background: transparent;
  font-family: inherit;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: #6C5B42;
  cursor: pointer;
  transition: all 0.24s ease;
}

.ord-op:hover {
  background: rgba(200, 169, 110, 0.16);
  border-color: rgba(200, 169, 110, 0.52);
}

.ord-op--go {
  background: #2A231B;
  border-color: #2A231B;
  color: #F6EFE3;
}

.ord-op--go:hover {
  background: #3C3128;
  border-color: #3C3128;
  transform: translateY(-1px);
}

.ord-card__confirm {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(200, 169, 110, 0.12);
  border: 1px dashed rgba(200, 169, 110, 0.42);
  font-size: 11px;
  letter-spacing: 0.04em;
  color: #7A6A50;
}

.ord-card__confirm span {
  flex: 1;
  min-width: 0;
}

.ord-fade-enter-active,
.ord-fade-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.ord-fade-enter-from,
.ord-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 面板进出场：遮罩淡入 + 面板上浮缩放 */
.sheet-enter-active { transition: opacity 0.34s ease; }
.sheet-leave-active { transition: opacity 0.24s ease; }

.sheet-enter-active .co-sheet,
.sheet-enter-active .ord-sheet {
  transition: transform 0.46s cubic-bezier(0.2, 0.9, 0.25, 1), opacity 0.4s ease;
}

.sheet-leave-active .co-sheet,
.sheet-leave-active .ord-sheet {
  transition: transform 0.28s ease, opacity 0.24s ease;
}

.sheet-enter-from { opacity: 0; }

.sheet-enter-from .co-sheet,
.sheet-enter-from .ord-sheet {
  opacity: 0;
  transform: translateY(26px) scale(0.972);
}

.sheet-leave-to { opacity: 0; }

.sheet-leave-to .co-sheet,
.sheet-leave-to .ord-sheet {
  opacity: 0;
  transform: translateY(14px) scale(0.985);
}

@media (max-width: 820px) {
  .co-overlay {
    padding: 0;
    align-items: stretch;
  }

  .co-sheet,
  .ord-sheet {
    width: 100vw;
    max-width: 100vw;
    height: 100vh;
    max-height: 100vh;
    border-radius: 0;
  }

  .co-head {
    flex-wrap: wrap;
    gap: 12px;
    padding: 20px 18px 14px;
  }

  .co-track {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 14px;
  }

  .co-close { margin-left: auto; }

  .co-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
    overflow-y: auto;
  }

  .co-body--done { grid-template-rows: 1fr; }

  .co-list {
    max-height: 42vh;
    padding: 16px 18px;
    border-right: none;
    border-bottom: 1px solid rgba(120, 96, 62, 0.1);
  }

  .co-side { padding: 18px 18px 24px; }

  .co-go { margin-top: 12px; }

  .ord-head {
    flex-wrap: wrap;
    gap: 10px;
    padding: 20px 18px 14px;
  }

  .ord-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }

  .ord-nav {
    flex-direction: row;
    gap: 6px;
    padding: 12px 14px;
    border-right: none;
    border-bottom: 1px solid rgba(120, 96, 62, 0.1);
    overflow-x: auto;
  }

  .ord-nav__item {
    width: auto;
    padding: 8px 14px;
    white-space: nowrap;
  }

  .ord-main { padding: 14px 16px 20px; }

  .ord-card {
    gap: 12px;
    padding: 13px;
  }

  .ord-card__thumb {
    width: 66px;
    height: 66px;
    border-radius: 13px;
  }
}

</style>
