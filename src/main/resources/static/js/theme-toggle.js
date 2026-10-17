(function () {
  var STORAGE_KEY = 'foodcourt-theme';

  function updateIcon(theme) {
    var btn = document.getElementById('theme-toggle-btn');
    if (!btn) return;
    btn.textContent = theme === 'dark' ? '☀️' : '🌙';
    var label = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
    btn.setAttribute('aria-label', label);
    btn.title = label;
  }

  function applyTheme(theme) {
    if (theme === 'dark') {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
    updateIcon(theme);
  }

  function toggleTheme() {
    var isDark = document.body.classList.contains('dark-mode');
    var newTheme = isDark ? 'light' : 'dark';
    try {
      localStorage.setItem(STORAGE_KEY, newTheme);
    } catch (e) {
      // localStorage unavailable (e.g. private browsing) - theme just won't persist.
    }
    applyTheme(newTheme);
  }

  function createToggleButton() {
    var btn = document.createElement('button');
    btn.id = 'theme-toggle-btn';
    btn.type = 'button';
    btn.className = 'theme-toggle-btn';
    btn.addEventListener('click', toggleTheme);

    // Prefer placing it naturally inside an existing navbar list, if present.
    var navList = document.querySelector('nav ul, .navbar ul, header ul');
    if (navList) {
      var li = document.createElement('li');
      li.appendChild(btn);
      navList.appendChild(li);
    } else {
      // No navbar on this page (e.g. login, cart, orders) - use a consistent
      // floating button in the same corner so every page still has one.
      btn.classList.add('floating');
      document.body.appendChild(btn);
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    createToggleButton();
    var saved = 'light';
    try {
      saved = localStorage.getItem(STORAGE_KEY) || 'light';
    } catch (e) {
      // ignore
    }
    applyTheme(saved);
  });
})();
