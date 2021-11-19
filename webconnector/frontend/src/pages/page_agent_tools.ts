/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable import/no-duplicates */
import { TextField } from '@material/mwc-textfield';
import { html, css, customElement, property } from 'lit-element';

import { downloadBlobFile } from '../helpers/blob_downloader.js';
import { showNotificationToast } from '../helpers/notification_helper.js';
import { PageElement } from '../helpers/page-element.js';
import '@material/mwc-textfield';
import '@material/mwc-button';
import '@polymer/iron-collapse/iron-collapse.js';
import {
  request,
  requestFile,
  RequestResponse,
} from '../helpers/request_helper.js';
import '@lrnwebcomponents/a11y-collapse';

@customElement('page-agent-tools')
export class PageAgentTools extends PageElement {
  static styles = css`
    section {
      padding: 1rem;
    }
    .group-member-item {
      white-space: nowrap;
      overflow: hidden;
      width: 100%;
      text-overflow: ellipsis;
    }
    #group-members-list-item {
      margin-top: 1em;
      margin-bottom: 1em;
    }
  `;
  fileToUpload: File | null = null;

  @property({ type: Array })
  groupMembers: GetAgentData[] = [];

  @property({ type: Object })
  addedAgent: AddAgentResponse | undefined;

  @property({ type: Object })
  addedGroup: AddGroupResponse | undefined;

  render() {
    return html`
      <section>
        <h1>Home</h1>
        <a11y-collapse-group>
          <a11y-collapse accordion>
            <p slot="heading">Create Agent</p>
            <div>
              <mwc-textfield
                id="agent-username"
                placeholder="username"
                helper="At least 4 characters"
                ?required=${true}
                validationMessage="At least 4 characters"
                min="4"
                minLength="4"
              ></mwc-textfield>
              <mwc-textfield
                id="agent-email"
                placeholder="email (optional)"
                validationMessage="Please enter valid email"
                pattern="^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+.[a-zA-Z0-9-.]+$"
              ></mwc-textfield>

              <mwc-textfield
                id="agent-password"
                placeholder="password"
              ></mwc-textfield>
              <mwc-button @click=${this.addAgent}>Add</mwc-button>
            </div>
            ${this.addedAgent == undefined
              ? html``
              : html`Agent created with id: ${this.addedAgent.agentid}`}
          </a11y-collapse>
          <a11y-collapse accordion>
            <p slot="heading">Export Agent</p>
            <div>
              <mwc-textfield
                id="agent-export-username"
                placeholder="username"
              ></mwc-textfield>

              <mwc-textfield
                id="agent-export-agentid"
                placeholder="agent-id"
              ></mwc-textfield>

              <mwc-textfield
                id="agent-export-email"
                placeholder="email"
              ></mwc-textfield>

              <mwc-button @click=${this.exportAgent}>Export</mwc-button>
            </div>
          </a11y-collapse>
          <a11y-collapse accordion>
            <p slot="heading">Upload Agent</p>

            <div>
              <input type="file" id="file" />
              <mwc-textfield
                id="agent-upload-password"
                placeholder="password (optional)"
              ></mwc-textfield>

              <mwc-button @click=${this.uploadAgent}>Upload</mwc-button>
            </div>
          </a11y-collapse>
          <a11y-collapse accordion>
            <p slot="heading">Create Group</p>
            <div>
              <mwc-textfield
                id="agent-group-member-username"
                placeholder="username"
              ></mwc-textfield>

              <mwc-textfield
                id="agent-group-member-agentid"
                placeholder="agent-id"
              ></mwc-textfield>

              <mwc-textfield
                id="agent-group-member-email"
                placeholder="email"
              ></mwc-textfield>
              <mwc-button @click=${this.getAgent}>Add Member</mwc-button>

              <mwc-textfield
                id="agent-group-name"
                placeholder="group name"
              ></mwc-textfield>
              <mwc-button @click=${this.createGroup}>Create Group</mwc-button>
              ${this.groupMembers.map(
                (member) => html` <div id="group-members-list-item">
                  <div class="group-member-item">
                    <b>Agentid: </b>
                    ${member.agentid}
                  </div>
                  <div class="group-member-item">
                    <b>Email: </b>${member.email}
                  </div>
                  <div class="group-member-item">
                    <b>Username: </b>${member.username}
                  </div>
                </div>`
              )}
              ${this.addedGroup == undefined
                ? html``
                : html`Group created with id: ${this.addedGroup.agentid}`}
            </div>
          </a11y-collapse>
        </a11y-collapse-group>
      </section>
    `;
  }

