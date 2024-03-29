import { pageNotFoundMeta } from '../router/routes.js';
import { updateMeta } from './html-meta-manager/index.js';
import { setMetaTag, removeMetaTag } from './html-meta-manager/utils.js';
import { PageElement } from './page-element.js';

export class PageElementNotFound extends PageElement {
  connectedCallback() {
    super.connectedCallback();

    setMetaTag('name', 'render:status_code', '404');

    updateMeta({
      ...this.defaultMeta,
      ...pageNotFoundMeta,
    });
  }

  disconnectedCallback() {
    removeMetaTag('name', 'render:status_code');

    super.disconnectedCallback();
  }
}
