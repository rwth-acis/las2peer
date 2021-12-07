import { css, customElement, html, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';
import { request } from '../helpers/request_helper.js';
import { OtherNodesInfo, ServicesEntity } from '../pages/page_status.js';

@customElement('other-node-information')
export class OtherNodesInformation extends PageElement {
  static styles = css`
    li {
      list-style-type: none;
    }
    .other-node {
      margin-top: 1rem;
      margin-bottom: 1rem;
      background-color: rgb(160 202 245);
      border-radius: 1rem;
      padding: 1rem;
    }
    .other-node-service-name-and-version {
      overflow-wrap: break-word;
    }
    .other-node-node-id {
      overflow-wrap: break-word;
    }
    .current-node-id {
      overflow-wrap: break-word;
    }
    .other-node-field-descriptions {
      font-size: 1.2rem;
    }
  `;

  @property({ type: Array })
  otherNodesInfo: OtherNodesInfo[] | null = [
    {
      code: 0,
      text: '',
      nodeAdminReputation: 0.0,
      nodeInfo: {
        organization: 'loading ...',
        'service-count': 1,
        description: 'loading ...',
        'admin-name': 'loading ...',
        'admin-mail': 'loading ...',
        services: [
          {
            'service-name': 'loading ...',
            'service-version': '',
          },
        ],
      },
      nodeID: 'loading ...',
    },
  ];

  render() {
    return html`
      ${this.otherNodesInfo
        ? html`${this.otherNodesInfo?.map(
            (nodeInfo: OtherNodesInfo) =>
              html` <div class="other-node">
                <li>
                  <p class="other-node-node-description">
                    <b class="other-node-field-descriptions">Description: </b
                    >${nodeInfo.nodeInfo.description}
                  </p>
                  ${nodeInfo.nodeInfo.services
                    ? html` <li>
                        <p class="other-node-service-name-and-version">
                          <b class="other-node-field-descriptions">Service: </b>
                          ${nodeInfo.nodeInfo.services.map(
                            (service: ServicesEntity) => {
                              return (
                                service['service-name'] +
                                ' ' +
                                service['service-version']
                              );
                            }
                          )}
                        </p>
                      </li>`
                    : html``}
                </li>
                <li>
                  <b class="other-node-field-descriptions">Admin: </b>${nodeInfo
                    .nodeInfo['admin-name']}
                </li>
                <li>
                  <p class="other-node-node-id">
                    <b class="other-node-field-descriptions">Node Id: </b
                    >${nodeInfo.nodeID}
                  </p>
                </li>
              </div>`
          )}`
        : html`<div class="other-node">No known Nodes available</div>`}
    `;
  }
  firstUpdated() {
    this.fetchOtherNodesInfo();
  }
  async fetchOtherNodesInfo() {
    const response = await request<any>(
      '/las2peer/getOtherNodesInfo',
      {
        method: 'GET',
      }
    );
    if (response.length) {
      this.otherNodesInfo = response;
    } else {
      this.otherNodesInfo = null;
    }
  }
}
