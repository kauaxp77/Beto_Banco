import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import { iniciarAnalytics } from './lib/analytics';
import './index.css';

/**
 * Secao 25 -- a camada de analytics sobe antes do primeiro render, mas nao
 * dispara nada: ela apenas passa a escutar o evento de consentimento. Nenhum
 * evento sai do navegador antes do aceite (secao 22).
 */
iniciarAnalytics();

/**
 * Secao 09 -- PWA instalavel, nao app nativo.
 *
 * O registro fica atras do evento load para nao competir por banda com o
 * conteudo da primeira pintura, que e o que a secao 15 mede como LCP.
 * Em desenvolvimento o service worker fica fora do caminho: cache de bundle
 * durante o dev e uma fonte inesgotavel de "mas eu salvei o arquivo".
 */
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((erro) => {
      console.warn('Service worker nao registrado:', erro);
    });
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
