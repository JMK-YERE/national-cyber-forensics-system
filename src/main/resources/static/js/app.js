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
    s.setAttribute('aria-hidden',String(!open));
    if(o)o.classList.toggle('show',open);
    if(o)o.setAttribute('aria-hidden',String(!open));
    document.body.classList.toggle('sidebar-open',open);
    document.documentElement.classList.toggle('sidebar-open',open);
    const t=document.querySelector('.mobile-toggle');
    if(t)t.setAttribute('aria-expanded',String(open));
}
function closeSidebar(){
    const s=document.getElementById('appSidebar'),o=document.getElementById('sidebarOverlay');
    if(s){s.classList.remove('show');s.setAttribute('aria-hidden','true');}
    if(o){o.classList.remove('show');o.setAttribute('aria-hidden','true');}
    document.body.classList.remove('sidebar-open');
    document.documentElement.classList.remove('sidebar-open');
    const t=document.querySelector('.mobile-toggle');
    if(t)t.setAttribute('aria-expanded','false');
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

/* Close the mobile drawer for every real navigation link, even when
   another component stops bubbling or a label/icon is the clicked target. */
function handleMobileSidebarNavigation(e) {
    if (window.innerWidth > 900) return;
    const target = e.target instanceof Element ? e.target : null;
    if (!target) return;
    const link = target.closest('#appSidebar a.sidebar-item, .app-sidebar .sidebar-nav a[href]');
    if (!link) return;
    const href = link.getAttribute('href') || '';
    if (!href || href === '#' || href.startsWith('javascript:')) return;
    closeSidebar();
}
document.addEventListener('click', handleMobileSidebarNavigation, true);
document.addEventListener('touchend', function(e) {
    if (window.innerWidth > 900) return;
    const target = e.target instanceof Element ? e.target : null;
    const link = target && target.closest('#appSidebar a.sidebar-item, .app-sidebar .sidebar-nav a[href]');
    if (link) window.setTimeout(closeSidebar, 0);
}, {passive: true});
window.addEventListener('resize', function() {
    if (window.innerWidth > 900) closeSidebar();
    else forceMobileSidebarClosed();
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


/* Global mobile drawer hardening: never persist an open sidebar across navigation, resize, or bfcache restores. */
function forceMobileSidebarClosed(){
    if(window.innerWidth<=900){
        closeSidebar();
        const s=document.getElementById('appSidebar');
        const o=document.getElementById('sidebarOverlay');
        const t=document.querySelector('.mobile-toggle');
        if(s)s.setAttribute('aria-hidden','true');
        if(o)o.setAttribute('aria-hidden','true');
        if(t)t.setAttribute('aria-expanded','false');
    }
}
window.addEventListener('pageshow',forceMobileSidebarClosed);
window.addEventListener('popstate',forceMobileSidebarClosed);
document.addEventListener('visibilitychange',function(){});
document.addEventListener('DOMContentLoaded',function(){
    forceMobileSidebarClosed();
    const t=document.querySelector('.mobile-toggle');
    if(t)t.setAttribute('aria-expanded','false');
});


/* Loading overlays intentionally disabled: navigation and authentication should not block the screen. */

/* Live unread-notification badge; polling is quiet and never blocks navigation. */
(function(){
  async function refreshUnreadNotifications(){
    const link=document.querySelector('.app-sidebar a[href$="/notifications"]');
    if(!link)return;
    try{
      const response=await fetch('/notifications/unread-count',{credentials:'same-origin',headers:{'Accept':'application/json'}});
      if(!response.ok)return;
      const payload=await response.json();
      const count=Math.max(0,Number(payload.count)||0);
      let badge=link.querySelector('.notification-count');
      if(count>0&&!badge){
        badge=document.createElement('span');
        badge.className='notification-count';
        badge.setAttribute('aria-label','Unread notifications');
        link.appendChild(badge);
      }
      if(!badge)return;
      badge.textContent=count>99?'99+':String(count);
      badge.style.display=count>0?'inline-flex':'none';
      badge.title=count+' unread notification'+(count===1?'':'s');
      link.setAttribute('aria-label',count>0?'Notifications, '+count+' unread':'Notifications');
    }catch(_){/* keep the current page usable if polling is temporarily unavailable */}
  }
  document.addEventListener('DOMContentLoaded',function(){
    refreshUnreadNotifications();
    window.setInterval(refreshUnreadNotifications,30000);
  });
})();
 
/* ===== RESPONSIVE TABLES + MOBILE KEYBOARD SUPPORT ===== */
(function(){
  function prepareResponsiveTables(){
    document.querySelectorAll('.app-main table').forEach(function(table){
      if(table.closest('.app-table-wrap,.table-responsive,.table-wrap,.overflow-x-auto'))return;
      const parent=table.parentElement;
      if(!parent)return;
      const wrap=document.createElement('div');
      wrap.className='app-table-wrap';
      wrap.setAttribute('role','region');
      wrap.setAttribute('aria-label','Scrollable table');
      wrap.setAttribute('tabindex','0');
      parent.insertBefore(wrap,table);
      wrap.appendChild(table);
    });
  }
  function syncDrawerState(){
    const sidebar=document.getElementById('appSidebar');
    const overlay=document.getElementById('sidebarOverlay');
    const toggle=document.querySelector('.mobile-toggle');
    const open=!!(sidebar&&sidebar.classList.contains('show')&&window.innerWidth<=900);
    if(sidebar)sidebar.setAttribute('aria-hidden',String(!open));
    if(overlay)overlay.setAttribute('aria-hidden',String(!open));
    if(toggle)toggle.setAttribute('aria-expanded',String(open));
    document.body.classList.toggle('sidebar-open',open);
    document.documentElement.classList.toggle('sidebar-open',open);
  }
  document.addEventListener('keydown',function(event){
    if(event.key==='Escape'){
      closeSidebar();
      const dropdown=document.getElementById('profileDropdown');
      if(dropdown)dropdown.classList.remove('show');
    }
  });
  document.addEventListener('DOMContentLoaded',function(){
    prepareResponsiveTables();
    syncDrawerState();
  });
  window.addEventListener('pageshow',function(){
    prepareResponsiveTables();
    if(window.innerWidth<=900)closeSidebar();
    else syncDrawerState();
  });
})();
