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
import '@polymer/iron-form/iron-form.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-button/paper-button.js';
import './shared-styles.js';

class ServicesView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxServiceData"
                 auto
                 url="/las2peer/services/services"
                 handle-as="json"
                 last-response="{{_services}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>
      <iron-ajax id="ajaxCommunityTags"
                 auto
                 url="/las2peer/services/registry/tags"
                 handleAs="json"
                 last-response="{{communityTags}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>
      <iron-ajax id="ajaxUploadService"
                 method="POST"
                 url="/las2peer/services/upload"
                 handle-as="json"
                 on-response="_handleUploadServiceResponse"
                 on-error="_handleError"
                 loading = "{{_submittingUpload}}"></iron-ajax>

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
      </style>

      <div class="card">
        <h1>las2peer Services</h1>
        <h2>Registered Services</h2>
        These service names have been registered by some author on the blockchain-based service registry:
        <ul>
          <template is="dom-repeat" items="[[_toArray(_services.authors)]]">
            <li>[[item.name]] by <emph>[[item.value]]</emph></li>
          </template>
        </ul>

        <h2>Service Releases</h2>
        These specific versions have been published:
        <ul>
          <template is="dom-repeat" items="[[_toArray(_services.releases)]]">
            <li>[[item.name]] @
              <template is="dom-repeat" items="[[item.value]]">
                <strong>[[item.version]]</strong>
              </template>
            </li>
          </template>
        </ul>

        <h2>Service Deployments</h2>
        Running service instances on nodes in this network:
        <ul>
          <template is="dom-repeat" items="[[_toArray(_services.deployments)]]">
            <li>[[item.name]]
              <ul>
                <template is="dom-repeat" items="[[item.value]]">
                <li>[[item.packageName]].<strong>[[item.className]]</strong> @[[item.version]]
                  <ul>
                    <li>Running on Node <strong>[[item.nodeId]]</strong></li>
                    <li>Last announced at <strong>[[item.time]]</strong></li>
                  </ul>
                </li>
                </template>
              </ul>
            </li>
          </template>
        </ul>

        <h2>Community Tags</h2>
        These tags currently serve no function …
        <ul>
          <template is="dom-repeat" items="[[_toArray(communityTags)]]">
          <li><strong>[[item.name]]</strong>: “[[item.value]]”</li>
          </template>
        </ul>

        <h2>Upload Service</h2>
        Upload a service to the network.
        <iron-form on-keypress="_keyPressedUploadService">
          <paper-input label="service jar file" id="serviceUploadFile" disabled="[[_submittingUpload]]" type="file" required="true"></paper-input>
          <paper-button raised on-tap="uploadService" disabled="[[_submittingUpload]]">Upload Service</paper-button>
        </iron-form>
        <div id="uploadServiceMsg" style="font-weight: bold"></div>
      </div>
    `;
  }

  static get properties() {
    return {
      agentid: String,
      error: { type: Object, notify: true },
      _services: { type: Object },
      _submittingSearch: { type: Boolean },
      _submittingUpload: { type: Boolean }
    };
  }

  ready() {
    super.ready();
    let appThis = this;
    window.setTimeout(function() { appThis.refresh(); }, 1);
    window.setInterval(function() { appThis.refresh(); }, 5000);
  }

  refresh() {
    this.$.ajaxCommunityTags.generateRequest();
    this.$.ajaxServiceData.generateRequest();
  }

  _toArray(obj) {
    return Object.keys(obj).map(k => ({ name: k, value: obj[k] }));
  }

  _keyPressedUploadService(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.uploadService();
      return false;
    }
    return true;
  }

  uploadService(event) {
    let req = this.$.ajaxUploadService;
    req.body = new FormData();
    req.body.append('jarfile', this.$.serviceUploadFile.inputElement.inputElement.files[0]); // this is an input inside an iron-input inside a paper-input
    req.generateRequest();
  }

  _handleUploadServiceResponse(event) {
    this.$.serviceUploadFile.value = '';
    this.$.uploadServiceMsg.innerHTML = event.detail.response.msg;
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

window.customElements.define('services-view', ServicesView);
