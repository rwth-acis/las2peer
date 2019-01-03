/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */

import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import { setPassiveTouchGestures, setRootPath } from '@polymer/polymer/lib/utils/settings.js';
import '@polymer/app-layout/app-drawer/app-drawer.js';
import '@polymer/app-layout/app-drawer-layout/app-drawer-layout.js';
import '@polymer/app-layout/app-header/app-header.js';
import '@polymer/app-layout/app-header-layout/app-header-layout.js';
import '@polymer/app-layout/app-scroll-effects/app-scroll-effects.js';
import '@polymer/app-layout/app-toolbar/app-toolbar.js';
import '@polymer/app-route/app-location.js';
import '@polymer/app-route/app-route.js';
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/iron-pages/iron-pages.js';
import '@polymer/iron-selector/iron-selector.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-dialog/paper-dialog.js';
import '@polymer/paper-icon-button/paper-icon-button.js';
import '@polymer/paper-input/paper-input.js';
import 'openidconnect-signin/openidconnect-signin.js'
import 'openidconnect-signin/openidconnect-popup-signin-callback.js'
import 'openidconnect-signin/openidconnect-popup-signout-callback.js'
import 'openidconnect-signin/openidconnect-signin-silent-callback.js'

// Gesture events like tap and track generated from touch will not be
// preventable, allowing for better scrolling performance.
setPassiveTouchGestures(true);

// Set Polymer's root path to the same value we passed to our service worker
// in `index.html`.
setRootPath(NodeFrontendGlobals.rootPath);

