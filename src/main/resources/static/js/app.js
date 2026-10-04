// CLOCK — Live
function updateClock() {
    const now = new Date();
    const days = ['Jumapili', 'Jumatatu', 'Jumanne', 'Jumatano', 'Alhamisi', 'Ijumaa', 'Jumamosi'];
    const months = ['Jan', 'Feb', 'Mac', 'Apr', 'Mei', 'Jun', 'Jul', 'Ago', 'Sep', 'Okt', 'Nov', 'Des'];
    const dateStr = days[now.getDay()] + ', ' + now.getDate() + ' ' + months[now.getMonth()] + ' ' + now.getFullYear();
    const pad = n => String(n).padStart(2, '0');
    const timeStr = pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
    const d = document.getElementById('clockDate');
    const t = document.getElementById('clockTime');
    if (d) d.textContent = dateStr;
    if (t) t.textContent = timeStr;
}

function toggleSidebar() {
    const s = document.getElementById('appSidebar');
    const o = document.getElementById('sidebarOverlay');
    if (s) s.classList.toggle('show');
    if (o) o.classList.toggle('show');
}

function toggleSubmenu(id, btn) {
    const sub = document.getElementById(id);
    if (sub) sub.classList.toggle('show');
    if (btn) btn.classList.toggle('expanded');
}

function toggleProfileMenu() {
    const d = document.getElementById('profileDropdown');
    if (d) d.classList.toggle('show');
}

document.addEventListener('click', function(e) {
    const btn = document.querySelector('.profile-btn');
    const dd = document.getElementById('profileDropdown');
    if (dd && btn && !btn.contains(e.target) && !dd.contains(e.target)) {
        dd.classList.remove('show');
    }
});

document.addEventListener('click', function(e) {
    // On phones/tablets, selecting a sidebar destination must immediately
    // close the drawer so the destination page gets the full viewport.
    if (window.innerWidth <= 900) {
        const link = e.target.closest('.app-sidebar a.sidebar-item');
        if (link) closeSidebar();
    }
});

function closeSidebar() {
    const s = document.getElementById('appSidebar');
    const o = document.getElementById('sidebarOverlay');
    if (s) s.classList.remove('show');
    if (o) o.classList.remove('show');
}

window.addEventListener('resize', function() {
    if (window.innerWidth > 900) closeSidebar();
});

function markActiveNavigation() {
    const current = window.location.pathname.replace(/\/$/, '') || '/';
    document.querySelectorAll('.app-sidebar a.sidebar-item').forEach(link => {
        try {
            const target = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '') || '/';
            const active = target === current || (target !== '/dashboard' && target.length > 1 && current.startsWith(target + '/'));
            link.classList.toggle('active', active);
            if (active) link.setAttribute('aria-current', 'page');
            else link.removeAttribute('aria-current');
        } catch (_) {}
    });
}

document.addEventListener('DOMContentLoaded', function() {
    updateClock();
    markActiveNavigation();
    setInterval(updateClock, 1000);
});
