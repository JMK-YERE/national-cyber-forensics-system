document.addEventListener('DOMContentLoaded', function () {
  const slider = document.querySelector('#heroSlider');
  if (!slider) return;

  const slides = Array.from(slider.querySelectorAll('.hero-slide'));
  const dots = Array.from(slider.querySelectorAll('.slider-dots [data-slide]'));
  if (slides.length < 2) return;

  const intervalMs = 6500;
  let current = Math.max(0, slides.findIndex(slide => slide.classList.contains('active')));
  let timer = null;
  let paused = false;
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Add accessible controls without changing the server-rendered slide content.
  let controls = slider.querySelector('.slider-controls');
  if (!controls) {
    controls = document.createElement('div');
    controls.className = 'slider-controls';
    controls.setAttribute('aria-label', 'Hero slideshow controls');
    controls.innerHTML = '<button type="button" class="slider-arrow slider-prev" aria-label="Previous slide">&#10094;</button><button type="button" class="slider-arrow slider-next" aria-label="Next slide">&#10095;</button>';
    slider.appendChild(controls);
  }

  let status = slider.querySelector('.slider-status');
  if (!status) {
    status = document.createElement('span');
    status.className = 'slider-status';
    status.setAttribute('aria-live', 'polite');
    status.setAttribute('aria-atomic', 'true');
    status.style.cssText = 'position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0';
    slider.appendChild(status);
  }

  function showSlide(nextIndex) {
    current = (nextIndex + slides.length) % slides.length;
    slides.forEach((slide, index) => {
      const active = index === current;
      slide.classList.toggle('active', active);
      slide.setAttribute('aria-hidden', String(!active));
      if ('inert' in slide) slide.inert = !active;
    });
    dots.forEach((dot, index) => {
      const active = index === current;
      dot.classList.toggle('active', active);
      if (active) dot.setAttribute('aria-current', 'true');
      else dot.removeAttribute('aria-current');
    });
    status.textContent = 'Slide ' + (current + 1) + ' of ' + slides.length;
  }

  function stopTimer() {
    if (timer !== null) window.clearInterval(timer);
    timer = null;
  }

  function startTimer() {
    stopTimer();
    if (paused || reducedMotion || document.hidden) return;
    timer = window.setInterval(() => showSlide(current + 1), intervalMs);
  }

  slider.querySelector('.slider-prev').addEventListener('click', () => {
    showSlide(current - 1);
    startTimer();
  });
  slider.querySelector('.slider-next').addEventListener('click', () => {
    showSlide(current + 1);
    startTimer();
  });
  dots.forEach((dot, index) => dot.addEventListener('click', () => {
    showSlide(index);
    startTimer();
  }));

  slider.addEventListener('mouseenter', () => { paused = true; stopTimer(); });
  slider.addEventListener('mouseleave', () => { paused = false; startTimer(); });
  slider.addEventListener('focusin', () => { paused = true; stopTimer(); });
  slider.addEventListener('focusout', event => {
    if (!slider.contains(event.relatedTarget)) { paused = false; startTimer(); }
  });
  document.addEventListener('visibilitychange', startTimer);
  slider.addEventListener('keydown', event => {
    if (event.key === 'ArrowLeft') { showSlide(current - 1); startTimer(); }
    if (event.key === 'ArrowRight') { showSlide(current + 1); startTimer(); }
  });

  showSlide(current);
  startTimer();
});