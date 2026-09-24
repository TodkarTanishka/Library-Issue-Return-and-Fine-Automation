/* UI Presentation Helpers (No API/Data logic) */
function toggleSidebar() {
    const sidebar = document.getElementById("appSidebar");
    if (sidebar) {
        sidebar.classList.toggle("collapsed");
    }
}

function toggleMobileSidebar() {
    const mobileNav = document.getElementById("mobileNavDrawer");
    if (mobileNav) {
        mobileNav.classList.toggle("hidden");
    }
}
