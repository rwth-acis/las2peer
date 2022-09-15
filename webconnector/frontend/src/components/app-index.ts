import {
  LitElement,
  html,
  css,
  customElement,
  query,
  property,
} from 'lit-element';

import config from '../config.js';
import { request } from '../helpers/request_helper.js';
import { attachRouter } from '../router/index.js';
import 'las2peer-frontend-statusbar/las2peer-frontend-statusbar.js';
import 'pwa-helper-components/pwa-install-button.js';
import 'pwa-helper-components/pwa-update-available.js';
import './notification_toast.js';
import '@polymer/paper-icon-button/paper-icon-button.js';
import '@polymer/iron-icons/iron-icons.js';
import './information_bar.js';

let oidcUser: User | undefined = undefined;

export { oidcUser };
@customElement('app-index')
export class AppIndex extends LitElement {
  @query('main')
  private main!: HTMLElement;

  private _loadUrl = document.URL;

  static styles = css`
    :host {
      --app-primary-color: #4285f4;
    }

    #statusbar {
      --statusbar-background: var(--app-primary-color);
      --user-widget-button-background: white;
      position: sticky;
      z-index: 10;
      top: 0;
      width: 100%;
      color: white;
    }

    .sidebar {
      height: 100%;
      width: 250px;
      position: fixed;
      z-index: 1;
      top: 0;
      left: 0;
      background-color: #f0f0f0;
      overflow-x: hidden;
      padding-top: 16px;
    }

    .sidebar a {
      padding: 6px 8px 6px 16px;
      text-decoration: none;
      font-size: 20px;
      color: #353434;
      display: block;
    }

    .sidebar a:hover {
      color: #bababa;
    }
    @media screen and (min-width: 800px) {
      main {
        display: flex;
        flex: 1;
        flex-direction: column;
        margin-left: 250px;
      }
    }
    @media screen and (max-width: 800px) {
      .sidebar {
        display: none;
      }
    }
    footer {
      padding: 1rem;
      text-align: center;
      background-color: #eee;
    }
  `;

  render() {
    return html`
      <div class="sidebar">
        <information-bar
          id="information-bar"
          class="information-bar"
        ></information-bar>
        <a href="/"><i class="fa fa-fw fa-welcome"></i> Welcome</a>
        <a href="status"><i class="fa fa-fw fa-status"></i> Status</a>
        <a href="view-services"
          ><i class="fa fa-fw fa-view-services"></i> View Services</a
        >
        <a href="publish-service"
          ><i class="fa fa-fw fa-publish-service"></i> Publish Service</a
        >
        <a href="eth-tools"
          ><i class="fa fa-fw fa-eth-tools"></i> Blockchain and Reputation</a
        >
        <i class="fas fa-address-book"></i>
        <a href="agent-tools"
          ><i class="fa fa-fw fa-agent-tools"></i> Agent Tools</a
        >
      </div>
      <!-- The main content is added / removed dynamically by the router -->
      <main role="main">
        <las2peer-frontend-statusbar
          id="statusbar"
          base-url="[[hostUrl]]"
          service="las2peer Node Front-End"
          oidcclientid="bdda7396-3f6d-4d83-ac21-65b4069d0eab"
          oidcpopupsigninurl=${this._loadUrl}
          oidcpopupsignouturl=${this._loadUrl}
          oidcsilentsigninurl=${this._loadUrl}
          loginoidctoken="[[_oidcUser.access_token]]"
          loginoidcprovider="https://auth.las2peer.org/auth/realms/main"
          subtitle="vLAS2PEER_VERSION"
        ></las2peer-frontend-statusbar>

        <openidconnect-popup-signin-callback></openidconnect-popup-signin-callback>
        <openidconnect-popup-signout-callback></openidconnect-popup-signout-callback>
        <openidconnect-signin-silent-callback></openidconnect-signin-silent-callback>
        
      </main>
      <footer>
        <span>Environment: ${config.environment}</span>
      </footer>
    `;
  }

  firstUpdated() {
    const the = this;
    this.shadowRoot!.getElementById('statusbar')!.addEventListener(
      'signed-in',
      function (event) {
        the.storeOidcUser((event as SignedInEvent).detail);
      }
    );
    attachRouter(this.main);
  }
  storeOidcUser(userObject: any) {
    if (userObject.token_type !== 'Bearer')
      throw 'unexpected OIDC token type, fix me';
    oidcUser = userObject;
    this.sendLogin();
  }

  async sendLogin() {
    const PREFIX_OIDC_SUB = 'OIDC_SUB-';

    const credentials = {
      oidcSub: ((oidcUser || {}).profile || {}).sub,
    };
    const prefixedIdentifier = PREFIX_OIDC_SUB + credentials.oidcSub;
    const response = await request<LoginResponse>(
      '/las2peer/auth/login',
      {
        method: 'GET',
        headers: {
          Authorization:
            'Basic ' + btoa(prefixedIdentifier + ':' + credentials.oidcSub),
          'access-token': (oidcUser || {}).access_token || '',
        },
      }
    );
    console.log(response);
  }
}
interface SignedInEvent extends Event {
  detail: User;
}
export interface User {
  id_token: string;
  session_state: string;
  access_token: string;
  token_type: string;
  profile: Profile;
  expires_at: number;
}
export interface Profile {
  auth_time: number;
  jti: string;
  sub: string;
  typ: string;
  azp: string;
  session_state: string;
  acr: string;
  s_hash: string;
  sid: string;
  email_verified: boolean;
  name: string;
  preferred_username: string;
  given_name: string;
  family_name: string;
  email: string;
}
interface LoginResponse {
  agentid: string;
  code: number;
  ethaddress: string;
  text: string;
  email: string;
  username: string;
}
