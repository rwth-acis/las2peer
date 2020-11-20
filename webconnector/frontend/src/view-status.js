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
import '@polymer/paper-checkbox/paper-checkbox.js';
import '@polymer/paper-spinner/paper-spinner.js';
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
      <iron-ajax id="ajaxOtherNodesInfo"
                 url$="[[apiEndpoint]]/getOtherNodesInfo"
                 handle-as="json"
                 last-response="{{_otherNodeInfo}}"
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

          paper-checkbox {
            --paper-checkbox-label-color: #757575;
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
	    <strong>RAM Usage</strong>: <meter value="[[_status.ramLoad]]" min="0" max="[[_status.maxRamLoad]]"></meter> [[_status.ramLoadStr]] of [[_status.maxRamLoadStr]] used <br />
            <strong>Local Storage</strong>: <meter value="[[_status.storageSize]]" min="0" max="[[_status.maxStorageSize]]"></meter> [[_status.storageSizeStr]] of [[_status.maxStorageSizeStr]] used <br />
            <strong>Node ID</strong>: [[_status.nodeId]] <br />
          </div>
          <div class="flexchild">
            <!-- NODE ADMIN INFO + DESCRIPTION-->
            <strong>Node Hoster</strong>: [[_status.nodeAdminName]] 
              <template is="dom-if" if="[[_status.nodeAdminReputation]]">
                <custom-star-rating value="[[_status.nodeAdminReputation]]" readonly></custom-star-rating>
              </template>
              <template is="dom-if" if="[[!_status.nodeAdminReputation]]">
                <custom-star-rating disable-rating readonly></custom-star-rating>
              </template>
            <br /> 
            <strong>Node Hoster Email</strong> <[[_status.nodeAdminEmail]]> <br />
            <strong>Node Organization</strong>: 
              <p>[[_status.nodeOrganization]]</p>
            <strong>Node Description</strong>:
              <p>[[_status.nodeDescription]]</p>
          </div>
        </div>

        <h3>Known Nodes In Network <paper-checkbox id="checkbox" checked="{{queryAdvancedInfo}}" on-click="queryAdvancedClick">Query Extended Info</paper-checkbox></h3>
        <p>"<em>Extended info</em>" queries the <b>nodeInfo.xml</b> of the nodes in las2peer. <br /> 
        The provided list of services for each node is <u>not verified via the blockchain</u>, but has been included for backwards compatibility. <br />
        <small><em>This was necessary since some services (e.g. MobSOS) cannot be easily compressed to a jar file due to external requirements.</em></small></p>
        <div class="container">
          <template is="dom-if" if="[[queryAdvancedInfo]]">
            <template is="dom-repeat" items="[[_otherNodeInfo]]" as="otherNode">
              <p>
                <strong>NodeID:</strong> [[otherNode.nodeID]] <br />
                <template is="dom-if" if="[[otherNode.nodeInfo]]">
                  <template is="dom-if" if="[[otherNode.nodeInfo.admin-name]]">
                    <strong>NodeAdmin:</strong> [[otherNode.nodeInfo.admin-name]] 
                    
                    <template is="dom-if" if="[[otherNode.nodeAdminReputation]]">
                      <custom-star-rating value="[[otherNode.nodeAdminReputation]]" readonly></custom-star-rating>
                    </template>
                    <template is="dom-if" if="[[!otherNode.nodeAdminReputation]]">
                      <custom-star-rating disable-rating readonly></custom-star-rating>
                    </template>
                    
                    <br />
                  </template>
                  <template is="dom-if" if="[[otherNode.nodeInfo.services]]">
                    <strong>Services ([[otherNode.nodeInfo.service-count]]):</strong> <br />
                    <ul>
                      <template is="dom-repeat" items="[[otherNode.nodeInfo.services]]" as="service">
                        [[service.service-name]] @ [[service.service-version]]
                      </template>
                    </ul>
                  </template>
                </template>
              </p>
            </template>
          </template>
          <template is="dom-if" if="[[!queryAdvancedInfo]]">
            <ul>
              <template is="dom-repeat" items="[[_status.otherNodes]]">
                <li>[[item]]</li>
              </template>
            </ul>
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
      queryAdvancedInfo: { type: Boolean, value: false, notify: true },
      error: { type: Object, notify: true },
      _otherNodeInfo: { type: Object, notify: true },
      _status: {
        type: Object,
        value: {
          nodeAdminName: "...",
          nodeAdminEmail: "...@...",
          nodeOrganization: "...",
          nodeDescription: "...",
          nodeId: "...",
          cpuLoad: "...",
          ramLoad: 0,
          maxRamLoad: 1,
          ramLoadStr: "...",
          maxRamLoadStr: "...",
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
    window.setTimeout(function() { appThis.refreshStatus(); }, 1); // initial refresh
    window.setInterval(function() { appThis.refreshStatus(); }, 5000); // recurring refresh every 5 s
  }

  refreshStatus() {
    this.queryAdvancedClick();
    this.$.ajaxStatus.generateRequest();
  }

  queryAdvancedClick() {
    if ( this.queryAdvancedInfo )
    {
      this.$.ajaxOtherNodesInfo.generateRequest();
    }
  }

  _handleError(object, title, message) {
    window.rootThis._handleError(object, title, message)
  }
}

window.customElements.define('status-view', StatusView);
