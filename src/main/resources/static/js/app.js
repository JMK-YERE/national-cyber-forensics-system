// =====================================================
// CLOCK — Live date/time
// =====================================================
function updateClock() {
    const now = new Date();

    const days = ['Jumapili', 'Jumatatu', 'Jumanne', 'Jumatano', 'Alhamisi', 'Ijumaa', 'Jumamosi'];
    const months = ['Jan', 'Feb', 'Mac', 'Apr', 'Mei', 'Jun', 'Jul', 'Ago', 'Sep', 'Okt', 'Nov', 'Des'];

    const dateStr = days[now.getDay()] + ', ' + now.getDate() + ' ' + months[now.getMonth()] + ' ' + now.getFullYear();

    const pad = n => String(n).padStart(2, '0');
    const timeStr = pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());

    const dateEl = document.getElementById('clockDate');
    const timeEl = document.getElementById('clockTime');
    if (dateEl) dateEl.textContent = dateStr;
    if (timeEl) timeEl.textContent = timeStr;
}

// =====================================================
// SIDEBAR — Mobile toggle
// =====================================================
function toggleSidebar() {
    const sidebar = document.getElementById('appSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) sidebar.classList.toggle('show');
    if (overlay) overlay.classList.toggle('show');
}

// =====================================================
// SUBMENU — Expand/collapse
// =====================================================
function toggleSubmenu(id, btn) {
    const submenu = document.getElementById(id);
    if (submenu) submenu.classList.toggle('show');
    if (btn) btn.classList.toggle('expanded');
}

// =====================================================
// PROFILE — Dropdown menu
// =====================================================
function toggleProfileMenu() {
    const dropdown = document.getElementById('profileDropdown');
    if (dropdown) dropdown.classList.toggle('show');
}

// Close dropdown when clicking outside
document.addEventListener('click', function(e) {
    const profileBtn = document.querySelector('.profile-btn');
    const dropdown = document.getElementById('profileDropdown');
    if (dropdown && profileBtn && !profileBtn.contains(e.target) && !dropdown.contains(e.target)) {
        dropdown.classList.remove('show');
    }
});

// =====================================================
// INIT
// =====================================================
document.addEventListener('DOMContentLoaded', function() {
    updateClock();
    setInterval(updateClock, 1000);
});
