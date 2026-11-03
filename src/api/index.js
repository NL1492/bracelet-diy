/**
 * 手串DIY — 后端 API 对接模块
 *
 * 使用方式：
 *   import { braceletApi } from '@/api'
 *   const data = await braceletApi.getCountData(beads)
 *
 * 登录态：JWT token 存 localStorage，请求自动携带 Authorization 头；
 * 收到业务码 401 时清除本地登录态并广播 auth:required 事件（App.vue 监听后弹出登录框）
 *
 * Vite 开发代理配置（vite.config.js 添加）：
 *   server: {
 *     proxy: {
 *       '/api': 'http://localhost:8080'
 *     }
 *   }
 */

const BASE_URL = '/api/bracelet'

// ==================== 登录态管理 ====================

const TOKEN_KEY = 'bracelet-token'

/** 读取本地 token */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 写入/清除本地 token */
export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** 401 统一处理：清登录态 + 通知 App 弹出登录框 */
function handleUnauthorized() {
  setToken('')
  localStorage.removeItem('bracelet-user')
  window.dispatchEvent(new CustomEvent('auth:required'))
}

/** 通用请求头 */
function buildHeaders(extra = {}) {
  const headers = { 'Content-Type': 'application/json', ...extra }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return headers
}

/** 业务码 401 检查 */
function checkAuth(json) {
  if (json && json.code === 401) {
    handleUnauthorized()
    throw new Error(json.msg || '请先登录')
  }
  return json
}

/** 通用 POST 请求 */
async function post(url, body) {
  const res = await fetch(`${BASE_URL}${url}`, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return checkAuth(await res.json())
}

/** 通用 GET 请求 */
async function get(url) {
  const res = await fetch(`${BASE_URL}${url}`, {
    headers: buildHeaders(),
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return checkAuth(await res.json())
}

/** 通用 PUT 请求 */
async function put(url, body) {
  const res = await fetch(`${BASE_URL}${url}`, {
    method: 'PUT',
    headers: buildHeaders(),
    body: JSON.stringify(body || {}),
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return checkAuth(await res.json())
}

/** 通用 DELETE 请求 */
async function del(url) {
  const res = await fetch(`${BASE_URL}${url}`, {
    method: 'DELETE',
    headers: buildHeaders(),
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return checkAuth(await res.json())
}

// ==================== 导出 API 方法 ====================

export const braceletApi = {
  /**
   * 查询全部上架珠子信息（灵感详情构成解析使用）
   * @returns {Promise<{code:number, msg:string, data:Array}>}
   */
  listBeads() {
    return get('/beads')
  },

  /**
   * 实时统计订单数据
   */
  getCountData(beads) {
    return post('/getCountData', beads)
  },

  /**
   * 保存设计记录（新建 / 覆盖）
   * @param {{designId?: number, designName: string, braceleted: boolean, beads: Array}} dto
   */
  saveDesign(dto) {
    return post('/saveDesign', dto)
  },

  /**
   * 加入购物车
   */
  addCart(dto) {
    return post('/addCart', dto)
  },

  /**
   * 查询设计方案列表（当前用户）
   * @returns {Promise<{code:number, msg:string, data:Array}>}
   */
  listDesigns() {
    return get('/designs')
  },

  /**
   * 查询单个方案详情
   * @param {number} id 方案ID
   */
  getDesign(id) {
    return get(`/designs/${id}`)
  },

  /**
   * 删除设计方案（逻辑删除，仅限本人）
   * @param {number} id 方案ID
   */
  deleteDesign(id) {
    return del(`/designs/${id}`)
  },

  /**
   * 查询购物车列表（当前用户）
   */
  listCart() {
    return get('/cart')
  },

  /**
   * 删除购物车记录
   * @param {number} id 购物车记录ID
   */
  deleteCart(id) {
    return del(`/cart/${id}`)
  },

  // ==================== 用户模块 ====================

  /**
   * 注册（成功即返回 token，自动登录）
   * @param {{username: string, password: string, nickname?: string}} dto
   */
  register(dto) {
    return post('/user/register', dto)
  },

  /**
   * 登录
   * @param {{username: string, password: string}} dto
   */
  login(dto) {
    return post('/user/login', dto)
  },

  /**
   * 查询当前登录用户信息（用于启动时校验 token）
   */
  getUserInfo() {
    return get('/user/info')
  },

  /**
   * 退出登录（JWT 无状态，前端主要负责清 token）
   */
  logout() {
    return post('/user/logout', {})
  },

  // ==================== 灵感集录 / 分享码 ====================

  /**
   * 查询上架灵感（公开接口）→ { design: [...], guest: [...] }
   */
  listInspirations() {
    return get('/inspirations')
  },

  /**
   * 灵感被应用计数 +1（公开，静默调用）
   */
  useInspiration(id) {
    return post(`/inspirations/${id}/use`, {})
  },

  /**
   * 为我的设计方案生成分享码（已有则返回原码）
   * @param {number} designId 方案ID
   * @returns {Promise<{code:number, data:string}>} data = 16 位分享码
   */
  createShareCode(designId) {
    return post(`/designs/${designId}/share`, {})
  },

  /**
   * 凭分享码载入方案（需登录），对方方案载入次数 +1
   * @returns {Promise<{code:number, data:{designId,designName,ownerNickname,beadListJson,shareUseCount}}>}
   */
  loadShareCode(code) {
    return get(`/share/${encodeURIComponent(code)}`)
  },

  // ==================== 订单模块 ====================

  /**
   * 结算购物车勾选项 → 逐项生成订单（一单一方案）
   * @param {{cartIds: number[], receiverName: string, receiverPhone: string, receiverAddress: string, remark?: string}} dto
   */
  createOrders(dto) {
    return post('/orders', dto)
  },

  /**
   * 我的订单列表（status 为空则全部；0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款）
   */
  listOrders(status) {
    const qs = status === undefined || status === null ? '' : `?status=${status}`
    return get(`/orders${qs}`)
  },

  /** 订单详情 */
  getOrder(id) {
    return get(`/orders/${id}`)
  },

  /** mock 支付：待付款 → 待发货 */
  payOrder(id) {
    return post(`/orders/${id}/pay`, {})
  },

  /** 取消订单：待付款 → 已取消 */
  cancelOrder(id) {
    return post(`/orders/${id}/cancel`, {})
  },

  /** 确认收货：待收货 → 已完成 */
  confirmOrder(id) {
    return post(`/orders/${id}/confirm`, {})
  },

  /** 申请退款：待发货/待收货 → 退款中 */
  refundOrder(id) {
    return post(`/orders/${id}/refund`, {})
  },

  // ==================== 收货地址簿 ====================

  /**
   * 我的收货地址列表（默认地址排在最前）
   * @returns {Promise<{code:number, data:Array}>}
   */
  listAddresses() {
    return get('/addresses')
  },

  /**
   * 新增收货地址
   * @param {{receiverName:string, receiverPhone:string, region:string, detail:string, isDefault?:number}} dto
   */
  createAddress(dto) {
    return post('/addresses', dto)
  },

  /**
   * 编辑收货地址（仅限本人）
   * @param {number} id 地址ID
   * @param {{receiverName:string, receiverPhone:string, region:string, detail:string, isDefault?:number}} dto
   */
  updateAddress(id, dto) {
    return put(`/addresses/${id}`, dto)
  },

  /** 删除收货地址（逻辑删除） */
  deleteAddress(id) {
    return del(`/addresses/${id}`)
  },

  /** 设为默认地址 */
  setDefaultAddress(id) {
    return put(`/addresses/${id}/default`, {})
  },
}
