<!-- 用户中心视图：用户账号管理（列表 / 新增 / 编辑 / 重置密码 / 删除二次认证） -->
<template>
  <div class="view view--on" data-view="users">
    <div class="us-main">
      <div class="us-toolbar">
        <div class="us-title">
          <b>用户管理</b>
          <span>共 {{ users.length }} 个账号</span>
        </div>
        <label class="us-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><path d="m20 20-3.5-3.5"></path></svg>
          <input v-model="q" type="text" placeholder="搜索用户名 / 昵称…">
        </label>
        <button class="us-add" @click="openCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 5v14M5 12h14"></path></svg>
          新增用户
        </button>
      </div>

      <div class="us-table anim" :key="listKey">
        <div class="us-thead">
          <span>ID</span><span>用户名</span><span>昵称</span><span>角色</span><span>注册时间</span><span class="us-op-col">操作</span>
        </div>
        <div v-for="(u, i) in filtered" :key="u.id" class="us-row" :style="{ '--i': i }">
          <span class="us-id">#{{ u.id }}</span>
          <span class="us-name">{{ u.username }}<i v-if="isSelf(u)" class="us-me">我</i></span>
          <span class="us-nick">{{ u.nickname }}</span>
          <span><i class="us-badge" :class="{ 'us-badge--admin': u.role === 'admin' }">{{ u.role === 'admin' ? '管理员' : '用户' }}</i></span>
          <span class="us-time">{{ fmtTime(u.createTime) }}</span>
          <span class="us-ops">
            <button @click="openPassword(u)">改密</button>
            <button @click="openEdit(u)">编辑</button>
            <button v-if="!isSelf(u) && u.role !== 'admin'" class="us-del" @click="openDelete(u)">删除</button>
          </span>
        </div>
        <div v-if="!filtered.length" class="us-empty">没有匹配的用户</div>
      </div>
    </div>

    <!-- 新增 / 编辑用户弹窗 -->
    <transition name="mup">
      <div v-if="form.open" class="mup-mask" @click.self="form.open = false">
        <div class="mup-panel">
          <div class="mup-head"><b>{{ form.mode === 'create' ? '新增用户' : '编辑用户' }}</b><span @click="form.open = false">✕</span></div>
          <div class="mup-form">
            <label>用户名<input :value="form.username" :disabled="form.mode === 'edit'" maxlength="20" placeholder="3-20 位，中文/字母/数字/下划线"></label>
            <label>昵称<input v-model="form.nickname" maxlength="20" placeholder="默认同用户名"></label>
            <label v-if="form.mode === 'create'">密码<input v-model="form.password" type="password" maxlength="32" placeholder="6-32 位"></label>
            <label>角色
              <select v-model="form.role" :disabled="form.mode === 'edit' && isSelfId(form.id)">
                <option value="user">普通用户</option>
                <option value="admin">管理员</option>
              </select>
            </label>
            <p v-if="form.mode === 'edit' && isSelfId(form.id)" class="mup-note">出于安全考虑，不能修改自己的角色。</p>
          </div>
          <div class="mup-foot">
            <button class="mup-btn" @click="form.open = false">取消</button>
            <button class="mup-btn mup-btn--go" :disabled="form.busy" @click="submitForm">{{ form.busy ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 重置密码弹窗 -->
    <transition name="mup">
      <div v-if="pw.open" class="mup-mask" @click.self="pw.open = false">
        <div class="mup-panel">
          <div class="mup-head"><b>重置密码</b><span @click="pw.open = false">✕</span></div>
          <p class="us-pw-hint">为用户 <b>{{ pw.user && pw.user.username }}</b>（{{ pw.user && pw.user.nickname }}）设置新密码。重置后请通知该用户重新登录。</p>
          <div class="mup-form">
            <label>新密码<input v-model="pw.p1" type="password" maxlength="32" placeholder="6-32 位"></label>
            <label>确认密码<input v-model="pw.p2" type="password" maxlength="32" placeholder="再次输入新密码" @keyup.enter="submitPassword"></label>
          </div>
          <div class="mup-foot">
            <button class="mup-btn" @click="pw.open = false">取消</button>
            <button class="mup-btn mup-btn--go" :disabled="pw.busy" @click="submitPassword">{{ pw.busy ? '提交中…' : '确认重置' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除用户：二次认证（输入用户名确认） -->
    <transition name="mup">
      <div v-if="del.open" class="mup-mask" @click.self="closeDelete">
        <div class="mup-panel us-panel--danger">
          <div class="mup-head"><b class="us-danger-title">删除用户</b><span @click="closeDelete">✕</span></div>
          <div class="us-danger-box">
            即将删除用户 <b>{{ del.user && del.user.username }}</b>（{{ del.user && del.user.nickname }}）。<br>
            删除后该账号将<b>无法登录</b>，且此操作<b>不可恢复</b>。<br>
            为防止误删，请输入该用户的<b>用户名</b>以确认。
          </div>
          <input class="us-confirm-input" v-model="del.confirm" :placeholder="del.user && del.user.username" maxlength="20">
          <div class="mup-foot">
            <button class="mup-btn" @click="closeDelete">取消</button>
            <button class="mup-btn us-btn-danger" :disabled="del.confirm !== (del.user && del.user.username) || del.busy" @click="submitDelete">{{ del.busy ? '删除中…' : '确认删除' }}</button>
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
import { ref, reactive, computed, onMounted } from 'vue'

/* ================= 数据 ================= */
const users = ref([])
const q = ref('')
const listKey = ref(0)

const me = (() => {
  try { return JSON.parse(localStorage.getItem('bracelet-user') || 'null') } catch { return null }
})()
function isSelf(u) { return !!me && u.id === me.userId }
function isSelfId(id) { return !!me && id === me.userId }

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  if (!kw) return users.value
  return users.value.filter(u =>
    (u.username || '').toLowerCase().includes(kw) ||
    (u.nickname || '').toLowerCase().includes(kw))
})

/* ================= 轻提示 ================= */
const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 3800)
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

async function load() {
  const json = await req('/admin/users')
  if (json.code === 200) {
    users.value = json.data || []
    listKey.value++
  } else {
    showToast(json.msg || '加载用户列表失败')
  }
}

function fmtTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 16)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/* ================= 新增 / 编辑 ================= */
const form = reactive({ open: false, mode: 'create', id: null, username: '', nickname: '', password: '', role: 'user', busy: false })
function openCreate() {
  Object.assign(form, { open: true, mode: 'create', id: null, username: '', nickname: '', password: '', role: 'user', busy: false })
}
function openEdit(u) {
  Object.assign(form, { open: true, mode: 'edit', id: u.id, username: u.username, nickname: u.nickname, password: '', role: u.role, busy: false })
}
async function submitForm() {
  if (form.busy) return
  if (form.mode === 'create') {
    const username = form.username.trim()
    if (username.length < 3) { showToast('用户名至少 3 位'); return }
    if (form.password.length < 6) { showToast('密码至少 6 位'); return }
    form.busy = true
    const json = await req('/admin/users', {
      method: 'POST',
      body: JSON.stringify({ username, password: form.password, nickname: form.nickname.trim(), role: form.role }),
    })
    form.busy = false
    if (json.code === 200) { showToast(`用户「${username}」已创建`); form.open = false; load() }
    else showToast(json.msg || '创建失败')
  } else {
    form.busy = true
    const json = await req('/admin/users/' + form.id, {
      method: 'PUT',
      body: JSON.stringify({ nickname: form.nickname.trim(), role: form.role }),
    })
    form.busy = false
    if (json.code === 200) { showToast('用户信息已更新'); form.open = false; load() }
    else showToast(json.msg || '保存失败')
  }
}

