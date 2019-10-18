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
          width: 6.5em;
          margin-right: 1em;
        }
        details {
          cursor: pointer;
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
        <div style="float: right; width: 15em; margin-top: -8px">
          <paper-input label="search" id="filterField" on-value-changed="_triggerFilter" value=""></paper-input>
        </div>
        <h1>Services in this Network</h1>

        <p hidden$="[[_toBool(_services)]]">
          There are no services published in this network.<br/>
          Feel free to use the <a href="[[rootPath]]publish-service">Publish Service</a> tab.
        </p>
        <template id="serviceList" is="dom-repeat" items="[[_services]]" as="service" sort="_sort" filter="_filter">
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
                      Service consists of [[_count(release.supplement.class)]] microservice[[_pluralS(release.supplement.class)]]<br/>
                      [[_countRunningLocally(release)]] running locally on this node, [[_countInstancesRunningRemoteOnly(release)]] running remotely in network
                      <div hidden="[[!_fullyAvailableLocally(release)]]">
                        Service available locally, authenticity verified
                        <iron-icon icon="hardware:security" title="Running locally"></iron-icon>
                      </div>
                      <div hidden="[[_fullyAvailableLocally(release)]]">
                        <div hidden="[[!_fullyAvailableAnywhere(release)]]">
                          Service available remotely on other nodes
                          <iron-icon icon="icons:cloud" title="Running on network nodes"></iron-icon>
                        </div>
                      </div>
                      <div hidden="[[_fullyAvailableAnywhere(release)]]">
                        Service not available
                      </div>
                      <!--
                      [[_countRunningLocally(release)]] of [[_count(release.supplement.class)]] Service classes running on this node
                      <iron-icon icon="hardware:security" title="Running locally"></iron-icon><br/>
                      <span hidden$="[[_fullyAvailableLocally(release)]]">
                      [[_countRunningRemoteOnly(release)]] of [[_countMissingLocally(release)]] running remotely in network
                      <iron-icon icon="icons:cloud" title="Running on network nodes"></iron-icon>
                      </span>
                      -->
                    </div>
                  </summary>
                  <ul style="list-style: none"><!-- TODO: this could/should actually be an HTML table, for once -->
                      <li>
                        <div style="display: inline-block; vertical-align: top; width: 17em; overflow: hidden">
                          <strong>Microservice</strong>
                        </div>
                        <ul style="display: inline-block; list-style: none; padding-left: 0">
                          <li style="margin-left: 0">
                            <span class="nodeId"><iron-icon icon="hardware:device-hub" title="Running on Node"></iron-icon> <strong>Node ID</strong></span>
                            <span class="time"><iron-icon icon="device:access-time" title="Last Announcement"></iron-icon> <strong>Last announced</strong></span>
                          </li>
                        </ul>
                      </li>
                    <template is="dom-repeat" items="[[_split(release.supplement.class)]]" as="class">
                      <li>
                        <div style="display: inline-block; vertical-align: top; width: 17em; overflow: hidden">
                          [[class]]
                          <iron-icon hidden$="[[!_hasLocalRunningInstance(release.instances, class)]]" icon="hardware:security" title="Running locally"></iron-icon>
                          <iron-icon hidden$="[[!_hasOnlyRemoteRunningInstance(release.instances, class)]]" icon="icons:cloud" title="Running on network nodes"></iron-icon>
                        </div>
                        <ul style="display: inline-block; list-style: none; padding-left: 0">
                          <span hidden$="[[_hasRunningInstance(release.instances, class)]]">not running</span>
                          <template is="dom-repeat" items="[[_filterInstances(release.instances, class)]]" as="instance">
                            <li style="margin-left: 0">
                              <span class="nodeId">[[instance.nodeId]]</span>
                              <span class="time">[[_toHumanDate(instance.announcementEpochSeconds)]]</span>
                            </li>
                          </template>
                        </ul>
                      </li>
                    </template>
                  </ul>
                  <span style="margin-right:1em"><iron-icon icon="hardware:security" title="Running locally"></iron-icon> Microservice running locally</span>
                  <span><iron-icon icon="icons:cloud" title="Running on network nodes"></iron-icon> Microservice running remotely only</span>
                </details>
              </div>
              <div class="card-actions">
                  <paper-button on-click="_handleStartButton"
                                data-args$="[[service.name]]#[[_classesNotRunningLocally(release)]]@[[release.version]]">Start on this Node</paper-button>
                  <paper-button on-click="_handleStopButton"
                                disabled$="[[!_countRunningLocally(release)]]"
                                data-args$="[[service.name]]#[[release.supplement.class]]@[[release.version]]">Stop</paper-button>
                  <paper-button hidden$="[[!release.supplement.vcsUrl]]"
                                on-click="_handleVcsButton"
                                data-args$="[[release.supplement.vcsUrl]]">View source code</paper-button>
                  <paper-button hidden$="[[!release.supplement.frontendUrl]]"
                                on-click="_handleFrontendButton"
                                disabled$="[[!_fullyAvailableAnywhere(release)]]"
                                data-args$="[[_frontendUrlIfServiceAvailable(release)]]">Open front-end</paper-button>
              </div>
            </paper-card>
          </template>
        </template>
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

  _sort(service, otherService) {
    try {
      // alphabetically by packagename
      return (service.name < otherService.name) ? -1 : 1;
    } catch(err) {
      return -1; // whatever
    }
  }

  _filter(service) {
    try {
      let query = this.$.filterField.value.trim();
      if (query.length < 1) return true;
      let serviceAsJson = JSON.stringify(service);
      return serviceAsJson.match(new RegExp(query, 'i'));
    } catch(err) {
      return true;
    }
  }

  _triggerFilter(event) {
    this.$.serviceList.render();
  }

  _stringify(obj) {
    return JSON.stringify(obj);
  }

  _toArray(obj) {
    return Object.keys(obj).map(k => ({ name: k, value: obj[k] }));
  }

  _toBool(obj) {
    return !!obj;
  }

  _split(stringWithCommas) {
    return (stringWithCommas || "").split(',');
  }

  _count(stringWithCommas) {
    return this._split(stringWithCommas).length
  }

  _pluralS(stringWithCommas) {
    return (this._count(stringWithCommas) > 1) ? "s" : "";
  }

  _toHumanDate(epochSeconds) {
    return new Date(epochSeconds * 1000).toLocaleString();
  }

  _getLatestVersionNumber(obj) {
    // NOTE: sorting issue fixed
    let latestVersion = Object.keys(obj).map( a => a.split('.').map( n => +n+1000000 ).join('.') ).sort()
        .map( a => a.split('.').map( n => +n-1000000 ).join('.') ).reverse()[0];
    return latestVersion;
  }

  _getLatest(obj) {
    let latestVersionNumber = this._getLatestVersionNumber(obj);
    let latestRelease = obj[latestVersionNumber];
    // version number is key, let's add it so we can access it
    (latestRelease || {}).version = latestVersionNumber;
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

  _hasLocalRunningInstance(instances, serviceClass) {
    return this._filterInstances(instances, serviceClass).filter(i => i.nodeId === (this._nodeId || {}).id).length > 0;
  }

  _hasOnlyRemoteRunningInstance(instances, serviceClass) {
    return this._hasRunningInstance(instances, serviceClass) && !this._hasLocalRunningInstance(instances, serviceClass);
  }

  _classesNotRunningAnywhere(release) {
    let classes = this._split((release.supplement || {}).class)
    let missing = classes.filter(c => {
      let instancesOfClass = (release.instances || []).filter(i => i.className === c);
      return instancesOfClass < 1;
    });
    return missing;
  }

  _classesNotRunningLocally(release) {
    let classes = this._split((release.supplement || {}).class);
    let missing = classes.filter(c => {
      let localInstancesOfClass = (release.instances || []).filter(i => i.className === c && i.nodeId === (this._nodeId || {}).id);
      return localInstancesOfClass < 1;
    });
    return missing;
  }

  // uh yeah, there's prettier ways to handle this
  _classesNotRunningLocallySeparatedByCommas(release) {
    return this._classesNotRunningLocally(release).join(',');
  }

  _countRunning(release) {
    let classes = this._split((release.supplement || {}).class);
    let missing = this._classesNotRunningAnywhere(release);
    return classes.length - missing.length;
  }

  _countRunningLocally(release) {
    let classes = this._split((release.supplement || {}).class);
    let missing = this._classesNotRunningLocally(release);
    return classes.length - missing.length;
  }

  _countMissingLocally(release) {
   return this._count((release.supplement || {}).class) - this._countRunningLocally(release);
  }

  _countRunningRemoteOnly(release) {
    return this._countRunning(release) - this._countRunningLocally(release);
  }

  // this counts several instances of a service class (in contrast, most other methods here ignore duplicates)
  _countInstancesRunningRemoteOnly(release) {
    return (release.instances || "").length - this._countRunningLocally(release);
  }

  _fullyAvailableAnywhere(release) {
    return this._classesNotRunningAnywhere(release).length === 0;
  }

  _fullyAvailableLocally(release) {
    return this._countMissingLocally(release) === 0;
  }

  _frontendUrlIfServiceAvailable(release) {
    if (this._fullyAvailableAnywhere(release)) {
      return (release.supplement || {}).frontendUrl;
    } else {
      return false;
    }
  }

  _keyPressedUploadService(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.uploadService();
      return false;
    }
    return true;
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

  _handleVcsButton(event) {
    if (event.target.getAttribute('data-args')) {
      window.open(event.target.getAttribute('data-args'));
    }
  }

  _handleFrontendButton(event) {
    if (event.target.getAttribute('data-args')) {
      window.open(event.target.getAttribute('data-args'));
    }
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
