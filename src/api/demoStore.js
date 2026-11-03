/**
 * 游客演示模式 — 无后端时的本地兜底数据层
 *
 * 触发条件：API 请求网络不通（静态托管如 GitHub Pages 上 /api/** 返回 404）时，
 * api/index.js 会把请求转交到这里的 demoRequest，用 localStorage 模拟后端。
 *
 * 覆盖范围：登录/注册（任意账号密码均可进入，昵称取输入值）、设计方案、
 * 购物车、订单（含状态机 0待付款/1待发货/2待收货/3已完成/4已取消/5退款中）、
 * 收货地址簿（默认地址规则与后端一致）、分享码。
 * 数据仅存于访问者浏览器，清缓存即清空。
 */

export const demoState = { active: false }

/** 激活演示模式（只通知一次） */
function activate() {
  if (demoState.active) return
  demoState.active = true
  try { window.dispatchEvent(new CustomEvent('demo:mode')) } catch (e) {}
}

// ==================== 本地集合存取 ====================

function read(key) {
  try { return JSON.parse(localStorage.getItem('demo-' + key)) || [] } catch (e) { return [] }
}
function write(key, list) {
  try { localStorage.setItem('demo-' + key, JSON.stringify(list)) } catch (e) {}
}
function nextId(key) {
  const seq = read('seq')
  const n = (seq[0] || 1000) + 1
  write('seq', [n])
  return n
}
function now() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// ==================== 伪价格（无后端报价时的确定性估算） ====================

function hashCode(s) {
  let h = 0
  const str = String(s || '')
  for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) | 0
  return Math.abs(h)
}
/** 单颗珠子估算单价：6 元起 + 名称哈希扰动 + 尺寸系数 */
function beadPrice(item) {
  const size = Number(item && item.size) || 10
  const seed = hashCode(item && (item.beadName || item.name || item.beadId || item.id))
  return 6 + (seed % 20) + size * 0.4
}
/** 由 beads 数组估算总价 */
function estimatePrice(beads) {
  return +(Array.isArray(beads) ? beads.reduce((s, b) => s + beadPrice(b), 0) : 0).toFixed(1)
}

// ==================== 游客用户 ====================

const GUEST_KEY = 'demo-guest-user'
function getGuest() {
  try { return JSON.parse(localStorage.getItem(GUEST_KEY)) } catch (e) { return null }
}
function guestVO(nickname) {
  return {
    token: 'demo-' + Date.now(),
    userId: 1,
    username: 'guest',
    nickname: nickname || '游客',
    avatar: null,
    role: 'user',
  }
}

// ==================== 各模块路由处理 ====================

function handleUser(method, path, body) {
  if (path === '/user/login' || path === '/user/register') {
    const vo = guestVO(body && (body.nickname || body.username))
    try { localStorage.setItem(GUEST_KEY, JSON.stringify(vo)) } catch (e) {}
    return { code: 200, msg: 'ok', data: vo }
  }
  if (path === '/user/info') {
    const g = getGuest()
    if (!g) return { code: 401, msg: '请先登录', data: null }
    const { token, ...info } = g
    return { code: 200, msg: 'ok', data: info }
  }
  if (path === '/user/logout') {
    try { localStorage.removeItem(GUEST_KEY) } catch (e) {}
    return { code: 200, msg: 'ok', data: null }
  }
  return { code: 404, msg: '接口不存在', data: null }
}

