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
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/device-icons.js';
import '@polymer/iron-icons/hardware-icons.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-input/paper-input.js';
import './shared-styles.js';

class ServicesView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxServiceData"
                 auto
                 url$="[[apiEndpoint]]/services/services"
                 handle-as="json"
                 last-response="{{_services}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>
      <iron-ajax id="ajaxCommunityTags"
                 auto
                 url$="[[apiEndpoint]]/services/registry/tags"
                 handleAs="json"
                 last-response="{{communityTags}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>
      <iron-ajax id="ajaxUploadService"
                 method="POST"
                 url$="[[apiEndpoint]]/services/upload"
                 handle-as="json"
                 on-response="_handleUploadServiceResponse"
                 on-error="_handleError"
                 loading = "{{_submittingUpload}}"></iron-ajax>
      <iron-ajax id="ajaxStartService"
                 method="POST"
                 url$="[[apiEndpoint]]/services/start"
                 handle-as="text"
                 on-response="_handleStartServiceResponse"
                 on-error="_handleError"></iron-ajax>

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
        .service .node, .service .time {
          width: 15em;
          display: inline-block;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      </style>

      <div class="card">
        <h2>Services in this Network</h2>

        <template is="dom-repeat" items="[[_services]]">
          <paper-card heading$="[[_getLatestName(item.releases)]]" style="width: 100%;margin-bottom: 1em" class="service">
            <div class="card-content">
              <div>Author: [[item.authorName]]</div>
              <div>Latest version: [[_getLatestVersionNumber(item.releases)]]</div>
              <p>[[_getLatestDescription(item.releases)]]</p>
              <div><iron-icon icon="icons:archive" title="Part of package"></iron-icon> [[item.name]]</div>
              <ul>
                <template is="dom-repeat" items="[[_getLatestInstances(item.releases)]]">
                  <li>
                    <span class="node"><iron-icon icon="hardware:device-hub" title="Running on Node"></iron-icon> [[item.nodeId]]</span>
                    <span class="time"><iron-icon icon="device:access-time" title="Last Announcement"></iron-icon> [[item.humanTime]]</span>
                  </li>
                </template>
              </ul>
            </div>
            <div class="card-actions">
                <paper-button on-click="startService" data-args$="[[item.name]].[[_getLatestDefaultClass(item.releases)]],[[_getLatestVersionNumber(item.releases)]]">Start on this Node</paper-button>
                <a href$="[[_getLatestVcsUrl(item.releases)]]" hidden$="[[!_getLatestVcsUrl(item.releases)]]" target="_blank" tabindex="-1"><paper-button>View source code</paper-button></a>
                <a href$="[[_getLatestFrontendUrl(item.releases)]]" hidden$="[[!_getLatestFrontendUrl(item.releases)]]" target="_blank" tabindex="-1"><paper-button>Open front-end</paper-button></a>
            </div>
          </paper-card>
        </template>
      </div>

      <div class="card">
        <h2>Upload and Register Service</h2>
        <p>Release a service in the network by uploading its JAR file and providing some metadata.<p>
        <p>The service package name will automatically be registered to your name, if it isn’t already. Further releases can only be uploaded by you.</p>
        <p>The additional metadata will help users discover your service and its features. The name should be a human-readable variant of the package name. The description should consist of a few short sentences.</p>
        <iron-form on-keypress="_keyPressedUploadService">
          <paper-input label="JAR file" id="serviceUploadFile" disabled="[[_submittingUpload]]" type="file" required="true"></paper-input>
          <paper-input label="Default class to start" id="serviceUploadClass" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Name" id="serviceUploadName" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Description" id="serviceUploadDescription" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Source code URL (e.g., GitHub project)" id="serviceUploadVcsUrl" disabled="[[_submittingUpload]]"></paper-input>
          <paper-input label="Front-end URL" id="serviceUploadFrontendUrl" disabled="[[_submittingUpload]]"></paper-input>
          <paper-button raised on-tap="uploadService" disabled="[[_submittingUpload]]">Upload Service</paper-button>
        </iron-form>
        <div id="uploadServiceMsg" style="font-weight: bold"></div>
      </div>
    `;
  }

  static get properties() {
    return {
      apiEndpoint: { type: String, notify: true },
      agentId: { type: String, notify: true },
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

  _stringify(obj) {
    return JSON.stringify(obj);
  }

  _toArray(obj) {
    return Object.keys(obj).map(k => ({ name: k, value: obj[k] }));
  }

  // is this really the only way to get that stuff into the template? no nested function?
  // why the hell is this so ugly?? surely that's not right.
  _getLatestVersionNumber(obj) {
    // FIXME: use proper semver sort
    let latestVersion = Object.keys(obj).sort().reverse()[0];
    return latestVersion;
  }

  _getLatest(obj) {
    return obj[this._getLatestVersionNumber(obj)];
  }

  _getLatestInstances(obj) {
    return this._getLatest(obj).instances
  }

  _getLatestSupplement(obj) {
    return this._getLatest(obj).supplement;
  }

  _getLatestName(obj) {
    return this._getLatest(obj).supplement.name;
  }

  _getLatestDescription(obj) {
    return this._getLatest(obj).supplement.description;
  }

  _getLatestDefaultClass(obj) {
    return this._getLatest(obj).supplement.class;
  }

  _getLatestVcsUrl(obj) {
    return this._getLatest(obj).supplement.vcsUrl;
  }

  _getLatestFrontendUrl(obj) {
    return this._getLatest(obj).supplement.frontendUrl;
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

    let supplement = {
      'class': this.$.serviceUploadClass.inputElement.inputElement.value,
      'name': this.$.serviceUploadName.inputElement.inputElement.value,
      'description': this.$.serviceUploadDescription.inputElement.inputElement.value,
      'vcsUrl': this.$.serviceUploadVcsUrl.inputElement.inputElement.value,
      'frontendUrl': this.$.serviceUploadFrontendUrl.inputElement.inputElement.value,
    };
    req.body.append('supplement', JSON.stringify(supplement));

    req.generateRequest();
  }

  _handleUploadServiceResponse(event) {
    this.$.serviceUploadFile.value = '';
    this.$.uploadServiceMsg.innerHTML = event.detail.response.msg;
  }

  startService(event) {
    let args = event.target.getAttribute('data-args').split(',');
    let req = this.$.ajaxStartService;
    req.params = { 'serviceName': args[0], 'version': args[1] };
    console.log("Requesting start of '" + args[0] + "'@'" + args[1] + "' ...");
    req.generateRequest();
  }

  _handleStartServiceResponse(event) {
    // TODO
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
