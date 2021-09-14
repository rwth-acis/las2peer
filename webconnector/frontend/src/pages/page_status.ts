import { html, css, customElement, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import '../components/other_node_information.js';
import '../components/progress_indicator.js';
import '../components/current_node_status.js';
import { request } from '../helpers/request_helper.js';

@customElement('page-status')
export class PageStatus extends PageElement {
  static styles = css`
    li {
      list-style-type: none;
    }
    section {
      margin: 2rem;
    }
    h1 {
      font-size: 2rem;
    }
    @media screen and (min-width: 550px) {
      .page-status-indicators {
        display: flex;
        flex-direction: row;
        justify-content: space-evenly;
        align-items: center;
      }
    }
    @media screen and (max-width: 550px) {
      .page-status-indicators {
        display: flex;
        flex-direction: column;
        justify-content: space-evenly;
        align-items: center;
      }
    }

    .page-status-single-indicator {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }
    .page-status-node-information {
      padding: 0.5rem;
    }

    .page-status-known-nodes-information {
      margin: 1rem;
    }
  `;

  @property({ type: Object })
  nodeStatus: NodeStatus = {
    nodeOrganization: 'loading ...',
    nodeAdminReputation: 0.0,
    ramLoadStr: '? MB',
    nodeAdminName: 'loading ...',
    maxRamLoad: 42,
    localServices: [],
    nodeDescription: 'loading ...',
    uptime: 'loading ...',
    nodeAdminEmail: 'loading ...',
    otherNodes: [],
    cpuLoad: 42,
    ramLoad: 4,
    storageSize: 4,
    maxStorageSize: 42,
    maxStorageSizeStr: '? GB',
    maxRamLoadStr: '? MB',
    nodeId: 'loading ...',
    storageSizeStr: '? MB',
  };

  progressColor = 10;
  timer: NodeJS.Timeout | undefined;

  render() {
    return html`
      <section>
        <h1>Status</h1>
        <div class="page-status-indicators">
          <div class="page-status-single-indicator">
            <b>RAM Usage</b>
            <progress-indicator
              progressAmount=${this.convertToPercentage(
                this.nodeStatus.ramLoad,
                this.nodeStatus.maxRamLoad
              )}
            ></progress-indicator>
            ${this.nodeStatus?.ramLoadStr} of ${this.nodeStatus?.maxRamLoadStr}
          </div>
          <div class="page-status-single-indicator">
            <b>CPU Load</b>
            <progress-indicator
              progressAmount=${this.nodeStatus.cpuLoad}
            ></progress-indicator>
            <p style="opacity: 0; margin: 0;">Some invisible text</p>
          </div>
          <div class="page-status-single-indicator">
            <b>Local Storage</b>
            <progress-indicator
              progressAmount=${this.convertToPercentage(
                this.nodeStatus.storageSize,
                this.nodeStatus.maxStorageSize
              )}
            ></progress-indicator>
            ${this.nodeStatus?.storageSizeStr} of
            ${this.nodeStatus?.maxStorageSizeStr}
          </div>
        </div>
        <h1>Node Information</h1>
        <div class="page-status-node-information">
          <current-node-status
            .nodeStatus=${this.nodeStatus}
          ></current-node-status>
        </div>

        <h1>Known Nodes in Network</h1>
        <div class="page-status-known-nodes-information">
          <div>
            The provided list of services for each node is not verified via the
            blockchain, but has been included for backwards compatibility.
          </div>
          <div>
            This was necessary since some services (e.g. MobSOS) cannot be
            easily compressed to a jar file due to external requirements.
          </div>
          <div>
            <other-node-information></other-node-information>
          </div>
        </div>
      </section>
    `;
  }
  firstUpdated() {
    this.timer = setInterval(() => this.fetchNodeInfo(), 1000);
  }

  convertToPercentage(currentValue: number, maxValue: number): number {
    return parseInt(
      Math.ceil((currentValue / maxValue) * 100)
        .toString()
        .split('.')[0]
    );
  }
  async fetchNodeInfo() {
    this.nodeStatus = await request<NodeStatus>(
      config.url + '/las2peer/status',
      {
        method: 'GET',
      }
    );
  }
}
export interface NodeStatus {
  nodeOrganization: string;
  nodeAdminReputation: number;
  ramLoadStr: string;
  nodeAdminName: string;
  maxRamLoad: number;
  localServices?: null[] | null;
  nodeDescription: string;
  uptime: string;
  nodeAdminEmail: string;
  otherNodes: string[];
  cpuLoad: number;
  ramLoad: number;
  storageSize: number;
  maxStorageSize: number;
  maxStorageSizeStr: string;
  maxRamLoadStr: string;
  nodeId: string;
  storageSizeStr: string;
}

export interface OtherNodesInfo {
  nodeAdminReputation: number;
  nodeInfo: NodeInfo;
  nodeID: string;
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
