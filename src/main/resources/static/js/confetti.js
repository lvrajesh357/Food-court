/* ==========================================================================
   Colour-paper confetti celebration.
   Usage:  window.celebrate();            // default burst
           window.celebrate({ duration: 4000 });
   Draws on its own full-screen canvas (pointer-events: none) so it never
   blocks clicks, and removes itself when the animation finishes.
   ========================================================================== */
(function () {
  var COLORS = ['#ff4d6d', '#ff9a44', '#ffd23f', '#3ddc97', '#4cc9f0', '#7b6dff', '#ff6fd8', '#ffffff'];

  function rand(min, max) { return Math.random() * (max - min) + min; }

  function celebrate(opts) {
    opts = opts || {};
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    var duration = opts.duration || 3800;
    var canvas = document.createElement('canvas');
    canvas.style.cssText =
      'position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:99999;';
    document.body.appendChild(canvas);
    var ctx = canvas.getContext('2d');
    var dpr = window.devicePixelRatio || 1;
    var W, H;

    function resize() {
      W = window.innerWidth; H = window.innerHeight;
      canvas.width = W * dpr; canvas.height = H * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }
    resize();
    window.addEventListener('resize', resize);

    var pieces = [];

    function piece(x, y, vx, vy) {
      return {
        x: x, y: y, vx: vx, vy: vy,
        w: rand(7, 14), h: rand(4, 8),
        rot: rand(0, Math.PI * 2), vr: rand(-0.3, 0.3),
        tilt: rand(0, Math.PI * 2), vt: rand(0.08, 0.25), // gives the paper "flutter"
        color: COLORS[Math.floor(Math.random() * COLORS.length)],
        round: Math.random() < 0.2
      };
    }

    // Two party-poppers firing from the bottom corners...
    var count = W < 600 ? 55 : 90;
    for (var i = 0; i < count; i++) {
      pieces.push(piece(0, H, rand(6, 16) * (W < 600 ? 0.7 : 1), -rand(10, 24) * (H < 700 ? 0.8 : 1)));
      pieces.push(piece(W, H, -rand(6, 16) * (W < 600 ? 0.7 : 1), -rand(10, 24) * (H < 700 ? 0.8 : 1)));
    }
    // ...plus a rain of paper from the top, slightly delayed.
    var rainTimer = setTimeout(function () {
      for (var j = 0; j < count; j++) {
        pieces.push(piece(rand(0, W), rand(-H * 0.4, -10), rand(-1.5, 1.5), rand(1, 4)));
      }
    }, 350);

    var start = null, last = null;

    function frame(ts) {
      if (start === null) { start = ts; last = ts; }
      // Clamp the time step so the animation resumes smoothly after an alert()
      // or a background tab freezes requestAnimationFrame.
      var dt = Math.min((ts - last) / 16.67, 2);
      last = ts;
      var elapsed = ts - start;

      ctx.clearRect(0, 0, W, H);
      var alive = 0;
      for (var k = 0; k < pieces.length; k++) {
        var p = pieces[k];
        p.vx *= Math.pow(0.985, dt);       // air drag
        p.vy = p.vy * Math.pow(0.985, dt) + 0.32 * dt; // gravity
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.rot += p.vr * dt;
        p.tilt += p.vt * dt;

        if (p.y > H + 40) continue;
        alive++;

        var fade = elapsed > duration - 800 ? Math.max(0, (duration - elapsed) / 800) : 1;
        ctx.save();
        ctx.globalAlpha = fade;
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rot);
        ctx.scale(1, Math.cos(p.tilt)); // flip like real paper
        ctx.fillStyle = p.color;
        if (p.round) {
          ctx.beginPath(); ctx.arc(0, 0, p.h * 0.6, 0, Math.PI * 2); ctx.fill();
        } else {
          ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
        }
        ctx.restore();
      }

      if (elapsed < duration && alive > 0) {
        requestAnimationFrame(frame);
      } else {
        clearTimeout(rainTimer);
        window.removeEventListener('resize', resize);
        canvas.remove();
      }
    }
    requestAnimationFrame(frame);
  }

  window.celebrate = celebrate;
})();
