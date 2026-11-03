<!-- 管理端 · 运营中枢（壳：rail / 问候条 / 面板容器 / dock / 昼夜旋钮 / 消息卡；视图经 vue-router 子路由挂载） -->
<template>
<!-- SVG 玻璃滤镜（第 2 层：动态位移贴图 + RGB 色差） -->
  <svg width="0" height="0" style="position:absolute">
    <filter id="glass-disp" color-interpolation-filters="sRGB"
            x="-12%" y="-28%" width="124%" height="156%">
      <feImage id="glass-map" x="0" y="0" width="100%" height="100%"
               preserveAspectRatio="none" result="map"></feImage>
      <feDisplacementMap in="SourceGraphic" in2="map" scale="180"
        xChannelSelector="R" yChannelSelector="B" result="dR"></feDisplacementMap>
      <feOffset in="dR" dx="-90" dy="0" result="oR"></feOffset>
      <feMerge result="mR"><feMergeNode in="SourceGraphic"></feMergeNode><feMergeNode in="oR"></feMergeNode></feMerge>
      <feColorMatrix in="mR" type="matrix"
        values="1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0" result="red"></feColorMatrix>
      <feDisplacementMap in="SourceGraphic" in2="map" scale="170"
        xChannelSelector="R" yChannelSelector="B" result="dG"></feDisplacementMap>
      <feColorMatrix in="dG" type="matrix"
        values="0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 1 0" result="green"></feColorMatrix>
      <feDisplacementMap in="SourceGraphic" in2="map" scale="160"
        xChannelSelector="R" yChannelSelector="B" result="dB"></feDisplacementMap>
      <feColorMatrix in="dB" type="matrix"
        values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0" result="blue"></feColorMatrix>
      <feOffset in="dB" dx="90" dy="0" result="oB"></feOffset>
      <feMerge result="mB"><feMergeNode in="SourceGraphic"></feMergeNode><feMergeNode in="oB"></feMergeNode></feMerge>
      <feColorMatrix in="mB" type="matrix"
        values="0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 1 0" result="blue"></feColorMatrix>
      <feBlend in="red" in2="green" mode="screen" result="rg"></feBlend>
      <feBlend in="rg" in2="blue" mode="screen" result="out"></feBlend>
      <feGaussianBlur in="out" stdDeviation="0.5"></feGaussianBlur>
    </filter>
  </svg>

  <!-- 背景 -->
  <div class="photo"></div>
  <div class="photo-fallback"></div>
  <div class="orb orb--o"></div>
  <div class="orb orb--t"></div>
  <div class="orb orb--p"></div>
  <div class="orb orb--y"></div>

  <div class="wrap">

    <!-- 问候条 -->
    <header class="hello glass">
      <b id="greetText">您好，管理员</b>
      <div class="hello__avatar"><i id="avatarInitial">管</i><span id="avatarName">管理员</span></div>
    </header>

    <nav class="rail glass">
      <div class="rail__logo"><img src="/logo/logo.png" alt="logo"></div>
      <div class="rail__sp"></div>
      <div class="rail__ic" id="sunBtn" title="昼夜切换">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"></circle><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9 17 7M7 17l-2.1 2.1"></path></svg>
      </div>
    </nav>

    <main class="panel glass">
      <!-- 路由视图：旧视图淡出下沉，新视图卡片级联入场 -->
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component"></component>
        </transition>
      </router-view>
    </main>

    <!-- 底部胶囊 -->
    <nav class="dock glass">
      <router-link class="dock__tab" active-class="dock__tab--on" :to="{ name: 'overview' }">经营总览</router-link>
      <router-link class="dock__tab" active-class="dock__tab--on" :to="{ name: 'orders' }">订单管理</router-link>
      <router-link class="dock__tab" active-class="dock__tab--on" :to="{ name: 'materials' }">素材库</router-link>
      <router-link class="dock__tab" active-class="dock__tab--on" :to="{ name: 'inspirations' }">灵感管理</router-link>
      <router-link class="dock__tab" active-class="dock__tab--on" :to="{ name: 'users' }">用户中心</router-link>
    </nav>

  </div>

  <!-- 右上角：昼夜旋钮（拖拽旋转 → 背景白转黑 / 文字黑转白） -->
  <div class="knob-widget">
    <div class="outer-ring">
      <div class="track-ring" id="brightTrack"></div>
      <div class="knob" id="brightKnob">
        <div class="indicator"></div>
      </div>
    </div>
    <div class="knob-label">BRIGHTNESS</div>
  </div>
