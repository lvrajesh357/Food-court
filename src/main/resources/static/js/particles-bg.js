/* ==========================================================================
   Interactive particle background + glowing cursor ring with comet trail.
   - Particles drift gently and are pushed away / swept along when the pointer
     moves near them, then spring back to their place.
   - Faint "network" lines connect nearby particles and the cursor.
   - Works with mouse, pen and touch (pointer events). The normal system
     cursor is left visible so form fields still behave as expected.
   Used on the register page; drop <script src="/js/particles-bg.js"> on any
   other page to get the same effect.
   ========================================================================== */
(function () {
  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  var canvas = document.createElement('canvas');
  canvas.id = 'particles-bg';
  canvas.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;z-index:0;pointer-events:none;';
  document.body.insertBefore(canvas, document.body.firstChild);
  var ctx = canvas.getContext('2d');

  // Separate overlay layer for the cursor ring + comet trail so it floats ABOVE
  // the form card, while the particles stay behind it.
  var cursorCanvas = document.createElement('canvas');
  cursorCanvas.id = 'cursor-fx';
  cursorCanvas.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;z-index:9998;pointer-events:none;';
  document.body.appendChild(cursorCanvas);
  var cctx = cursorCanvas.getContext('2d');

  var W = 0, H = 0, dpr = 1;
  var particles = [];
  var RADIUS = 170;            // pointer influence radius
  var LINK_DIST = 95;          // particle <-> particle line distance
  var TRAIL_LEN = 26;

  var pointer = { x: -9999, y: -9999, vx: 0, vy: 0, active: false };
  var ring = { x: -9999, y: -9999, r: 9, targetR: 9 };
  var trail = [];

  function rand(a, b) { return Math.random() * (b - a) + a; }

  function makeParticles() {
    var n = Math.min(150, Math.max(45, Math.round((W * H) / 9000)));
    particles = [];
    for (var i = 0; i < n; i++) {
      var roll = Math.random();
      var color = roll < 0.78 ? '255,236,205' : (roll < 0.9 ? '255,154,68' : '126,231,255');
      var hx = rand(0, W), hy = rand(0, H);
      particles.push({
        hx: hx, hy: hy, x: hx, y: hy, vx: 0, vy: 0,
        size: rand(0.8, 2.3),
        color: color,
        phase: rand(0, Math.PI * 2),
        orbit: rand(4, 16),
        speed: rand(0.0004, 0.0012),
        twinkle: rand(0.0008, 0.002)
      });
    }
  }

  function resize() {
    dpr = window.devicePixelRatio || 1;
    W = window.innerWidth; H = window.innerHeight;
    canvas.width = cursorCanvas.width = W * dpr;
    canvas.height = cursorCanvas.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    cctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    makeParticles();
    if (reduceMotion) draw(performance.now());
  }

  function onMove(e) {
    var nx = e.clientX, ny = e.clientY;
    if (pointer.active) { pointer.vx = nx - pointer.x; pointer.vy = ny - pointer.y; }
    if (!pointer.active) { ring.x = nx; ring.y = ny; }
    pointer.x = nx; pointer.y = ny; pointer.active = true;

    var t = e.target;
    var interactive = t && t.closest && t.closest('a, button, input, label, select, textarea');
    ring.targetR = interactive ? 15 : 9;
  }
  function onLeave() { pointer.active = false; trail.length = 0; ring.x = ring.y = -9999; }

  window.addEventListener('pointermove', onMove, { passive: true });
  window.addEventListener('pointerdown', onMove, { passive: true });
  window.addEventListener('pointerup', function (e) { if (e.pointerType === 'touch') onLeave(); }, { passive: true });
  document.documentElement.addEventListener('mouseleave', onLeave);
  window.addEventListener('resize', resize);

  function step(t) {
    // Ease the ring toward the real pointer -> this lag is what creates the comet tail.
    if (pointer.active) {
      ring.x += (pointer.x - ring.x) * 0.2;
      ring.y += (pointer.y - ring.y) * 0.2;
      ring.r += (ring.targetR - ring.r) * 0.2;
      trail.push({ x: ring.x, y: ring.y });
      if (trail.length > TRAIL_LEN) trail.shift();
    } else if (trail.length) {
      trail.shift();
    }

    // Decay pointer velocity so a still cursor stops "pushing" particles.
    pointer.vx *= 0.85; pointer.vy *= 0.85;

    for (var i = 0; i < particles.length; i++) {
      var p = particles[i];
      // Lazy orbit around the home position.
      var tx = p.hx + Math.cos(t * p.speed + p.phase) * p.orbit;
      var ty = p.hy + Math.sin(t * p.speed * 1.3 + p.phase) * p.orbit;

      // Spring back toward the (orbiting) home position.
      p.vx += (tx - p.x) * 0.02;
      p.vy += (ty - p.y) * 0.02;

      if (pointer.active) {
        var dx = p.x - ring.x, dy = p.y - ring.y;
        var d2 = dx * dx + dy * dy;
        if (d2 < RADIUS * RADIUS && d2 > 0.01) {
          var d = Math.sqrt(d2);
          var f = 1 - d / RADIUS;
          // push away from the cursor...
          p.vx += (dx / d) * f * 2.2;
          p.vy += (dy / d) * f * 2.2;
          // ...and get swept along by the cursor's motion.
          p.vx += pointer.vx * f * 0.12;
          p.vy += pointer.vy * f * 0.12;
        }
      }
      p.vx *= 0.9; p.vy *= 0.9;
      p.x += p.vx; p.y += p.vy;
    }
  }

  function draw(t) {
    ctx.clearRect(0, 0, W, H);

    // soft glow under the cursor (kept on the background layer)
    if (pointer.active) {
      var g = ctx.createRadialGradient(ring.x, ring.y, 0, ring.x, ring.y, RADIUS);
      g.addColorStop(0, 'rgba(255,154,68,0.10)');
      g.addColorStop(1, 'rgba(255,154,68,0)');
      ctx.fillStyle = g;
      ctx.fillRect(ring.x - RADIUS, ring.y - RADIUS, RADIUS * 2, RADIUS * 2);
    }

    // network lines between close particles
    ctx.lineWidth = 0.6;
    for (var i = 0; i < particles.length; i++) {
      var a = particles[i];
      for (var j = i + 1; j < particles.length; j++) {
        var b = particles[j];
        var dx = a.x - b.x, dy = a.y - b.y;
        var d2 = dx * dx + dy * dy;
        if (d2 < LINK_DIST * LINK_DIST) {
          ctx.strokeStyle = 'rgba(255,200,140,' + (0.13 * (1 - Math.sqrt(d2) / LINK_DIST)).toFixed(3) + ')';
          ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
        }
      }
      // line from nearby particle to the cursor
      if (pointer.active) {
        var cx = a.x - ring.x, cy = a.y - ring.y;
        var cd = Math.sqrt(cx * cx + cy * cy);
        if (cd < RADIUS) {
          ctx.strokeStyle = 'rgba(255,154,68,' + (0.35 * (1 - cd / RADIUS)).toFixed(3) + ')';
          ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(ring.x, ring.y); ctx.stroke();
        }
      }
    }

    // particles
    for (var k = 0; k < particles.length; k++) {
      var p = particles[k];
      var alpha = 0.45 + 0.45 * Math.sin(t * p.twinkle + p.phase);
      ctx.fillStyle = 'rgba(' + p.color + ',' + alpha.toFixed(3) + ')';
      ctx.shadowColor = 'rgba(' + p.color + ',0.9)';
      ctx.shadowBlur = p.size * 4;
      ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2); ctx.fill();
    }
    ctx.shadowBlur = 0;

    drawCursor();
  }

  function drawCursor() {
    cctx.clearRect(0, 0, W, H);

    // comet trail: tapered, fading
    if (trail.length > 2) {
      for (var s = 1; s < trail.length; s++) {
        var frac = s / trail.length;
        cctx.strokeStyle = 'rgba(255,154,68,' + (frac * 0.7).toFixed(3) + ')';
        cctx.lineWidth = 0.5 + frac * 4;
        cctx.lineCap = 'round';
        cctx.beginPath();
        cctx.moveTo(trail[s - 1].x, trail[s - 1].y);
        cctx.lineTo(trail[s].x, trail[s].y);
        cctx.stroke();
      }
    }

    // glowing ring + core dot
    if (pointer.active) {
      cctx.shadowColor = 'rgba(255,154,68,1)';
      cctx.shadowBlur = 20;
      cctx.strokeStyle = '#ff9a44';
      cctx.lineWidth = 2;
      cctx.beginPath(); cctx.arc(ring.x, ring.y, ring.r, 0, Math.PI * 2); cctx.stroke();
      cctx.fillStyle = 'rgba(255,111,97,0.9)';
      cctx.beginPath(); cctx.arc(ring.x, ring.y, 2.2, 0, Math.PI * 2); cctx.fill();
      cctx.shadowBlur = 0;
    }
  }

  function loop(t) {
    step(t);
    draw(t);
    requestAnimationFrame(loop);
  }

  resize();
  if (!reduceMotion) requestAnimationFrame(loop);
})();
