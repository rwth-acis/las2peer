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
import '@polymer/iron-ajax/iron-ajax.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/paper-icon-button/paper-icon-button.js';
import './shared-styles.js';

class StatusView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxStatus"
                 url$="[[apiEndpoint]]/status"
                 handle-as="json"
                 last-response="{{_status}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
      </style>
      <custom-style>
        <style is="custom-style">
          .flex-horizontal {
            @apply --layout-horizontal;
          }
          .flexchild {
            @apply --layout-flex;
          }
        </style>
      </custom-style>

      <div class="card">
        <h1>Node Status <paper-icon-button icon="refresh" on-tap="refreshStatus"></paper-icon-button></h1>

        <div class="container flex-horizontal">
          <div class="flexchild">
            <!-- NODE STATUS -->
            <strong>Uptime</strong>: [[_status.uptime]] <br />
            <strong>CPU Load</strong>: [[_status.cpuLoad]]% <br />
            <strong>Local Storage</strong>: <meter value="[[_status.storageSize]]" min="0" max="[[_status.maxStorageSize]]"></meter> [[_status.storageSizeStr]] of [[_status.maxStorageSizeStr]] used <br />
            <strong>Node ID</strong>: [[_status.nodeId]] <br />
          </div>
          <div class="flexchild">
            <!-- NODE ADMIN INFO + DESCRIPTION-->
            <strong>Node Administrator</strong>: [[_status.nodeAdminName]] <[[_status.nodeAdminEmail]]> <br />
            <strong>Node Organization</strong>: 
              <p>[[_status.nodeOrganization]]</p>
            <strong>Node Description</strong>:
              <p>[[_status.nodeDescription]]</p>
          </div>
        </div>

        <h3>Known Nodes In Network</h3>
        <div class="container flex-horizontal">
          <template is="dom-repeat" items="[[_status.otherNodeInfos]]">
            <div class="flexchild">
              <strong>NodeID:</strong> [[item.nodeID]] <br />
              <template is="dom-if" if="[[item.nodeInfo]]">
                <template is="dom-if" if="[[item.nodeInfo.node-admin]]">
                  <strong>NodeAdmin:</strong> [[item.nodeInfo.node-admin]] <br />
                </template>
                <template is="dom-if" if="[[item.nodeInfo.services]]">
                  <strong>Services:</strong> <br />
                  <ul>
                    <template is="dom-repeat" items="[[item.nodeInfo.services]]" as="service">
                      [[service]]
                    </template>
                  </ul>
                </template>
              </template>
            </div>
          </template>
        </div>

        <!--
        <h3>Local Running Services</h3>
        <table width="100%">
          <tr><th>Name</th><th width="10%">Version</th><th style="width: 1%; white-space: nowrap">Swagger</th></tr>
          <template is="dom-repeat" items="[[_status.localServices]]">
            <tr>
              <td><b style="color: #0A0">[[item.name]]</b></td>
              <td>[[item.version]]</td>
              <td style="width: 1%; white-space: nowrap">
                <template is="dom-if" if="[[item.swagger]]">
                  <a href="[[apiEndpoint]]//swagger-ui/index.html?url=[[item.swagger]]" tabindex="-1"><paper-button raised title="Show Swagger API Doc" style="background-color: #89bf04"><iron-icon src="/las2peer/swagger-ui/favicon-32x32.png"></iron-icon></paper-button></a>
                </template>
              </td>
            </tr>
          </template>
        </table>
        -->

        <h2>Secure Node SSL Encryption</h2>
        <p>If you trust this node, it’s recommended to import the node’s certificate authority into your browsers trust store. You can download the certificate authority file <a href="[[apiEndpoint]]/cacert">here</a>.</p>
      </div>
    `;
  }

  static get properties() {
    return {
      apiEndpoint: { type: String, notify: true },
      agentId: { type: String, notify: true },
      error: { type: Object, notify: true },
      _status: {
        type: Object,
        value: {
          nodeAdminName: "...",
          nodeAdminEmail: "...@...",
          nodeOrganization: "...",
          nodeDescription: "...",
          nodeId: "...",
          cpuLoad: "...",
          storageSize: 0,
          maxStorageSize: 1,
          storageSizeStr: "...",
          maxStorageSizeStr: "...",
          uptime: "..."
        }
      }
    };
  }

  ready() {
    super.ready();
    let appThis = this;
    window.setTimeout(function() { appThis.refreshStatus(); }, 1);
    window.setInterval(function() { appThis.refreshStatus(); }, 5000);
  }

  refreshStatus() {
    this.$.ajaxStatus.generateRequest();
  }

  _handleError(event) {
    console.log(event);
    let errorTitle = 'Error', errorMsg = 'An unknown error occurred. Please check console output.';
    if (event.detail.request.xhr.readyState == 4 && event.detail.request.xhr.status == 0) { // network issues
      errorTitle = 'Network Connection Error';
      errorMsg = 'Could not connect to: ' + event.detail.request.url;
    } else if (event.detail.request.xhr.response && event.detail.request.xhr.response.msg) {
      errorTitle = event.detail.request.xhr.status + " - " + event.detail.request.xhr.statusText;
      errorMsg = event.detail.request.xhr.response.msg;
    } else if (event.detail.error && event.detail.error.message) {
      errorTitle = event.detail.request.xhr.status + " - " + event.detail.request.xhr.statusText;
      errorMsg = event.detail.error.message;
    }
    console.log(errorTitle + ' - ' + errorMsg);
    // do not set error dialog params to prevent dialog spamming
  }
}

window.customElements.define('status-view', StatusView);
