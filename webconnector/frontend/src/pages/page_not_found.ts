import { html, css, customElement } from 'lit-element';

import { PageElementNotFound } from '../helpers/page-element-not-found.js';
import { urlForName } from '../router/index.js';

@customElement('page-not-found')
export class PageNotFound extends PageElementNotFound {
  static styles = css`
    :host {
      display: block;
    }

    section {
      padding: 1rem;
      text-align: center;
    }
  `;

  render() {
    return html`
      <section>
        <h1>Page not found</h1>

        <p>
          <a href="${urlForName('home')}">Back to home</a>
        </p>
      </section>
    `;
  }
}
