import { html } from 'lit-element';

export const renderPageNotFound = () => {
  import('../pages/page_not_found.js');

  return html`<page-not-found></page-not-found>`;
};
