// 素材目录共享数据源：C 端作坊（App.vue）与管理端素材库（MaterialsView.vue）共用
// 结构 1:1 取自 App.vue 原 categories 定义；素材增删统一改这里，勿在两端各自维护

export const materialCategories = [
  {
    id: 'main-bead',
    label: '主珠',
    defaultSize: 10,
    sizeMin: 8,
    sizeMax: 14,
    subCategories: [
      { id: 'main-bead-in-use', label: '正在使用', items: [] },
      { id: 'main-bead-all', label: '所有主珠', items: [] },
      {
        id: 'crystal-white',
        label: '白色系',
        items: [
          '和田白玉', '喜马拉雅白', '白兔毛', '白幽灵', '白月光',
          '白松石', '白水晶', '白玉髓', '白珊瑚', '白珍珠',
          '白翡翠', '白萤石', '白蝶贝', '白阿塞',
        ].map(name => ({ name, path: `/水晶/白色系/${name}.png` })),
      },
      {
        id: 'crystal-black',
        label: '黑色系',
        items: [
          '冰种黑曜', '地狱海蓝', '彩虹黑曜', '毒液超七', '茶晶',
          '金曜石', '金运石', '阴曜石', '黑发晶', '黑曜石',
          '黑月光', '黑极光', '黑水晶', '黑烟超七', '黑玛瑙',
          '黑碧玺', '黑草莓', '黑透辉石', '黑金阿七', '黑金阿塞',
          '黑金骨干', '黑银钛', '黑闪灵', '黑骨干', '黑龙晶',
        ].map(name => ({ name, path: `/水晶/黑色系/${name}.png` })),
      },
      {
        id: 'crystal-red',
        label: '红色系',
        items: [
          '冰粉晶', '南红玛瑙', '星光粉', '朱砂', '樱花玛瑙',
          '石榴石', '粉兔毛', '粉幽灵', '粉月光', '粉欧珀',
          '粉烟超七', '粉玉髓', '粉白幽', '红兔毛', '红发晶',
          '红幽灵', '红玉髓', '红玛瑙', '红碧玺', '红纹石',
          '红胶花', '红虎眼', '红超七', '红锂云母', '草莓晶',
          '莫粉', '蔷薇石', '金太阳', '金草莓', '马粉',
        ].map(name => ({ name, path: `/水晶/红色系/${name}.png` })),
      },
      {
        id: 'crystal-yellow',
        label: '黄色系',
        items: [
          '利比亚陨石', '太阳石', '柠檬黄晶', '橙月光', '橙红石榴',
          '橙黄水晶', '红东陵玉', '蜜蜡', '金发晶', '金葡萄',
          '铜发晶', '黄兔毛', '黄发晶', '黄塔晶', '黄幽灵',
          '黄托帕', '黄方解石', '黄水晶', '黄玛瑙', '黄碧玺',
          '黄胶花', '黄萤石', '黄虎眼', '黄阿塞', '黄龙玉',
        ].map(name => ({ name, path: `/水晶/黄色系/${name}.png` })),
      },
      {
        id: 'crystal-blue',
        label: '蓝色系',
        items: [
          '坦桑石', '堇青石', '天使石', '天河石', '异极矿',
          '彼得石', '海纹石', '海蓝宝', '绿松石', '苏打石',
          '蓝东陵玉', '蓝兔毛', '蓝发晶', '蓝托帕', '蓝方解石',
          '蓝晶石', '蓝月光', '蓝玛瑙', '蓝砂石', '蓝碧玺',
          '蓝磷辉', '蓝纹方纳', '蓝纹玛瑙', '蓝绿晶', '蓝萤石',
          '蓝虎眼', '蓝针', '蓝铜矿', '青金石',
        ].map(name => ({ name, path: `/水晶/蓝色系/${name}.png` })),
      },
      {
        id: 'crystal-green',
        label: '绿色系',
        items: [
          '东陵玉', '孔雀石', '岫玉', '捷克陨石', '橄榄石',
          '祖母绿', '绿兔毛', '绿发晶', '绿幽灵', '绿方解',
          '绿松石', '绿水晶', '绿玉髓', '绿碧玺', '绿草莓',
          '绿萤石', '绿虎眼', '绿锂云母', '绿龙晶', '翠榴石',
          '翡翠', '莎弗莱', '葡萄石', '透辉石', '金绿石',
        ].map(name => ({ name, path: `/水晶/绿色系/${name}.png` })),
      },
    ],
  },
  {
    id: 'accessories',
    label: '配饰',
    defaultSize: 12,
    sizeMin: 8,
    sizeMax: 18,
    subCategories: [
      { id: 'acc-in-use', label: '正在使用', items: [] },
      { id: 'acc-all', label: '所有配饰', items: [] },
      {
        id: 'acc-pendant',
        label: '挂件',
        items: [
          { name: '好事发生', path: '/配饰/挂件/好事发生.png', size: 24 },
          { name: '繁花', path: '/配饰/挂件/繁花.png', size: 24 },
          { name: '纳福', path: '/配饰/挂件/纳福.png', size: 24 },
          { name: '银色树叶', path: '/配饰/挂件/银色树叶.png', size: 24 },
          { name: '银色流苏', path: '/配饰/挂件/银色流苏.png', size: 24 },
          { name: '玛瑙福袋', path: '/配饰/挂件/玛瑙福袋.png', size: 16 },
          { name: '祥云坠', path: '/配饰/挂件/祥云坠.png', size: 16 },
          { name: '紫晶福袋', path: '/配饰/挂件/紫晶福袋.png', size: 16 },
          { name: '莲蓬', path: '/配饰/挂件/莲蓬.png', size: 16 },
          { name: '金圆坠', path: '/配饰/挂件/金圆坠.png', size: 16 },
          { name: '玛瑙铃兰', path: '/配饰/挂件/玛瑙铃兰.png' },
          { name: '金花', path: '/配饰/挂件/金花.png' },
        ],
      },
      {
        id: 'acc-sugar',
        label: '方糖',
        items: [
          '奶白渐变方糖', '橙红胶花方糖', '浅粉紫玉方糖', '紫罗兰紫晶方糖',
          '红发丝方糖', '黄褐幽灵方糖', '黑幽灵方糖',
        ].map(name => ({ name, path: `/配饰/方糖/${name}.png` })),
      },
      {
        id: 'acc-carve',
        label: '雕件',
        items: [
          '三重花', '沉香药片珠', '白水晶kitty猫', '白水晶药片珠',
          '红玛瑙算盘珠', '金马',
        ].map(name => ({ name, path: `/配饰/雕件/${name}.png` })),
      },
    ],
  },
  {
    id: 'spacer',
    label: '隔片',
    defaultSize: 8,
    sizeMin: 6,
    sizeMax: 14,
    subCategories: [
      { id: 'spacer-in-use', label: '正在使用', items: [] },
      { id: 'spacer-all', label: '所有隔片', items: [] },
      {
        id: 'spacer-flower',
        label: '花托',
        items: [
          '右花托', '左花托',
        ].map(name => ({ name, path: `/隔片/花托/${name}.png` })),
      },
      {
        id: 'spacer-ring',
        label: '跑环',
        items: [
          '白水晶跑环', '紫水晶跑环', '蓝水晶跑环',
        ].map(name => ({ name, path: `/隔片/跑环/${name}.png` })),
      },
      {
        id: 'spacer-sheet',
        label: '隔片',
        items: [
          '叶纹筒', '珍珠隔', '白色金隔片', '福僵桶',
          '红色金隔片', '绿色金隔片', '蓝色金隔片', '金色隔片',
        ].map(name => ({ name, path: `/隔片/隔片/${name}.png` })),
      },
      {
        id: 'spacer-bead',
        label: '隔珠',
        items: [
          '真言珠', '祥云', '祥云珠', '镂空球',
        ].map(name => ({ name, path: `/隔片/隔珠/${name}.png` })),
      },
    ],
  },
  {
    id: 'wenwan',
    label: '文玩',
    defaultSize: 12,
    sizeMin: 10,
    sizeMax: 18,
    subCategories: [
      { id: 'wenwan-in-use', label: '正在使用', items: [] },
      { id: 'wenwan-all', label: '所有文玩', items: [] },
    ],
  },
]

