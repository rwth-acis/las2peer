import { css, customElement, html } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';
import './eth_node_information.js';
import './contact_service_information.js';
import './file_service_information.js';

@customElement('information-bar')
export class InformationBar extends PageElement {
  static styles = css``;

  render() {
    return html` <div
      style="margin-left: auto;order: 2;display: flex;align-content: center;align-items: center;justify-content: center;"
    >
      <!-- Is running blockchain? -->
      <eth-node-information></eth-node-information>

      <!-- Is FileService running? -->
      <file-service-information></file-service-information>

      <!-- Is ContactService running? -->
      <contact-service-information></contact-service-information>
      <paper-icon-button
        icon="refresh"
        title="Refresh Status"
        @click=""
      ></paper-icon-button>
    </div>`;
  }
}
