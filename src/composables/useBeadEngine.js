import { ref } from 'vue'
import gsap from 'gsap'
import Matter from 'matter-js'

// ==================== 常量 ====================
const MAX_BEADS = 25
const BEAD_RADIUS = 22          // 珠子视觉半径 (px)
const GROOVE_RING_WIDTH = 6     // 凹槽环线宽
const LAUNCH_DURATION = 0.65    // 发射动画时长 (s)
const SHIFT_DURATION = 0.4      // 已有珠子移位时长 (s)
const STRING_DURATION = 1.2     // 串线收拢时长 (s)
const GAP_OPEN_DURATION = 0.25  // 间隙张开时长 (s)
const REDISTRIBUTE_DURATION = 0.55 // 插入后重排时长 (s)
const MIN_BEADS_FOR_BRACELET = 8 // 成环最少珠子数
const SIZE_MIN = 8               // 珠子最小尺寸 (mm)
const SIZE_MAX = 12              // 珠子最大尺寸 (mm)
const DRAG_DETACH_THRESHOLD = 18 // 拖动脱离环的像素阈值
const GAP_DETECT_RADIUS_TOLERANCE = 0.45 // 距离容差 (相对 braceletRadius)

// ==================== 物理引擎常量 ====================
const BOUNDARY_SEGMENTS = 48        // 圆形边界段数
const BOUNDARY_WALL_THICKNESS = 8   // 边界壁厚 (px)
const BOUNDARY_FRICTION = 0.05      // 边界壁面摩擦（光滑碗壁：防止珠子嵌进段接缝后摩擦锁死）
const LAUNCH_SPEED = 6              // 发射初始速度
const LAUNCH_SPREAD = 0.3           // 发射角度随机散布 (弧度)
const PHYSICS_STEP_MS = 1000 / 60   // 物理固定时间步长
const GRAVITY_SCALE = 0.001         // 极低重力
const FRICTION_AIR = 0.015          // 空气阻力（低，保证发射轨迹）
const FRICTION = 0.5                // 表面摩擦
const RESTITUTION = 0.05            // 弹性系数（几乎不弹）
const BRACELET_PACKING_COEFF = 1.0  // 手串紧密系数 (1.0 = 紧挨无间隙)

// 预设色板（image 加载失败时的降级方案）
const BEAD_COLORS = [
  '#E85D5D', '#F4A460', '#F0C75E', '#6BCB77',
  '#4D96FF', '#9B72CF', '#E97EB2', '#45B7D1',
  '#FF6B6B', '#FFD93D', '#6BCB77', '#4ECDC4',
  '#FF8C42', '#C9B1FF', '#FFB3BA', '#7EC8E3',
  '#A0E7E5', '#FFAEBC', '#B4F8C8', '#FBE7C6',
  '#55C1E9', '#E8A0BF', '#BAE1C8', '#F8C471', '#D7BDE2',
]

// ==================== 工具函数 ====================

// 素材图片缓存：同一素材只创建/解码一次 Image 对象。
// 快速连续添加珠子时避免重复触发解码（大 PNG 主线程解码会阻塞造成卡顿），
// 后续 launchBead 直接复用已解码的 img，Canvas 绘制无需再次解码。
const imageCache = new Map()

function getOrLoadImage(path) {
  if (!path) return null
  let img = imageCache.get(path)
  if (!img) {
    img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = path
    imageCache.set(path, img)
  }
  return img
}

function normalizeAngle(a) {
  while (a > Math.PI) a -= 2 * Math.PI
  while (a < -Math.PI) a += 2 * Math.PI
  return a
}

/** 将任意角度映射到 [0, 2π) */
function toPositiveAngle(a) {
  a = a % (2 * Math.PI)
  if (a < 0) a += 2 * Math.PI
  return a
}

function quadraticBezier(p0, p1, p2, t) {
  const u = 1 - t
  return {
    x: u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
    y: u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y,
  }
}

function computeControlPoint(start, end, centerX, centerY) {
  const mx = (start.x + end.x) / 2
  const my = (start.y + end.y) / 2
  const dx = mx - centerX
  const dy = my - centerY
  const dist = Math.sqrt(dx * dx + dy * dy) || 1
  const bulge = Math.max(60, Math.min(160, dist * 0.5))
  return {
    x: mx + (dx / dist) * bulge * 0.6 + (Math.random() - 0.5) * 40,
    y: my + (dy / dist) * bulge * 0.6 - Math.abs(start.y - end.y) * 0.35 - 30,
  }
}

function lightenColor(hex, p) {
  const n = parseInt(hex.replace('#', ''), 16)
  const r = Math.min(255, (n >> 16) + p)
  const g = Math.min(255, ((n >> 8) & 0xff) + p)
  const b = Math.min(255, (n & 0xff) + p)
  return `rgb(${r},${g},${b})`
}

function darkenColor(hex, p) {
  const n = parseInt(hex.replace('#', ''), 16)
  const r = Math.max(0, (n >> 16) - p)
  const g = Math.max(0, ((n >> 8) & 0xff) - p)
  const b = Math.max(0, (n & 0xff) - p)
  return `rgb(${r},${g},${b})`
}

/** 距离 */
function dist(x1, y1, x2, y2) {
  return Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2)
}

/**
 * 获取珠子在手串上的有效占用宽度（像素直径）。
 * 占位必须与 drawBeads 的渲染宽度一致，否则成环周长被高估导致手串宽松：
 * - 圆形主珠：完整直径
 * - 非圆形素材（隔片/配饰/文玩）：按图片真实宽高比折算的横向渲染宽度
 * - 挂件：主体外垂到环外，仅顶部连接环贴线，占位再按 0.55 收窄
 */
function getBeadEffectiveDiameter(bead) {
  const fullDiameter = BEAD_RADIUS * 2 * (bead.size / 10)
  const isNonBead = bead.categoryId && bead.categoryId !== 'main-bead'
  if (!isNonBead) return fullDiameter
  // 与渲染一致：aspect >= 1 占满全宽，aspect < 1 按实际横向宽度
  let widthRatio = 1
  if (bead.image && bead.imageLoaded) {
    const img = bead.image
    const iw = img.naturalWidth || img.width || 100
    const ih = img.naturalHeight || img.height || 100
    widthRatio = Math.min(iw / ih, 1)
  }
  // 挂件：连接环窄边贴线，主体外悬不占环内弧长
  if (bead.subCategoryId === 'acc-pendant') {
    return fullDiameter * widthRatio * 0.55
  }
  return fullDiameter * widthRatio
}

/**
 * 获取物理碰撞刚体半径。
 * 非圆形素材按视觉宽度比例缩放，平衡碰撞精度与视觉重叠。
 */
function getBeadPhysicsRadius(bead) {
  const fullRadius = BEAD_RADIUS * (bead.size / 10)
  const isNonBead = bead.categoryId && bead.categoryId !== 'main-bead'
  if (!isNonBead) return fullRadius
  // 挂件：主体外悬，碰撞半径折中取 45%
  if (bead.subCategoryId === 'acc-pendant') {
    return fullRadius * 0.45
  }
  if (bead.image && bead.imageLoaded) {
    const img = bead.image
    const iw = img.naturalWidth || img.width || 100
    const ih = img.naturalHeight || img.height || 100
    const aspect = iw / ih
    if (aspect < 1) {
      return fullRadius * Math.max(aspect, 0.45)
    }
  }
  return fullRadius
}

