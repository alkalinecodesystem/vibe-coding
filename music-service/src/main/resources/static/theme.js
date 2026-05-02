// Theme management
document.addEventListener('DOMContentLoaded', function() {
    const themeSelect = document.getElementById('themeSelect');
    if (themeSelect) {
        // Load saved theme
        const savedTheme = localStorage.getItem('theme') || 'default';
        themeSelect.value = savedTheme;
        applyTheme(savedTheme);

        // Handle theme change
        themeSelect.addEventListener('change', function() {
            const selectedTheme = this.value;
            localStorage.setItem('theme', selectedTheme);
            applyTheme(selectedTheme);
        });
    }
});

function applyTheme(theme) {
    document.body.className = document.body.className.replace(/theme-\w+/g, '').trim();
    if (theme !== 'default') {
        document.body.classList.add('theme-' + theme);
    }
}