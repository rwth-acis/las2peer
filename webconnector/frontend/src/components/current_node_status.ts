import { css, customElement, html, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';
import { NodeStatus } from '../pages/page_status.js';

@customElement('current-node-status')
export class CurrentNodeStatus extends PageElement {
  static styles = css`
    li {
      list-style-type: none;
    }
    .page-status-current-node-key {
      font-size: 1.5rem;
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
    ramLoad: 42,
    storageSize: 42,
    maxStorageSize: 42,
    maxStorageSizeStr: '? GB',
    maxRamLoadStr: '? MB',
    nodeId: 'loading ...',
    storageSizeStr: '? MB',
    code: 0,
    text: '',
  };

  render() {
    return html`
      ${this.nodeStatus
        ? html` <div class="page-status-current-node-id">
              <b class="page-status-current-node-key"> Current Node ID: </b>
              ${this.nodeStatus?.nodeId}
            </div>
            <div class="page-status-current-node-host">
              <b class="page-status-current-node-key"> Node Hoster:</b> ${this
                .nodeStatus?.nodeAdminName}
            </div>
            <div class="page-status-current-node-host-email">
              <b class="page-status-current-node-key"> Node Hoster Email:</b>
              ${this.nodeStatus?.nodeAdminEmail}
            </div>
            <div class="page-status-current-node-organization">
              <b class="page-status-current-node-key"> Node Organization:</b>

              ${this.nodeStatus?.nodeOrganization}
            </div>
            <div class="page-status-current-node-description">
              <b class="page-status-current-node-key"> Node Description:</b>

              ${this.nodeStatus?.nodeDescription}
            </div>`
        : html`<div class="other-node">No node information available</div>`}
    `;
  }
}
