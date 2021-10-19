import { css, customElement, html, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import { request } from '../helpers/request_helper.js';
import { oidcUser } from './app-index.js';

@customElement('contact-service-information')
export class ContactServiceInformation extends PageElement {
  static styles = css``;

  @property({ type: Boolean })
  isContactServiceRunning = false;

  render() {
    return html`
      <!-- Is ContactService running? -->
      ${this.isContactServiceRunning
        ? html`
            <iron-icon
              icon="perm-contact-calendar"
              title="ContactService is Running"
              style="width: 18px; height: 18px; color: green;"
            ></iron-icon>
          `
        : html`
            <iron-icon
              icon="perm-contact-calendar"
              title="ContactService is NOT Running OR user is not logged in."
              style="width: 18px; height: 18px; color: red;"
            ></iron-icon>
          `}
    `;
  }
  firstUpdated() {
    this.checkContactService();
  }
  async checkContactService() {
    const PREFIX_OIDC_SUB = 'OIDC_SUB-';

    const credentials = {
      oidcSub: ((oidcUser || {}).profile || {}).sub,
    };
    const prefixedIdentifier = PREFIX_OIDC_SUB + credentials.oidcSub;
    const response = await request(config.url + '/contactservice/groups', {
      method: 'GET',
      headers: {
        Authorization:
          'Basic ' + btoa(prefixedIdentifier + ':' + credentials.oidcSub),
        'access-token': (oidcUser || {}).access_token || '',
      },
    });
    if (response.code === 200) {
      this.isContactServiceRunning = true;
    }
  }
}