/* ================= 重置密码 ================= */
const pw = reactive({ open: false, user: null, p1: '', p2: '', busy: false })
function openPassword(u) {
  Object.assign(pw, { open: true, user: u, p1: '', p2: '', busy: false })
}
async function submitPassword() {
  if (pw.busy) return
  if (pw.p1.length < 6) { showToast('密码至少 6 位'); return }
  if (pw.p1 !== pw.p2) { showToast('两次输入的密码不一致'); return }
  pw.busy = true
  const json = await req(`/admin/users/${pw.user.id}/password`, {
    method: 'PUT',
    body: JSON.stringify({ password: pw.p1 }),
  })
  pw.busy = false
  if (json.code === 200) { showToast(`「${pw.user.username}」的密码已重置`); pw.open = false }
  else showToast(json.msg || '重置失败')
}

/* ================= 删除（二次认证） ================= */
const del = reactive({ open: false, user: null, confirm: '', busy: false })
function openDelete(u) {
  Object.assign(del, { open: true, user: u, confirm: '', busy: false })
}
function closeDelete() {
  del.open = false
  del.confirm = ''
}
async function submitDelete() {
  if (del.busy || del.confirm !== del.user.username) return
  del.busy = true
  const json = await req('/admin/users/' + del.user.id, { method: 'DELETE' })
  del.busy = false
  if (json.code === 200) {
    showToast(`用户「${del.user.username}」已删除`)
    closeDelete()
    load()
  } else {
    showToast(json.msg || '删除失败')
  }
}

onMounted(load)
</script>