// 填充 "所有" 子分类（App.vue 原逻辑 1:1）
export function buildAllItems(cat) {
  const all = []
  cat.subCategories.forEach(sub => {
    if (sub.id.endsWith('-in-use') || sub.id.endsWith('-all')) return
    all.push(...sub.items)
  })
  return all
}

// ==================== 库存 store（管理端维护，localStorage 持久化） ====================
// v2：按「素材 × 尺寸」二维细分（用户端可选珠子尺寸，库存需按尺寸统计）；
//     v1 扁平数字 { 名: 总数 } 在首次访问时惰性迁移：总数挂到素材默认尺寸，其余尺寸 0；
//     总库存 = 各尺寸之和。素材库详情面板（编辑）与经营总览「库存预警」（合计）共用同一数据源
const STOCK_KEY = 'material-stock-v1'
// 默认库存种子：未在 localStorage 中改过的素材取此值（挂默认尺寸）；其余素材默认 40
const DEFAULT_STOCK = { '紫水晶': 8, '黑曜石': 3, '草莓晶': 12, '金刚菩提': 9, '金曜石': 14, '小叶紫檀': 18, '多宝': 12 }
const STOCK_FALLBACK = 40
let _stockStore = null
function ensureStock() {
  if (!_stockStore) {
    let saved = {}
    try { saved = JSON.parse(localStorage.getItem(STOCK_KEY)) || {} } catch (e) { saved = {} }
    _stockStore = Object.assign({}, DEFAULT_STOCK, saved)
  }
  return _stockStore
}
function persistStock() {
  try { localStorage.setItem(STOCK_KEY, JSON.stringify(_stockStore)) } catch (e) {}
}
// 单条记录归一化：v1 数字（或缺失）→ { bySize: { 默认尺寸: 值 } }
function normEntry(v, defaultSize) {
  if (v && typeof v === 'object' && v.bySize) return v
  return { bySize: { [defaultSize || 10]: Number.isFinite(v) ? v : STOCK_FALLBACK } }
}
// 各尺寸明细：按大类 sizeMin..sizeMax 逐档产出（缺失档补 0），并完成 v1 → v2 惰性迁移
export function getStockDetail(name, cat) {
  const s = ensureStock()
  const def = cat && cat.defaultSize || 10
  s[name] = normEntry(s[name], def)
  const bySize = s[name].bySize
  const min = cat && cat.sizeMin || 10, max = cat && cat.sizeMax || def
  const out = []
  for (let z = min; z <= max; z++) out.push({ size: z, qty: Math.max(0, Math.round(bySize[z]) || 0) })
  return out
}
export function setStockSize(name, size, qty, cat) {
  const s = ensureStock()
  s[name] = normEntry(s[name], cat && cat.defaultSize)
  s[name].bySize[size] = Math.max(0, Math.round(qty) || 0)
  persistStock()
  return s[name].bySize[size]
}
// 总库存 = 各尺寸之和（兼容读取 v1 扁平数字）
export function getStock(name) {
  const v = ensureStock()[name]
  if (v && typeof v === 'object' && v.bySize) {
    return Object.values(v.bySize).reduce((a, b) => a + (Math.max(0, Math.round(b)) || 0), 0)
  }
  return Number.isFinite(v) ? v : STOCK_FALLBACK
}

