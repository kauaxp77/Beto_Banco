(() => {
  const banks = [
    { id: 101, name: 'BANCO DE DESENVOLVIMENTO', sigla: 'EDICAO PRO', subtitle: 'Curadoria especial para editais regionais e bancos de fomento.', status: 'published', linkCompra: 'https://example.com/comprar/edicao-pro-bancos', colors: ['#0b1f4a', '#1d4ed8', '#60a5fa'] },
    { id: 102, name: 'PACOTE CARREIRA BANCARIA', sigla: 'TRILHA MAX', subtitle: 'Plano com disciplinas nucleares e simulados por banca.', status: 'published', linkCompra: 'https://example.com/comprar/trilha-max', colors: ['#1e293b', '#334155', '#64748b'] },
    { id: 103, name: 'BANCO DE DESENVOLVIMENTO', sigla: 'BDMG', subtitle: 'Preparacao intensiva para concursos do BDMG.', status: 'published', linkCompra: 'https://example.com/comprar/bdmg', colors: ['#4a0c2a', '#7f1d1d', '#a21caf'] },
    { id: 104, name: 'BANCO REGIONAL DE DESENVOLVIMENTO', sigla: 'BRDE', subtitle: 'Foco em legislacao aplicada e conhecimentos especificos.', status: 'published', linkCompra: 'https://example.com/comprar/brde', colors: ['#0b3b2e', '#14532d', '#166534'] },
    { id: 105, name: 'BANCO DE DESENVOLVIMENTO DO ESPIRITO SANTO', sigla: 'BANDES', subtitle: 'Preparacao premium para etapas objetiva e discursiva.', status: 'published', linkCompra: 'https://example.com/comprar/bandes', colors: ['#6f2f1f', '#b45309', '#f97316'] },
  ];

  const grid = document.getElementById('banksGrid');
  const template = document.getElementById('bankCardTemplate');
  const openMenuId = { value: null };

  function renderBanks() {
    grid.innerHTML = '';

    banks.forEach((bank) => {
      const fragment = template.content.cloneNode(true);
      const card = fragment.querySelector('.bank-card');
      const image = fragment.querySelector('.bank-card__image');
      const status = fragment.querySelector('.bank-card__status');
      const title = fragment.querySelector('.bank-card__title');
      const sigla = fragment.querySelector('.bank-card__sigla');
      const subtitle = fragment.querySelector('.bank-card__subtitle');
      const buyButton = fragment.querySelector('.bank-card__buy-button');
      const menuButton = fragment.querySelector('.bank-card__menu-button');
      const menu = fragment.querySelector('.bank-card__menu');

      const artwork = window.BankPlatform.createBankArtwork(bank.name, bank.sigla, bank.colors);
      image.src = artwork;
      image.alt = `${bank.name} - ${bank.sigla}`;
      image.dataset.logoPath = `images/bancos/outros/${bank.sigla.toLowerCase()}.png`;
      title.textContent = bank.name;
      sigla.textContent = bank.sigla;
      subtitle.textContent = bank.subtitle;

      status.textContent = 'PUBLICADO';
      status.classList.remove('is-coming-soon');
      buyButton.textContent = 'COMPRAR AGORA';
      buyButton.classList.remove('is-disabled');
      buyButton.href = bank.linkCompra;
      buyButton.target = '_blank';
      buyButton.rel = 'noopener noreferrer';
      buyButton.removeAttribute('aria-disabled');

      menuButton.addEventListener('click', (event) => {
        event.stopPropagation();
        const cardId = String(bank.id);
        openMenuId.value = openMenuId.value === cardId ? null : cardId;
        syncMenus();
      });

      menu.dataset.cardMenu = String(bank.id);
      card.dataset.cardId = String(bank.id);
      grid.appendChild(fragment);
    });

    syncMenus();
  }

  function syncMenus() {
    document.querySelectorAll('.bank-card__menu').forEach((menu) => {
      menu.hidden = menu.dataset.cardMenu !== openMenuId.value;
    });
  }

  document.addEventListener('click', (event) => {
    if (!event.target.closest('.bank-card__menu') && !event.target.closest('.bank-card__menu-button')) {
      openMenuId.value = null;
      syncMenus();
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      openMenuId.value = null;
      syncMenus();
    }
  });

  document.addEventListener('DOMContentLoaded', renderBanks);
})();
