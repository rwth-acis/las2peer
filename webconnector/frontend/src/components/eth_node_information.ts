import { css, customElement, html, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import { request } from '../helpers/request_helper.js';

@customElement('eth-node-information')
export class EthNodeInformation extends PageElement {
  static styles = css``;

  @property({ type: Boolean })
  isEthNode = false;

  render() {
    return html`
      ${this.isEthNode
        ? html`<iron-icon
            icon="fingerprint"
            title="Node is running on Ethereum"
            style="width: 18px; height: 18px; color: green;"
          ></iron-icon> `
        : html`
            <iron-icon
              icon="fingerprint"
              title="Node is NOT running on Ethereum"
              style="width: 18px; height: 18px; color: red;"
            ></iron-icon>
          `}
    `;
  }
  firstUpdated() {
    this.checkETHNode();
  }
  async checkETHNode() {
    const response = await request(config.url + '/las2peer/check-eth', {
      method: 'GET',
    });
    if (response.code === 200) {
      this.isEthNode = true;
    }
  }
}
