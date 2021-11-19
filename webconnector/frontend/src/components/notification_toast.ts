import { css, customElement, html, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

@customElement('notification-toast')
export class NotificationToast extends PageElement {
  static styles = css`
    #notification-toast {
      border-radius: 20px;
      box-shadow: 0px 12px 42px rgb(0 0 0 / 24%);
      background-color: springgreen;
      color: black;
    }
  `;

  @property({ type: Array })
  notificationToastMessage: NotificationToastMessage = {
    message: 'Default Message',
    'error-code': '400',
    'error-code-message': 'demo-default-code-error-message',
  };

  render() {
    const url = window.location.href;
    const last2 = url.toString().slice(-2);
    return html`<paper-toast
      id="notification-toast"
      text="${this.notificationToastMessage.message}"
      duration="6000"
    >
      <paper-button
        onclick="document.getElementById('notification').shadowRoot.getElementById('notification-toast').toggle()"
        class="close-button"
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
