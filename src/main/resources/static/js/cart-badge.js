/* ==========================================================================
   Live cart counter for the navbar.
   Shows "Cart (1 item)" / "Cart (3 items)" next to the Cart link and keeps it
   in sync whenever an item is added/removed on ANY menu page (veg, non-veg,
   starters, veg biryani, non-veg biryani). Reads the count from the server
   (/cart/count) so it always matches the session cart.
   ========================================================================== */
(function () {
  function getCartLink() {
    return document.querySelector('nav a[href$="/cart"]');
  }

  function render(count) {
    var link = getCartLink();
    if (!link) return;

    // Re-use the label if the server already rendered one (home page).
    var label = link.querySelector('.cart-count-label') || link.querySelector('span');
    if (!label) {
      label = document.createElement('span');
      link.appendChild(document.createTextNode(' '));
      link.appendChild(label);
    }
    label.className = 'cart-count-label';

    if (count > 0) {
      label.textContent = '(' + count + ' item' + (count > 1 ? 's' : '') + ')';
      label.style.display = '';
      // small "pop" so the user notices the change
      label.classList.remove('cart-count-pop');
      void label.offsetWidth; // restart the animation
      label.classList.add('cart-count-pop');
    } else {
      label.textContent = '';
      label.style.display = 'none';
    }
  }

  function refreshCartCount() {
    return fetch('/cart/count', { credentials: 'same-origin' })
      .then(function (r) { return r.json(); })
      .then(function (count) { render(parseInt(count, 10) || 0); })
      .catch(function (e) { console.error('Cart count refresh failed:', e); });
  }

  window.refreshCartCount = refreshCartCount;

  document.addEventListener('DOMContentLoaded', refreshCartCount);
  // Back/forward navigation restores pages from cache - re-sync the count.
  window.addEventListener('pageshow', function (e) { if (e.persisted) refreshCartCount(); });
})();