</template>

<script>
export default {
  name: 'AdminApp',
  mounted() {
    /* 问候语 + 操作员信息：按当前时段问候，昵称/头像取自登录态（不再写死「晚上好 · 系统管理员」） */
    (function () {
      var h = new Date().getHours();
      var part = h < 6 ? '凌晨好' : h < 11 ? '上午好' : h < 13 ? '午安' : h < 18 ? '下午好' : '晚上好';
      var greet = document.getElementById('greetText');
      if (greet) greet.textContent = part + '，管理员';

      var token = localStorage.getItem('bracelet-token');
      if (!token) return;
      fetch('/api/bracelet/user/info', { headers: { Authorization: 'Bearer ' + token } })
        .then(function (r) { return r.json(); })
        .then(function (j) {
          if (!j || j.code !== 200 || !j.data) return;
          var name = j.data.nickname || j.data.username || '管理员';
          if (greet) greet.textContent = part + '，' + name;
          var ini = document.getElementById('avatarInitial');
          var nm = document.getElementById('avatarName');
          if (ini) ini.textContent = name.slice(0, 1);
          if (nm) nm.textContent = name;
        })
        .catch(function () {});
    })();

    /* SVG 玻璃能力检测：Chromium/Edge 支持 backdrop-filter: url()，Safari/Firefox 走第 1 层降级 */
    (function () {
      var el = document.createElement('div');
      el.style.cssText = 'backdrop-filter: url(#glass-disp)';
      var ok = el.style.backdropFilter && el.style.backdropFilter.indexOf('url') !== -1;
      var isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
      var isFirefox = /firefox/i.test(navigator.userAgent);
      if (ok && !isSafari && !isFirefox) document.documentElement.classList.add('glass-svg-ok');
    })();

    /* 动态位移贴图：红蓝渐变 difference 生成边缘位移场，中心灰矩形压制扭曲 */
    function genMap(w, h, r) {
      var svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + w + ' ' + h + '">'
        + '<defs>'
        + '<linearGradient id="gr" x1="100%" y1="0%" x2="0%" y2="0%">'
        + '<stop offset="0%" stop-color="#0000"/><stop offset="100%" stop-color="red"/>'
        + '</linearGradient>'
        + '<linearGradient id="gb" x1="0%" y1="0%" x2="0%" y2="100%">'
        + '<stop offset="0%" stop-color="#0000"/><stop offset="100%" stop-color="blue"/>'
        + '</linearGradient>'
        + '</defs>'
        + '<rect width="' + w + '" height="' + h + '" fill="black"/>'
        + '<rect width="' + w + '" height="' + h + '" rx="' + r + '" fill="url(#gr)"/>'
        + '<rect width="' + w + '" height="' + h + '" rx="' + r + '" fill="url(#gb)" style="mix-blend-mode:difference"/>'
        + '<rect x="' + (w * .07) + '" y="' + (h * .09) + '" width="' + (w * .86) + '" height="' + (h * .82) + '" fill="hsl(0 0% 50% / 1)" style="filter:blur(14px)"/>'
        + '</svg>';
      return 'data:image/svg+xml,' + encodeURIComponent(svg);
    }
    var feImg = document.getElementById('glass-map');
    var panel = document.querySelector('.panel');
    function updateMap() {
      var r = panel.getBoundingClientRect();
      var uri = genMap(Math.round(r.width), Math.round(r.height), 24);
      feImg.setAttribute('href', uri);
      feImg.setAttributeNS('http://www.w3.org/1999/xlink', 'href', uri);
    }
    new ResizeObserver(updateMap).observe(panel);
    updateMap();

    /* ================= 昼夜旋钮：拖拽旋转角度 → 背景白转黑 / 文字黑转白，localStorage 记忆 ================= */
    (function () {
      var knob = document.getElementById('brightKnob');
      var track = document.getElementById('brightTrack');
      var total = 19;
      var minAngle = -125, maxAngle = 125;
      var isDragging = false, lastMouseDeg = 0;

      /* 昼夜主题：[亮色 RGBA, 暗色 RGBA]，p 为暗度（0=亮度拉满/白，1=最暗/黑），逐通道插值 */
      var root = document.documentElement;
      var THEME = {
        '--page-bg':      [[255,255,255,1],   [10,10,12,1]],     // 页面背景：白 → 黑
        '--ink':          [[38,34,29,1],      [241,237,230,1]],  // 主文字：黑 → 白
        '--dim':          [[38,34,29,.55],    [241,237,230,.55]],// 次级文字
        '--ic':           [[38,34,29,.4],     [241,237,230,.4]], // 栏图标
        '--line':         [[0,0,0,.08],       [255,255,255,.10]],// 边框线
        '--glass-bg':     [[0,0,0,.10],       [255,255,255,.06]],// 玻璃底色
        '--glass-bg-svg': [[0,0,0,.08],       [255,255,255,.05]],
        '--panel-bg':     [[255,255,255,.42], [24,26,31,.48]],   // 卡片底色
        '--soft':         [[0,0,0,.06],       [255,255,255,.08]],// 浅色小底
        '--soft-2':       [[0,0,0,.08],       [255,255,255,.10]],
        '--switch-off':   [[0,0,0,.15],       [255,255,255,.16]],
        '--dash':         [[0,0,0,.18],       [255,255,255,.18]],// 环形刻度
        '--tri':          [[38,34,29,.8],     [241,237,230,.85]],// 播放三角
        '--card-solid':   [[255,255,255,.96], [26,28,32,.96]],   // 环形中心实底
        '--card-sub':     [[138,133,124,1],   [241,237,230,.5]]
      };
      function applyTheme(p) {
        for (var k in THEME) {
          var a = THEME[k][0], b = THEME[k][1];
          var r = Math.round(a[0] + (b[0] - a[0]) * p);
          var g = Math.round(a[1] + (b[1] - a[1]) * p);
          var bl = Math.round(a[2] + (b[2] - a[2]) * p);
          var al = (a[3] + (b[3] - a[3]) * p).toFixed(3);
          root.style.setProperty(k, 'rgba(' + r + ',' + g + ',' + bl + ',' + al + ')');
        }
      }

      // 生成 19 个指示灯
      var dots = [];
      for (var i = 1; i <= total; i++) {
        var d = document.createElement('div');
        d.className = 'dot-container';
        d.style.setProperty('--i', i);
        d.innerHTML = '<span></span>';
        track.appendChild(d);
        dots.push(d);
      }

      // 初始角度：亮度拉满（顺时针到底 = 白）；有记录则按暗度反推角度
      var currentAngle = maxAngle;
      var saved = parseFloat(localStorage.getItem('admin3-brightness'));
      if (!isNaN(saved)) currentAngle = maxAngle - saved * (maxAngle - minAngle);

      function update(angle) {
        currentAngle = Math.max(minAngle, Math.min(maxAngle, angle));
        knob.style.transform = 'rotate(' + currentAngle + 'deg)';
        var p = (maxAngle - currentAngle) / (maxAngle - minAngle);    // 暗度：顺时针到底 = 白
        var lit = (currentAngle - minAngle) / (maxAngle - minAngle);  // 亮度进度：顺时针灯增多
        var n = Math.round(lit * (total - 1));
        dots.forEach(function (dot, index) {
          if (n === 0) {
            dot.classList.remove('active');
          } else {
            if (index <= n) dot.classList.add('active');
            else dot.classList.remove('active');
          }
        });
        applyTheme(p);   // 角度 → 背景白转黑 / 文字黑转白
        try { localStorage.setItem('admin3-brightness', p); } catch (e) {}
      }

      function getMouseAngle(clientX, clientY) {
        var rect = knob.getBoundingClientRect();
        var cx = rect.left + rect.width / 2;
        var cy = rect.top + rect.height / 2;
        var dx = clientX - cx;
        var dy = clientY - cy;
        return Math.atan2(dy, dx) * (180 / Math.PI);
      }

      var sunGen = 0;
      knob.addEventListener('mousedown', function (e) {
        isDragging = true;
        sunGen++;                       // 手动拖拽打断日光按钮的自动旋转动画
        document.body.style.userSelect = 'none';
        lastMouseDeg = getMouseAngle(e.clientX, e.clientY);
        e.preventDefault();
      });
      window.addEventListener('mousemove', function (e) {
        if (!isDragging) return;
        var mouseDeg = getMouseAngle(e.clientX, e.clientY);
        var delta = mouseDeg - lastMouseDeg;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        currentAngle += delta;
        update(currentAngle);
        lastMouseDeg = mouseDeg;
      });
      window.addEventListener('mouseup', function () {
        isDragging = false;
        document.body.style.userSelect = '';
      });
      // 触摸兼容
      knob.addEventListener('touchstart', function (e) {
        isDragging = true;
        sunGen++;                       // 手动拖拽打断自动旋转动画
        var touch = e.touches[0];
        lastMouseDeg = getMouseAngle(touch.clientX, touch.clientY);
        e.preventDefault();
      });
      window.addEventListener('touchmove', function (e) {
        if (!isDragging) return;
        var touch = e.touches[0];
        var mouseDeg = getMouseAngle(touch.clientX, touch.clientY);
        var delta = mouseDeg - lastMouseDeg;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        currentAngle += delta;
        update(currentAngle);
        lastMouseDeg = mouseDeg;
      });
      window.addEventListener('touchend', function () { isDragging = false; });

      /* 左侧日光按钮：点击 → 旋钮自动转到最亮/最暗，背景与指示灯随旋转同步过渡 */
      var sunBtn = document.getElementById('sunBtn');
      if (sunBtn) sunBtn.addEventListener('click', function () {
        var pNow = (maxAngle - currentAngle) / (maxAngle - minAngle);   // 当前暗度
        var targetAngle = pNow < 0.5 ? minAngle : maxAngle;             // 偏亮→转到最暗 / 偏暗→转到最亮
        var gen = ++sunGen, from = currentAngle, t0 = performance.now(), DUR = 700;
        (function step(now) {
          if (gen !== sunGen) return;                                   // 被打断（再次点击/手动拖拽）则中止
          var k = Math.min(1, (now - t0) / DUR);
          k = k < .5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;     // easeInOutQuad 缓动
          update(from + (targetAngle - from) * k);
          if (k < 1) requestAnimationFrame(step);
        })(t0);
      });

      update(currentAngle);   // 恢复上一次状态（无记录则亮度拉满）
    })();
  }
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
  :root {
    --blue: #4A7CE8; --blue-deep: #3E6FE0;
    --ink: #26221d; --dim: rgba(38,34,29,.55); --ic: rgba(38,34,29,.4);
    --line: rgba(0,0,0,.08);
    --page-bg: #ffffff;
    --glass-bg: rgba(0,0,0,.10); --glass-bg-svg: rgba(0,0,0,.08);
    --panel-bg: rgba(255,255,255,.42);
    --soft: rgba(0,0,0,.06); --soft-2: rgba(0,0,0,.08);
    --switch-off: rgba(0,0,0,.15); --dash: rgba(0,0,0,.18);
    --tri: rgba(38,34,29,.8);
    --card-solid: rgba(255,255,255,.96); --card-sub: #8a857c;
  }
  html, body { height: 100%; }
  body {
    margin: 0;
    font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
    color: var(--ink);
    background: var(--page-bg);
    display: grid; place-items: center;
    overflow: hidden;
    position: relative;
  }

  /* ================= 背景层（迎合观看：亮细节 + 彩色光斑 + 珠环） ================= */
  .photo {
    position: fixed; inset: -40px;
    background: url("/底盘/大理石.png") center / cover no-repeat;
    filter: blur(18px) brightness(.78) saturate(1.15);
    transform: scale(1.08);
  }
  .photo, .orb { display: none; }
  .photo-fallback {
    position: fixed; inset: 0;
    background: var(--page-bg);
  }

  /* ================= 三层玻璃系统 ================= */
  /* 第 1 层：基础毛玻璃（全浏览器降级方案） */
  .glass {
    background: var(--glass-bg);
    backdrop-filter: blur(12px) saturate(1.8) brightness(1.16);
    -webkit-backdrop-filter: blur(12px) saturate(1.8) brightness(1.16);
    border: 1px solid var(--line);
    /* 8 层阴影：细白边 + 柔光晕 + 三层外投影 + 三层内暗角 */
    box-shadow:
      inset 0 0 2px 1px rgba(255,255,255,.55),
      inset 0 0 10px 4px rgba(255,255,255,.25),
      0 4px 16px rgba(17,17,26,.05),
      0 8px 24px rgba(17,17,26,.05),
      0 16px 56px rgba(17,17,26,.05),
      inset 0 4px 16px rgba(17,17,26,.05),
      inset 0 8px 24px rgba(17,17,26,.05),
      inset 0 16px 56px rgba(17,17,26,.05);
  }
  /* 第 2 层：SVG 动态位移贴图 + RGB 色差（Chromium / Edge） */
  html.glass-svg-ok .glass {
    background: var(--glass-bg-svg);
    backdrop-filter: url(#glass-disp) blur(10px) saturate(1.4);
    -webkit-backdrop-filter: url(#glass-disp) blur(10px) saturate(1.4);
  }

  /* ================= 居中骨架：rail 跨 hello+panel 总高 ================= */
  .wrap {
    position: relative; z-index: 3;
    width: min(1080px, 94vw);
    display: grid;
    grid-template-columns: 62px 1fr;
    gap: 16px 18px;
    align-items: stretch;
  }

  /* 问候条 */
  .hello { grid-column: 2; grid-row: 1; border-radius: 18px; padding: 15px 22px; display: flex; align-items: center; gap: 12px; }
  .hello b { font-size: 16.5px; }
  .hello__avatar { margin-left: auto; display: flex; align-items: center; gap: 9px; font-size: 12.5px; }
  .hello__avatar i {
    width: 32px; height: 32px; border-radius: 50%; font-style: normal; color: #fff;
    background: linear-gradient(140deg, #a08b6d, #6d5c44);
    display: grid; place-items: center; font-size: 12px; font-weight: 700;
  }

  /* 图标栏（跨 hello + panel 总高） */
  .rail {
    grid-column: 1; grid-row: 1 / 3;
    border-radius: 999px;
    display: flex; flex-direction: column; align-items: center;
    padding: 16px 0 18px; gap: 6px;
  }
  .rail__logo {
    position: relative; width: 38px; height: 38px;
    margin-bottom: 16px;
  }
  .rail__logo img {
    position: absolute; left: 50%; top: 50%;
    width: 300%; height: 300%; object-fit: contain;
    transform: translate(-50%, -50%);
    display: block;
  }
  .rail__ic {
    width: 38px; height: 38px; border-radius: 13px;
    display: grid; place-items: center;
    color: var(--ic); cursor: pointer; transition: all .2s;
  }
  .rail__ic:hover { color: var(--ink); background: var(--soft); }
  .rail__ic svg { width: 17px; height: 17px; }
  .rail__sp { flex: 1; }

  /* 主面板 */
  .panel {
    grid-column: 2; grid-row: 2;
    height: 640px;                 /* 面板固定尺寸：所有模块页一致，不随内容增减 */
    border-radius: 24px; padding: 20px;
    position: relative;
  }
  /* 视图容器：占满面板内容区（600px），超高内容在视图内部滚动 */
  .view {
    height: 600px; overflow-y: auto; overscroll-behavior: contain;
    scrollbar-width: thin; scrollbar-color: var(--line) transparent;
  }
  .view::-webkit-scrollbar { width: 6px; }
  .view::-webkit-scrollbar-thumb { background: var(--line); border-radius: 3px; }
  .view::-webkit-scrollbar-track { background: transparent; }
  .view--on {
    display: grid;
    grid-template-columns: 1.52fr 1fr;
    grid-template-rows: 132px auto auto;   /* 统计卡 / 异形卡 / 热门占比卡 */
    gap: 14px;
  }
  /* 卡片级联入场（每个视图挂载时给子卡编号 --i 触发） */
  .view--in > * { animation: cardIn .6s cubic-bezier(.22, 1, .36, 1) both; animation-delay: calc(var(--i, 0) * 80ms); }
  @keyframes cardIn {
    from { opacity: 0; transform: translateY(26px) scale(.985); }
  }
  /* 路由转场：旧视图淡出下沉（out-in 模式），新视图入场由卡片级联负责 */
  .page-leave-active { transition: opacity .32s ease, transform .32s ease, filter .32s ease; }
  .page-leave-to { opacity: 0; transform: translateY(16px) scale(.99); filter: blur(3px); }
  .page-enter-active { transition: opacity .18s ease; }
  .page-enter-from { opacity: 0; }

  /* 通用小卡头（总览 TOP/库存卡 + 素材库侧卡共用） */
  .mini-card {
    flex: 1; min-width: 0; height: 210px;
    background: var(--panel-bg);
    border: 1px solid var(--line);
    border-radius: 20px;
    padding: 14px 16px;
    display: flex; flex-direction: column; overflow: hidden;
  }
  .mini-card__head { display: flex; align-items: center; gap: 8px; flex: none; }
  .mini-card__head b { font-size: 13.5px; font-weight: 800; color: var(--ink); }
  .mini-card__more { margin-left: auto; font-size: 10.5px; color: var(--dim); cursor: pointer; }

  .blk {
    background: var(--panel-bg);
    border: 1px solid var(--line);
    border-radius: 18px;
    padding: 16px 18px;
    display: flex; flex-direction: column;
    position: relative; overflow: hidden;
  }
  .play-tri {
    width: 0; height: 0; opacity: .85;
    border-left: 20px solid var(--tri);
    border-top: 13px solid transparent; border-bottom: 13px solid transparent;
    margin-left: 5px;
  }
  .mini { font-size: 11px; color: var(--dim); }

  /* Usage Status */
  .usage { grid-column: 1; grid-row: 1; }
  .usage__nums { display: flex; gap: 40px; margin-top: 10px; }
  .usage__nums b { display: block; font-size: 19px; font-weight: 700; margin-top: 2px; }
  .bars { flex: 1; display: flex; align-items: flex-end; gap: 11px; margin-top: 14px; min-height: 0; }
  .bars .col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 7px; height: 100%; justify-content: flex-end; }
  .bars .bar { width: 100%; max-width: 34px; border-radius: 11px; background: var(--soft-2); position: relative; }
  .bars .bar--hot { background: linear-gradient(180deg, #5b88ec, var(--blue-deep)); box-shadow: 0 10px 26px rgba(74,124,232,.5); }
  .bars .tag {
    position: absolute; top: -24px; left: 50%; transform: translateX(-50%);
    background: var(--blue); color: #fff; font-size: 10px; font-weight: 700;
    padding: 2px 9px; border-radius: 999px; white-space: nowrap;
  }
  .bars .x { font-size: 10.5px; color: var(--dim); }
  .play-btn {
    position: absolute; left: 50%; top: 55%; transform: translate(-50%, -50%);
    width: 64px; height: 64px; border-radius: 50%;
    background: rgba(0,0,0,.10);
    backdrop-filter: blur(12px) saturate(1.8) brightness(1.16);
    -webkit-backdrop-filter: blur(12px) saturate(1.8) brightness(1.16);
    box-shadow:
      inset 0 0 2px 1px rgba(255,255,255,.34),
      inset 0 0 10px 4px rgba(255,255,255,.13),
      0 10px 30px rgba(0,0,0,.18);
    display: grid; place-items: center;
    pointer-events: none;
  }

  /* 天气卡 → 用户入驻 */
  .weather { grid-column: 2; grid-row: 1; flex-direction: row; align-items: center; gap: 14px; }
  .weather__ic {
    position: absolute; right: 14px; top: 12px;
    width: 74px; height: 74px; border-radius: 50%;
    background: radial-gradient(circle at 35% 32%, #f6d76c, #e9b83e 62%);
    box-shadow: 0 10px 26px rgba(233,184,62,.4);
  }
  .weather__ic::after {
    content: ""; position: absolute; left: -14px; bottom: 2px;
    width: 58px; height: 30px; border-radius: 999px;
    background: linear-gradient(180deg, #f4f2ee, #c9c5bd);
  }
  .wtemp { font-size: 44px; font-weight: 800; line-height: 1; }
  .wtemp small { font-size: 17px; color: var(--dim); font-weight: 600; }
  .wmeta { display: flex; gap: 16px; margin-top: 10px; font-size: 11.5px; color: var(--dim); }

  /* TV / Router → 热卖 / 告警 */
  .duo { grid-column: 2; grid-row: 2; display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 0; background: none; border: none; backdrop-filter: none; -webkit-backdrop-filter: none; }
  .duo .blk { padding: 13px 15px; }
  .switch {
    width: 34px; height: 19px; border-radius: 999px; position: relative;
    background: var(--blue); margin-left: auto; flex: none;
  }
  .switch::after {
    content: ""; position: absolute; top: 2.5px; right: 3px;
    width: 14px; height: 14px; border-radius: 50%; background: #fff;
  }
  .switch--off { background: var(--switch-off); }
  .switch--off::after { right: auto; left: 3px; }
  .duo__dev {
    margin: 14px auto 6px;
    width: 52px; height: 52px; border-radius: 16px;
    display: grid; place-items: center; font-size: 22px;
    background: var(--soft);
  }
  .duo .blk--on { background: linear-gradient(165deg, rgba(74,124,232,.88), rgba(52,92,199,.85)); border-color: rgba(255,255,255,.2); }
  .duo .blk--on .duo__dev { background: rgba(255,255,255,.18); }
  .duo .name { text-align: center; font-size: 13px; font-weight: 700; }
  .duo .sub { text-align: center; font-size: 10.5px; color: var(--dim); margin-top: 3px; }
  .duo .blk--on .sub { color: rgba(255,255,255,.72); }

  /* 温控 → 年度目标 */
  .thermo { grid-column: 1; grid-row: 2; flex-direction: row; align-items: center; gap: 18px; }
  .thermo__head { display: flex; align-items: center; gap: 10px; position: absolute; top: 14px; left: 16px; right: 16px; }
  .thermo__ic {
    width: 34px; height: 34px; border-radius: 11px;
    background: linear-gradient(140deg, #e09a4a, #cf7c2e);
    display: grid; place-items: center; font-size: 15px;
  }
  .thermo__ring { position: relative; width: 118px; height: 118px; margin: 14px auto 0; flex: none; }
  .thermo__ring .dash {
    position: absolute; inset: -10px; border-radius: 50%;
    background: repeating-conic-gradient(var(--dash) 0 1.6deg, transparent 1.6deg 6deg);
    -webkit-mask: radial-gradient(circle, transparent 56px, #000 57px);
            mask: radial-gradient(circle, transparent 56px, #000 57px);
  }
  .thermo__c {
    position: absolute; inset: 0; border-radius: 50%;
    background: var(--card-solid); color: var(--ink);
    display: grid; place-items: center;
    box-shadow: 0 14px 34px rgba(0,0,0,.4);
  }
  .thermo__c b { font-size: 23px; font-weight: 800; }
  .thermo__c small { display: block; text-align: center; font-size: 9.5px; color: var(--card-sub); }
  .thermo__pm { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--dim); }
  .thermo__pm .btn {
    width: 26px; height: 26px; border-radius: 50%;
    background: var(--blue); color: #fff;
    display: grid; place-items: center; font-size: 13px;
  }
  .thermo__side { margin-left: auto; display: flex; flex-direction: column; gap: 14px; }
  .thermo__side .chip { display: flex; align-items: center; gap: 9px; }
  .thermo__side .ic {
    width: 28px; height: 28px; border-radius: 10px;
    background: var(--blue); display: grid; place-items: center; font-size: 12px;
  }
  .thermo__side .ic--dn { background: var(--soft-2); }
  .thermo__side b { display: block; font-size: 13.5px; }
  .thermo__side span { font-size: 10px; color: var(--dim); }

  /* Camera → 实时订单 */
  .cam { grid-column: 2; grid-row: 2; }
  .cam__top { display: flex; align-items: center; gap: 10px; }
  .cam__live {
    display: inline-flex; align-items: center; gap: 5px;
    font-size: 10px; font-weight: 700; color: #fff;
    background: #c0392b; border-radius: 6px; padding: 3px 8px;
  }
  .cam__live::before { content: ""; width: 5px; height: 5px; border-radius: 50%; background: #fff; }
  .cam__res { position: absolute; right: 12px; top: 14px; font-size: 10px; color: var(--dim); border: 1px solid var(--line); border-radius: 6px; padding: 3px 8px; }
  .cam__view {
    flex: 1; margin-top: 12px; border-radius: 14px; min-height: 90px;
    color: #fff;
    background:
      radial-gradient(220px 130px at 22% 88%, rgba(224,166,74,.4), transparent 60%),
      radial-gradient(200px 150px at 80% 30%, rgba(120,140,150,.3), transparent 60%),
      linear-gradient(160deg, rgba(58,50,48,.75), rgba(36,31,30,.75) 70%);
    position: relative;
  }
  .cam__view::after {
    content: ""; position: absolute; left: 18%; right: 18%; bottom: 12%;
    height: 34%; border-radius: 10px;
    background: linear-gradient(180deg, rgba(240,238,234,.18), rgba(240,238,234,.06));
  }

  /* 底部胶囊 */
  .dock {
    grid-column: 2; grid-row: 3;
    border-radius: 999px; padding: 7px 9px;
    display: flex; align-items: center; gap: 4px;
    justify-self: center;
    min-width: 420px;
  }
  .dock__tab {
    padding: 9px 22px; border-radius: 999px;
    font-size: 12.5px; color: var(--dim); cursor: pointer; transition: all .2s;
    text-decoration: none;
  }
  .dock__tab:hover { color: var(--ink); }
  .dock__tab--on { background: var(--blue); color: #fff; font-weight: 700; box-shadow: 0 8px 20px rgba(74,124,232,.45); }

  /* ================= 右上角：亮度旋钮（拖拽旋转 → 亮 ↔ 暗） ================= */
  .knob-widget {
    position: fixed; top: 14px; right: 14px; z-index: 60;
    width: 150px; height: 150px;
    display: grid; place-items: center;
  }
  .knob-widget .outer-ring {
    position: relative;
    width: 150px; height: 150px;
    border-radius: 50%;
    background-image: linear-gradient(90deg, #DFE4E1, #BFC9CA);
    border: 1.5px solid #c6ccca;
    box-shadow: 0 0 2px #9fa5a3, 0 14px 34px rgba(0,0,0,.18);
    display: flex; justify-content: center; align-items: center;
  }
  .knob-widget .track-ring {
    position: absolute;
    width: 130px; height: 130px;
    border-radius: 50%;
    background-image: linear-gradient(90deg, #eaefed, #95a7ae);
    border: 1.5px solid #c6ccca;
    box-shadow:
      inset 1px 1px 2px rgba(255,255,255,.6),
      inset -1px -1px 1px rgba(170,184,183,.6);
  }
  .knob-widget .dot-container {
    position: absolute;
    top: 50%; left: 50%;
    width: 100%; height: 100%;
    transform: translateX(-50%) translateY(-50%) rotate(calc(-125deg + 12.6deg * var(--i)));
  }
  .knob-widget .dot-container span {
    position: absolute;
    top: 7px; left: 50%;
    width: 5px; height: 5px;
    border-radius: 50%;
    transform: translateX(-50%);
    background-color: #5d7178;
    box-shadow: inset 1px 1px 2px rgba(0,0,0,.4);
    transition: background-color .15s ease, box-shadow .15s ease;
  }
  .knob-widget .dot-container.active span {
    background-color: #ffffff;
    box-shadow: 0 0 6px #ffffff, 0 0 10px rgba(255,255,255,.8);
  }
  .knob-widget .knob {
    position: absolute;
    width: 88px; height: 88px;
    border-radius: 50%;
    background-image: linear-gradient(90deg, #DAE1DE, #a4b6c1);
    border: 1.5px solid #c6ccca;
    box-shadow:
      inset 1px 1px 2px rgba(255,255,255,.5),
      inset -1px -1px 1px rgba(156,170,169,.6),
      -11px 0 14px rgba(255,255,255,.4),
      4px 0 8px rgba(2,29,37,.1),
      22px 0 28px rgba(2,29,37,.6),
      inset -2px 0 2px rgba(1,22,25,.4),
      inset 2px 0 2px rgba(255,255,255,.4);
    cursor: grab;
    display: flex; justify-content: center; align-items: center;
    user-select: none;
    touch-action: none;
  }
  .knob-widget .knob:active { cursor: grabbing; }
  .knob-widget .indicator {
    position: absolute;
    top: 8px;
    width: 4px; height: 11px;
    background-color: #ffffff;
    border-radius: 4px;
    box-shadow: 0 0 8px rgba(255,255,255,.8);
  }
  .knob-widget .knob-label {
    position: absolute; bottom: -18px; left: 0; right: 0;
    text-align: center; font-size: 10px; color: var(--dim); letter-spacing: 2px;
  }

  @media (max-width: 1020px) {
    body { overflow: auto; }
    .wrap { grid-template-columns: 1fr; }
    .rail { grid-row: auto; grid-column: 1; flex-direction: row; padding: 8px 14px; }
    .rail__sp { display: none; }
    .panel { grid-column: 1; grid-row: auto; height: auto; min-height: 0; }
    .view { height: auto; overflow: visible; }
    .view--on { grid-template-columns: 1fr; grid-template-rows: auto; }
    .view > * { grid-column: auto !important; grid-row: auto !important; min-height: 200px; }
    .hello { grid-column: 1; grid-row: auto; }
    .dock { grid-column: 1; grid-row: auto; }
  }
</style>