// 素材长度（mm）：item.size 优先，其次大类 defaultSize（与 C 端 beadMetaMap 口径一致）
export function getSize(item, cat) {
  return item.size || cat.defaultSize || 10
}

// 初始化目录：填充所有 -all 子分类，并追加文玩的独立素材（App.vue 原启动逻辑 1:1，幂等可重复调用）
let _inited = false
export function initCatalog() {
  if (_inited) return materialCategories
  _inited = true
  materialCategories.forEach(cat => {
    cat.subCategories.forEach(sub => {
      if (sub.id.endsWith('-all')) {
        sub.items = buildAllItems(cat)
      }
    })
    // 如果原始有 flat items（文玩），合并到 all 里
    if (cat.items && cat.items.length) {
      const allSub = cat.subCategories.find(s => s.id.endsWith('-all'))
      if (allSub) {
        allSub.items = [...allSub.items, ...cat.items]
      }
      delete cat.items
    }
  })

  // 追加文玩的独立素材
  const wenwanCat = materialCategories.find(c => c.id === 'wenwan')
  const wenwanAllSub = wenwanCat.subCategories.find(s => s.id === 'wenwan-all')
  wenwanAllSub.items.push(
    ...Array.from({ length: 20 }, (_, i) => ({
      name: `文玩_${String(i + 1).padStart(2, '0')}`,
      path: `/文玩/文玩/文玩_${String(i + 1).padStart(2, '0')}.png`,
    })),
  )

  return materialCategories
}
