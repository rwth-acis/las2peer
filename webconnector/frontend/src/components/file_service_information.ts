import { css, customElement, html, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import { request } from '../helpers/request_helper.js';
import { oidcUser } from './app-index.js';

@customElement('file-service-information')
export class FileServiceInformation extends PageElement {
  static styles = css``;

  @property({ type: Boolean })
  isFileServiceRunning = false;

  render() {
    return html`
      <!-- Is FileService running? -->
      ${this.isFileServiceRunning
        ? html`
            <iron-icon
              icon="description"
              title="FileService is Running"
              style="width: 18px; height: 18px; color: green;"
            ></iron-icon>
          `
        : html`
            <iron-icon
              icon="description"
              title="FileService is NOT Running"
              style="width: 18px; height: 18px; color: red;"
            ></iron-icon>
          `}
    `;
  }
  firstUpdated() {
    this.checkFileService();
  }
  async checkFileService() {
    const response = await request(
      config.url + '/las2peer/fileservice/index.html',
      {
        method: 'GET',
      }
    );
    if (response.code === 200) {
      this.isFileServiceRunning = true;
    }
  }
}
