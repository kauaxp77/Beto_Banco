(() => {
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const root = document.documentElement;
  const revealElements = [...document.querySelectorAll('[data-reveal], .reveal')];

  if (!reducedMotion) {
    root.classList.add('has-motion');

    revealElements.forEach((element) => {
      const delay = element.dataset.revealDelay;
      if (delay) {
        element.style.setProperty('--reveal-delay', `${delay}ms`);
      }
    });

    const revealObserver = new IntersectionObserver((entries, observer) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.16 });

    revealElements.forEach((element) => revealObserver.observe(element));

    if (window.matchMedia('(pointer: fine)').matches) {
      window.addEventListener('pointermove', (event) => {
        const horizontalOffset = (event.clientX / window.innerWidth - 0.5) * 18;
        const verticalOffset = (event.clientY / window.innerHeight - 0.5) * 14;
        root.style.setProperty('--parallax-x', `${horizontalOffset}px`);
        root.style.setProperty('--parallax-y', `${verticalOffset}px`);
      }, { passive: true });
    }
  } else {
    revealElements.forEach((element) => element.classList.add('is-revealed'));
  }
})();