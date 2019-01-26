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
import '@polymer/paper-tooltip/paper-tooltip.js';
import './shared-styles.js';

class ServicePublishView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxNodeId"
                 auto
                 url$="[[apiEndpoint]]/services/node-id"
                 handleAs="json"
                 last-response="{{_nodeId}}"
                 on-error="_handleError"
                 debounce-duration="300"></iron-ajax>
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
                 last-response="{{_communityTags}}"
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
                 on-error="_handleError"></iron-ajax>
      <iron-ajax id="ajaxStopService"
                 method="POST"
                 url$="[[apiEndpoint]]/services/stop"
                 handle-as="text"
                 on-error="_handleError"></iron-ajax>

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
      </style>

      <div class="card">
        <h1>Publish Service</h1>
        <p>Publish a service in the network by uploading its JAR file and providing some metadata.<p>
        <p>The service package name will automatically be registered to your name, if it isn’t already. Further releases can only be uploaded by you.</p>
        <p>The additional metadata will help users discover your service and its features. The name should be a human-readable variant of the package name. The description should consist of a few short sentences.</p>
        <iron-form on-keypress="_keyPressedUploadService">
          <paper-input label="JAR file" id="serviceUploadFile" disabled="[[_submittingUpload]]" type="file" required="true"></paper-input>
          <paper-input label="Service classes to start (comma-separated)" id="serviceUploadClass" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Name" id="serviceUploadName" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Description" id="serviceUploadDescription" disabled="[[_submittingUpload]]" required="true"></paper-input>
          <paper-input label="Source code URL (e.g., GitHub project)" id="serviceUploadVcsUrl" disabled="[[_submittingUpload]]"></paper-input>
          <paper-input label="Front-end URL" id="serviceUploadFrontendUrl" disabled="[[_submittingUpload]]"></paper-input>
          <paper-button raised on-tap="uploadService" disabled="[[_submittingUpload]]">Publish Service</paper-button>
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
      _nodeId: { type: Object }, // nested as .id FIXME
      _services: { type: Object },
      _communityTags: { type: Object },
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
    this.$.ajaxNodeId.generateRequest();
    this.$.ajaxServiceData.generateRequest();
    this.$.ajaxCommunityTags.generateRequest();
  }

  _stringify(obj) {
    return JSON.stringify(obj);
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

    this.error = { title: errorTitle, msg: errorMsg, obj: event };
  }
}

window.customElements.define('service-publish-view', ServicePublishView);
