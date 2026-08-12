(() => {
  const banks = [
    { id: 1, name: 'BANCO DO BRASIL', sigla: 'BB', subtitle: 'Curso preparatório para concursos do Banco do Brasil.', artworkLabel: 'BB', imageKey: 'bb', status: 'published', linkCompra: 'https://example.com/comprar/banco-do-brasil', colors: ['#0b153a', '#1148c7', '#ffd84d'] },
    { id: 2, name: 'CAIXA ECONÔMICA FEDERAL', sigla: 'CAIXA', subtitle: 'Preparação estratégica completa para a CAIXA.', artworkLabel: 'CAIXA', imageKey: 'caixa', status: 'published', linkCompra: 'https://example.com/comprar/caixa-economica-federal', colors: ['#061b4a', '#0e46b8', '#2d7fff'] },
    { id: 3, name: 'BANCO DA AMAZÔNIA', sigla: 'BASA', subtitle: 'Curso focado nas oportunidades do BASA.', artworkLabel: 'BASA', imageKey: 'basa', status: 'published', linkCompra: 'https://example.com/comprar/basa', colors: ['#06281f', '#0f6f57', '#3fe2b6'] },
    { id: 4, name: 'BANCO DO NORDESTE', sigla: 'BNB', subtitle: 'Formação objetiva para concursos do BNB.', artworkLabel: 'BNB', imageKey: 'bnb', status: 'published', linkCompra: 'https://example.com/comprar/banco-do-nordeste', colors: ['#120f4f', '#1d3192', '#6ba1ff'] },
    { id: 5, name: 'BANCO NACIONAL DE DESENVOLVIMENTO ECONÔMICO E SOCIAL', sigla: 'BNDES', subtitle: 'Preparação premium para concursos do BNDES.', artworkLabel: 'BNDES', imageKey: 'bndes', status: 'published', linkCompra: 'https://example.com/comprar/bndes', colors: ['#132752', '#2c3f88', '#803c86'] },
    { id: 6, name: 'BANCO DE BRASÍLIA', sigla: 'BRB', subtitle: 'Curso completo para o Banco de Brasília.', artworkLabel: 'BRB', imageKey: 'brb', status: 'published', linkCompra: 'https://example.com/comprar/brb', colors: ['#071749', '#12358c', '#75b5ff'] },
    { id: 7, name: 'BANRISUL', sigla: 'BANRISUL', subtitle: 'Preparatório moderno para o Banrisul.', artworkLabel: 'BANRISUL', imageKey: 'banrisul', status: 'published', linkCompra: 'https://example.com/comprar/banrisul', colors: ['#081c53', '#1d4ed8', '#5d8dff'] },
    { id: 8, name: 'BANESTES', sigla: 'BANCO DO ESTADO DO ESPÍRITO SANTO', subtitle: 'Preparação estratégica para o Banestes.', artworkLabel: 'BANESTES', imageKey: 'banestes', status: 'published', linkCompra: 'https://example.com/comprar/banestes', colors: ['#0b2f64', '#1b4d95', '#2f86cf'] },
    { id: 9, name: 'BANPARÁ', sigla: 'BANCO DO ESTADO DO PARÁ', subtitle: 'Trilha completa para concursos do Banpará.', artworkLabel: 'BANPARA', imageKey: 'banpara', status: 'published', linkCompra: 'https://example.com/comprar/banpara', colors: ['#5e141a', '#8f1f2a', '#c24b58'] },
    { id: 10, name: 'BANESE', sigla: 'BANCO DO ESTADO DE SERGIPE', subtitle: 'Curso direcionado para oportunidades no Banese.', artworkLabel: 'BANESE', imageKey: 'banese', status: 'published', linkCompra: 'https://example.com/comprar/banese', colors: ['#0d3b20', '#1a6a33', '#4ba65f'] },
    { id: 11, name: 'BDMG', sigla: 'BANCO DE DESENVOLVIMENTO DE MINAS GERAIS', subtitle: 'Preparação estratégica para o BDMG.', artworkLabel: 'BDMG', imageKey: 'bdmg', status: 'published', linkCompra: 'https://example.com/comprar/bdmg', colors: ['#5a1026', '#7d1e29', '#b83c44'] },
    { id: 12, name: 'BRDE', sigla: 'BANCO REGIONAL DE DESENVOLVIMENTO DO EXTREMO SUL', subtitle: 'Curso completo para concursos do BRDE.', artworkLabel: 'BRDE', imageKey: 'brde', status: 'published', linkCompra: 'https://example.com/comprar/brde', colors: ['#063c2f', '#0d6a4d', '#1f9c76'] },
    { id: 13, name: 'BANDES', sigla: 'BANCO DE DESENVOLVIMENTO DO ESPÍRITO SANTO', subtitle: 'Trilha completa para oportunidades no BANDES.', artworkLabel: 'BANDES', imageKey: 'bandes', status: 'published', linkCompra: 'https://example.com/comprar/bandes', colors: ['#5c230c', '#8c3b14', '#c85b1e'] },
  ];

  const grid = document.getElementById('banksGrid');
  const template = document.getElementById('bankCardTemplate');

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
      const cartButton = fragment.querySelector('.bank-card__cart-button');

      const localArtwork = `images/bancos/${bank.imageKey}.png`;
      const svgFallback = `images/bancos/${bank.imageKey}.svg`;
      const fallbackArtwork = window.BankPlatform.createBankArtwork(bank.name, bank.artworkLabel || bank.sigla, bank.colors);
      image.src = localArtwork;
      image.alt = `${bank.name} - ${bank.sigla}`;
      image.onerror = () => {
        image.onerror = () => {
          image.onerror = null;
          image.src = fallbackArtwork;
        };
        image.src = svgFallback;
      };
      image.dataset.logoPath = localArtwork;
      title.textContent = bank.name;
      sigla.textContent = bank.sigla;
      subtitle.textContent = bank.subtitle;
      card.style.setProperty('--card-index', bank.id - 1);
      card.style.setProperty('--cover-artwork', `url("${localArtwork}")`);

      const isComingSoon = bank.status === 'coming-soon';
      status.textContent = isComingSoon ? 'EM BREVE' : 'PUBLICADO';
      status.classList.toggle('is-coming-soon', isComingSoon);

      if (isComingSoon) {
        buyButton.innerHTML = '<span>EM BREVE</span>';
        buyButton.classList.add('is-disabled');
        cartButton.classList.add('is-disabled');
        buyButton.removeAttribute('href');
        buyButton.removeAttribute('target');
        buyButton.removeAttribute('rel');
        buyButton.setAttribute('aria-disabled', 'true');
        cartButton.removeAttribute('href');
        cartButton.removeAttribute('target');
        cartButton.removeAttribute('rel');
        cartButton.setAttribute('aria-disabled', 'true');
      } else {
        buyButton.innerHTML = '<span>COMPRAR AGORA</span>';
        buyButton.classList.remove('is-disabled');
        cartButton.classList.remove('is-disabled');
        buyButton.href = bank.linkCompra;
        buyButton.target = '_blank';
        buyButton.rel = 'noopener noreferrer';
        buyButton.removeAttribute('aria-disabled');
        cartButton.href = bank.linkCompra;
        cartButton.target = '_blank';
        cartButton.rel = 'noopener noreferrer';
        cartButton.removeAttribute('aria-disabled');
      }

      grid.appendChild(fragment);
    });
  }

  document.addEventListener('DOMContentLoaded', renderBanks);
})();
