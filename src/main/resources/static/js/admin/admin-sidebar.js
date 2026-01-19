/**
 * admin/layout/sidebar.html - 관리자 사이드바
 */

// 페이지 로드 시 저장된 상태 복원 (데스크탑)
document.addEventListener('DOMContentLoaded', function() {
    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (isCollapsed && window.innerWidth > 768) {
        document.getElementById('sidebar').classList.add('collapsed');
        document.querySelector('.main-content')?.classList.add('expanded');
    }
});

// 데스크탑 사이드바 토글
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.querySelector('.main-content');

    sidebar.classList.toggle('collapsed');
    mainContent?.classList.toggle('expanded');

    const isCollapsed = sidebar.classList.contains('collapsed');
    localStorage.setItem('sidebarCollapsed', isCollapsed);
}

// 모바일 메뉴 열기
function toggleMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    const menuBtn = document.getElementById('mobileMenuBtn');

    sidebar.classList.add('mobile-open');
    overlay.classList.add('show');
    menuBtn.classList.add('active');
    document.body.style.overflow = 'hidden';
}

// 모바일 메뉴 닫기
function closeMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    const menuBtn = document.getElementById('mobileMenuBtn');

    sidebar.classList.remove('mobile-open');
    overlay.classList.remove('show');
    menuBtn.classList.remove('active');
    document.body.style.overflow = '';
}

// 화면 크기 변경 시 모바일 메뉴 상태 초기화
window.addEventListener('resize', function() {
    if (window.innerWidth > 768) {
        closeMobileMenu();
    }
});
