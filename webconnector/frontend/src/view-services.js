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

class ServicesView extends PolymerElement {
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
        .service .nodeId, .service .time {
          display: inline-block;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .service .nodeId {
          width: 8em;
        }
      </style>
      <custom-style>
        <style is="custom-style">
          paper-tooltip.large {
            --paper-tooltip: {
              font-size: medium;
            }
          }
        </style>
      </custom-style>

      <div class="card">
        <h2>Services in this Network</h2>

        <template is="dom-repeat" items="[[_services]]" as="service">
          <template is="dom-repeat" items="[[_getLatestAsArray(service.releases)]]" as="release">
            <!-- we actually just want a single item here: the latest release. but I don't know how to do that without abusing repeat like this -->
            <paper-card heading$="[[release.supplement.name]]" style="width: 100%;margin-bottom: 1em" class="service">
              <div class="card-content" style="padding-top: 0px">
                <div style="margin-bottom: 8px">
                  <span class="package"><iron-icon icon="icons:archive" title="Part of package"></iron-icon>[[service.name]]</span>
                </div>
                <div>Author: <span class="author">[[service.authorName]]</span></div>
                <div>
                  Latest version: <span class="version">[[release.version]]</span>
                  published <span class="timestamp">[[_toHumanDate(release.publicationEpochSeconds)]]</span>
                  <span class="history">
                    <iron-icon icon="icons:info" title="Release history"></iron-icon>
                    <paper-tooltip position="right" class="large">
                      Release History<br/>
                      <ul>
                        <template is="dom-repeat" items="[[_toArray(service.releases)]]" as="version">
                          <li>[[version.name]] at [[_toHumanDate(version.value.publicationEpochSeconds)]]</li>
                        </template>
                      </ul>
                    </paper-tooltip>
                  </span>
                </div>
                <p class="description">[[release.supplement.description]]</p>
                <details>
                  <summary>
                    <div style="display: inline-block; vertical-align: top">
                      [[_countRunningLocally(release)]] of [[_count(release.supplement.class)]] Service classes running on this node
                      <iron-icon icon="hardware:security" title="Running on network nodes"></iron-icon><br/>
                      <span hidden$="[[_fullyAvailableLocally(release)]]">
                      [[_countRunningRemoteOnly(release)]] of [[_countMissingLocally(release)]] running remotely in network
                      <iron-icon icon="icons:cloud" title="Running on network nodes"></iron-icon>
                      </span>
                    </div>
                  </summary>
                  <ul style="list-style: none">
                    <template is="dom-repeat" items="[[_split(release.supplement.class)]]" as="class">
                      <li>
                        <div style="display: inline-block; vertical-align: top; width: 15em; overflow: hidden">[[class]]</div>
                        <ul style="display: inline-block; list-style: none; padding-left: 0">
                          <span hidden$="[[_hasRunningInstance(release.instances, class)]]">—</span>
                          <template is="dom-repeat" items="[[_filterInstances(release.instances, class)]]" as="instance">
                            <li style="margin-left: 0">
                              <span class="nodeId"><iron-icon icon="hardware:device-hub" title="Running on Node"></iron-icon> [[instance.nodeId]]</span>
                              <span class="time"><iron-icon icon="device:access-time" title="Last Announcement"></iron-icon> [[_toHumanDate(instance.announcementEpochSeconds)]]</span>
                            </li>
                          </template>
                        </ul>
                      </li>
                    </template>
                  </ul>
                </details>
              </div>
              <div class="card-actions">
                  <paper-button on-click="_handleStartButton" data-args$="[[service.name]]#[[_classesNotRunningLocally(release)]]@[[release.version]]">Start on this Node</paper-button>
                  <paper-button on-click="_handleStopButton" data-args$="[[service.name]]#[[release.supplement.class]]@[[release.version]]">Stop</paper-button>
                  <a href$="[[release.supplement.vcsUrl]]" hidden$="[[!release.supplement.vcsUrl]]" target="_blank" tabindex="-1"><paper-button>View source code</paper-button></a>
                  <a href$="[[release.supplement.frontendUrl]]" hidden$="[[!release.supplement.frontendUrl]]" target="_blank" tabindex="-1"><paper-button>Open front-end</paper-button></a>
              </div>
            </paper-card>
          </template>
        </template>
      </div>

      <div class="card">
        <h2>Upload and Register Service</h2>
        <p>Release a service in the network by uploading its JAR file and providing some metadata.<p>
        <p>The service package name will automatically be registered to your name, if it isn’t already. Further releases can only be uploaded by you.</p>
        <p>The additional metadata will help users discover your service and its features. The name should be a human-readable variant of the package name. The description should consist of a few short sentences.</p>
        <iron-form on-keypress="_keyPressedUploadService">
          <paper-input label="JAR file" id="serviceUploadFile" disabled="[[_submittingUpload]]" type="file" required="true"></paper-input>
          <paper-input label="Service classes to start (comma-separated)" id="serviceUploadClass" disabled="[[_submittingUpload]]" required="true"></paper-input>
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

  _toArray(obj) {
    return Object.keys(obj).map(k => ({ name: k, value: obj[k] }));
  }

  _split(stringWithCommas) {
    return stringWithCommas.split(',');
  }

  _count(stringWithCommas) {
    return this._split(stringWithCommas).length
  }

  _toHumanDate(epochSeconds) {
    return new Date(epochSeconds * 1000).toLocaleString();
  }

  _getLatestVersionNumber(obj) {
    // FIXME: use proper semver sort
    let latestVersion = Object.keys(obj).sort().reverse()[0];
    return latestVersion;
  }

  _getLatest(obj) {
    let latestVersionNumber = this._getLatestVersionNumber(obj);
    let latestRelease = obj[latestVersionNumber];
    // version number is key, let's add it so we can access it
    latestRelease.version = latestVersionNumber;
    return latestRelease;
  }

  _getLatestAsArray(obj) {
    return [this._getLatest(obj)];
  }

  _filterInstances(instances, serviceClass) {
    return instances.filter(i => i.className === serviceClass);
  }

  _hasRunningInstance(instances, serviceClass) {
    return this._filterInstances(instances, serviceClass).length > 0
  }

  _classesNotRunningAnywhere(release) {
    let classes = this._split(release.supplement.class)
    let missing = classes.filter(c => {
      let instancesOfClass = release.instances.filter(i => i.className === c);
      return instancesOfClass < 1;
    });
    return missing;
  }

  _classesNotRunningLocally(release) {
    let classes = this._split(release.supplement.class)
    let missing = classes.filter(c => {
      let localInstancesOfClass = release.instances.filter(i => i.className === c && i.nodeId === this._nodeId.id);
      return localInstancesOfClass < 1;
    });
    return missing;
  }

  // uh yeah, there's prettier ways to handle this
  _classesNotRunningLocallySeparatedByCommas(release) {
    return this._classesNotRunningLocally(release).join(',');
  }

  _countRunning(release) {
    let classes = this._split(release.supplement.class);
    let missing = this._classesNotRunningAnywhere(release);
    return classes.length - missing.length
  }

  _countRunningLocally(release) {
    let classes = this._split(release.supplement.class);
    let missing = this._classesNotRunningLocally(release);
    return classes.length - missing.length
  }

  _countMissingLocally(release) {
   return this._count(release.supplement.class) - this._countRunningLocally(release)
  }

  _countRunningRemoteOnly(release) {
    return this._countRunning(release) - this._countRunningLocally(release)
  }

  _fullyAvailableLocally(release) {
    return this._countMissingLocally(release) === 0
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

  _handleStartButton(event) {
    let arg = event.target.getAttribute('data-args');
    let packageName = arg.split('#')[0];
    let version = arg.split('@')[1];
    let classes= arg.split('#')[1].split('@')[0].split(',');

    for (let c of classes) {
      this.startService(packageName + '.' + c, version);
    }
  }

  startService(fullClassName, version) {
    let req = this.$.ajaxStartService;
    req.params = { 'serviceName': fullClassName, 'version': version };
    console.log("Requesting start of '" + fullClassName + "'@'" + version + "' ...");
    req.generateRequest();
  }

  _handleStopButton(event) {
    let arg = event.target.getAttribute('data-args');
    let packageName = arg.split('#')[0];
    let version = arg.split('@')[1];
    let classes= arg.split('#')[1].split('@')[0].split(',');

    for (let c of classes) {
      this.stopService(packageName + '.' + c, version);
    }
  }

  stopService(fullClassName, version) {
    let req = this.$.ajaxStopService;
    req.params = { 'serviceName': fullClassName, 'version': version };
    console.log("Requesting stop of '" + fullClassName + "'@'" + version + "' ...");
    req.generateRequest();
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
