import { css, customElement, html, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

@customElement('notification-toast')
export class NotificationToast extends PageElement {
  static styles = css``;

  @property({ type: Array })
  notificationToastMessage: NotificationToastMessage = {
    message: 'demo-default-message',
    'error-code': '400',
    'error-code-message': 'demo-default-code-error-message',
  };

  render() {
    return html`<paper-toast
      id="notification-toast"
      text="${this.notificationToastMessage.message}"
    >
      <paper-button
        onclick="document.getElementById('notification').shadowRoot.getElementById('notification-toast').toggle()"
        class="yellow-button"
        >Close</paper-button
      >
    </paper-toast> `;
  }
}

export interface NotificationToastMessage {
  message: string;
  'error-code': string;
  'error-code-message': string;
}