function handleDesigns(method, path, body) {
  const designs = read('designs')
  const save = () => write('designs', designs)

  if (method === 'GET' && path === '/designs') {
    return { code: 200, msg: 'ok', data: [...designs].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || '')) }
  }
  if (method === 'POST' && path === '/saveDesign') {
    const beads = Array.isArray(body && body.beads) ? body.beads : []
    if (body && body.designId) {
      const it = designs.find(d => d.id === Number(body.designId))
      if (it) {
        it.designName = body.designName || it.designName
        it.beadListJson = JSON.stringify({ braceleted: !!body.braceleted, beads })
        it.beadCount = beads.length
        it.totalPrice = estimatePrice(beads)
        save()
        return { code: 200, msg: 'ok', data: it.id }
      }
    }
    const id = nextId('designs')
    designs.push({
      id,
      designName: (body && body.designName) || '未命名方案',
      beadListJson: JSON.stringify({ braceleted: !!(body && body.braceleted), beads }),
      beadCount: beads.length,
      totalPrice: estimatePrice(beads),
      createTime: now(),
    })
    save()
    return { code: 200, msg: 'ok', data: id }
  }

  const dm = path.match(/^\/designs\/(\d+)$/)
  if (dm) {
    const id = Number(dm[1])
    const it = designs.find(d => d.id === id)
    if (method === 'GET') {
      return it
        ? { code: 200, msg: 'ok', data: it }
        : { code: 500, msg: '方案不存在', data: null }
    }
    if (method === 'DELETE') {
      write('designs', designs.filter(d => d.id !== id))
      return { code: 200, msg: 'ok', data: null }
    }
  }

  const sm = path.match(/^\/designs\/(\d+)\/share$/)
  if (sm && method === 'POST') {
    const it = designs.find(d => d.id === Number(sm[1]))
    if (!it) return { code: 500, msg: '方案不存在', data: null }
    if (!it.shareCode) {
      let c = ''
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
      for (let i = 0; i < 16; i++) c += chars[Math.floor(Math.random() * chars.length)]
      it.shareCode = c
      it.shareUseCount = 0
      save()
    }
    return { code: 200, msg: 'ok', data: it.shareCode }
  }

  const lm = path.match(/^\/share\/([^/]+)$/)
  if (lm && method === 'GET') {
    const code = decodeURIComponent(lm[1])
    const it = designs.find(d => d.shareCode === code)
    if (!it) return { code: 500, msg: '分享码无效', data: null }
    it.shareUseCount = (it.shareUseCount || 0) + 1
    save()
    return { code: 200, msg: 'ok', data: { designId: it.id, designName: it.designName, ownerNickname: '游客', beadListJson: it.beadListJson, shareUseCount: it.shareUseCount } }
  }

  return { code: 404, msg: '接口不存在', data: null }
}

function handleCart(method, path, body) {
  const cart = read('cart')

  if (method === 'GET' && path === '/cart') {
    return { code: 200, msg: 'ok', data: [...cart].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || '')) }
  }
  if (method === 'POST' && path === '/addCart') {
    const beads = Array.isArray(body && body.beads) ? body.beads : []
    cart.push({
      id: nextId('cart'),
      designName: (body && body.designName) || '未命名手串',
      beadListJson: JSON.stringify({ braceleted: true, beads }),
      beadCount: beads.length,
      totalPrice: estimatePrice(beads),
      createTime: now(),
    })
    write('cart', cart)
    return { code: 200, msg: 'ok', data: null }
  }
  const dm = path.match(/^\/cart\/(\d+)$/)
  if (dm && method === 'DELETE') {
    const id = Number(dm[1])
    write('cart', cart.filter(c => c.id !== id))
    return { code: 200, msg: 'ok', data: null }
  }
  return { code: 404, msg: '接口不存在', data: null }
}

function handleOrders(method, path, body, qs) {
  const orders = read('orders')
  const save = () => write('orders', orders)

  if (method === 'POST' && path === '/orders') {
    const cart = read('cart')
    const ids = (body && body.cartIds) || []
    const created = []
    ids.forEach(cid => {
      const it = cart.find(c => c.id === Number(cid))
      if (!it) return
      const o = {
        id: nextId('orders'),
        orderNo: 'DEMO' + Date.now() + String(Math.floor(Math.random() * 90) + 10),
        designName: it.designName,
        beadListJson: it.beadListJson,
        beadCount: it.beadCount,
        totalAmount: it.totalPrice,
        status: 0,
        receiverName: body.receiverName,
        receiverPhone: body.receiverPhone,
        receiverAddress: body.receiverAddress,
        remark: body.remark || '',
        createTime: now(),
        payTime: null,
      }
      orders.push(o)
      created.push(o)
      write('cart', cart.filter(c => c.id !== it.id))
    })
    save()
    return { code: 200, msg: 'ok', data: created }
  }

  if (method === 'GET' && path === '/orders') {
    const st = qs.get('status')
    let list = [...orders].sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
    if (st !== null && st !== '') list = list.filter(o => o.status === Number(st))
    return { code: 200, msg: 'ok', data: list }
  }

  const pm = path.match(/^\/orders\/(\d+)\/(pay|cancel|confirm|refund)$/)
  if (pm && method === 'POST') {
    const o = orders.find(x => x.id === Number(pm[1]))
    if (!o) return { code: 500, msg: '订单不存在', data: null }
    const act = pm[2]
    const ALLOWED = { pay: [0], cancel: [0], confirm: [2], refund: [1, 2] }
    if (!ALLOWED[act].includes(o.status)) return { code: 500, msg: '当前状态不可执行该操作', data: null }
    if (act === 'pay') { o.status = 1; o.payTime = now() }
    if (act === 'cancel') o.status = 4
    if (act === 'confirm') o.status = 3
    if (act === 'refund') o.status = 5
    save()
    return { code: 200, msg: 'ok', data: null }
  }

  const dm = path.match(/^\/orders\/(\d+)$/)
  if (dm && method === 'GET') {
    const o = orders.find(x => x.id === Number(dm[1]))
    return o ? { code: 200, msg: 'ok', data: o } : { code: 500, msg: '订单不存在', data: null }
  }

  return { code: 404, msg: '接口不存在', data: null }
}

