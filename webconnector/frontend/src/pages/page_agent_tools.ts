/* eslint-disable import/no-duplicates */
import { TextField } from '@material/mwc-textfield';
import { html, css, customElement } from 'lit-element';

import config from '../config.js';
import { showNotificationToast } from '../helpers/notification_helper.js';
import { PageElement } from '../helpers/page-element.js';
import '@material/mwc-textfield';
import {
  request,
  requestFile,
  RequestResponse,
} from '../helpers/request_helper.js';
import { downloadBlobFile } from '../helpers/blob_downloader.js';

@customElement('page-agent-tools')
export class PageAgentTools extends PageElement {
  static styles = css`
    section {
      padding: 1rem;
    }
  `;

  render() {
    return html`
      <section>
        <h1>Home</h1>
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
        <div>
          <input type="file" id="file" />
          <mwc-textfield
            id="agent-upload-password"
            placeholder="password (optional)"
          ></mwc-textfield>

          <mwc-button @click=${this.uploadAgent}>Upload</mwc-button>
        </div>
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

          <mwc-button @click=${this.exportAgent}>Create Group</mwc-button>
        </div>
      </section>
    `;
  }
  fileToUpload: File | null = null;

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

    const response: AddAgentResponse = await request<AddAgentResponse>(
      config.url + '/las2peer/agents/getAgent',
      {
        method: 'POST',
        body: formData,
      }
    );
    showNotificationToast(response.text);
  }
  async createGroup() {
    const groupNameField = this.shadowRoot!.getElementById(
      'agent-group-name'
    ) as TextField;

    const group: GroupData = {
      name: groupNameField.value,
      members: [''],
    };

    const formData = new FormData();
    formData.append('members', JSON.stringify(group.members));
    formData.append('name', group.name);

    const response: AddAgentResponse = await request<AddAgentResponse>(
      config.url + '/las2peer/agents/createGroup',
      {
        method: 'POST',
        body: formData,
      }
    );
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
        config.url + '/las2peer/agents/createAgent',
        {
          method: 'POST',
          body: formData,
        }
      );
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
      config.url + '/las2peer/agents/exportAgent',
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
      config.url + '/las2peer/agents/uploadAgent',
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
  members: [string];
}
interface GetAgentData {
  username: string;
  email: string;
  agentid: string;
}
interface GetAgentResponse {
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
interface UploadAgentResponse {
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

interface FileList {
  readonly length: number;
  item(index: number): File;
  [index: number]: File;
}