class NodeFrontend extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxLogin"
                 url="/las2peer/auth/login"
                 handle-as="json"
                 on-response="_handleLoginResponse"
                 on-error="_handleError"
                 loading="{{_submittingLogin}}"></iron-ajax>
      <iron-ajax id="ajaxDestroySession"
                 method="POST"
                 url="/las2peer/auth/logout"
                 handle-as="json"
                 on-response="_handleLogoutResponse"
                 on-error="_handleError"
                 loading="{{_submittingLogout}}"></iron-ajax>
      <iron-ajax id="ajaxValidateSession"
                 url="/las2peer/auth/validate"
                 handle-as="json"
                 on-response="_handleValidateResponse"
                 on-error="_handleError"
                 loading="{{_submittingLogin}}"></iron-ajax>


      <style>
        :host {
          --app-primary-color: #4285f4;
          --app-secondary-color: black;

          display: block;
        }

        app-drawer-layout:not([narrow]) [drawer-toggle] {
          display: none;
        }

        app-header {
          color: #fff;
          background-color: var(--app-primary-color);
        }

        app-header paper-icon-button {
          --paper-icon-button-ink-color: white;
        }

        .drawer-list {
          margin: 0 20px;
        }

        .drawer-list a {
          display: block;
          padding: 0 16px;
          text-decoration: none;
          color: var(--app-secondary-color);
          line-height: 40px;
        }

        .drawer-list a.iron-selected {
          color: black;
          font-weight: bold;
        }
      </style>

      <app-location route="{{route}}" url-space-regex="^[[rootPath]]"></app-location>

      <app-route route="{{route}}" pattern="[[rootPath]]:page" data="{{routeData}}" tail="{{subroute}}"></app-route>

      <app-drawer-layout fullbleed="" narrow="{{narrow}}">
        <!-- Drawer content -->
        <app-drawer id="drawer" slot="drawer" swipe-open="[[narrow]]">
          <app-toolbar>Menu</app-toolbar>
          <iron-selector selected="[[page]]" attr-for-selected="name" class="drawer-list" role="navigation">
            <a name="view-status" href="[[rootPath]]view-status">Status</a>
            <a name="view-services" href="[[rootPath]]view-services">Services</a>
            <a name="view-agents" href="[[rootPath]]view-agents">Agent Tools</a>
          </iron-selector>
        </app-drawer>

        <!-- Main content -->
        <app-header-layout has-scrolling-region="">

          <app-header slot="header" condenses="" reveals="" effects="waterfall">
            <app-toolbar>
              <paper-icon-button icon="icons:menu" drawer-toggle=""></paper-icon-button>
              <div main-title="">las2peer Node Front-End</div>
              
              <template is="dom-if" if="[[_agentid]]">
                <paper-button on-tap="destroySession">Logout <iron-icon icon="account-circle"></iron-icon></paper-button>
              </template>
              <template is="dom-if" if="[[!_agentid]]">
                <paper-button on-tap="showLoginDialog">Login <iron-icon icon="account-circle"></iron-icon></paper-button>
              </template>
            </app-toolbar>
          </app-header>

          <iron-pages selected="[[page]]" attr-for-selected="name" fallback-selection="view404" role="main">
            <status-view name="view-status" agentid="[[_agentid]]" error="{{_error}}"></status-view>
            <services-view name="view-services" agentid="[[_agentid]]" error="{{_error}}"></services-view>
            <agents-view name="view-agents" agentid="[[_agentid]]" error="{{_error}}"></agents-view>
            <my-view404 name="view404"></my-view404>
          </iron-pages>
        </app-header-layout>
        
        <!-- modal dialogs -->
        <paper-dialog id="loginDialog" modal="[[_submittingLogin]]">
          <h2>Login</h2>
          
          <dom-if if="[[_oidcUser]]">
            <template>
              <div>You are logged in via Layers. Welcome!</div>
            </template>
          </dom-if>
          <openidconnect-signin id="signin"
                                scope="openid profile email"
                                clientid="a4b3f15a-eaec-489a-af08-1dc9cf57347e"
                                authority="https://api.learning-layers.eu/o/oauth2"
                                providername="Layers"
                                popupredirecturi$="[[_loadUrl]]"
                                popuppostlogoutredirecturi$="[[_loadUrl]]"
                                silentredirecturi$="[[_loadUrl]]"
                                ></openidconnect-signin>
          <!-- no idea if this is a bad way to do it, but it seems to work -->
          <openidconnect-popup-signin-callback></openidconnect-popup-signin-callback>
          <openidconnect-popup-signout-callback></openidconnect-popup-signout-callback>
          <openidconnect-signin-silent-callback></openidconnect-signin-silent-callback>
          
          <div hidden$="[[!!_oidcUser]]">
            <div>Or use your las2peer agent credentials:</div>
            <form is="iron-form" id="loginForm" on-keypress="_keyPressedLogin">
              <paper-input label="email or username" id="useridField" disabled="[[_submittingLogin]]" value="" autofocus></paper-input>
              <paper-input label="password" id="passwordField" disabled="[[_submittingLogin]]" value="" type="password">
                <paper-icon-button id="loginButton" icon="send" slot="suffix"></paper-icon-button>
              </paper-input>
              <!-- hidden button is triggered via paper button above -->
              <input type="submit" id="loginSubmitButton" style="display: none" />
            </form>
            <dom-if if="[[_submittingLogin]]">
              <template>
              <paper-spinner style="left: 38%; position: absolute; z-index: 10" active="[[_submittingLogin]]"></paper-spinner>
              </template>
            </dom-if>
            <div>To register, use the <a name="view-agents" href="[[rootPath]]view-agents">Agents</a> tab.</div>
          </div>
        </paper-dialog>
        
        <paper-dialog id="las2peerErrorDialog">
          <h2 id="las2peerErrorDialogTitle">Error</h2>
          <div id="las2peerErrorDialogMessage"></div>
          <div class="buttons">
            <paper-button dialog-dismiss>OK</paper-button>
          </div>
        </paper-dialog>

      </app-drawer-layout>
    `;
  }

  static get properties() {
    return {
      page: { type: String, reflectToAttribute: true, observer: '_pageChanged' },
      routeData: Object,
      subroute: Object,
      _agentid: { type: String, value: '' },
      _submittingLogin: { type: Boolean, value: false },
      _error: { type: Object, observer: '_errorChanged' },
      _oidcUser: Object
    };
  }

  static get observers() {
    return [
      '_routePageChanged(routeData.page)'
    ];
  }

  _routePageChanged(page) {
     // Show the corresponding page according to the route.
     //
     // If no page was found in the route data, page will be an empty string.
     // Show 'view1' in that case. And if the page doesn't exist, show 'view404'.
    if (!page) {
      this.page = 'view-status';
    } else if (['view-status', 'view-services', 'view-agents'].indexOf(page) !== -1) {
      this.page = page;
    } else {
      this.page = 'view404';
    }

    // Close a non-persistent drawer when the page & route are changed.
    if (!this.$.drawer.persistent) {
      this.$.drawer.close();
    }
  }

  _pageChanged(page) {
    // Import the page component on demand.
    //
    // Note: `polymer build` doesn't like string concatenation in the import
    // statement, so break it up.
    switch (page) {
      case 'view-status':
        import('./view-status.js');
        break;
      case 'view-services':
        import('./view-services.js');
        break;
      case 'view-agents':
        import('./view-agents.js');
        break;
      case 'view404':
        import('./my-view404.js');
        break;
    }
  }

  ready() {
    super.ready();
    let appThis = this;

    this._loadUrl = document.URL; // there's definitely better ways to do this, but I have no idea

    this.$.ajaxValidateSession.generateRequest(); // validate old session

    this.$.signin.addEventListener('signed-in', function(event) { appThis.loginOidc(event.detail); });

    // should this sign the user out of las2peer too?
    // I guess not, since that session is separate and in principle unrelated
    this.$.signin.addEventListener('signed-out', e => appThis._oidcUser = null);

    // trigger the hidden, real submit button
    this.$.loginButton.addEventListener('click', function() { appThis.$.loginSubmitButton.click(); });

    this.$.loginForm.addEventListener('submit', function(event) { event.preventDefault(); appThis.loginUseridPassword(event); });
  }

  showLoginDialog() {
    // before showing the dialog, check whether OIDC is already logged in
    // if yes, attempt to log in to las2peer with access token
    // if no or failure, open dialog
    if (this._oidcUser && this.oidcTokenStillValid(this._oidcUser)) {
      this.loginOidc(this._oidcUser)
      // if there is an error, the error dialog should be triggered
      // so we should not have to handle anything here
    } else {
      this.$.loginDialog.open();
    }
  }

  oidcTokenStillValid(userObject) {
    // it's possible that the OIDC element triggers a logout when the token
    // expires, maybe. I don't know. if yes, this would be unnecessary
    return (userObject.expires_at * 1000) > Date.now();
  }

  destroySession() {
    this.$.ajaxDestroySession.generateRequest();
  }

  loginOidc(userObject) {
    console.log("OIDC sign-in triggered, logging in to las2peer ...");
    console.log("[DEBUG] OIDC user obj:" + userObject);
    try {
      if (userObject.token_type !== "Bearer") throw "unexpected OIDC token type, fix me";
      let accessToken = userObject.access_token;

      this._oidcUser = userObject;

      let req = this.$.ajaxLogin;
      req.headers = { Authorization: 'Bearer ' + accessToken };
      req.generateRequest();
    } catch (err) {
      this._handleError(userObject, "login failed", err)
    }
  }

  loginUseridPassword(event) {
    let req = this.$.ajaxLogin;
    req.headers = { Authorization: 'Basic ' + btoa(this.$.useridField.value + ':' + this.$.passwordField.value) };
    req.generateRequest();
  }

  _keyPressedLogin(event) {
    if (event.which == 13 || event.keyCode == 13) {
      this.$.loginSubmitButton.click();
      return false;
    }
    return true;
  }

  _handleLoginResponse(event) {
    console.log("login response: ", event);
    let resp = event.detail.response;
    if (resp && resp.hasOwnProperty('agentid')) {
      this._agentid = resp.agentid;
      console.log("login successful. agent ID: " + this._agentid);
      this.$.loginDialog.close();
      this.$.useridField.value = '';
      this.$.passwordField.value = '';
    } else {
      this._handleError(event, "Bad response", "Login returned no agent ID")
    }
  }

  _handleLogoutResponse() {
    this._agentid = '';
  }

  _handleValidateResponse(event) {
    console.log("validate response: ", event);
    let resp = event.detail.response;
    if (!resp || resp.agentid === undefined || resp.agentid === '') {
      this._handleLogoutResponse();
    } else {
      this._agentid = resp.agentid;
    }
  }

  _handleError(object, title, message) {
    if (!title && !message) {
      // try to get details of known possible errors
      let maybeDetail = (object || {}).detail;
      let maybeError = (maybeDetail || {}).error;
      let maybeXhr = ((maybeDetail || {}.request) || {}).xhr;

      if (maybeXhr.readyState === 4 && maybeXhr.status === 0) { // network issues
        title = 'Network Connection Error';
        message = 'Could not connect to: ' + object.detail.request.url;
      } else if (maybeXhr.response && maybeXhr.response.msg) {
        title = maybeXhr.status + " - " + maybeXhr.statusText;
        message = maybeXhr.response.msg;
      } else if (maybeError.message) {
        title = maybeXhr.status + " - " + maybeXhr.statusText;
        message = maybeError.message;
      } else {
        title = "Unknown error";
        message = "Could not determine type of error, check manually in console"
      }
    }
    console.log(title + ' - ' + message);
    console.log("[DEBUG] object which apparently caused error: " + object);
    this._error = { title: title, msg: message, obj: object };
  }

  _errorChanged(error) {
    this.$.las2peerErrorDialog.close(); // otherwise the dialog is rendered in wrong place
    this.$.las2peerErrorDialogTitle.innerHTML = error.title;
    this.$.las2peerErrorDialogMessage.innerHTML = error.msg;
    this.$.las2peerErrorDialog.open();
  }
}

window.customElements.define('node-frontend', NodeFrontend);
