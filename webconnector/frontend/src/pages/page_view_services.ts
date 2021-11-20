import { html, css, customElement, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import { request, RequestResponse } from '../helpers/request_helper.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/notification-icons.js';
import '../components/custom_star_rating.js';

@customElement('page-view-services')
export class PageHome extends PageElement {
  static styles = css`
    section {
      padding: 1rem;
    }
    :host {
      display: block;
      padding: 10px;
    }
    .service .nodeId,
    .service .nodeAdmin,
    .service .nodeAdminRating,
    .service .time {
      display: inline-block;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .service .nodeId,
    .service .nodeAdmin,
    .service .nodeAdminRating {
      margin-right: 1em;
    }
    .service .nodeId {
      width: 6.5em;
    }
    .service .nodeAdmin {
      min-width: 8em;
    }
    .service .nodeAdminRating {
      min-width: 120px;
    }
    details {
      cursor: pointer;
    }
  `;

  @property({ type: Object })
  services: Service[] = [];

  @property({ type: Number })
  nodeId: NodeId = {
    id: '',
    code: 0,
    text: '',
  };
  @property({ type: Boolean, attribute: true })
  _working = false;
  render() {
    return html`
      <section>
        <h1>Services in this Network</h1>
        <div>
          ${this.services.map(
            (service) => html`
              ${this._getLatestAsArray(service.releases).map(
                (release) => html`
                  <paper-card
                    heading=${service.name}
                    style="width: 100%;margin-bottom: 1em"
                    class="service"
                  >
                    <div class="card-content" style="padding-top: 0px">
                      <div style="margin-bottom: 8px">
                        <span class="package"
                          ><iron-icon
                            icon="icons:archive"
                            title="Part of package"
                          ></iron-icon
                          >${service.name}</span
                        >
                      </div>
                      <div>
                        Author:
                        <span class="author">${service.authorName}</span>
                        <template is="dom-if" if=${service.authorReputation}>
                          <custom-star-rating
                            value=${service.authorReputation}
                            readonly
                            single
                          ></custom-star-rating>
                        </template>
                        <template is="dom-if" if=${!service.authorReputation}>
                          <custom-star-rating
                            disable-rating
                            readonly
                            single
                          ></custom-star-rating>
                        </template>
                      </div>
                      <div>
                        Latest version:
                        <span class="version"
                          >${release == null ? '' : release.version}</span
                        >
                        published
                        <span class="timestamp"
                          >${this._toHumanDate(
                            release == null
                              ? 0
                              : release.publicationEpochSeconds
                          )}</span
                        >
                        <span class="history">
                          <iron-icon
                            icon="icons:info"
                            title="Release history"
                          ></iron-icon>
                          <paper-tooltip position="right" class="large">
                            Release History<br />
                            <ul>
                              ${this._toArray(service.releases).map(
                                (version) => html`
                                  <li>
                                    ${version.name} at
                                    ${this._toHumanDate(
                                      version.value.publicationEpochSeconds
                                    )}
                                  </li>
                                `
                              )}
                            </ul>
                          </paper-tooltip>
                        </span>
                      </div>
                      <p class="description">
                        ${release == null ? '' : release.supplement.description}
                      </p>
                      <details>
                        <summary>
                          <div
                            style="display: inline-block; vertical-align: top"
                          >
                            Service consists of
                            ${this._count(
                              release == null ? '' : release.supplement.class
                            )}
                            microservice${this._pluralS(
                              release == null ? '' : release.supplement.class
                            )}<br />
                            ${this._countRunningLocally(release)} running
                            locally on this node,
                            ${this._countInstancesRunningRemoteOnly(release)}
                            running remotely in network
                            <div
                              ?hidden=${!this._fullyAvailableLocally(release)}
                            >
                              Service available locally, authenticity verified
                              <iron-icon
                                icon="hardware:security"
                                title="Running locally"
                              ></iron-icon>
                            </div>
                            <div
                              ?hidden=${this._fullyAvailableLocally(release)}
                            >
                              <div
                                ?hidden=${!this._fullyAvailableAnywhere(
                                  release
                                )}
                              >
                                Service available remotely on other nodes
                                <iron-icon
                                  icon="icons:cloud"
                                  title="Running on network nodes"
                                ></iron-icon>
                              </div>
                            </div>
                            <div
                              hidden=${this._fullyAvailableAnywhere(release)}
                            >
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
                        <ul style="list-style: none">
                          <!-- TODO: this could/should actually be an HTML table, for once -->
                          <li>
                            <div
                              style="display: inline-block; vertical-align: top; width: 17em; overflow: hidden"
                            >
                              <strong>Microservice</strong>
                            </div>
                            <ul
                              style="display: inline-block; list-style: none; padding-left: 0"
                            >
                              <li style="margin-left: 0">
                                <span class="nodeId"
                                  ><iron-icon
                                    icon="hardware:device-hub"
                                    title="Running on Node"
                                  ></iron-icon>
                                  <strong>Node ID</strong></span
                                >
                                <span class="nodeAdmin"
                                  ><iron-icon
                                    icon="account-circle"
                                    title="Service Hoster"
                                  ></iron-icon>
                                  <strong>Service Hoster</strong></span
                                >
                                <span class="nodeAdminRating"
                                  ><iron-icon
                                    icon="face"
                                    title="Hoster Rating"
                                  ></iron-icon>
                                  <strong>Hoster Rating</strong></span
                                >
                                <span class="time"
                                  ><iron-icon
                                    icon="device:access-time"
                                    title="Last Announcement"
                                  ></iron-icon>
                                  <strong>Last announced</strong></span
                                >
                              </li>
                            </ul>
                          </li>

                          <li>
                            <div
                              style="display: inline-block; vertical-align: top; width: 17em; overflow: hidden"
                            >
                              ${this._split(release.supplement.class)}
                              <iron-icon
                                ?hidden=${!this._hasLocalRunningInstance(
                                  release.instances,
                                  this._split(release.supplement.class)[0]
                                )}
                                icon="hardware:security"
                                title="Running locally"
                              ></iron-icon>
                              <iron-icon
                                ?hidden=${!this._hasOnlyRemoteRunningInstance(
                                  release.instances,
                                  this._split(release.supplement.class)[0]
                                )}
                                icon="icons:cloud"
                                title="Running on network nodes"
                              ></iron-icon>
                            </div>
                            <ul
                              style="display: inline-block; list-style: none; padding-left: 0"
                            >
                              <span
                                hidden=${this._hasRunningInstance(
                                  release.instances,
                                  this._split(release.supplement.class)[0]
                                )}
                                >not running</span
                              >

                              ${this._filterInstances(
                                release.instances,
                                this._split(release.supplement.class)[0]
                              ).map(
                                (instance) =>
                                  html`
                                    <li style="margin-left: 0">
                                      <span class="nodeId"
                                        >${instance.nodeId}</span
                                      >
                                      <template
                                        is="dom-if"
                                        if=${instance.nodeInfo['admin-name']}
                                      >
                                        <span class="nodeAdmin">
                                          ${instance.nodeInfo['admin-name']}
                                        </span>
                                        <span class="nodeAdminRating">
                                          <template
                                            is="dom-if"
                                            if=${instance.hosterReputation}
                                          >
                                            <custom-star-rating
                                              value=${instance.hosterReputation}
                                              readonly
                                              single
                                            ></custom-star-rating>
                                          </template>
                                          <template
                                            is="dom-if"
                                            if=${!instance.hosterReputation}
                                          >
                                            <custom-star-rating
                                              disable-rating
                                              readonly
                                              single
                                            ></custom-star-rating>
                                          </template>
                                        </span>
                                      </template>
                                      <span class="time"
                                        >${this._toHumanDate(
                                          instance.announcementEpochSeconds
                                        )}</span
                                      >
                                    </li>
                                  `
                              )}
                            </ul>
                          </li>
                        </ul>
                        <span style="margin-right:1em"
                          ><iron-icon
                            icon="hardware:security"
                            title="Running locally"
                          ></iron-icon>
                          Microservice running locally</span
                        >
                        <span
                          ><iron-icon
                            icon="icons:cloud"
                            title="Running on network nodes"
                          ></iron-icon>
                          Microservice running remotely only</span
                        >
                      </details>
                    </div>
                    <div class="card-actions">
                      <paper-button
                        @click=${this._handleStartButton}
                        data-args="${service.name}#${this._classesNotRunningLocally(
                          release
                        )}@${release.version}"
                        ?disabled=${this._working}
                        >Start on this Node</paper-button
                      >
                      <paper-button
                        @click=${this._handleStopButton}
                        ?disabled=${!this._countRunningLocally(release)}
                        data-args="${service.name}#${release.supplement
                          .class}@${release.version}"
                        >Stop</paper-button
                      >
                      <paper-button
                        ?hidden="${!release.supplement.vcsUrl}"
                        @click=${this._handleVcsButton}
                        data-args="${release.supplement.vcsUrl}"
                        >View source code</paper-button
                      >
                      <paper-button
                        ?hidden="${!release.supplement.frontendUrl}"
                        @click=${this._handleFrontendButton}
                        ?disabled="${!this._fullyAvailableAnywhere(release)}"
                        data-args="${this._frontendUrlIfServiceAvailable(
                          release
                        )}"
                        >Open front-end</paper-button
                      >
                      <paper-spinner
                        style="padding: 0.7em;float: right;"
                        ?active=${this._working}
                      ></paper-spinner>
                    </div>
                  </paper-card>
                `
              )}
            `
          )}
        </div>
      </section>
    `;
  }
  firstUpdated() {
    this.fetchServicesInfo();
    this.fetchNodeId();
  }
  async fetchNodeId() {
    this.nodeId = await request<NodeId>(
      '/las2peer/services/node-id',
      {
        method: 'GET',
      }
    );
  }
  async fetchServicesInfo() {
    this.services = await request<any>(
      '/las2peer/services/services',
      {
        method: 'GET',
      }
    );
    console.log(this.services);
  }
  _handleStartButton(event: {
    target: { getAttribute: (arg0: string) => any };
  }) {
    const arg = event.target.getAttribute('data-args');
    const packageName = arg.split('#')[0];
    const version = arg.split('@')[1];
    const classes = arg.split('#')[1].split('@')[0].split(',');

    for (const c of classes) {
      this.startService(packageName + '.' + c, version);
    }
  }
  async startService(fullClassName: string, version: string) {
    const response = await request(
        '/las2peer/services/start?' +
        'serviceName=' +
        fullClassName +
        '&version=' +
        version,
      {
        method: 'POST',
      }
    );
    this._handleServiceStart(response);
    console.log(
      "Requesting start of '" + fullClassName + "'@'" + version + "' ..."
    );
  }
  _handleVcsButton(event: {
    target: { getAttribute: (arg0: string) => string | URL | undefined };
  }) {
    if (event.target.getAttribute('data-args')) {
      window.open(event.target.getAttribute('data-args'));
    }
  }
  _handleServiceStart(_event: RequestResponse) {
    // window.rootThis.checkStatus();
  }
  _handleStopButton(event: {
    target: { getAttribute: (arg0: string) => any };
  }) {
    const arg = event.target.getAttribute('data-args');
    const packageName = arg.split('#')[0];
    const version = arg.split('@')[1];
    const classes = arg.split('#')[1].split('@')[0].split(',');

    for (const c of classes) {
      this.stopService(packageName + '.' + c, version);
    }
  }

  async stopService(fullClassName: string, version: string) {
    const response = await request(
        '/las2peer/services/stop?' +
        'serviceName=' +
        fullClassName +
        '&version=' +
        version,
      {
        method: 'POST',
      }
    );
    console.log(
      "Requesting stop of '" + fullClassName + "'@'" + version + "' ..."
    );
  }

  _handleFrontendButton(event: {
    target: { getAttribute: (arg0: string) => string | URL | undefined };
  }) {
    if (event.target.getAttribute('data-args')) {
      window.open(event.target.getAttribute('data-args'));
    }
  }
  _fullyAvailableLocally(release: Release) {
    return this._countMissingLocally(release) === 0;
  }
  _toHumanDate(epochSeconds: number) {
    return new Date(epochSeconds * 1000).toLocaleString();
  }
  _frontendUrlIfServiceAvailable(release: Release) {
    if (this._fullyAvailableAnywhere(release)) {
      return (release.supplement || {}).frontendUrl;
    } else {
      return false;
    }
  }
  _getLatestVersionNumber(obj: Releases) {
    // NOTE: sorting issue fixed
    const latestVersion = Object.keys(obj)
      .map((a) =>
        a
          .split('.')
          .map((n) => +n + 1000000)
          .join('.')
      )
      .sort()
      .map((a) =>
        a
          .split('.')
          .map((n) => +n - 1000000)
          .join('.')
      )
      .reverse()[0];
    return latestVersion;
  }
  _toArray(obj: any[] | Releases) {
    return Object.keys(obj).map((k) => ({ name: k, value: obj[k] }));
  }
  _split(stringWithCommas: any) {
    return (stringWithCommas || '').split(',');
  }
  _count(stringWithCommas: any) {
    return this._split(stringWithCommas).length;
  }
  _countRunning(release: Release) {
    const classes = this._split((release.supplement || {}).class);
    const missing = this._classesNotRunningAnywhere(release);
    return classes.length - missing.length;
  }
  _fullyAvailableAnywhere(release: Release) {
    return this._classesNotRunningAnywhere(release).length === 0;
  }
  _classesNotRunningAnywhere(release: Release) {
    const classes = this._split((release.supplement || {}).class);
    const missing = classes.filter((c: string) => {
      const instancesOfClass = (release.instances || []).filter(
        (i) => i.className === c
      );
      return instancesOfClass.length < 1;
    });
    return missing;
  }
  _hasOnlyRemoteRunningInstance(
    instances: any[] | null | undefined,
    serviceClass: any
  ) {
    return (
      this._hasRunningInstance(instances, serviceClass) &&
      !this._hasLocalRunningInstance(instances, serviceClass)
    );
  }
  _hasLocalRunningInstance(
    instances: InstancesEntity[] | null | undefined,
    serviceClass: any
  ): boolean {
    return (
      this._filterInstances(instances, serviceClass).filter(
        (i) => i.nodeId === (this.nodeId || {}).id
      ).length > 0
    );
  }

  _filterInstances(
    instances: any[] | null | undefined,
    serviceClass: any
  ): InstancesEntity[] {
    if (instances) {
      return instances.filter((i) => i.className === serviceClass);
    } else {
      return [];
    }
  }
  _countRunningLocally(release: { supplement: any }) {
    const classes = this._split((release.supplement || {}).class);
    const missing = this._classesNotRunningLocally(release);
    return classes.length - missing.length;
  }

  _countMissingLocally(release: { supplement: any }) {
    return (
      this._count((release.supplement || {}).class) -
      this._countRunningLocally(release)
    );
  }
  _classesNotRunningLocally(release: { supplement: any; instances?: any }) {
    const classes = this._split((release.supplement || {}).class);
    const missing = classes.filter((c: any) => {
      const localInstancesOfClass = (release.instances || []).filter(
        (i: { className: any; nodeId: any }) =>
          i.className === c && i.nodeId === (this.nodeId || {}).id
      );
      return localInstancesOfClass < 1;
    });
    return missing;
  }

  // uh yeah, there's prettier ways to handle this
  _classesNotRunningLocallySeparatedByCommas(release: {
    supplement: any;
    instances?: any;
  }) {
    return this._classesNotRunningLocally(release).join(',');
  }
  _countRunningRemoteOnly(release: Release) {
    return this._countRunning(release) - this._countRunningLocally(release);
  }
  _pluralS(stringWithCommas: any) {
    return this._count(stringWithCommas) > 1 ? 's' : '';
  }
  _countInstancesRunningRemoteOnly(release: Release) {
    return (
      (release.instances || '').length - this._countRunningLocally(release)
    );
  }
  _hasRunningInstance(instances: any[] | null | undefined, serviceClass: any) {
    return this._filterInstances(instances, serviceClass).length > 0;
  }
  _getLatest(obj: Releases) {
    const latestVersionNumber = this._getLatestVersionNumber(obj);
    const latestRelease = obj[latestVersionNumber] as Release;
    // version number is key, let's add it so we can access it
    (latestRelease || {}).version = latestVersionNumber;
    return latestRelease;
  }

  _getLatestAsArray(service: Releases) {
    console.log(this._getLatest(service));
    console.log(this._getLatest(service));
    console.log(this._getLatest(service));
    return [this._getLatest(service)];
  }
}
export interface Service {
  code: number;
  text: string;
  authorReputation: number;
  authorName: string;
  name: string;
  releases: Releases;
}
export interface Releases {
  [version: number]: Release;
}
export interface Release {
  version: string;
  instances?: InstancesEntity[] | null;
  supplement: Supplement;
  publicationEpochSeconds: number;
}
export interface InstancesEntity {
  className: string;
  nodeInfo: NodeInfo;
  hosterReputation: number;
  nodeId: string;
  announcementEpochSeconds: number;
}
export interface NodeId {
  id: string;
  code: number;
  text: string;
}
export interface NodeInfo {
  organization: string;
  'service-count': number;
  description: string;
  'admin-name': string;
  'admin-mail': string;
  services?: ServicesEntity[] | null;
}
export interface ServicesEntity {
  'service-name': string;
  'service-version': string;
}
export interface Supplement {
  frontendUrl: string;
  name: string;
  description: string;
  class: string;
  vcsUrl: string;
}