  async getAgent() {
    const usernameField = this.shadowRoot!.getElementById(
      'agent-group-member-username'
    ) as TextField;
    const agentidField = this.shadowRoot!.getElementById(
      'agent-group-member-agentid'
    ) as TextField;
    const emailField = this.shadowRoot!.getElementById(
      'agent-group-member-email'
    ) as TextField;

    const user: GetAgentData = {
      username: usernameField.value,
      email: emailField.value,
      agentid: agentidField.value,
    };
    const formData = new FormData();
    formData.append('email', user.email);
    formData.append('username', user.username);
    formData.append('agentid', user.agentid);

    const response: GetAgentResponse = await request<GetAgentResponse>(
      '/las2peer/agents/getAgent',
      {
        method: 'POST',
        body: formData,
      }
    );
    if (response.code == 200) {
      const receivedUser: GetAgentData = {
        username: response.username,
        email: response.email,
        agentid: response.agentid,
      };
      this.requestUpdate();
      console.log(this.groupMembers);
      if (!this.groupMembers.some((e) => e.agentid == receivedUser.agentid)) {
        this.groupMembers.push(receivedUser);
      }
      console.log(this.groupMembers);
    }
    showNotificationToast(response.text);
  }
  async createGroup() {
    const groupNameField = this.shadowRoot!.getElementById(
      'agent-group-name'
    ) as TextField;

    const group: GroupData = {
      name: groupNameField.value,
      members: this.groupMembers,
    };

    const formData = new FormData();
    formData.append('members', JSON.stringify(group.members));
    formData.append('name', group.name);

    const response: AddGroupResponse = await request<AddGroupResponse>(
      '/las2peer/agents/createGroup',
      {
        method: 'POST',
        body: formData,
      }
    );
    this.addedGroup = response;
    showNotificationToast(response.text);
  }
  async addAgent() {
    const usernameField = this.shadowRoot!.getElementById(
      'agent-username'
    ) as TextField;
    const emailField = this.shadowRoot!.getElementById(
      'agent-email'
    ) as TextField;
    const passwordField = this.shadowRoot!.getElementById(
      'agent-password'
    ) as TextField;
    if (
      usernameField.validity.valid &&
      emailField.validity.valid &&
      passwordField.validity.valid
    ) {
      const user: AddAgentData = {
        username: usernameField.value,
        email: emailField.value,
        mnemonic: '',
        password: passwordField.value,
      };
      const formData = new FormData();
      formData.append('email', user.email);
      formData.append('username', user.username);
      formData.append('mnemonic', user.mnemonic);
      formData.append('password', user.password);

      const response: AddAgentResponse = await request<AddAgentResponse>(
        '/las2peer/agents/createAgent',
        {
          method: 'POST',
          body: formData,
        }
      );
      this.addedAgent = response;
      showNotificationToast(response.text);
    } else {
      showNotificationToast('Check your input');
    }
  }

  async exportAgent() {
    const usernameField = this.shadowRoot!.getElementById(
      'agent-export-username'
    ) as TextField;
    const emailField = this.shadowRoot!.getElementById(
      'agent-export-email'
    ) as TextField;
    const agentidField = this.shadowRoot!.getElementById(
      'agent-export-agentid'
    ) as TextField;

    const user: ExportAgentData = {
      username: usernameField.value,
      email: emailField.value,

      agentid: agentidField.value,
    };
    const formData = new FormData();
    formData.append('email', user.email);
    formData.append('username', user.username);
    formData.append('agentid', user.agentid);

    const responseFile = await requestFile(
      '/las2peer/agents/exportAgent',
      {
        method: 'POST',
        body: formData,
      }
    );
    if (responseFile.code == 200) {
      downloadBlobFile('agent.xml', responseFile.blob);
      showNotificationToast('Agent downloaded');
    } else {
      showNotificationToast('Agent not found');
    }
  }

  async uploadAgent() {
    const file = this.shadowRoot!.getElementById('file') as HTMLInputElement;
    const passwordField = this.shadowRoot!.getElementById(
      'agent-upload-password'
    ) as TextField;

    const user: UploadAgentData = {
      agentFile: file.files![0],
      password: passwordField.value,
    };
    const formData = new FormData();
    formData.append('agentFile', user.agentFile);
    formData.append('password', user.password);

    const response = await request<UploadAgentResponse>(
      '/las2peer/agents/uploadAgent',
      {
        method: 'POST',
        body: formData,
      }
    );
    if (response.code == 200) {
      showNotificationToast(response.text);
    } else {
      showNotificationToast('Error uploading agent');
    }
  }
}

interface GroupData {
  name: string;
  members: GetAgentData[];
}
interface GetAgentData {
  username: string;
  email: string;
  agentid: string;
}
interface GetAgentResponse extends RequestResponse {
  username: string;
  email: string;
  agentid: string;
}
interface AddAgentData {
  username: string;
  email: string;
  mnemonic: string;
  password: string;
}
interface ExportAgentData {
  username: string;
  agentid: string;
  email: string;
}
interface UploadAgentData {
  agentFile: any;
  password: string;
}
interface UploadAgentResponse extends RequestResponse {
  agentFile: any;
  code: number;
  text: string;
}
interface AddAgentResponse extends RequestResponse {
  agentid: string;
  registryAddress: string;
  code: number;
  text: string;
  email: string;
  username: string;
}
interface AddGroupResponse extends RequestResponse {
  agentid: string;
  groupName: string;
  code: number;
  text: string;
}
