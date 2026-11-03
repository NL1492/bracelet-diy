<!-- 经营总览视图：统计卡 / 今日待办 / 销售趋势+LED / 热门占比 / TOP商品 / 库存预警 -->
<template>
  <div class="view view--on" data-view="overview">
    <div class="stat-row">
      <section class="stat">
        <canvas></canvas>
        <div class="stat__body">
          <span class="stat__tag">TODAY / 今日入驻</span>
          <b class="stat__name">今日入驻</b>
          <div class="stat__num" id="stUsers">—<small>人</small></div>
          <div class="stat__delta" id="dUsers">较昨日 | <i>—</i></div>
        </div>
      </section>
      <section class="stat stat--light">
        <canvas></canvas>
        <div class="stat__body">
          <span class="stat__tag">STOCK / 总体库存</span>
          <b class="stat__name">总体库存</b>
          <div class="stat__num" id="stStock">—<small>件</small></div>
          <div class="stat__delta" id="dStock">素材库实时库存</div>
        </div>
      </section>
      <section class="stat">
        <canvas></canvas>
        <div class="stat__body">
          <span class="stat__tag">DEALS / 成交数量</span>
          <b class="stat__name">成交数量</b>
          <div class="stat__num" id="stOrders">—<small>单</small></div>
          <div class="stat__delta" id="dOrders">较昨日 | <i>—</i></div>
        </div>
      </section>
      <section class="stat">
        <canvas></canvas>
        <div class="stat__body">
          <span class="stat__tag">BACKLOG / 待办订单</span>
          <b class="stat__name">待办订单</b>
          <div class="stat__num" id="stPending">—<small>单</small></div>
          <div class="stat__delta" id="dPending">待发货 + 退款待审</div>
        </div>
      </section>
    </div>
    <section class="feature-block">
      <div class="todo-head">
        <b>今日待办</b>
        <span class="todo-more">更多 ›</span>
      </div>
      <div class="todo-list" id="todoList"></div>
    </section>
    <div class="wt-card" id="customCard">
      <svg class="wt-card__stroke" aria-hidden="true">
        <path fill="none" style="stroke: var(--line); stroke-width: 1.5"></path>
      </svg>
      <div class="wt-chart-head">
        <b>销售趋势</b>
        <div class="wt-chart-head__legend">
          <span><i class="l1"></i>订单量</span>
          <span><i class="l2"></i>销售额</span>
        </div>
      </div>
      <div class="wt-card-content">
        <div class="wt-chart" id="wtChart">
          <svg>
            <line id="wtDash" x1="0" y1="0" x2="0" y2="0"></line>
            <path id="wtL2" d=""></path>
            <path id="wtL1" d=""></path>
            <circle id="wtD2" r="3.5" cx="-10" cy="-10"></circle>
            <circle id="wtD1" r="3.5" cx="-10" cy="-10"></circle>
          </svg>
          <div class="wt-chart__labels" id="wtLabels"></div>
          <div class="wt-chart__tip" id="wtTip">
            <b id="wtTipTitle"></b>
            <div class="row"><i class="d1"></i><span>订单</span><em id="wtTipS"></em></div>
            <div class="row"><i class="d2"></i><span>销售额</span><em id="wtTipV"></em></div>
          </div>
          <div class="wt-chart__pill" id="wtPill"></div>
        </div>
        <div class="wt-card__side"><canvas id="ledScreen"></canvas></div>
      </div>
    </div>

    <!-- 底部三卡：当前热门 / TOP 商品 / 库存预警 -->
    <div class="hot-row">
      <div class="hot-card">
        <div class="hot-card__head">
          <b>当前热门</b><small>近 30 日销量占比</small>
          <span class="hot-pill">30D</span>
        </div>
        <div class="hot-body">
          <div class="hot-donut">
            <svg viewBox="0 0 148 148">
              <g id="hotRing" transform="rotate(-90 74 74)"></g>
            </svg>
            <div class="hot-donut__center" id="hotCenter">
              <small>总销量</small>
              <b>1,124<i>件</i></b>
            </div>
          </div>
          <div class="hot-legend" id="hotLegend"></div>
        </div>
      </div>

      <div class="mini-card">
        <div class="mini-card__head">
          <b>TOP 商品</b>
          <span class="mini-card__more">更多 ›</span>
        </div>
        <div class="rank-list" id="rankList"></div>
      </div>

      <div class="mini-card">
        <div class="mini-card__head">
          <b>库存预警</b>
          <span class="mini-card__more">更多 ›</span>
        </div>
        <div class="stock-head"><span class="c-nm">商品</span><span class="c-qty">库存</span><span class="c-st">状态</span></div>
        <div class="stock-list" id="stockList"></div>
      </div>
    </div><!-- /hot-row -->
  </div>
</template>

<script>
import { initCatalog, getStock } from '../../materials/catalog'

/** 拉取经营总览真实统计（管理端接口，需 Bearer token） */
async function fetchStats() {
  const token = localStorage.getItem('bracelet-token')
  const res = await fetch('/api/bracelet/admin/overview/stats', {
    headers: token ? { Authorization: 'Bearer ' + token } : {},
  })
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.msg || '统计接口异常')
  return json.data
}

/** 环比文案：today vs yesterday → ↑/↓/持平 */
function deltaHtml(today, yesterday) {
  if (!yesterday) {
    return today > 0 ? '较昨日 | <i class="up">新增</i>' : '较昨日 | <i>持平</i>';
  }
  const p = Math.round((today - yesterday) / yesterday * 100);
  if (p === 0) return '较昨日 | <i>持平</i>';
  return `较昨日 | <i class="${p > 0 ? 'up' : 'down'}">${p > 0 ? '↑' : '↓'} ${Math.abs(p)}%</i>`;
}