function handleAddresses(method, path, body) {
  const list = read('addresses')
  const save = () => write('addresses', list)

  if (method === 'GET' && path === '/addresses') {
    return { code: 200, msg: 'ok', data: [...list].sort((a, b) => (b.isDefault ? 1 : 0) - (a.isDefault ? 1 : 0)) }
  }
  if (method === 'POST' && path === '/addresses') {
    const isFirst = list.length === 0
    const wantDefault = (body && body.isDefault) || isFirst
    if (wantDefault) list.forEach(a => { a.isDefault = 0 })
    list.push({
      id: nextId('addresses'),
      receiverName: body.receiverName,
      receiverPhone: body.receiverPhone,
      region: body.region || '',
      detail: body.detail || '',
      isDefault: wantDefault ? 1 : 0,
      createTime: now(),
    })
    save()
    return { code: 200, msg: 'ok', data: null }
  }
  const um = path.match(/^\/addresses\/(\d+)$/)
  if (um) {
    const id = Number(um[1])
    const it = list.find(a => a.id === id)
    if (!it) return { code: 500, msg: '地址不存在', data: null }
    if (method === 'PUT' && !/\/default$/.test(path)) {
      if (body.isDefault) list.forEach(a => { a.isDefault = 0 })
      Object.assign(it, {
        receiverName: body.receiverName,
        receiverPhone: body.receiverPhone,
        region: body.region || '',
        detail: body.detail || '',
        isDefault: body.isDefault ? 1 : it.isDefault,
      })
      save()
      return { code: 200, msg: 'ok', data: null }
    }
    if (method === 'DELETE') {
      const wasDefault = it.isDefault
      write('addresses', list.filter(a => a.id !== id))
      if (wasDefault) {
        const rest = read('addresses')
        if (rest.length) { rest[0].isDefault = 1; write('addresses', rest) }
      }
      return { code: 200, msg: 'ok', data: null }
    }
    if (method === 'PUT' && /\/default$/.test(path)) {
      list.forEach(a => { a.isDefault = a.id === id ? 1 : 0 })
      save()
      return { code: 200, msg: 'ok', data: null }
    }
  }
  return { code: 404, msg: '接口不存在', data: null }
}

// ==================== 总路由 ====================

/**
 * 演示模式请求入口：按 method + url 路由到本地存储，返回与后端一致的 {code,msg,data}
 */
export function demoRequest(method, url, body) {
  activate()
  const path = url.split('?')[0]
  const qs = new URLSearchParams(url.split('?')[1] || '')
  let res
  if (path.startsWith('/user') || path === '/user/logout') res = handleUser(method, path, body)
  else if (path.startsWith('/designs') || path.startsWith('/share') || path === '/saveDesign') res = handleDesigns(method, path, body)
  else if (path === '/cart' || path === '/addCart' || path.startsWith('/cart/')) res = handleCart(method, path, body)
  else if (path === '/orders' || path.startsWith('/orders/')) res = handleOrders(method, path, body, qs)
  else if (path.startsWith('/addresses')) res = handleAddresses(method, path, body)
  else if (path === '/beads') res = { code: 200, msg: 'ok', data: [] }
  else if (path === '/getCountData') res = { code: 200, msg: 'ok', data: null }
  else res = { code: 404, msg: '演示模式暂不支持该功能', data: null }
  return Promise.resolve(res)
}