<style>
/* ===== 用户管理视图（us- 前缀；复用 mup-mask/mup-panel 弹窗体系） ===== */
/* 覆盖壳层 .view--on 的总览双列网格：行高自适应 + 内容横跨全部列，占满面板 */
.view--on[data-view="users"] { grid-template-rows: auto; }
.us-main { grid-column: 1 / -1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.us-toolbar { display: flex; align-items: center; gap: 12px; }
.us-title { display: flex; flex-direction: column; gap: 2px; }
.us-title b { font-size: 17px; font-weight: 800; color: var(--ink); }
.us-title span { font-size: 12px; color: var(--dim); }
.us-search {
  margin-left: auto; display: flex; align-items: center; gap: 8px;
  border: 1px solid var(--line); border-radius: 12px; padding: 8px 12px;
  background: var(--soft); color: var(--dim);
}
.us-search:focus-within { border-color: var(--blue); background: var(--card-solid); }
.us-search svg { width: 15px; height: 15px; flex-shrink: 0; }
.us-search input { border: 0; outline: 0; background: transparent; font: inherit; font-size: 13px; color: var(--ink); width: 150px; }
.us-add {
  display: flex; align-items: center; gap: 6px;
  border: 0; border-radius: 12px; padding: 9px 16px;
  background: var(--blue); color: #fff; font: inherit; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: filter .2s;
}
.us-add svg { width: 15px; height: 15px; }
.us-add:hover { filter: brightness(1.08); }

.us-table { display: flex; flex-direction: column; border: 1px solid var(--line); border-radius: 16px; background: var(--card-solid); overflow: hidden; }
.us-thead, .us-row {
  display: grid; grid-template-columns: 56px 1.25fr 1fr 86px 132px 176px;
  align-items: center; gap: 8px; padding: 11px 16px;
}
.us-thead { background: var(--soft); font-size: 11.5px; font-weight: 700; color: var(--dim); letter-spacing: .04em; }
.us-row { border-top: 1px solid var(--line); font-size: 13px; color: var(--ink); transition: background .15s; }
.us-row:hover { background: var(--soft); }
.us-table.anim .us-row { animation: usRowIn .45s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i, 0) * 26ms); }
@keyframes usRowIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }

.us-id { color: var(--dim); font-size: 12px; }
.us-name { font-weight: 700; display: flex; align-items: center; gap: 6px; min-width: 0; }
.us-me {
  font-style: normal; font-size: 10px; padding: 1px 6px; border-radius: 6px;
  background: var(--blue); color: #fff; flex-shrink: 0;
}
.us-nick { color: var(--dim); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.us-badge {
  font-style: normal; font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 999px;
  background: var(--soft); color: var(--dim); border: 1px solid var(--line);
}
.us-badge--admin { background: rgba(196,150,74,.14); color: #8B6F4E; border-color: rgba(196,150,74,.35); }
.us-time { color: var(--dim); font-size: 12px; }
.us-op-col { text-align: right; }
.us-ops { display: flex; justify-content: flex-end; gap: 6px; }
.us-ops button {
  border: 1px solid var(--line); background: var(--card-solid); color: var(--ink);
  border-radius: 9px; padding: 5px 11px; font: inherit; font-size: 12px; cursor: pointer;
  transition: all .15s;
}
.us-ops button:hover { border-color: var(--blue); color: var(--blue); }
.us-ops .us-del { color: #c2543e; }
.us-ops .us-del:hover { border-color: #c2543e; color: #c2543e; background: rgba(194,84,62,.08); }
.us-empty { padding: 56px 0; text-align: center; color: var(--dim); font-size: 13px; }

/* 删除二次认证 */
.us-panel--danger { border-color: rgba(194,84,62,.4); }
.us-danger-title { color: #c2543e !important; }
.us-danger-box {
  font-size: 12.5px; line-height: 1.9; color: var(--dim);
  background: rgba(194,84,62,.07); border: 1px solid rgba(194,84,62,.22);
  border-radius: 12px; padding: 12px 14px;
}
.us-danger-box b { color: #c2543e; }
.us-confirm-input {
  width: 100%; box-sizing: border-box;
  border: 1.5px solid rgba(194,84,62,.45); border-radius: 10px; outline: 0;
  background: var(--page-bg); color: var(--ink);
  font: inherit; font-size: 14px; padding: 10px 14px;
}
.us-confirm-input:focus { border-color: #c2543e; }
.us-btn-danger { background: #c2543e !important; border-color: transparent !important; color: #fff !important; font-weight: 700; }
.us-btn-danger:disabled { opacity: .4; cursor: not-allowed; }

/* 重置密码提示 */
.us-pw-hint { font-size: 12.5px; line-height: 1.7; color: var(--dim); margin: 0; }
.us-pw-hint b { color: var(--ink); }

/* 轻提示 */
.us-toast {
  position: fixed; left: 50%; bottom: 34px; transform: translateX(-50%);
  background: var(--ink); color: var(--card-solid);
  padding: 10px 20px; border-radius: 12px; font-size: 13px; font-weight: 600;
  box-shadow: 0 14px 34px rgba(10,14,24,.3); z-index: 99; max-width: 78vw;
}
.us-toast-enter-active, .us-toast-leave-active { transition: opacity .25s ease, transform .25s ease; }
.us-toast-enter-from, .us-toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(8px); }

@media (max-width: 1020px) {
  .view--on[data-view="users"] { grid-template-columns: 1fr; }
  .us-thead, .us-row { grid-template-columns: 48px 1.2fr 1fr 80px 120px 150px; padding: 10px 12px; }
}
</style>
