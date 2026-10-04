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
    const s=document.getElementById('appSidebar');
    const o=document.getElementById('sidebarOverlay');
    if(!s)return;
    const open=!s.classList.contains('show');
    s.classList.toggle('show',open);
    if(o)o.classList.toggle('show',open);
    document.body.classList.toggle('sidebar-open',open);
    document.documentElement.classList.toggle('sidebar-open',open);
}
function closeSidebar(){
    const s=document.getElementById('appSidebar'),o=document.getElementById('sidebarOverlay');
    if(s)s.classList.remove('show');
    if(o)o.classList.remove('show');
    document.body.classList.remove('sidebar-open');
    document.documentElement.classList.remove('sidebar-open');
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
    // Mobile: close the drawer, then explicitly continue to the selected destination.
    // This avoids a race where CSS/overlay state changes before the browser completes navigation.
    if (window.innerWidth <= 900) {
        const link = e.target.closest('.app-sidebar a.sidebar-item');
        if (link) {
            const href = link.href;
            if (href && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey && e.button !== 1) {
                e.preventDefault();
                closeSidebar();
                window.setTimeout(function(){ window.location.assign(href); }, 40);
            } else {
                closeSidebar();
            }
        }
    }
}, true);

document.addEventListener('pointerdown', function(e) {
    if (window.innerWidth <= 900 && e.target.closest('.app-sidebar a.sidebar-item')) {
        closeSidebar();
    }
}, true);

document.addEventListener('touchstart', function(e) {
    if (window.innerWidth <= 900 && e.target.closest('.app-sidebar a.sidebar-item')) {
        closeSidebar();
    }
}, {passive:true,capture:true});

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


/* ===== GLOBAL THEME PREFERENCE ===== */
function applyTheme(){
    const dark=localStorage.getItem('cftz-theme')==='dark';
    document.documentElement.dataset.theme=dark?'dark':'light';
    document.body.classList.toggle('theme-dark',dark);
    document.querySelectorAll('.theme-btn').forEach(b=>{b.textContent=dark?'☀':'☾';b.setAttribute('aria-pressed',dark?'true':'false');});
}
function toggleTheme(){
    const next=document.documentElement.dataset.theme==='dark'?'light':'dark';
    localStorage.setItem('cftz-theme',next);
    applyTheme();
}
document.addEventListener('DOMContentLoaded',applyTheme);
applyTheme();