/**
 * 统一质量：所有珠子不论大小，质量等同标准尺寸珠子。
 * 标准珠（size=10, radius=BEAD_RADIUS, density=0.002）质量作为基准。
 * 轻质小半径刚体会导致飞行轨迹弯曲，这里通过提高密度补偿。
 */
function getPhysicsDensity(physicsRadius) {
  const standardRadius = BEAD_RADIUS
  const standardDensity = 0.002
  const standardMass = standardDensity * Math.PI * standardRadius * standardRadius
  const area = Math.PI * physicsRadius * physicsRadius
  return standardMass / area
}

// ==================== 核心组合式函数 ====================
export function useBeadEngine() {
  // ---- 响应式状态 ----
  const beads = ref([])
  const beadCount = ref(0)
  const isBracelet = ref(false)
  const isStringing = ref(false)
  const isFull = ref(false)
  const isDragging = ref(false)
  const isShowcasing = ref(false)
  const toastMessage = ref('')

  // ---- 内部状态 ----
  let canvasEl = null
  let ctx = null
  let canvasWidth = 0
  let canvasHeight = 0
  let centerX = 0
  let centerY = 0
  let grooveRadius = 0
  let braceletRadius = 0
  let dpr = 1

  // ---- 物理引擎状态 ----
  let physicsEngine = null        // Matter.Engine 实例
  let physicsWorld = null         // Matter.World 引用
  let boundaryBodies = []         // 静态边界刚体数组
  let physicsActive = false       // 物理引擎是否运行中
  let beadBodyMap = new Map()     // bead.id → Matter.Body 映射

  let _toastTimer = null
  let animFrameId = null

  // ---- 展示模式 ----
  let showcaseRotation = 0          // 累计旋转角度
  let showcaseGrooveOpacity = 1     // 凹槽/线绳透明度
  let lastFrameTime = 0             // 上一帧时间戳
  const SHOWCASE_ROTATION_SPEED = 0.5 // 旋转速度 (弧度/秒)

  // ---- 拖拽状态机 ----
  const drag = {
    active: false,
    bead: null,        // 被拖拽的珠子对象引用
    beadIndex: -1,     // 原始数组索引
    mouseX: 0,
    mouseY: 0,
    grabOffsetX: 0,    // 鼠标相对珠子中心的偏移
    grabOffsetY: 0,
    hoverGapIndex: -1, // 当前悬停的插入位置 (插在 index 之后)
    gapOpen: false,    // 间隙是否已展开
  }

  // ==================== 物理引擎 ====================

  /** 构建圆形边界：用静态矩形段围成近似圆环 */
  function buildBoundary(radius) {
    const segments = []
    const angleStep = (2 * Math.PI) / BOUNDARY_SEGMENTS

    for (let i = 0; i < BOUNDARY_SEGMENTS; i++) {
      const angle = angleStep * i
      const chordLength = 2 * radius * Math.sin(angleStep / 2)
      const x = centerX + radius * Math.cos(angle)
      const y = centerY + radius * Math.sin(angle)

      const segment = Matter.Bodies.rectangle(x, y, chordLength, BOUNDARY_WALL_THICKNESS, {
        isStatic: true,
        angle: angle + Math.PI / 2,
        render: { visible: false },
        friction: BOUNDARY_FRICTION,
        restitution: RESTITUTION,
        label: 'boundary',
      })
      segments.push(segment)
    }

    return segments
  }

  /** 初始化 Matter.js 物理引擎 */
  function initPhysics() {
    if (physicsEngine) return

    physicsEngine = Matter.Engine.create({
      gravity: { x: 0, y: GRAVITY_SCALE, scale: 0.001 },
      enableSleeping: false,
    })
    physicsWorld = physicsEngine.world

    boundaryBodies = buildBoundary(grooveRadius)
    Matter.Composite.add(physicsWorld, boundaryBodies)

    physicsActive = true
    beadBodyMap = new Map()
  }

  /** 销毁物理引擎及所有刚体 */
  function destroyPhysics() {
    if (!physicsEngine) return

    for (const body of beadBodyMap.values()) {
      Matter.Composite.remove(physicsWorld, body)
    }
    beadBodyMap.clear()

    for (const seg of boundaryBodies) {
      Matter.Composite.remove(physicsWorld, seg)
    }
    boundaryBodies = []

    Matter.Engine.clear(physicsEngine)
    physicsEngine = null
    physicsWorld = null
    physicsActive = false
  }

  // ==================== Canvas 初始化 ====================
  function mount(el) {
    if (!el) return
    canvasEl = el
    dpr = window.devicePixelRatio || 1
    ctx = el.getContext('2d')
    resize()
    bindPointerEvents()
    startRenderLoop()
    initPhysics()
  }

  function resize() {
    if (!canvasEl) return
    const parent = canvasEl.parentElement
    if (!parent) return
    // 用布局尺寸而非视觉尺寸：getBoundingClientRect 会受 CSS transform 影响，
    // 页面转场（scale 动画）期间挂载时会把坐标系建小，导致成环后环心偏离底盘中心
    const rect = { width: parent.clientWidth, height: parent.clientHeight }
    // 挂件溢出边距：canvas 比底盘容器大一圈，pendant 超出底盘也能绘制
    const overflowRatio = 0.18
    const overflowPx = Math.min(rect.width, rect.height) * overflowRatio
    canvasWidth = rect.width + overflowPx * 2
    canvasHeight = rect.height + overflowPx * 2
    dpr = window.devicePixelRatio || 1
    canvasEl.width = canvasWidth * dpr
    canvasEl.height = canvasHeight * dpr
    canvasEl.style.width = canvasWidth + 'px'
    canvasEl.style.height = canvasHeight + 'px'
    ctx = canvasEl.getContext('2d')
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

    centerX = canvasWidth / 2
    centerY = canvasHeight / 2
    // groove/bracelet 半径基于底盘尺寸而非扩展后画布尺寸
    const containerSize = Math.min(rect.width, rect.height)
    const newGrooveRadius = containerSize * 0.36
    const newBraceletRadius = containerSize * 0.24

    // 若物理引擎运行中，重建边界并钳制珠子
    if (physicsActive && physicsEngine) {
      for (const seg of boundaryBodies) {
        Matter.Composite.remove(physicsWorld, seg)
      }
      boundaryBodies = buildBoundary(newGrooveRadius)
      Matter.Composite.add(physicsWorld, boundaryBodies)

      // 将超界珠子钳制到新边界内
      for (const bead of beads.value) {
        const body = beadBodyMap.get(bead.id)
        if (body) {
          const dx = body.position.x - centerX
          const dy = body.position.y - centerY
          const dist = Math.sqrt(dx * dx + dy * dy)
          const maxDist = newGrooveRadius - BEAD_RADIUS * (bead.size / 10)
          if (dist > maxDist && dist > 0) {
            const s = maxDist / dist
            Matter.Body.setPosition(body, {
              x: centerX + dx * s,
              y: centerY + dy * s,
            })
          }
        }
      }
    }

    grooveRadius = newGrooveRadius
    braceletRadius = newBraceletRadius

    syncAllPositions()
  }

  // ==================== 事件绑定 ====================
  function bindPointerEvents() {
    if (!canvasEl) return
    canvasEl.style.pointerEvents = 'auto'
    canvasEl.addEventListener('pointerdown', onPointerDown)
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
  }

  /** Canvas 坐标转换 */
  function canvasCoords(e) {
    const rect = canvasEl.getBoundingClientRect()
    return { x: e.clientX - rect.left, y: e.clientY - rect.top }
  }

  // ==================== 命中检测 ====================
  function hitTest(cx, cy) {
    const list = beads.value
    // 倒序遍历（渲染在上层的珠子优先）
    for (let i = list.length - 1; i >= 0; i--) {
      const b = list[i]
      let hx = b.x, hy = b.y, hitRadius = BEAD_RADIUS + 4

      // 挂件：视觉中心向外偏移，扩大命中半径匹配实际尺寸
      if (b.subCategoryId === 'acc-pendant' && b.imageLoaded && b.image) {
        const r = BEAD_RADIUS * (b.size / 10)
        const base = r * 2
        const img = b.image
        const iw = img.naturalWidth || img.width || 100
        const ih = img.naturalHeight || img.height || 100
        const aspect = iw / ih
        const dh = aspect >= 1 ? base / aspect : base
        const beadAngle = Math.atan2(b.y - centerY, b.x - centerX)
        hx = b.x + (dh / 2) * Math.cos(beadAngle)
        hy = b.y + (dh / 2) * Math.sin(beadAngle)
        hitRadius = Math.max(r, dh * 0.6) + 4
      }

      const d = dist(cx, cy, hx, hy)
      if (d <= hitRadius) {
        return { bead: b, index: i }
      }
    }
    return null
  }

  // ==================== 插入间隙检测 ====================
  /**
   * 根据被拖拽珠子的角度，找出它在环上两颗相邻珠子之间的插入位置。
   * 返回: { gapIndex, gapAngle } 或 null（珠子离环太远）
   */
  function findInsertionGap(dragAngle, dragRadius) {
    const list = beads.value
    if (list.length < 1) return null

    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius
    const tolerance = currentRadius * GAP_DETECT_RADIUS_TOLERANCE

    // 拖拽珠子必须离环足够近
    if (Math.abs(dragRadius - currentRadius) > tolerance + BEAD_RADIUS * 2) {
      return null
    }

    // 收集环上珠子的 [正角度, 原始索引]（排除被拖拽的珠子）
    const ringBeads = []
    for (let i = 0; i < list.length; i++) {
      if (drag.active && i === drag.beadIndex) continue
      ringBeads.push({ angle: toPositiveAngle(list[i].angle), index: i })
    }
    if (ringBeads.length === 0) return null

    ringBeads.sort((a, b) => a.angle - b.angle)

    const dAngle = toPositiveAngle(dragAngle)

    // 找到 dragAngle 落在哪两个角度之间
    // 环形查找: 在相邻 pair 之间
    for (let i = 0; i < ringBeads.length; i++) {
      const a0 = ringBeads[i].angle
      const a1 = ringBeads[(i + 1) % ringBeads.length].angle

      let between = false
      if (a0 <= a1) {
        between = dAngle >= a0 && dAngle <= a1
      } else {
        // 跨越 2π 边界
        between = dAngle >= a0 || dAngle <= a1
      }

      if (between) {
        // 返回哪个 index 之后插入
        return {
          gapAfterIndex: ringBeads[i].index,
          gapAngle: a0,
        }
      }
    }

    // 兜底：返回第一个之前
    return { gapAfterIndex: ringBeads[0].index, gapAngle: ringBeads[0].angle }
  }

  // ==================== 指针事件处理 ====================
  function onPointerDown(e) {
    if (isStringing.value || isShowcasing.value) return
    const { x, y } = canvasCoords(e)
    const hit = hitTest(x, y)
    if (!hit) return

    const { bead, index } = hit

    drag.active = true
    drag.bead = bead
    drag.beadIndex = index
    drag.mouseX = x
    drag.mouseY = y
    drag.grabOffsetX = bead.x - x
    drag.grabOffsetY = bead.y - y
    drag.hoverGapIndex = -1
    drag.gapOpen = false

    isDragging.value = true

    // 提升被拖珠子到渲染顶层（移到数组末尾）
    if (index !== beads.value.length - 1) {
      const list = beads.value
      list.splice(index, 1)
      list.push(bead)
      drag.beadIndex = list.length - 1
    }

    // 停止该珠子的所有动画
    gsap.killTweensOf(bead)

    // 物理模式：将刚体设为静态以便拖拽
    if (physicsActive && !isBracelet.value) {
      const body = beadBodyMap.get(bead.id)
      if (body) {
        Matter.Body.setStatic(body, true)
      }
    }

    canvasEl.setPointerCapture(e.pointerId)
    e.preventDefault()
  }

  function onPointerMove(e) {
    if (!drag.active) return
    const { x, y } = canvasCoords(e)
    drag.mouseX = x
    drag.mouseY = y

    // 被拖珠子跟随鼠标
    const newX = x + drag.grabOffsetX
    const newY = y + drag.grabOffsetY
    drag.bead.x = newX
    drag.bead.y = newY

    // 物理模式：同步移动 Matter.js 刚体
    if (physicsActive && !isBracelet.value) {
      const body = beadBodyMap.get(drag.bead.id)
      if (body) {
        Matter.Body.setPosition(body, { x: newX, y: newY })
      }
    }

    // 手串模式：仅检测插入间隙位置，不移动其他珠子
    if (isBracelet.value) {
      const dx = drag.bead.x - centerX
      const dy = drag.bead.y - centerY
      const dragAngle = Math.atan2(dy, dx)
      const dragRadius = Math.sqrt(dx * dx + dy * dy)

      const gap = findInsertionGap(dragAngle, dragRadius)

      if (gap) {
        drag.hoverGapIndex = gap.gapAfterIndex
      } else {
        drag.hoverGapIndex = -1
      }
    }
  }

  function onPointerUp(_e) {
    if (!drag.active) return

    const bead = drag.bead
    const gapIdx = drag.hoverGapIndex

    // 检查珠子是否被拖到底盘外（远离中心一定距离则删除）
    const beadDist = dist(bead.x, bead.y, centerX, centerY)
    const deleteThreshold = grooveRadius * 1.25
    const isOutside = beadDist > deleteThreshold

    if (isOutside) {
      // 拖出底盘外：手串珠子≤最低数量时散开，否则删除
      if (isBracelet.value && beads.value.length <= MIN_BEADS_FOR_BRACELET) {
        dissolveBracelet()
      } else {
        removeBead(bead)
      }
    } else if (isBracelet.value) {
      // 手串模式：间隙插入或回环
      if (gapIdx >= 0 && bead) {
        insertBeadAtGap(bead, gapIdx)
      } else if (bead) {
        returnBeadToRing(bead)
      }
    } else if (physicsActive) {
      // 物理模式：珠子落在原地，不动
      const body = beadBodyMap.get(bead.id)
      if (body) {
        Matter.Body.setStatic(body, false)
        Matter.Body.setVelocity(body, { x: 0, y: 0 })
      }
    }

    // 重置拖拽状态
    drag.active = false
    drag.bead = null
    drag.beadIndex = -1
    drag.hoverGapIndex = -1
    drag.gapOpen = false
    isDragging.value = false
  }

  // ==================== 间隙动画 ====================
  /**
   * 在 gapAfterIndex 之后张开一个插入间隙（将该位置之后的珠子顺时针推开）。
   * gapAfterIndex: 间隙在该索引的珠子之后。
   */
  function openGap(gapAfterIndex) {
    const list = beads.value
    const N = list.length
    if (N < 1) return

    // 排除被拖拽珠子
    const ringCount = drag.active ? N - 1 : N
    if (ringCount < 1) return

    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius
    const gapSize = (2 * Math.PI) / (ringCount + 1) // 给新珠子留的空间

    // 为环上珠子重新分配角度：间隙后的珠子顺时针偏移 gapSize
    for (let i = 0; i < N; i++) {
      const b = list[i]
      if (drag.active && b === drag.bead) continue

      // 找到该珠子在"环上序列"中的位置
      const ringIdx = getRingIndex(i)
      const insertRingIdx = getRingIndexForGap(gapAfterIndex)

      let targetAngle
      if (ringIdx <= insertRingIdx) {
        // 间隙之前的珠子：保持原有分布
        targetAngle = Math.PI / 2 + ((2 * Math.PI - gapSize) / ringCount) * ringIdx
      } else {
        // 间隙之后的珠子：顺时针偏移 gapSize
        targetAngle = Math.PI / 2 + ((2 * Math.PI - gapSize) / ringCount) * ringIdx + gapSize
      }

      gsap.killTweensOf(b, 'angle')
      gsap.to(b, {
        angle: targetAngle,
        duration: GAP_OPEN_DURATION,
        ease: 'power2.out',
        overwrite: 'auto',
        onUpdate: () => {
          if (drag.active && b === drag.bead) return
          const pos = angleToXY(b.angle, currentRadius)
          b.x = pos.x
          b.y = pos.y
        },
      })
    }

    drag.gapOpen = true
  }

  function closeGap(instant = false) {
    const list = beads.value
    const N = list.length
    const ringCount = drag.active ? N - 1 : N
    if (ringCount < 1) return

    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius
    const dur = instant ? 0.05 : GAP_OPEN_DURATION

    // 恢复均匀分布
    for (let i = 0; i < N; i++) {
      const b = list[i]
      if (drag.active && b === drag.bead) continue
      const ringIdx = getRingIndex(i)
      const targetAngle = Math.PI / 2 + (2 * Math.PI / ringCount) * ringIdx

      gsap.killTweensOf(b, 'angle')
      gsap.to(b, {
        angle: targetAngle,
        duration: dur,
        ease: 'power2.out',
        overwrite: 'auto',
        onUpdate: () => {
          if (drag.active && b === drag.bead) return
          const pos = angleToXY(b.angle, currentRadius)
          b.x = pos.x
          b.y = pos.y
        },
      })
    }

    drag.gapOpen = false
    drag.hoverGapIndex = -1
  }

  // ==================== 珠子删除 ====================
  /** 从手串中移除一颗珠子 */
  function removeBead(bead) {
    const list = beads.value
    const idx = list.indexOf(bead)
    if (idx < 0) return

    // 终止该珠子的所有动画
    gsap.killTweensOf(bead)

    // 清理物理刚体
    if (physicsActive) {
      const body = beadBodyMap.get(bead.id)
      if (body) {
        Matter.Composite.remove(physicsWorld, body)
        beadBodyMap.delete(bead.id)
      }
    }

    // 从数组中移除
    list.splice(idx, 1)
    beadCount.value = list.length

    // 手串模式下重新计算环半径并按有效宽度重新分布剩余珠子
    if (isBracelet.value && list.length >= 2) {
      // 根据剩余珠子有效宽度重新计算手串半径
      const totalDiameter = list.reduce((sum, b) => sum + getBeadEffectiveDiameter(b), 0)
      const newR = (totalDiameter * BRACELET_PACKING_COEFF) / (2 * Math.PI)
      braceletRadius = Math.max(BEAD_RADIUS, Math.min(newR, grooveRadius * 0.95))

      // 按有效宽度比例分配角度
      let currentAngle = Math.PI / 2
      list.forEach((b) => {
        const beadDiameter = getBeadEffectiveDiameter(b)
        const arcLength = totalDiameter > 0 ? (beadDiameter / totalDiameter) * (2 * Math.PI) : (2 * Math.PI) / list.length
        const targetAngle = normalizeAngle(currentAngle + arcLength / 2)
        const targetPos = angleToXY(targetAngle, braceletRadius)
        gsap.killTweensOf(b)
        gsap.to(b, {
          x: targetPos.x,
          y: targetPos.y,
          angle: targetAngle,
          radius: braceletRadius,
          duration: 0.3,
          ease: 'power2.out',
          overwrite: 'auto',
        })
        currentAngle += arcLength
      })
    }
  }

  // ==================== 手串散开 ====================
  /** 珠子数降到最低（≤8）时，手串散开回到物理模式 */
  function dissolveBracelet() {
    const list = beads.value
    if (list.length === 0) return

    // 如果被拖拽珠子在末尾，先恢复到原始位置
    // （drag.bead 在 onPointerDown 时被移到了数组末尾）

    // 取消手串状态
    isBracelet.value = false

    // 重新初始化物理引擎
    initPhysics()

    // 给所有珠子创建物理刚体并赋予随机散射速度
    list.forEach((b) => {
      gsap.killTweensOf(b)
      const bodyRadius = getBeadPhysicsRadius(b)
      const body = Matter.Bodies.circle(b.x, b.y, bodyRadius, {
        restitution: RESTITUTION,
        friction: FRICTION,
        frictionStatic: 0,   // 静摩擦归零：杜绝珠子贴壁静止后被摩擦锁卡死
        frictionAir: FRICTION_AIR,
        density: getPhysicsDensity(bodyRadius),
        label: 'bead',
        render: { visible: false },
      })

      const scatterAngle = Math.random() * 2 * Math.PI
      const scatterSpeed = 3 + Math.random() * 4
      Matter.Body.setVelocity(body, {
        x: Math.cos(scatterAngle) * scatterSpeed,
        y: Math.sin(scatterAngle) * scatterSpeed,
      })

      Matter.Composite.add(physicsWorld, body)
      beadBodyMap.set(b.id, body)
      b._body = body
      b.radius = grooveRadius
    })
  }

  // ==================== 插入与重排 ====================
  /** 将被拖拽珠子插入到 gapAfterIndex 之后 */
  function insertBeadAtGap(bead, gapAfterIndex) {
    const list = beads.value

    // 被拖珠子在 onPointerDown 时已移到数组末尾，直接移除即可
    const dragIdx = list.indexOf(bead)
    if (dragIdx >= 0) list.splice(dragIdx, 1)

    // 直接将珠子插入到 gapAfterIndex + 1 位置
    const insertIdx = Math.min(gapAfterIndex + 1, list.length)
    list.splice(insertIdx, 0, bead)

    beadCount.value = list.length
    redistributeEvenly()
  }

  /** 珠子回到环上原位置（松手时无有效间隙） */
  function returnBeadToRing(bead) {
    const list = beads.value
    if (!list.includes(bead)) return

    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius
    const dx = bead.x - centerX
    const dy = bead.y - centerY
    const curAngle = Math.atan2(dy, dx)

    bead.angle = curAngle
    bead.radius = currentRadius
    const pos = angleToXY(curAngle, currentRadius)

    gsap.killTweensOf(bead)
    gsap.to(bead, {
      x: pos.x,
      y: pos.y,
      scale: 1,
      duration: 0.3,
      ease: 'back.out(1.5)',
      overwrite: 'auto',
    })

    // 归位后重新均匀分布
    setTimeout(() => redistributeEvenly(), 350)
  }

  /** 重新均匀分布所有珠子（手串模式下按有效宽度比例分配角度） */
  function redistributeEvenly() {
    const list = beads.value
    const N = list.length
    if (N === 0) return
    if (isStringing.value) return

    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius

    if (isBracelet.value) {
      // 手串模式：按每颗珠子的有效视觉宽度分配角度
      const totalDiameter = list.reduce((sum, b) => sum + getBeadEffectiveDiameter(b), 0)
      let currentAngle = Math.PI / 2
      list.forEach((b) => {
        const beadDiameter = getBeadEffectiveDiameter(b)
        const arcLength = totalDiameter > 0 ? (beadDiameter / totalDiameter) * (2 * Math.PI) : (2 * Math.PI) / N
        const targetAngle = normalizeAngle(currentAngle + arcLength / 2)
        const targetPos = angleToXY(targetAngle, currentRadius)
        gsap.killTweensOf(b)
        gsap.to(b, {
          x: targetPos.x,
          y: targetPos.y,
          angle: targetAngle,
          radius: currentRadius,
          duration: REDISTRIBUTE_DURATION,
          ease: 'power3.out',
          overwrite: 'auto',
        })
        currentAngle += arcLength
      })
    } else {
      // 非手串模式：均匀分布
      const angles = computeAngles(N)
      list.forEach((b, i) => {
        const targetAngle = angles[i]
        const targetPos = angleToXY(targetAngle, currentRadius)
        gsap.killTweensOf(b)
        gsap.to(b, {
          x: targetPos.x,
          y: targetPos.y,
          angle: targetAngle,
          radius: currentRadius,
          duration: REDISTRIBUTE_DURATION,
          ease: 'power3.out',
          overwrite: 'auto',
        })
      })
    }
  }

  // ==================== 环上索引映射 ====================
  /** 获取珠子在环上的顺序索引（排除被拖拽珠子） */
  function getRingIndex(arrayIndex) {
    const list = beads.value
    let ringIdx = 0
    for (let i = 0; i < arrayIndex; i++) {
      if (!drag.active || list[i] !== drag.bead) ringIdx++
    }
    return ringIdx
  }

  /** 间隙位置对应的环上索引 */
  function getRingIndexForGap(gapAfterArrayIndex) {
    let ringIdx = 0
    const list = beads.value
    for (let i = 0; i <= gapAfterArrayIndex; i++) {
      if (!drag.active || list[i] !== drag.bead) ringIdx++
    }
    return ringIdx - 1
  }

  // ==================== 角度与坐标 ====================
  function computeAngles(count) {
    const angles = []
    for (let i = 0; i < count; i++) {
      angles.push(Math.PI / 2 + (2 * Math.PI / count) * i)
    }
    return angles
  }

  function angleToXY(angle, radius) {
    return {
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle),
    }
  }

  function syncAllPositions() {
    beads.value.forEach((b) => {
      const pos = angleToXY(b.angle, b.radius)
      b.x = pos.x
      b.y = pos.y
    })
  }

  // ==================== 发射珠子 ====================
  /**
   * @param {string} color - 降级色
   * @param {string} imagePath - 实物图路径
   * @param {number} size - 珠子尺寸 (mm), 8~12, 默认 10
   */
  function launchBead(color, imagePath, size = 10, categoryId = 'main-bead', subCategoryId = null) {
    if (beads.value.length >= MAX_BEADS) {
      isFull.value = true
      return false
    }
    isFull.value = false

    if (!canvasEl) return false

    const beadColor = color || BEAD_COLORS[beads.value.length % BEAD_COLORS.length]
    const physicsRadius = BEAD_RADIUS * (size / 10)

    // 发射起点：始终从凹槽底部（6点钟方向）飞入，保证动画路径够长
    const startX = centerX
    const startY = centerY + (grooveRadius - physicsRadius)

    // 复用素材图片缓存（同一素材只解码一次，消除连点卡顿）
    const img = getOrLoadImage(imagePath)

    const newId = Date.now() + Math.random()
    const bead = {
      id: newId,
      color: beadColor,
      image: img,
      imagePath: imagePath || '',
      imageLoaded: false,
      angle: 0,
      radius: grooveRadius,
      x: startX,
      y: startY,
      scale: 1,
      size: size,
      physicsRadius,
      categoryId,
      subCategoryId,
    }

    if (img) {
      if (img.complete && img.naturalWidth > 0) {
        // 缓存中已解码完成：同步标记，无需等待
        bead.imageLoaded = true
      } else {
        img.addEventListener('load', () => { bead.imageLoaded = true }, { once: true })
        img.addEventListener('error', () => { bead.imageLoaded = false }, { once: true })
      }
    }

    beads.value.push(bead)
    beadCount.value = beads.value.length

    if (physicsActive && !isBracelet.value) {
      // ---- 物理模式：Matter.js 速度发射 ----
      const actualPhysicsRadius = getBeadPhysicsRadius(bead)
      bead.physicsRadius = actualPhysicsRadius
      const body = Matter.Bodies.circle(startX, startY, actualPhysicsRadius, {
        restitution: RESTITUTION,
        friction: FRICTION,
        frictionStatic: 0,   // 与散射路径同口径，防止贴壁摩擦锁死
        frictionAir: FRICTION_AIR,
        density: getPhysicsDensity(actualPhysicsRadius),
        label: 'bead',
        render: { visible: false },
      })

      // 计算指向底盘中心的速度方向 + 随机散布
      const dx = centerX - startX
      const dy = centerY - startY
      const baseAngle = Math.atan2(dy, dx)
      const launchAngle = baseAngle + (Math.random() - 0.5) * LAUNCH_SPREAD * 2
      const speed = LAUNCH_SPEED + Math.random() * 2

      Matter.Body.setVelocity(body, {
        x: Math.cos(launchAngle) * speed,
        y: Math.sin(launchAngle) * speed,
      })

      Matter.Composite.add(physicsWorld, body)
      beadBodyMap.set(newId, body)
      bead._body = body

      bead.x = body.position.x
      bead.y = body.position.y

    } else {
      // ---- 手串模式：环上排列 ----
      if (isBracelet.value) {
        // 已有手串：重新计算紧密环（含新珠子，非圆形素材按视觉宽度）
        const allBeads = beads.value
        const totalDiameter = allBeads.reduce((sum, b) => {
          return sum + getBeadEffectiveDiameter(b)
        }, 0)
        const braceletR = (totalDiameter * BRACELET_PACKING_COEFF) / (2 * Math.PI)
        const clampedR = Math.max(
          BEAD_RADIUS,
          Math.min(braceletR, grooveRadius * 0.95)
        )

        let currentAngle = Math.PI / 2
        const targets = []

        allBeads.forEach((b) => {
          const beadDiameter = getBeadEffectiveDiameter(b)
          const arcLength = (beadDiameter / totalDiameter) * (2 * Math.PI)
          b.angle = normalizeAngle(currentAngle + arcLength / 2)
          b.radius = clampedR
          const pos = angleToXY(b.angle, clampedR)
          targets.push({ bead: b, x: pos.x, y: pos.y, angle: b.angle })
          currentAngle += arcLength
        })

        // 新珠子从发射点出发，已有珠子从当前位置微调
        const tl = gsap.timeline()

        // 新珠子贝塞尔飞入
        const targetEntry = targets.find(t => t.bead.id === newId)
        const controlPoint = computeControlPoint(
          { x: startX, y: startY },
          { x: targetEntry.x, y: targetEntry.y },
          centerX, centerY
        )

        const proxy = { t: 0 }
        tl.to(proxy, {
          t: 1,
          duration: LAUNCH_DURATION,
          ease: 'power2.out',
          onUpdate: () => {
            const pt = quadraticBezier(
              { x: startX, y: startY },
              controlPoint,
              { x: targetEntry.x, y: targetEntry.y },
              proxy.t
            )
            bead.x = pt.x
            bead.y = pt.y
            bead.scale = 0.3 + proxy.t * 0.7
          },
        }, 0)

        // 已有珠子微调到新位置
        targets.forEach((t) => {
          if (t.bead.id === newId) return
          tl.to(t.bead, {
            x: t.x,
            y: t.y,
            duration: SHIFT_DURATION,
            ease: 'power2.inOut',
          }, 0)
        })

        tl.to(bead, {
          x: targetEntry.x,
          y: targetEntry.y,
          scale: 1,
          duration: 0.15,
          ease: 'power2.out',
        }, `-=${0.1}`)

        tl.eventCallback('onComplete', () => {
          targets.forEach((t) => {
            t.bead.x = t.x
            t.bead.y = t.y
            t.bead.angle = t.angle
            t.bead.radius = clampedR
            t.bead.scale = 1
          })
          braceletRadius = clampedR
        })

      } else {
        // 无手串（物理模式未启用时的兜底）：原有均匀分布逻辑
        const newCount = beads.value.length
        const newAngles = computeAngles(newCount)
        const targetAngle = newAngles[newCount - 1]
        const targetRadius = grooveRadius
        const targetPos = angleToXY(targetAngle, targetRadius)

        bead.angle = targetAngle
        bead.radius = targetRadius

        const controlPoint = computeControlPoint(
          { x: startX, y: startY },
          { x: targetPos.x, y: targetPos.y },
          centerX, centerY
        )

        const tl = gsap.timeline()

        beads.value.forEach((b, i) => {
          if (b.id === newId) return
          const oldAngle = b.angle
          const newAngle = newAngles[i]
          let delta = normalizeAngle(newAngle - oldAngle)
          const targetAngleAnim = oldAngle + delta

          tl.to(b, {
            angle: targetAngleAnim,
            duration: SHIFT_DURATION,
            ease: 'power2.inOut',
            onUpdate: () => {
              const pos = angleToXY(b.angle, b.radius)
              b.x = pos.x
              b.y = pos.y
            },
          }, 0)
        })

        const proxy = { t: 0 }
        tl.to(proxy, {
          t: 1,
          duration: LAUNCH_DURATION,
          ease: 'power2.out',
          onUpdate: () => {
            const pt = quadraticBezier(
              { x: startX, y: startY },
              controlPoint,
              { x: targetPos.x, y: targetPos.y },
              proxy.t
            )
            bead.x = pt.x
            bead.y = pt.y
            bead.scale = 0.3 + proxy.t * 0.7
          },
        }, 0)

        tl.to(bead, {
          x: targetPos.x,
          y: targetPos.y,
          scale: 1,
          duration: 0.15,
          ease: 'power2.out',
        }, `-=${0.1}`)

        tl.eventCallback('onComplete', () => {
          bead.x = targetPos.x
          bead.y = targetPos.y
          bead.scale = 1
          bead.angle = targetAngle
          bead.radius = targetRadius
        })
      }
    }

    return true
  }

  // ==================== Toast ====================
  function showToast(msg) {
    if (_toastTimer) clearTimeout(_toastTimer)
    toastMessage.value = msg
    _toastTimer = setTimeout(() => {
      toastMessage.value = ''
      _toastTimer = null
    }, 2500)
  }

  // ==================== 串线收拢 ====================
  function stringToBracelet() {
    if (isStringing.value || isBracelet.value) return
    const list = beads.value
    if (list.length < 2) return

    // 前置校验：至少需要 8 颗珠子
    if (list.length < MIN_BEADS_FOR_BRACELET) {
      showToast('再添点珠子吧，至少需要8颗才能串成串！')
      return
    }

    // 串线前，先关闭任何拖拽
    if (drag.active) {
      closeGap(true)
      drag.active = false
      drag.bead = null
      drag.gapOpen = false
      isDragging.value = false
    }

    isStringing.value = true

    // ---- 销毁物理引擎（珠子散落位置保留在 bead.x/y 上） ----
    destroyPhysics()

    // ---- 计算紧密手串环 ----
    // 1. 所有珠子的有效像素宽度总和（非圆形素材按视觉宽度）
    const totalDiameter = list.reduce((sum, b) => {
      return sum + getBeadEffectiveDiameter(b)
    }, 0)

    // 2. 手串半径 = 周长 / (2π)，紧密系数 1.0（珠子紧挨）
    const circumference = totalDiameter * BRACELET_PACKING_COEFF
    const braceletR = circumference / (2 * Math.PI)

    // 3. 仅限制上限不超过凹槽，下限用珠子最少直径兜底
    const clampedRadius = Math.max(
      BEAD_RADIUS,
      Math.min(braceletR, grooveRadius * 0.95)
    )

    // 4. 按珠子有效宽度分配角度：每个珠子占弧长与有效宽度成正比
    let currentAngle = Math.PI / 2

    list.forEach((b) => {
      const beadDiameter = getBeadEffectiveDiameter(b)
      const arcLength = (beadDiameter / totalDiameter) * (2 * Math.PI)
      const halfArc = arcLength / 2

      // 珠子圆心位于其所占弧段的中点
      b.angle = normalizeAngle(currentAngle + halfArc)
      b.radius = clampedRadius

      const targetPos = angleToXY(b.angle, clampedRadius)
      b._targetX = targetPos.x
      b._targetY = targetPos.y

      currentAngle += arcLength
    })

    // 5. GSAP 动画：所有珠子从散落位置飞到环上
    const tl = gsap.timeline()

    list.forEach((b) => {
      tl.to(b, {
        x: b._targetX,
        y: b._targetY,
        duration: STRING_DURATION,
        ease: 'elastic.out(1, 0.6)',
      }, 0)
    })

    tl.eventCallback('onComplete', () => {
      isStringing.value = false
      isBracelet.value = true
      list.forEach((b) => {
        b.x = b._targetX
        b.y = b._targetY
        // 从最终位置反推角度
        const dx = b.x - centerX
        const dy = b.y - centerY
        b.angle = Math.atan2(dy, dx)
        b.radius = clampedRadius
        delete b._targetX
        delete b._targetY
        delete b._body
      })
      braceletRadius = clampedRadius
    })
  }

  function skipAndString() {
    stringToBracelet()
  }

  // ==================== 渲染循环 ====================
  let renderPaused = false

  // 暂停渲染循环（canvas 不可见时调用，避免空耗主线程）
  function pauseRenderLoop() {
    renderPaused = true
  }

  // 恢复渲染循环
  function resumeRenderLoop() {
    renderPaused = false
  }

  function startRenderLoop() {
    lastFrameTime = 0
    function loop(ts) {
      // 暂停态：保持空转（几乎零开销），不执行物理与绘制
      if (renderPaused) {
        animFrameId = requestAnimationFrame(loop)
        return
      }
      // 计算帧间隔（秒），首帧跳过
      if (lastFrameTime === 0) lastFrameTime = ts
      const dt = Math.min((ts - lastFrameTime) / 1000, 0.1) // 上限 100ms 防跳帧
      lastFrameTime = ts

      // 展示模式：累加旋转角度
      if (isShowcasing.value) {
        showcaseRotation += SHOWCASE_ROTATION_SPEED * dt
      }

      draw()
      animFrameId = requestAnimationFrame(loop)
    }
    animFrameId = requestAnimationFrame(loop)
  }

  function draw() {
    if (!ctx) return

    // 物理步进 + 同步珠子位置
    if (physicsActive && physicsEngine) {
      Matter.Engine.update(physicsEngine, PHYSICS_STEP_MS)
      for (const bead of beads.value) {
        const body = beadBodyMap.get(bead.id)
        if (body) {
          bead.x = body.position.x
          bead.y = body.position.y
        }
      }
    }

    ctx.clearRect(0, 0, canvasWidth, canvasHeight)
    drawGroove()
    drawBeads()
    if (drag.active && drag.hoverGapIndex >= 0) {
      drawGapIndicator()
    }
  }

  function drawGroove() {
    const o = showcaseGrooveOpacity
    if (o <= 0.01) return

    ctx.save()
    ctx.globalAlpha = o

    ctx.beginPath()
    ctx.arc(centerX, centerY, grooveRadius + GROOVE_RING_WIDTH / 2, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(139, 111, 78, 0.2)'
    ctx.lineWidth = GROOVE_RING_WIDTH
    ctx.stroke()

    ctx.beginPath()
    ctx.arc(centerX, centerY, grooveRadius - GROOVE_RING_WIDTH / 2, 0, Math.PI * 2)
    ctx.strokeStyle = 'rgba(139, 111, 78, 0.15)'
    ctx.lineWidth = GROOVE_RING_WIDTH
    ctx.stroke()

    ctx.strokeStyle = 'rgba(139, 111, 78, 0.06)'
    ctx.lineWidth = 1
    ctx.setLineDash([6, 20])
    ctx.beginPath()
    ctx.moveTo(centerX - grooveRadius - 20, centerY)
    ctx.lineTo(centerX + grooveRadius + 20, centerY)
    ctx.moveTo(centerX, centerY - grooveRadius - 20)
    ctx.lineTo(centerX, centerY + grooveRadius + 20)
    ctx.stroke()
    ctx.setLineDash([])

    ctx.restore()
  }

  function drawThread() {
    const o = showcaseGrooveOpacity
    if (o <= 0.01) return

    const list = beads.value
    if (list.length < 2) return
    // 只绘制环上珠子的线（排除正在被拖拽的珠子）
    const ring = drag.active
      ? list.filter(b => b !== drag.bead)
      : list
    if (ring.length < 2) return

    const showcasing = isShowcasing.value

    ctx.save()
    ctx.globalAlpha = o
    ctx.strokeStyle = isBracelet.value
      ? 'rgba(160, 135, 110, 0.65)'
      : 'rgba(180, 160, 140, 0.55)'
    ctx.lineWidth = isBracelet.value ? 2 : 1.5
    ctx.lineCap = 'round'
    if (!isBracelet.value) ctx.setLineDash([4, 3])

    ctx.beginPath()
    // 按角度排序
    const sorted = [...ring].sort((a, b) => toPositiveAngle(a.angle) - toPositiveAngle(b.angle))
    const first = sorted[0]
    const firstPos = showcasing
      ? angleToXY(first.angle + showcaseRotation, first.radius)
      : { x: first.x, y: first.y }
    ctx.moveTo(firstPos.x, firstPos.y)
    for (let i = 1; i < sorted.length; i++) {
      const b = sorted[i]
      const pos = showcasing
        ? angleToXY(b.angle + showcaseRotation, b.radius)
        : { x: b.x, y: b.y }
      ctx.lineTo(pos.x, pos.y)
    }
    // 闭合
    ctx.lineTo(firstPos.x, firstPos.y)
    ctx.stroke()
    ctx.setLineDash([])
    ctx.restore()
  }

  function drawBeads() {
    const showcasing = isShowcasing.value

    beads.value.forEach((b) => {
      if (b.scale <= 0.01) return

      // 展示模式：计算旋转后的绘制坐标
      let drawX = b.x
      let drawY = b.y
      if (showcasing) {
        const pos = angleToXY(b.angle + showcaseRotation, b.radius)
        drawX = pos.x
        drawY = pos.y
      }

      // 珠子在环上的角度：图片"顶部"始终指向圆心
      const beadAngle = Math.atan2(drawY - centerY, drawX - centerX)
      // 旋转量：使图片顶部(↑)指向圆心方向
      // beadAngle=0(右侧) → 旋转-π/2 → 图片顶部朝左(圆心) ✓
      // beadAngle=π/2(下方) → 旋转0 → 图片顶部朝上(圆心) ✓
      const spin = beadAngle - Math.PI / 2

      const r = BEAD_RADIUS * b.scale * (b.size / 10)
      const isNonBead = b.categoryId && b.categoryId !== 'main-bead'

      if (isNonBead && b.imageLoaded && b.image) {
        // ── 隔片/配饰/文玩：按图片真实宽高比 + 跟随环上角度旋转 ──
        const img = b.image
        const iw = img.naturalWidth || img.width || 100
        const ih = img.naturalHeight || img.height || 100
        const aspect = iw / ih

        // 以 size 为基准，按原始比例计算绘制宽高
        const base = r * 2
        let dw, dh
        if (aspect >= 1) {
          dw = base
          dh = base / aspect
        } else {
          dh = base
          dw = base * aspect
        }

        // 挂件类：顶部连接环对齐手串线，整体向外偏移半高
        const isPendant = b.subCategoryId === 'acc-pendant'
        let cx = drawX, cy = drawY
        if (isPendant) {
          cx = drawX + (dh / 2) * Math.cos(beadAngle)
          cy = drawY + (dh / 2) * Math.sin(beadAngle)
        }

        // ── 底部投影 ──
        ctx.save()
        ctx.shadowColor = 'rgba(0, 0, 0, 0.28)'
        ctx.shadowBlur = r * 0.3
        ctx.shadowOffsetX = 1.5
        ctx.shadowOffsetY = 2
        ctx.translate(cx, cy)
        ctx.rotate(spin)
        ctx.drawImage(img, -dw / 2, -dh / 2, dw, dh)
        ctx.restore()

        // ── 主体：在旋转坐标系中清晰绘制 ──
        ctx.save()
        ctx.translate(cx, cy)
        ctx.rotate(spin)
        ctx.drawImage(img, -dw / 2, -dh / 2, dw, dh)

        // ── 表面微光 ──
        ctx.globalCompositeOperation = 'source-atop'
        const hlGrad = ctx.createLinearGradient(-dw / 2, -dh / 2, -dw / 2, dh / 2)
        hlGrad.addColorStop(0, 'rgba(255,255,255,0.10)')
        hlGrad.addColorStop(0.35, 'rgba(255,255,255,0.03)')
        hlGrad.addColorStop(0.65, 'rgba(0,0,0,0)')
        hlGrad.addColorStop(1, 'rgba(0,0,0,0.06)')
        ctx.fillStyle = hlGrad
        ctx.fillRect(-dw / 2, -dh / 2, dw, dh)
        ctx.restore()

        // 拖动高亮框（在旋转坐标系中）
        if (!showcasing && b === drag.bead) {
          ctx.save()
          ctx.translate(cx, cy)
          ctx.rotate(spin)
          ctx.strokeStyle = 'rgba(200, 169, 110, 0.7)'
          ctx.lineWidth = 2
          ctx.setLineDash([4, 2])
          ctx.strokeRect(-dw / 2 - 3, -dh / 2 - 3, dw + 6, dh + 6)
          ctx.setLineDash([])
          ctx.restore()
        }
      } else {
        // ── 主珠：圆形裁剪 + 纹理跟随角度旋转 + 球形光照 ──
        ctx.save()

        ctx.beginPath()
        ctx.arc(drawX, drawY, r, 0, Math.PI * 2)
        ctx.clip()

        if (b.imageLoaded && b.image) {
          const imgSize = r * 2
          // 纹理跟随环上位置旋转（圆形裁剪内旋转，边缘无变化，纹理有方向感）
          ctx.save()
          ctx.translate(drawX, drawY)
          ctx.rotate(spin)
          ctx.drawImage(b.image, -r, -r, imgSize, imgSize)
          ctx.restore()

          // 球形光照叠加（屏幕空间固定光源方向）
          const overlay = ctx.createRadialGradient(
            drawX - r * 0.3, drawY - r * 0.35, r * 0.05,
            drawX, drawY, r
          )
          overlay.addColorStop(0, 'rgba(255,255,255,0.15)')
          overlay.addColorStop(0.6, 'rgba(0,0,0,0)')
          overlay.addColorStop(1, 'rgba(0,0,0,0.12)')
          ctx.fillStyle = overlay
          ctx.fillRect(drawX - r, drawY - r, r * 2, r * 2)
        } else {
          const grad = ctx.createRadialGradient(
            drawX - r * 0.3, drawY - r * 0.35, r * 0.1,
            drawX, drawY, r
          )
          grad.addColorStop(0, lightenColor(b.color, 40))
          grad.addColorStop(0.5, b.color)
          grad.addColorStop(1, darkenColor(b.color, 30))
          ctx.fillStyle = grad
          ctx.fillRect(drawX - r, drawY - r, r * 2, r * 2)

          ctx.beginPath()
          ctx.arc(drawX - r * 0.3, drawY - r * 0.35, r * 0.28, 0, Math.PI * 2)
          ctx.fillStyle = 'rgba(255,255,255,0.45)'
          ctx.fill()
        }

        ctx.restore()

        // 拖动高亮环（屏幕空间，不旋转）
        if (!showcasing && b === drag.bead) {
          ctx.save()
          ctx.beginPath()
          ctx.arc(drawX, drawY, r + 3, 0, Math.PI * 2)
          ctx.strokeStyle = 'rgba(200, 169, 110, 0.7)'
          ctx.lineWidth = 2
          ctx.setLineDash([4, 2])
          ctx.stroke()
          ctx.setLineDash([])
          ctx.restore()
        }
      }
    })
  }

  /** 绘制插入间隙指示器 */
  function drawGapIndicator() {
    if (drag.hoverGapIndex < 0) return
    const list = beads.value
    const currentRadius = isBracelet.value ? braceletRadius : grooveRadius

    // 找到间隙两侧的珠子
    const gapAfter = drag.hoverGapIndex
    // 找下一个环上珠子
    let gapBeforeIdx = -1
    for (let i = 0; i < list.length; i++) {
      if (drag.active && list[i] === drag.bead) continue
      const ringIdx = getRingIndex(i)
      if (ringIdx === getRingIndexForGap(gapAfter) + 1) {
        gapBeforeIdx = i
        break
      }
    }

    const b1 = list[gapAfter]
    const b2 = gapBeforeIdx >= 0 ? list[gapBeforeIdx] : null

    if (!b1 || !b2) return

    const midX = (b1.x + b2.x) / 2
    const midY = (b1.y + b2.y) / 2
    const midAngle = Math.atan2(midY - centerY, midX - centerX)
    const indicatorX = centerX + currentRadius * Math.cos(midAngle)
    const indicatorY = centerY + currentRadius * Math.sin(midAngle)

    ctx.save()
    // 脉动圆点
    const pulse = 0.7 + 0.3 * Math.sin(Date.now() / 300)
    ctx.beginPath()
    ctx.arc(indicatorX, indicatorY, BEAD_RADIUS * 0.7 * pulse, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(200, 169, 110, 0.35)'
    ctx.fill()
    ctx.strokeStyle = 'rgba(200, 169, 110, 0.6)'
    ctx.lineWidth = 1.5
    ctx.setLineDash([3, 2])
    ctx.stroke()
    ctx.setLineDash([])
    ctx.restore()
  }

  // ==================== 展示模式切换 ====================
  /**
   * 进入/退出展示模式
   * @param {boolean} show - true 进入展示, false 退出
   */
  function setShowcasing(show) {
    if (show === isShowcasing.value) return

    isShowcasing.value = show

    if (show) {
      // 进入展示：凹槽与线绳渐隐
      showcaseRotation = 0
      gsap.to(_showcaseProxy, {
        grooveOpacity: 0,
        duration: 0.5,
        ease: 'power2.out',
        overwrite: 'auto',
        onUpdate: () => {
          showcaseGrooveOpacity = _showcaseProxy.grooveOpacity
        },
      })
    } else {
      // 退出展示：凹槽与线绳渐显，重置旋转
      gsap.to(_showcaseProxy, {
        grooveOpacity: 1,
        duration: 0.5,
        ease: 'power2.out',
        overwrite: 'auto',
        onUpdate: () => {
          showcaseGrooveOpacity = _showcaseProxy.grooveOpacity
        },
        onComplete: () => {
          showcaseRotation = 0
        },
      })
    }
  }

  // 展示模式动画代理对象
  const _showcaseProxy = { grooveOpacity: 1 }

  // ==================== 重置与清理 ====================
  function reset() {
    if (_toastTimer) clearTimeout(_toastTimer)
    _toastTimer = null
    toastMessage.value = ''
    gsap.killTweensOf(beads.value)

    // 清理物理
    destroyPhysics()

    drag.active = false
    drag.bead = null
    drag.beadIndex = -1
    drag.gapOpen = false
    drag.hoverGapIndex = -1
    isDragging.value = false
    isShowcasing.value = false
    showcaseRotation = 0
    showcaseGrooveOpacity = 1
    _showcaseProxy.grooveOpacity = 1
    beads.value = []
    beadCount.value = 0
    isBracelet.value = false
    isStringing.value = false
    isFull.value = false

    // 重新初始化物理（新鲜空底盘）
    initPhysics()
  }

  function destroy() {
    if (animFrameId) cancelAnimationFrame(animFrameId)
    if (_toastTimer) clearTimeout(_toastTimer)
    gsap.killTweensOf(beads.value)
    destroyPhysics()
    if (canvasEl) {
      canvasEl.removeEventListener('pointerdown', onPointerDown)
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
    }
  }

  return {
    beads,
    beadCount,
    isBracelet,
    isStringing,
    isFull,
    isDragging,
    isShowcasing,
    toastMessage,
    showToast,
    setShowcasing,
    mount,
    resize,
    pauseRenderLoop,
    resumeRenderLoop,
    launchBead,
    stringToBracelet,
    skipAndString,
    dissolveBracelet,
    reset,
    destroy,
    removeBead,
    // Exported geometric constants for Three.js layer
    get centerX() { return centerX },
    get centerY() { return centerY },
    get grooveRadius() { return grooveRadius },
    get braceletRadius() { return braceletRadius },
  }
}
