(() => {
  const themeKey = 'aprovacao-passoa-passo-theme';
  const themes = ['dark', 'midnight'];

  function applyTheme(theme) {
    const normalizedTheme = themes.includes(theme) ? theme : themes[0];
    document.documentElement.dataset.theme = normalizedTheme;
    try {
      window.localStorage.setItem(themeKey, normalizedTheme);
    } catch {
      // Ignore storage failures.
    }
  }

  function toggleTheme() {
    const currentTheme = document.documentElement.dataset.theme || themes[0];
    const nextTheme = currentTheme === themes[0] ? themes[1] : themes[0];
    applyTheme(nextTheme);
  }

  function createBankArtwork(name, sigla, colors) {
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360" role="img" aria-label="${name}">
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${colors[0]}" />
            <stop offset="55%" stop-color="${colors[1]}" />
            <stop offset="100%" stop-color="${colors[2]}" />
          </linearGradient>
          <radialGradient id="glow" cx="50%" cy="36%" r="60%">
            <stop offset="0%" stop-color="rgba(255,255,255,0.34)" />
            <stop offset="100%" stop-color="rgba(255,255,255,0)" />
          </radialGradient>
        </defs>
        <rect width="640" height="360" rx="28" fill="url(#bg)" />
        <rect width="640" height="360" rx="28" fill="url(#glow)" />
        <circle cx="130" cy="110" r="64" fill="rgba(255,255,255,0.14)" />
        <circle cx="530" cy="88" r="96" fill="rgba(255,255,255,0.12)" />
        <path d="M66 284h508" stroke="rgba(255,255,255,0.2)" stroke-width="2" />
        <text x="70" y="106" fill="rgba(255,255,255,0.95)" font-family="Inter, Arial, sans-serif" font-size="32" font-weight="800">${name}</text>
        <text x="70" y="246" fill="rgba(255,255,255,0.96)" font-family="Sora, Inter, Arial, sans-serif" font-size="56" font-weight="800">${sigla}</text>
        <g transform="translate(476 202)">
          <rect x="0" y="0" width="88" height="56" rx="18" fill="rgba(255,255,255,0.9)" />
          <path d="M24 27h40" stroke="#111827" stroke-width="7" stroke-linecap="round" />
          <path d="M44 11v32" stroke="#111827" stroke-width="7" stroke-linecap="round" />
        </g>
      </svg>
    `.trim();

    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
  }

  function restoreTheme() {
    let storedTheme = null;
    try {
      storedTheme = window.localStorage.getItem(themeKey);
    } catch {
      storedTheme = null;
    }

    applyTheme(storedTheme || themes[0]);
  }

  function bindThemeButtons() {
    document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
      button.addEventListener('click', toggleTheme);
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    restoreTheme();
    bindThemeButtons();
  });

  window.BankPlatform = {
    applyTheme,
    toggleTheme,
    createBankArtwork,
  };
})();
