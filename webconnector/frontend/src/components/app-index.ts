import {
  LitElement,
  html,
  css,
  customElement,
  query,
  property,
} from 'lit-element';

import config from '../config.js';
import { attachRouter, urlForName } from '../router/index.js';
import 'las2peer-frontend-statusbar/las2peer-frontend-statusbar.js';

import 'pwa-helper-components/pwa-install-button.js';
import 'pwa-helper-components/pwa-update-available.js';

@customElement('app-index')
export class AppIndex extends LitElement {
  @query('main')
  private main!: HTMLElement;

  static styles = css`
    #statusbar {
      position: sticky;
      z-index: 10;
      top: 0;
      width: 100%;
    }
    .sidebar {
      -moz-border-top-right-radius: 30px;
      -webkit-border-top-right-radius: 30px;
      border-top-right-radius: 30px;
      -khtml-border-top-right-radius: 30px;
      height: 100%;
      width: 250px;
      position: fixed;
      z-index: 1;
      top: 0;
      left: 0;
      background-color: #111;
      overflow-x: hidden;
      padding-top: 16px;
    }

    .sidebar a {
      padding: 6px 8px 6px 16px;
      text-decoration: none;
      font-size: 20px;
      color: #818181;
      display: block;
    }

    .sidebar a:hover {
      color: #f1f1f1;
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
        <a href="/"><i class="fa fa-fw fa-welcome"></i> Welcome</a>
        <a href="status"><i class="fa fa-fw fa-status"></i> Status</a>
        <a href="view-services"
          ><i class="fa fa-fw fa-view-services"></i> View Services</a
        >
        <a href="publish-service"
          ><i class="fa fa-fw fa-publish-service"></i> Publish Service</a
        >
        <a href="contact"
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
          oidcclientid="localtestclient"
          oidcpopupsigninurl="/callbacks/popup-signin-callback.html"
          oidcpopupsignouturl="/callbacks/popup-signout-callback.html"
          oidcsilentsigninturl="/callbacks/silent-callback.html"
          loginoidctoken="[[_oidcUser.access_token]]"
          loginoidcprovider="https://api.learning-layers.eu/o/oauth2"
          subtitle="v@LAS2PEER_VERSION@"
        ></las2peer-frontend-statusbar>
      </main>
      <footer>
        <span>Environment: ${config.environment}</span>
      </footer>
    `;
  }

  firstUpdated() {
    attachRouter(this.main);
  }
}
