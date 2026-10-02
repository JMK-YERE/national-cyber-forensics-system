// =====================================================
// SLIDESHOW — Auto-slide + Manual navigation
// =====================================================
document.addEventListener('DOMContentLoaded', function() {
    const slides = document.querySelectorAll('.slide');
    const dotsContainer = document.getElementById('slideDots');

    if (slides.length === 0) return;

    // Unda dots
    if (dotsContainer) {
        slides.forEach((s, i) => {
            const dot = document.createElement('div');
            dot.className = 'dot' + (i === 0 ? ' active' : '');
            dot.addEventListener('click', () => goToSlide(i));
            dotsContainer.appendChild(dot);
        });
    }

    const dots = document.querySelectorAll('.dot');
    let current = 0;

    function goToSlide(n) {
        slides[current].classList.remove('active');
        if (dots[current]) dots[current].classList.remove('active');

        current = (n + slides.length) % slides.length;

        slides[current].classList.add('active');
        if (dots[current]) dots[current].classList.add('active');
    }

    // Auto-slide kila sekunde 5
    setInterval(() => goToSlide(current + 1), 5000);
});