function money(n) {
  return '¥' + Number(n || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
}

export default {
  name: 'OverviewView',
  async mounted() {
    /* 进入页面：卡片级联入场（本视图子卡按 --i 依次上浮） */
    const root = this.$el;
    Array.prototype.forEach.call(root.children, function (c, i) { c.style.setProperty('--i', i); });
    root.classList.add('view--in');
    setTimeout(function () { root.classList.remove('view--in'); }, 1200);

    /* ================= 真实统计：接口失败时保持占位「—」，绝不回退到假数字 ================= */
    const router = this.$router;
    let S = null;
    try {
      S = await fetchStats();
    } catch (e) {
      console.warn('[overview] 统计接口失败：', e.message);
    }

    /* 总体库存：素材库实时库存合计（与库存预警同一数据源 catalog.js） */
    const STOCK_TOTAL = (function () {
      const seen = new Set();
      let sum = 0;
      initCatalog().forEach(c => (c.subCategories || []).forEach(s => {
        if (s.id.endsWith('-in-use') || s.id.endsWith('-all')) return;
        (s.items || []).forEach(it => {
          if (seen.has(it.path)) return;
          seen.add(it.path);
          sum += getStock(it.name);
        });
      }));
      return sum;
    })();

    if (S) {
      document.getElementById('stUsers').innerHTML = `${S.todayUsers}<small>人</small>`;
      document.getElementById('dUsers').innerHTML = deltaHtml(S.todayUsers, S.yesterdayUsers);
      document.getElementById('stStock').innerHTML = `${STOCK_TOTAL}<small>件</small>`;
      document.getElementById('stOrders').innerHTML = `${S.todayOrders}<small>单</small>`;
      document.getElementById('dOrders').innerHTML = deltaHtml(S.todayOrders, S.yesterdayOrders);
      document.getElementById('stPending').innerHTML = `${S.pendingShip + S.pendingRefund}<small>单</small>`;
      document.getElementById('dPending').textContent = `待发货 ${S.pendingShip} · 退款待审 ${S.pendingRefund}`;
    }

    /* ================= 顶部统计卡：WebGL 流体背景（FLUX 域扭曲 FBM，迁移自 WebGL-bg/1.html） ================= */
    (function () {
      const VS = `attribute vec2 aPos; void main(){ gl_Position = vec4(aPos, 0.0, 1.0); }`;
      const FS = `
    precision highp float;
    uniform vec2  uRes;
    uniform float uTime;
    uniform vec3  uC1;
    uniform vec3  uC2;
    uniform vec3  uC3;
    uniform float uSeed;

    float hash(vec2 p) {
      p = fract(p * vec2(123.34, 456.21) + uSeed);
      p += dot(p, p + 45.32);
      return fract(p.x * p.y);
    }
    float noise(vec2 p) {
      vec2 i = floor(p), f = fract(p);
      f = f * f * (3.0 - 2.0 * f);
      float a = hash(i);
      float b = hash(i + vec2(1.0, 0.0));
      float c = hash(i + vec2(0.0, 1.0));
      float d = hash(i + vec2(1.0, 1.0));
      return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }
    float fbm(vec2 p) {
      float v = 0.0, amp = 0.55;
      mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
      for (int i = 0; i < 5; i++) {
        v += amp * noise(p);
        p = rot * p * 2.0 + 3.7;
        amp *= 0.5;
      }
      return v;
    }

    void main() {
      vec2 uv = gl_FragCoord.xy / uRes;
      vec2 p = uv * vec2(uRes.x / uRes.y, 1.0) * 1.35;
      float t = uTime;

      /* 域扭曲:两层噪声互相推挤 */
      vec2 q = vec2(fbm(p + t * vec2(0.6, 0.2)),
                    fbm(p + t * vec2(-0.4, 0.5) + 5.2));
      vec2 r = vec2(fbm(p + 2.2 * q + t * vec2(0.3, -0.4) + 1.7),
                    fbm(p + 2.2 * q + t * vec2(-0.2, 0.3) + 8.3));
      float f = fbm(p + 2.4 * r);

      /* 三色混合 */
      vec3 col = mix(uC1, uC2, smoothstep(0.15, 0.62, f));
      col = mix(col, uC3, smoothstep(0.60, 0.95, clamp(q.x * 1.3, 0.0, 1.0)));
      col += 0.14 * r.y * uC2;

      col = col * col * 0.85 + col * 0.30;
      gl_FragColor = vec4(col, 1.0);
    }`;

      /* 每卡配色 / seed：AURORA / 银白 / KLEIN / ULTRAVIOLET（取自 FLUX THEMES） */
      const CARDS = [
        { colors: [[0.08,0.88,0.58],[0.18,0.48,1.00],[0.55,0.25,0.95]], seed: 7.3 },   // 今日入驻：AURORA
        { colors: [[0.93,0.95,0.96],[0.70,0.76,0.78],[0.52,0.58,0.61]], seed: 21.7 },  // 总体库存：银白
        { colors: [[0.00,0.18,0.65],[0.13,0.25,0.85],[1.00,0.36,0.12]], seed: 43.2 },  // 成交数量：KLEIN
        { colors: [[0.48,0.24,1.00],[0.72,0.83,0.24],[0.30,0.17,0.63]], seed: 66.9 },  // 待办订单：ULTRAVIOLET
      ];

      function sh(gl, type, src) {
        const s = gl.createShader(type);
        gl.shaderSource(s, src); gl.compileShader(s);
        if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) console.error(gl.getShaderInfoLog(s));
        return s;
      }

      const cards = [];
      document.querySelectorAll('.stat').forEach((el, i) => {
        const canvas = el.querySelector('canvas');
        const gl = canvas.getContext('webgl', { antialias: true });
        if (!gl) return;   // 降级：显示 CSS 渐变底色

        const prog = gl.createProgram();
        gl.attachShader(prog, sh(gl, gl.VERTEX_SHADER, VS));
        gl.attachShader(prog, sh(gl, gl.FRAGMENT_SHADER, FS));
        gl.linkProgram(prog);
        gl.useProgram(prog);

        const aPos = gl.getAttribLocation(prog, 'aPos');
        const buf = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, buf);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
        gl.enableVertexAttribArray(aPos);
        gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

        const U = n => gl.getUniformLocation(prog, n);
        gl.uniform1f(U('uSeed'), CARDS[i].seed);
        cards.push({
          canvas, gl,
          uRes: U('uRes'), uTime: U('uTime'),
          uC1: U('uC1'), uC2: U('uC2'), uC3: U('uC3'),
          colors: CARDS[i].colors,
          flowTime: Math.random() * 100,
        });
      });

      function resize() {
        cards.forEach(c => {
          const dpr = Math.min(window.devicePixelRatio || 1, 2);
          const w = Math.round(c.canvas.clientWidth * dpr), h = Math.round(c.canvas.clientHeight * dpr);
          if (w && h && (c.canvas.width !== w || c.canvas.height !== h)) {
            c.canvas.width = w; c.canvas.height = h;
            c.gl.viewport(0, 0, w, h);
          }
        });
      }
      window.addEventListener('resize', resize);
      resize();

      let last = performance.now();
      function frame(now) {
        const dt = Math.min((now - last) / 1000, 0.1); last = now;
        cards.forEach(c => {
          c.flowTime += dt * 0.24;
          c.gl.uniform2f(c.uRes, c.canvas.width, c.canvas.height);
          c.gl.uniform1f(c.uTime, c.flowTime);
          c.gl.uniform3fv(c.uC1, c.colors[0]);
          c.gl.uniform3fv(c.uC2, c.colors[1]);
          c.gl.uniform3fv(c.uC3, c.colors[2]);
          c.gl.drawArrays(c.gl.TRIANGLES, 0, 3);
        });
        requestAnimationFrame(frame);
      }
      if (cards.length) requestAnimationFrame(frame);
    })();

    /* ================= 异形长方卡片：clip-path（紧跟统计卡下方，凹槽让位给上方 feature-block 下缘） ================= */
    (function () {
      const PANEL_GAP = 14;
      function _buildClipPath(W, H, ox, dep) {
        const REF = 170;
        const s = H / REF;   // 特征尺寸随高度缩放
        const r32 = 32 * s;  // 底部 + 左上角圆角半径
        const rSlot = Math.min(16 * s, (dep - 6) / 2);  // 凹槽过渡弧半径（防槽过浅）
        // 左侧/垂直锚点（等比缩放）
        const x32 = 32 * s;
        const y32 = 32 * s;
        const y138 = H - 32 * s;

        const p = (n) => n.toFixed(2);
        return `M ${p(x32)} 0 L ${p(ox - rSlot)} 0 A ${p(rSlot)} ${p(rSlot)} 0 0 1 ${p(ox)} ${p(rSlot)} L ${p(ox)} ${p(dep - rSlot)} A ${p(rSlot)} ${p(rSlot)} 0 0 0 ${p(ox + rSlot)} ${p(dep)} L ${p(W - rSlot)} ${p(dep)} A ${p(rSlot)} ${p(rSlot)} 0 0 1 ${p(W)} ${p(dep + rSlot)} L ${p(W)} ${p(y138)} A ${p(r32)} ${p(r32)} 0 0 1 ${p(W - r32)} ${p(H)} L ${p(x32)} ${p(H)} A ${p(r32)} ${p(r32)} 0 0 1 0 ${p(y138)} L 0 ${p(y32)} A ${p(r32)} ${p(r32)} 0 0 1 ${p(x32)} 0 Z`;
      }

      const card = document.getElementById('customCard');
      if (!card) return;
      const panelEl = card.closest('.panel');
      const fb = panelEl.querySelector('.feature-block');
      const strokeSvg = card.querySelector('.wt-card__stroke');
      const strokePath = strokeSvg.querySelector('path');
      function apply() {
        const W = card.clientWidth, H = card.clientHeight;
        if (!W || !H || !fb) return;
        const cr = card.getBoundingClientRect();
        const fr = fb.getBoundingClientRect();
        // 凹口左缘 = feature-block 左缘 - gap；槽深 = feature-block 底部 + gap（方块下缘插进槽里，四周留缝）
        const ox = Math.max(120, fr.left - cr.left - PANEL_GAP);
        const dep = fr.bottom - cr.top + PANEL_GAP;
        const d = _buildClipPath(W, H, ox, dep);
        card.style.clipPath = `path('${d}')`;
        // 同步描边：同一条路径交给覆盖 SVG
        strokeSvg.setAttribute('viewBox', `0 0 ${W} ${H}`);
        strokePath.setAttribute('d', d);
      }
      new ResizeObserver(apply).observe(card);
      if (fb) new ResizeObserver(apply).observe(fb);
      apply();
    })();

    /* ================= wt-card 左侧：BKlit 风格动态折线图（原生 SVG 改造，零依赖） ================= */
    (function () {
      const box = document.getElementById('wtChart');
      if (!box) return;
      const svg = box.querySelector('svg');
      const dash = document.getElementById('wtDash');
      const L1 = document.getElementById('wtL1'), L2 = document.getElementById('wtL2');
      const D1 = document.getElementById('wtD1'), D2 = document.getElementById('wtD2');
      const labels = document.getElementById('wtLabels');
      const tip = document.getElementById('wtTip'), tipTitle = document.getElementById('wtTipTitle');
      const tipS = document.getElementById('wtTipS'), tipV = document.getElementById('wtTipV');
      const pill = document.getElementById('wtPill');

      /* 近 7 日真实数据：线 1 = 订单量，线 2 = 销售额（接口失败时全 0 平线） */
      const TREND  = (S && S.trend && S.trend.length) ? S.trend : [];
      const SALES  = TREND.length ? TREND.map(t => Number(t.orders || 0)) : Array(7).fill(0);
      const VISITS = TREND.length ? TREND.map(t => Number(t.gmv || 0)) : Array(7).fill(0);
      const N = SALES.length;
      const START = TREND.length ? new Date(TREND[0].date + 'T00:00:00') : new Date(Date.now() - 6 * 864e5);
      const PAD = { l: 6, r: 6, t: 70, b: 24 };       // 顶部留白容纳 tooltip
      const WEEK = ['日','一','二','三','四','五','六'];
      const dateOf = i => { const d = new Date(START); d.setDate(d.getDate() + i); return d; };

      let pts1 = [], pts2 = [], W = 0, H = 0, cur = N - 1, played = false;

      /* Catmull-Rom → 三次贝塞尔平滑曲线 */
      function smoothPath(pts) {
        let d = `M ${pts[0][0].toFixed(1)} ${pts[0][1].toFixed(1)}`;
        for (let i = 0; i < pts.length - 1; i++) {
          const p0 = pts[Math.max(0, i - 1)], p1 = pts[i], p2 = pts[i + 1], p3 = pts[Math.min(pts.length - 1, i + 2)];
          d += ` C ${(p1[0]+(p2[0]-p0[0])/6).toFixed(1)} ${(p1[1]+(p2[1]-p0[1])/6).toFixed(1)} ${(p2[0]-(p3[0]-p1[0])/6).toFixed(1)} ${(p2[1]-(p3[1]-p1[1])/6).toFixed(1)} ${p2[0].toFixed(1)} ${p2[1].toFixed(1)}`;
        }
        return d;
      }
      function mapPts(arr, top, bottom) {
        const lo = Math.min(...arr), hi = Math.max(...arr), iw = W - PAD.l - PAD.r;
        const span = hi - lo;
        return arr.map((v, i) => [PAD.l + i / (N - 1) * iw, span > 0 ? bottom - (v - lo) / span * (bottom - top) : (top + bottom) / 2]);
      }
      function build() {
        W = box.clientWidth; H = box.clientHeight;
        if (!W || !H) return;
        const bot = H - PAD.b;
        pts1 = mapPts(SALES, PAD.t, bot - 26);
        pts2 = mapPts(VISITS, PAD.t + 34, bot);
        L1.setAttribute('d', smoothPath(pts1));
        L2.setAttribute('d', smoothPath(pts2));
        /* x 轴日期标签（首尾 + 中间抽样，共 4 个） */
        labels.innerHTML = '';
        [0, Math.round((N - 1) / 3), Math.round((N - 1) * 2 / 3), N - 1].forEach(i => {
          const d = dateOf(i);
          const s = document.createElement('span');
          s.style.left = (pts1[i][0] / W * 100) + '%';
          s.textContent = (d.getMonth() + 1) + '/' + d.getDate();
          labels.appendChild(s);
        });
        if (!played) { played = true; playIntro(); }
        setIdx(cur);
      }
      function setIdx(i) {
        if (!W || !H) return;
        cur = Math.max(0, Math.min(N - 1, i));
        const p1 = pts1[cur], p2 = pts2[cur], d = dateOf(cur);
        dash.setAttribute('x1', p1[0]); dash.setAttribute('x2', p1[0]);
        dash.setAttribute('y1', Math.min(p1[1], p2[1]) + 8); dash.setAttribute('y2', H - PAD.b + 6);
        D1.setAttribute('cx', p1[0]); D1.setAttribute('cy', p1[1]);
        D2.setAttribute('cx', p2[0]); D2.setAttribute('cy', p2[1]);
        tipTitle.textContent = `${d.getMonth() + 1}月${d.getDate()}日 周${WEEK[d.getDay()]}`;
        tipS.textContent = SALES[cur].toLocaleString() + ' 单';
        tipV.textContent = money(VISITS[cur]);
        tip.style.left = Math.max(80, Math.min(W - 80, p1[0])) + 'px';
        tip.style.top = Math.min(p1[1], p2[1]) - 10 + 'px';
        pill.textContent = (d.getMonth() + 1) + '/' + d.getDate();
        pill.style.left = Math.max(28, Math.min(W - 28, p1[0])) + 'px';
      }
      /* 入场：线条绘制动画 */
      function playIntro() {
        [L1, L2].forEach(p => {
          const len = p.getTotalLength();
          p.style.strokeDasharray = len;
          p.style.strokeDashoffset = len;
          p.getBoundingClientRect();
          p.style.transition = 'stroke-dashoffset 1.4s cubic-bezier(.4,0,.2,1)';
          p.style.strokeDashoffset = '0';
        });
      }
      let locked = false;   // 点击锁定：钉住当前日期，不再跟随鼠标
      const idxAt = e => {
        const r = box.getBoundingClientRect();
        return Math.round((e.clientX - r.left - PAD.l) / (W - PAD.l - PAD.r) * (N - 1));
      };
      box.addEventListener('mousemove', e => {
        if (locked) return;
        setIdx(idxAt(e));
      });
      box.addEventListener('mouseleave', () => { if (!locked) setIdx(N - 1); });
      box.addEventListener('click', e => {
        locked = !locked;
        box.classList.toggle('locked', locked);
        if (locked) setIdx(idxAt(e));   // 钉住点击处的日期
      });
      new ResizeObserver(build).observe(box);
      build();
      /* 近 7 日无成交时给出空态提示，避免两条 0 线看起来像渲染错误 */
      if (SALES.every(v => v === 0)) {
        const note = document.createElement('div');
        note.className = 'wt-chart__empty';
        note.textContent = '近 7 日暂无成交';
        box.appendChild(note);
      }
    })();

    /* ================= 当前热门：环形占比图（原生 SVG，零依赖） ================= */
    (function () {
      const ring = document.getElementById('hotRing');
      if (!ring) return;
      const donutBox = ring.closest('.hot-donut');
      const center = document.getElementById('hotCenter');
      const legend = document.getElementById('hotLegend');

      /* 近 30 日真实数据：按珠子类型统计销量占比 */
      const PALETTE = ['#A78BFA', '#F472B6', '#FBBF24', '#34D399', '#FB923C', '#60A5FA'];
      const HOT = (S && S.hotTypes && S.hotTypes.length ? S.hotTypes : [])
        .map((t, i) => ({ name: t.label, pct: t.pct, count: t.count, color: PALETTE[i % PALETTE.length] }));
      const TOTAL = HOT.reduce((s, d) => s + d.count, 0);

      const R = 54, C = 2 * Math.PI * R, SW = 17;
      const GAP = SW + 4;                    // 圆角端点各延伸 SW/2，补偿出 4px 视觉缝隙
      const NS = 'http://www.w3.org/2000/svg';
      let acc = 0;
      const segs = [];

      /* 分段圆弧：入场前 dash 长度为 0，延迟后依次展开 */
      HOT.forEach((d, i) => {
        const arc = d.pct / 100 * C;
        const dash = Math.max(2, arc - GAP);
        const c = document.createElementNS(NS, 'circle');
        c.setAttribute('cx', 74); c.setAttribute('cy', 74); c.setAttribute('r', R);
        c.setAttribute('stroke', d.color);
        c.setAttribute('stroke-linecap', 'round');
        c.setAttribute('stroke-dasharray', `0 ${C.toFixed(2)}`);
        c.setAttribute('stroke-dashoffset', (-(acc + GAP / 2)).toFixed(2));
        c.style.transition = `stroke-dasharray .9s cubic-bezier(.4,0,.2,1) ${i * 70}ms, opacity .25s, stroke-width .25s`;
        ring.appendChild(c);
        segs.push(c);
        acc += arc;
      });

      /* 图例（两列，同参考图） */
      HOT.forEach(d => {
        const it = document.createElement('div');
        it.className = 'hot-leg';
        it.innerHTML = `<div class="hot-leg__name">${d.name}</div>
          <div class="hot-leg__row"><i style="background:${d.color}"></i><b>${d.pct}%</b></div>`;
        legend.appendChild(it);
      });

      /* hover 联动：中心切换为该项，其余分段淡化 */
      function focus(i) {
        segs.forEach((c, j) => {
          c.style.opacity = (i < 0 || j === i) ? 1 : .3;
          c.style.strokeWidth = j === i ? 21 : 17;
        });
        [...legend.children].forEach((el, j) => el.classList.toggle('hot-leg--on', j === i));
        center.innerHTML = i < 0
          ? `<small>总销量</small><b>${TOTAL.toLocaleString()}<i>件</i></b>`
          : `<small>${HOT[i].name}</small><b>${HOT[i].pct}%<i>${HOT[i].count}件</i></b>`;
      }
      HOT.forEach((d, i) => {
        legend.children[i].addEventListener('mouseenter', () => focus(i));
        segs[i].addEventListener('mouseenter', () => focus(i));
      });
      donutBox.addEventListener('mouseleave', () => focus(-1));
      legend.addEventListener('mouseleave', () => focus(-1));
      focus(-1);   // 初始中心数即真实总销量（无数据时为 0）

      /* 入场：分段依次展开 */
      requestAnimationFrame(() => requestAnimationFrame(() => {
        HOT.forEach((d, i) => {
          const arc = d.pct / 100 * C;
          const dash = Math.max(2, arc - GAP);
          segs[i].setAttribute('stroke-dasharray', `${dash.toFixed(2)} ${(C - dash).toFixed(2)}`);
        });
      }));
    })();

    /* ================= TOP 商品 + 库存预警 ================= */
    (function () {
      const rankList = document.getElementById('rankList');
      const stockList = document.getElementById('stockList');
      if (!rankList || !stockList) return;

      /* TOP 商品：近 30 日真实销量 TOP5，缩略图用 bead_info 的素材图 */
      const PALETTE = ['#A78BFA', '#F472B6', '#FBBF24', '#34D399', '#FB923C', '#60A5FA'];
      const TOPS = (S && S.topBeads && S.topBeads.length ? S.topBeads : []).map((b, i) => ({
        name: b.name,
        pct: b.pct,
        count: b.count,
        color: PALETTE[i % PALETTE.length],
        img: b.imgUrl || '',
      }));
      const MAXP = TOPS.length ? Math.max(...TOPS.map(t => t.pct), 1) : 1;

      TOPS.forEach((d, i) => {
        const it = document.createElement('div');
        it.className = 'rank-item';
        it.innerHTML = `
          <span class="no">${String(i + 1).padStart(2, '0')}</span>
          <span class="thumb" style="background:linear-gradient(140deg,${d.color},${d.color}88)">${d.img ? `<img src="${d.img}" alt="" onerror="this.remove()">` : ''}</span>
          <div class="mid">
            <div class="nm">${d.name}</div>
            <div class="bar"><i style="background:${d.color}"></i></div>
          </div>
          <b class="pct">${d.pct}%</b>`;
        rankList.appendChild(it);
      });
      if (!TOPS.length) {
        rankList.innerHTML = '<div class="ov-empty">近 30 日暂无成交数据</div>';
      }
      /* 入场：进度条依次填满 */
      requestAnimationFrame(() => requestAnimationFrame(() => {
        [...rankList.querySelectorAll('.bar i')].forEach((el, i) => {
          el.style.transitionDelay = (i * 70) + 'ms';
          el.style.width = (TOPS[i].pct / MAXP * 100) + '%';
        });
      }));

      /* 库存预警：与素材库共享同一库存 store（catalog.js · localStorage 持久化），
         遍历共享目录取真实库存，升序取最紧缺的前 5 条 */
      const STATUS = { ok: ['#10B981', '正常'], warn: ['#F59E0B', '偏低'], danger: ['#EF4444', '紧缺'] };
      const seen = new Set();
      const all = [];
      initCatalog().forEach(c => {
        c.subCategories.forEach(s => {
          if (s.id.endsWith('-in-use') || s.id.endsWith('-all')) return;
          s.items.forEach(it => {
            if (seen.has(it.path)) return;
            seen.add(it.path);
            all.push({ name: it.name, img: it.path, qty: getStock(it.name) });
          });
        });
      });
      const STOCK = all
        .map(d => ({ ...d, st: d.qty <= 5 ? 'danger' : (d.qty <= 12 ? 'warn' : 'ok') }))
        .sort((a, b) => a.qty - b.qty)
        .slice(0, 5);

      STOCK.forEach(d => {
        const [c, t] = STATUS[d.st];
        const it = document.createElement('div');
        it.className = 'stock-item';
        it.innerHTML = `
          <span class="thumb" style="background:linear-gradient(140deg,#dfe3ea,#c3c9d4)"><img src="${d.img}" alt="" onerror="this.remove()"></span>
          <span class="nm">${d.name}</span>
          <span class="qty">${d.qty}</span>
          <span class="st"><b style="color:${c};background:${c}1F"><i style="background:${c}"></i>${t}</b></span>`;
        stockList.appendChild(it);
      });
    })();

    /* ================= 今日待办：彩色条消息提醒 ================= */
    (function () {
      const list = document.getElementById('todoList');
      if (!list) return;

      /* 粉 / 紫 / 蓝 / 红 四色条，同参考图配色逻辑；数字全部来自真实统计接口 */
      const stockDanger = (function () {
        const seen = new Set();
        let n = 0;
        initCatalog().forEach(c => (c.subCategories || []).forEach(s => {
          if (s.id.endsWith('-in-use') || s.id.endsWith('-all')) return;
          (s.items || []).forEach(it => {
            if (seen.has(it.path)) return;
            seen.add(it.path);
            if (getStock(it.name) <= 5) n++;
          });
        }));
        return n;
      })();
      const TODO = S ? [
        { txt: `${S.pendingShip} 个订单待发货`,      tag: '待处理',   bg: '#FFD8DF', fg: '#EC4A7D', to: '/orders' },
        { txt: `${S.pendingRefund} 个退款待审核`,    tag: '待处理',   bg: '#CDBCF9', fg: '#7C3AED', to: '/orders' },
        { txt: `${S.pendingReview} 个灵感客订待审核`, tag: '待审核',   bg: '#C9E2FC', fg: '#2E6BE6', to: '/inspirations' },
        { txt: `${stockDanger} 款素材库存紧缺`,      tag: '库存预警', bg: '#FCD9D3', fg: '#E23B2E', to: '/materials' },
      ] : [];
      const ARROW = '<svg viewBox="0 0 12 12" fill="none"><path d="M2 6h7M6.2 3.2 9 6l-2.8 2.8" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';

      /* 纸张堆叠：每张顶部错开、全部延伸到列表底部，可见色带等高 */
      const rows = [];
      TODO.forEach(d => {
        const it = document.createElement('div');
        it.className = 'todo-item';
        it.style.background = d.bg;
        it.style.cursor = 'pointer';
        it.innerHTML = `<div class="todo-in">
          <b style="color:${d.fg}">${d.txt}</b>
          <span class="tag" style="color:${d.fg}">${d.tag}</span>
          <span class="go" style="background:${d.fg}">${ARROW}</span>
        </div>`;
        it.addEventListener('click', () => router.push(d.to));
        list.appendChild(it);
        rows.push(it);
      });
      const step = rows.length ? list.clientHeight / rows.length : 0;
      rows.forEach((it, i) => {
        it.style.top = (i * step).toFixed(1) + 'px';
        it.querySelector('.todo-in').style.height = step.toFixed(1) + 'px';
      });
    })();

    /* ================= 销售趋势右侧：点阵 LED 屏（日期时钟 + 今日指标轮播） ================= */
    (function () {
      const cvs = document.getElementById('ledScreen');
      if (!cvs) return;
      const ctx = cvs.getContext('2d');

      /* 5x7 点阵字库：每字符 7 行、每行 5 位（高位在左） */
      const FONT = {
        '0':[14,17,19,21,25,17,14],'1':[4,12,4,4,4,4,14],'2':[14,17,1,2,4,8,31],'3':[31,2,4,2,1,17,14],
        '4':[2,6,10,18,31,2,2],'5':[31,16,30,1,1,17,14],'6':[6,8,16,30,17,17,14],'7':[31,1,2,4,8,8,8],
        '8':[14,17,17,14,17,17,14],'9':[14,17,17,15,1,2,12],'A':[14,17,17,31,17,17,17],'B':[30,17,17,30,17,17,30],
        'C':[14,17,16,16,16,17,14],'D':[28,18,17,17,17,18,28],'E':[31,16,16,30,16,16,31],'F':[31,16,16,30,16,16,16],
        'G':[14,17,16,23,17,17,15],'H':[17,17,17,31,17,17,17],'I':[14,4,4,4,4,4,14],'J':[7,2,2,2,2,18,12],
        'K':[17,18,20,24,20,18,17],'L':[16,16,16,16,16,16,31],'M':[17,27,21,21,17,17,17],'N':[17,25,21,19,17,17,17],
        'O':[14,17,17,17,17,17,14],'P':[30,17,17,30,16,16,16],'Q':[14,17,17,17,21,18,13],'R':[30,17,17,30,20,18,17],
        'S':[15,16,16,14,1,1,30],'T':[31,4,4,4,4,4,4],'U':[17,17,17,17,17,17,14],'V':[17,17,17,17,17,10,4],
        'W':[17,17,17,21,21,21,10],'X':[17,17,10,4,10,17,17],'Y':[17,17,17,10,4,4,4],'Z':[31,1,2,4,8,16,31],
        ':':[0,4,4,0,4,4,0],'-':[0,0,0,14,0,0,0],' ':[0,0,0,0,0,0,0]
      };

      /* 轮播指标：与顶部统计卡同源，全部真实 */
      const METRICS = [
        { label: 'DEALS TODAY', value: (S ? S.todayOrders : 0) + ' SOLD' },
        { label: 'STOCK TOTAL', value: STOCK_TOTAL + ' PCS' },
        { label: 'BACKLOG',     value: (S ? S.pendingShip + S.pendingRefund : 0) + ' ORDERS' },
        { label: 'NEW MEMBERS', value: (S ? S.todayUsers : 0) + ' JOIN' },
      ];

      const GREEN = '#A3E635', WHITE = '#DFE6EA', DIM = 'rgba(223,230,234,.5)', OFF = 'rgba(255,255,255,.07)';
      const PAD = 4, GAP = 3, CH = 7, ROWS = PAD * 2 + CH * 4 + GAP * 3;   // 文字块总点阵行数 = 45
      const MARGIN_X = 8, MAX_TEXT_COLS = 65;                              // 最宽文本 65 点 + 左右各 8 点留白
      const ROTATE_MS = 4000, DIS_MS = 450;                                // 轮播间隔 / 打散过渡时长

      const WD = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
      const MO = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

      let W = 0, H = 0, pitch = 0, cols = 0, yOff = 0, dpr = 1;
      let cur = 0, rotAt = 0, rotIdx = 0, transStart = -1e9;

      /* 底网格暗点预渲染成离屏画布，每帧 drawImage，避免几千个 arc 逐帧重画 */
      const bg = document.createElement('canvas');
      function size() {
        const box = cvs.parentElement;
        W = box.clientWidth; H = box.clientHeight;
        dpr = window.devicePixelRatio || 1;
        cvs.width = Math.max(1, Math.round(W * dpr));
        cvs.height = Math.max(1, Math.round(H * dpr));
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        /* 点距取「高度约束」与「宽度约束」较小者：文本永不溢出，整体细密 */
        pitch = Math.min(H / ROWS, W / (MAX_TEXT_COLS + MARGIN_X * 2));
        cols = Math.floor(W / pitch);
        yOff = Math.round((H / pitch - ROWS) / 2);         // 文字块在屏内垂直居中
        bg.width = cvs.width; bg.height = cvs.height;
        const c = bg.getContext('2d');
        c.setTransform(dpr, 0, 0, dpr, 0, 0);
        c.fillStyle = OFF;
        for (let y = 0; y < Math.floor(H / pitch); y++)
          for (let x = 0; x < cols; x++) {
            c.beginPath();
            c.arc((x + .5) * pitch, (y + .5) * pitch, pitch * .34, 0, 6.2832);
            c.fill();
          }
      }
      new ResizeObserver(size).observe(cvs.parentElement);
      size();

      const rowY = i => PAD + i * (CH + GAP);
      const p2 = n => (n < 10 ? '0' : '') + n;
      const hash = (x, y, s) => { const v = Math.sin(x * 127.1 + y * 311.7 + s * 74.7) * 43758.5453; return v - Math.floor(v); };

      /* 字符串 → 点阵坐标（默认整体居中，可传 startX 指定起点） */
      function textDots(str, rowTop, startX) {
        const cw = 6;                                        // 5 点字宽 + 1 点间距
        const x0 = startX !== undefined ? startX : Math.round((cols - (str.length * cw - 1)) / 2);
        const dots = [];
        let x = x0;
        for (let i = 0; i < str.length; i++) {
          const g = FONT[str[i]];
          if (g) for (let r = 0; r < CH; r++)
            for (let c = 0; c < 5; c++)
              if (g[r] & (16 >> c)) dots.push([x + c, rowTop + r]);
          x += cw;
        }
        return dots;
      }

      function dot(x, y, color) {
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc((x + .5) * pitch, (yOff + y + .5) * pitch, pitch * .34, 0, 6.2832);
        ctx.fill();
      }

      const metricDots = m => textDots(m.label, rowY(2)).map(d => [d[0], d[1], DIM])
        .concat(textDots(m.value, rowY(3)).map(d => [d[0], d[1], GREEN]));

      function frame(now) {
        requestAnimationFrame(frame);
        ctx.clearRect(0, 0, W, H);
        ctx.drawImage(bg, 0, 0, W, H);

        const d = new Date();
        const colonOn = (now % 1000) < 550;

        /* 第 1 行：星期 + 日期（绿，对应参考图顶部绿字位置） */
        const dateStr = WD[d.getDay()] + ' ' + d.getDate() + ' ' + MO[d.getMonth()];
        for (const [x, y] of textDots(dateStr, rowY(0))) dot(x, y, GREEN);

        /* 第 2 行：时间（白），冒号 1Hz 闪烁 */
        const tx0 = Math.round((cols - 29) / 2);             // "HH:MM" 总宽 = 5*6-1 = 29 点
        for (const [x, y] of textDots(p2(d.getHours()), rowY(1), tx0)) dot(x, y, WHITE);
        if (colonOn) for (const [x, y] of textDots(':', rowY(1), tx0 + 12)) dot(x, y, WHITE);
        for (const [x, y] of textDots(p2(d.getMinutes()), rowY(1), tx0 + 18)) dot(x, y, WHITE);

        /* 第 3/4 行：指标轮播，随机打散过渡（逐点按随机阈值切换） */
        if (now - rotAt >= ROTATE_MS) {
          rotAt = now; transStart = now; rotIdx++;
          cur = (cur + 1) % METRICS.length;
        }
        const p = Math.min(1, (now - transStart) / DIS_MS);
        const m = METRICS[cur], prev = METRICS[(cur + METRICS.length - 1) % METRICS.length];
        if (p >= 1) {
          for (const [x, y, c] of metricDots(m)) dot(x, y, c);
        } else {
          const B = metricDots(m), A = metricDots(prev), kb = new Set();
          for (const [x, y, c] of B) { kb.add(x * 1000 + y); if (hash(x, y, rotIdx) < p) dot(x, y, c); }
          for (const [x, y, c] of A) if (!kb.has(x * 1000 + y) && hash(x, y, rotIdx) >= p) dot(x, y, c);
        }
      }
      requestAnimationFrame(frame);
    })();
  }
}
</script>

