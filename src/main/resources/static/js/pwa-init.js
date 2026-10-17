if ('serviceWorker' in navigator) {
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js').catch(function (err) {
      console.warn('Service worker registration failed:', err);
    });
  });
}

// ---- Custom "Install App" button ----
// The browser's built-in install prompt is small and easy to miss (a tiny
// icon in the address bar). This adds a visible button instead, using the
// same install flow under the hood.
(function () {
  var deferredInstallPrompt = null;

  function isRunningStandalone() {
    return window.matchMedia('(display-mode: standalone)').matches
        || window.navigator.standalone === true; // iOS Safari
  }

  function createInstallButton() {
    if (isRunningStandalone()) return; // already installed/running as an app

    var btn = document.createElement('button');
    btn.id = 'pwa-install-btn';
    btn.type = 'button';
    btn.className = 'pwa-install-btn';
    btn.textContent = '📲 Install App';
    btn.style.display = 'none'; // shown only once the browser confirms it's installable
    btn.addEventListener('click', function () {
      if (!deferredInstallPrompt) return;
      deferredInstallPrompt.prompt();
      deferredInstallPrompt.userChoice.finally(function () {
        deferredInstallPrompt = null;
        btn.style.display = 'none';
      });
    });
    document.body.appendChild(btn);
    return btn;
  }

  window.addEventListener('beforeinstallprompt', function (event) {
    event.preventDefault();
    deferredInstallPrompt = event;
    var btn = document.getElementById('pwa-install-btn') || createInstallButton();
    if (btn) btn.style.display = 'block';
  });

  window.addEventListener('appinstalled', function () {
    var btn = document.getElementById('pwa-install-btn');
    if (btn) btn.remove();
    deferredInstallPrompt = null;
  });

  document.addEventListener('DOMContentLoaded', function () {
    if (!isRunningStandalone()) createInstallButton();
  });
})();