<style>
  /* ===== 顶部统计卡：WebGL 流体背景 ===== */
  .stat-row {
    grid-column: 1; grid-row: 1; align-self: start;
    display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
  }
  .stat {
    position: relative; height: 132px;
    border-radius: 18px; overflow: hidden;
    border: 1px solid rgba(255,255,255,.16);
    box-shadow: 0 16px 32px -16px rgba(0,0,0,.4);
    background: linear-gradient(150deg, #1d3527, #101d14);   /* WebGL 不可用时的降级底色 */
  }
  .stat canvas { position: absolute; inset: 0; width: 100%; height: 100%; display: block; }
  .stat__body {
    position: relative; z-index: 1; height: 100%;
    padding: 13px 16px 12px;
    display: flex; flex-direction: column;
    color: #fff; text-shadow: 0 1px 8px rgba(0,0,0,.35);
  }
  .stat__tag { font-size: 9.5px; font-weight: 700; letter-spacing: 1.2px; opacity: .78; }
  .stat__name { font-size: 16px; font-weight: 700; margin-top: 5px; }
  .stat__num { font-size: 38px; font-weight: 800; line-height: 1.02; margin-top: 2px; letter-spacing: -.5px; }
  .stat__num small { font-size: 12px; font-weight: 600; margin-left: 5px; letter-spacing: 0; opacity: .85; }
  .stat__delta { margin-top: auto; font-size: 11px; font-weight: 600; opacity: .92; }
  .stat__delta .up   { color: #8dffc0; font-style: normal; }
  .stat__delta .down { color: #ff9d8a; font-style: normal; }
  /* 亮色卡：浅色流体上文字反转 */
  .stat--light .stat__body { color: #191b1d; text-shadow: 0 1px 6px rgba(255,255,255,.25); }
  .stat--light .stat__tag { opacity: .6; }
  .stat--light .stat__delta .up { color: #0d9663; }
  /* 右侧大圆角方块（跨两行，下缘插入异形卡凹槽） */
  .feature-block {
    position: relative; overflow: hidden;
    grid-column: 2; grid-row: 1 / 3;
    align-self: start;
    height: 198px;   /* 132 × 1.5 */
    border-radius: 20px;
    background: var(--panel-bg);
    border: 1px solid var(--line);
    box-shadow: 0 16px 32px -16px rgba(0,0,0,.16);
    z-index: 2;      /* 悬于异形卡凹槽之上 */
  }

  /* ===== 异形卡片（clip-path 右上凹缺口，等比缩放） ===== */
  .wt-card {
    grid-column: 1 / 3; grid-row: 2;   /* 宽度占满整个面板 */
    align-self: start; height: 230px;  /* 高度收窄，不占满剩余行高 */
    background: var(--panel-bg);
    border-radius: 48px;
    padding: 36px 32px;
    box-shadow: 0 8px 30px rgba(0,0,0,.04);
    position: relative; overflow: hidden;
  }
  /* 与 clip-path 同路径的 SVG 描边（border 会被 clip 裁掉，用叠加描边代替） */
  .wt-card__stroke {
    position: absolute; inset: 0; width: 100%; height: 100%;
    pointer-events: none; z-index: 3;
  }
  .wt-card-content {
    position: relative;
    z-index: 2;
    display: flex; gap: 20px; height: 100%;
  }
  /* 图表右侧：点阵 LED 屏（深底设备屏，不随主题换色） */
  .wt-card__side {
    flex: 1; min-width: 0; display: flex;
  }
  .wt-card__side canvas {
    flex: 1; height: 130px; align-self: flex-end;   /* 整体缩小 + 靠卡片下缘 */
    transform: translateY(16px);                    /* 再下移：压进卡片底部留白区 */
    border-radius: 20px;
    background: #14171B;
    box-shadow: inset 0 0 0 1px rgba(255,255,255,.06), 0 14px 26px -14px rgba(0,0,0,.5);
  }

  /* ===== wt-card 左侧：BKlit 风格动态折线图（原生 SVG 改造） ===== */
  .wt-chart {
    position: relative; width: 58%; height: 100%;
    color: var(--ink); user-select: none; cursor: crosshair;
  }
  .wt-chart svg { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; }
  .wt-chart__empty {
    position: absolute; left: 50%; top: 58%; transform: translate(-50%, -50%);
    font-size: 12px; letter-spacing: .06em; color: var(--dim); pointer-events: none;
  }
  .wt-chart svg path { fill: none; stroke-width: 2; stroke-linecap: round; }
  #wtL1 { stroke: var(--ink); opacity: .85; }
  #wtL2 { stroke: var(--ink); opacity: .26; }
  #wtDash { stroke: var(--dim); stroke-width: 1; stroke-dasharray: 3 4; }
  /* 锁定态：虚线转实线、tooltip 描边变蓝，提示当前日期已被钉住 */
  .wt-chart.locked #wtDash { stroke: var(--ink); stroke-dasharray: none; opacity: .6; }
  .wt-chart.locked .wt-chart__tip { border-color: var(--blue); box-shadow: 0 12px 28px rgba(74,124,232,.22); }
  .wt-chart.locked .wt-chart__pill { border-color: var(--blue); color: var(--blue); }
  #wtD1, #wtD2 { fill: var(--page-bg); stroke: var(--ink); stroke-width: 1.5; }
  .wt-chart__labels span {
    position: absolute; bottom: 2px; transform: translateX(-50%);
    font-size: 10px; color: var(--dim); white-space: nowrap;
  }
  .wt-chart__tip {
    position: absolute; transform: translate(-50%, -100%);
    min-width: 150px; padding: 9px 12px; border-radius: 12px;
    background: var(--card-solid); border: 1px solid var(--line);
    box-shadow: 0 12px 28px rgba(0,0,0,.14);
    font-size: 11px; font-weight: 700; color: var(--ink);
    pointer-events: none; z-index: 5; white-space: nowrap;
  }
  .wt-chart__tip .row {
    display: flex; align-items: center; gap: 6px; margin-top: 5px;
    font-size: 11px; font-weight: 400; color: var(--dim);
  }
  .wt-chart__tip .row em { margin-left: auto; font-style: normal; font-weight: 700; color: var(--ink); padding-left: 14px; }
  .wt-chart__tip .row i { width: 7px; height: 7px; border-radius: 50%; flex: none; }
  .wt-chart__tip .row i.d1 { background: var(--ink); opacity: .85; }
  .wt-chart__tip .row i.d2 { background: var(--ink); opacity: .26; }
  .wt-chart__pill {
    position: absolute; bottom: -4px; transform: translateX(-50%);
    padding: 4px 12px; border-radius: 999px;
    background: var(--card-solid); border: 1px solid var(--line);
    box-shadow: 0 6px 16px rgba(0,0,0,.12);
    font-size: 11px; font-weight: 700; color: var(--ink);
    pointer-events: none; z-index: 4; white-space: nowrap;
  }
  /* 图表头部：左上角标题 + 右上图例（不拦截鼠标，tooltip 可从其上方掠过） */
  .wt-chart-head {
    position: absolute; top: 28px; left: 32px; right: 32px; z-index: 4;
    display: flex; align-items: center; gap: 10px;
    pointer-events: none;
  }
  .wt-chart-head b { font-size: 15px; font-weight: 800; color: var(--ink); }
  .wt-chart-head__legend { margin-left: auto; display: flex; gap: 14px; }
  .wt-chart-head__legend span { display: flex; align-items: center; gap: 5px; font-size: 11px; color: var(--dim); }
  .wt-chart-head__legend i { width: 7px; height: 7px; border-radius: 50%; flex: none; }
  .wt-chart-head__legend .l1 { background: var(--ink); opacity: .85; }
  .wt-chart-head__legend .l2 { background: var(--ink); opacity: .26; }

  /* ===== wt-card 下方：底部三卡（当前热门 / TOP 商品 / 库存预警） ===== */
  .hot-row { grid-column: 1 / 3; grid-row: 3; display: flex; gap: 14px; }
  .hot-card {
    width: 400px; flex: none;   /* 适当收窄，不占满左列 */
    height: 210px;
    background: var(--panel-bg);
    border: 1px solid var(--line);
    border-radius: 20px;
    padding: 16px 22px;
    display: flex; flex-direction: column;
  }
  .hot-card__head { display: flex; align-items: center; }
  .hot-card__head b { font-size: 15px; font-weight: 800; color: var(--ink); }
  .hot-card__head small { margin-left: 10px; font-size: 10.5px; color: var(--dim); }
  .hot-pill {
    margin-left: auto; padding: 5px 15px; border-radius: 999px;
    background: var(--soft); border: 1px solid var(--line);
    font-size: 11.5px; font-weight: 700; color: var(--ink);
  }
  .hot-body { flex: 1; display: flex; align-items: center; gap: 28px; min-height: 0; }
  .hot-donut { position: relative; width: 148px; height: 148px; flex: none; }
  .hot-donut svg { width: 100%; height: 100%; display: block; }
  .hot-donut circle {
    fill: none; stroke-width: 17; cursor: pointer;
    transition: opacity .25s, stroke-width .25s;
  }
  .hot-donut__center {
    position: absolute; inset: 0; display: grid; place-content: center;
    text-align: center; pointer-events: none;
  }
  .hot-donut__center small { font-size: 10px; color: var(--dim); }
  .hot-donut__center b { font-size: 19px; font-weight: 800; color: var(--ink); line-height: 1.3; }
  .hot-donut__center b i { font-style: normal; font-size: 11px; font-weight: 600; color: var(--dim); margin-left: 2px; }
  .hot-legend { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 13px 24px; }
  .hot-leg { cursor: pointer; }
  .hot-leg__name { font-size: 11.5px; color: var(--dim); margin-bottom: 3px; white-space: nowrap; transition: color .2s; }
  .hot-leg__row { display: flex; align-items: center; gap: 7px; }
  .hot-leg__row i { width: 4px; height: 13px; border-radius: 2px; flex: none; }
  .hot-leg__row b { font-size: 13.5px; font-weight: 800; color: var(--ink); }
  .hot-leg--on .hot-leg__name { color: var(--ink); }

  /* --- TOP 商品 --- */
  .rank-list { flex: 1; display: flex; flex-direction: column; justify-content: space-evenly; margin-top: 2px; }
  .ov-empty { margin: auto; color: var(--dim); font-size: 12.5px; letter-spacing: .04em; }
  .rank-item { display: flex; align-items: center; gap: 9px; }
  .rank-item .no { width: 16px; font-size: 10.5px; font-weight: 700; color: var(--dim); flex: none; }
  .rank-item:first-child .no { color: #FB923C; }
  .rank-item .thumb {
    width: 24px; height: 24px; border-radius: 50%; flex: none;
    background: var(--soft); overflow: hidden;
  }
  .rank-item .thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
  .rank-item .mid { flex: 1; min-width: 0; }
  .rank-item .nm { font-size: 11px; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .rank-item .bar { height: 4px; border-radius: 2px; background: var(--soft-2); margin-top: 4px; overflow: hidden; }
  .rank-item .bar i { display: block; height: 100%; border-radius: 2px; width: 0; transition: width .9s cubic-bezier(.4,0,.2,1); }
  .rank-item .pct { width: 34px; text-align: right; font-size: 11.5px; font-weight: 800; color: var(--ink); flex: none; }

  /* --- 库存预警 --- */
  .stock-head { display: flex; align-items: center; gap: 8px; padding: 9px 0 2px; font-size: 10px; color: var(--dim); flex: none; }
  .stock-head .c-nm { flex: 1; }
  .stock-head .c-qty { width: 40px; text-align: center; }
  .stock-head .c-st { width: 56px; text-align: right; }
  .stock-list { flex: 1; display: flex; flex-direction: column; justify-content: space-evenly; }
  .stock-item { display: flex; align-items: center; gap: 9px; }
  .stock-item .thumb {
    width: 22px; height: 22px; border-radius: 50%; flex: none;
    background: var(--soft); overflow: hidden;
  }
  .stock-item .thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
  .stock-item .nm { flex: 1; font-size: 11.5px; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .stock-item .qty { width: 40px; text-align: center; font-size: 12px; font-weight: 700; color: var(--ink); flex: none; }
  .stock-item .st { width: 56px; display: flex; justify-content: flex-end; flex: none; }
  .stock-item .st b {
    display: inline-flex; align-items: center; gap: 4px;
    padding: 2.5px 8px; border-radius: 999px;
    font-size: 9.5px; font-weight: 700; white-space: nowrap;
  }
  .stock-item .st b i { width: 5px; height: 5px; border-radius: 50%; flex: none; }

  /* --- 今日待办（右上消息提醒：纸张堆叠，每张延到底部，仅顶部色带可见） --- */
  .feature-block { display: flex; flex-direction: column; padding: 12px 0 0; }
  .todo-head { display: flex; align-items: center; flex: none; padding: 0 14px; }
  .todo-head b { font-size: 14px; font-weight: 800; color: var(--ink); }
  .todo-more { margin-left: auto; font-size: 10.5px; color: var(--dim); cursor: pointer; }
  .todo-list { position: relative; flex: 1; min-height: 0; }
  .todo-item {
    position: absolute; left: 0; right: 0; bottom: 0;
    border-radius: 14px 14px 0 0;
    transition: filter .2s;
    box-shadow: 0 -5px 10px -5px rgba(0,0,0,.10);
  }
  .todo-item:first-child { box-shadow: none; }
  .todo-item:hover { filter: brightness(.965); }
  .todo-in { display: flex; align-items: center; gap: 8px; padding: 0 8px 0 14px; }
  .todo-item b { font-size: 12px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .todo-item .tag {
    padding: 3.5px 11px; border-radius: 999px; flex: none;
    background: rgba(255,255,255,.72);
    font-size: 10px; font-weight: 700; white-space: nowrap;
  }
  .todo-item .go {
    margin-left: auto; width: 24px; height: 24px; border-radius: 50%; flex: none;
    display: grid; place-items: center;
    transition: transform .2s;
  }
  .todo-item .go svg { width: 11px; height: 11px; }
  .todo-item:hover .go { transform: translateX(2px); }
</style>
