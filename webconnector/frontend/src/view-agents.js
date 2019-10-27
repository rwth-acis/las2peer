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
import '@polymer/iron-collapse/iron-collapse.js';
import '@polymer/iron-form/iron-form.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-spinner/paper-spinner.js';
import '@cwmr/iron-star-rating/iron-star-rating.js';
import './shared-styles.js';

class AgentsView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxCreateAgent"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/createAgent"
                 handle-as="json"
                 on-response="_handleCreateAgentResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxExportAgent"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/exportAgent"
                 handle-as="text"
                 on-response="_handleExportAgentResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxUploadAgent"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/uploadAgent"
                 handle-as="json"
                 on-response="_handleUploadAgentResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxChangePassphrase"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/changePassphrase"
                 handle-as="json"
                 on-response="_handleChangePassphraseResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxAddMember"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/getAgent"
                 handle-as="json"
                 on-response="_handleAddMemberResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxCreateGroup"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/createGroup"
                 handle-as="json"
                 on-response="_handleCreateGroupResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxLoadGroup"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/loadGroup"
                 handle-as="json"
                 on-response="_handleLoadGroupResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxChangeGroup"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/changeGroup"
                 handle-as="json"
                 on-response="_handleChangeGroupResponse"
                 on-error="_handleError"
                 loading = "{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxListAgents"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/listAgents"
                 handle-as="json"
                 on-response="_handleLoadAgentlistResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxRateAgent"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/rateAgent"
                 handle-as="json"
                 on-response="_handleRateAgentResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxGetEthProfile"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/getEthProfile"
                 handle-as="json"
                 on-response="_handleGetEthProfileResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxRequestFaucet"
                 method="POST"
                 url$="[[apiEndpoint]]/agents/requestFaucet"
                 handle-as="json"
                 on-response="_handleRequestFaucetResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
      </style>

      <div class="card">
        <h1>Agents</h1>

        <paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>

        <h2 on-click="toggleEthProfile" style="cursor:point">
          User Profile 
          <template is="dom-if" if="[[!_hasNoEthProfile]]"><small>(must be logged in)</small></template>
        </h2>
        <p>
        <paper-button raised on-click="refreshEthProfile" disabled="[[_working]]">
          <iron-icon icon="refresh"></iron-icon> Refresh User Profile
        </paper-button>
        <template is="dom-if" if="[[!_hasNoEthProfile]]">
          <paper-button raised on-click="requestEthFaucet" disabled="[[_working]]">
            <iron-icon icon="card-giftcard"></iron-icon> Request funds from faucet
          </paper-button>
        </template>
        </p>
        <iron-collapse opened id="collapseEthProfile">
          <template is="dom-if" if="[[!_hasNoEthProfile]]">
            <p>Welcome, [[_ethProfile.username]]</p>
            <p>
              <strong><iron-icon icon="fingerprint"></iron-icon> Eth Address: </strong> [[_ethProfile.eth-agent-address]] <br />
              <strong><iron-icon icon="account-balance"></iron-icon> Eth Balance: </strong> [[_ethProfile.eth-acc-balance]]
            </p>
        	</template>
        </iron-collapse>
        <h2 on-click="toggleCreateAgent" style="cursor: pointer">Register Agent</h2>
        <iron-collapse id="collapseCreateAgent">
          <p>
            Create a new user agent and register it in the network for later usage.
            You will then be able to log in with your username and password.
          </p>
          <p>
            <!--
            By specifying an Ethereum mnemonic phrase, you can use an existing Ethereum account.
            If you leave it blank, a new account will be created for you.
            -->
            <!-- The seed phrase is stored in the (publicly accessible) agent file.
                 The key pair is generated from the phrase + the password.
                 This follows BIP39 but not the HD Wallet standards, since Web3J does not fully support those right now.
                 You can POST a mnemonic to .../services/registry/mnemonic to see the generated key pair.
            -->
          </p>
          <iron-form on-keypress="_keyPressedCreateAgent">
            <form>
              <paper-input label="username (optional)" id="createAgentUsername" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="email (optional)" id="createAgentEmail" disabled="[[_working]]" value=""></paper-input>
              <paper-input hidden="true" label="ethereum mnemonic phrase (optional)" id="createAgentEthereumMnemonic" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="password" id="createAgentPassword" disabled="[[_working]]" value="" type="password" required="true"></paper-input>
              <paper-button raised on-click="createAgent" disabled="[[_working]]">Create Agent</paper-button>
            </form>
          </iron-form>
          <div id="createAgentMsg" style="font-weight: bold"></div>
        </iron-collapse>
        <h2 on-click="toggleExportAgent" style="cursor: pointer">Export Agent</h2>
        <iron-collapse id="collapseExportAgent">
          Dump an existing agent as XML file and download it from the network. Only one detail below is required.
          <iron-form on-keypress="_keyPressedExportAgent">
            <form>
              <paper-input label="agentid" id="exportAgentId" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="username" id="exportAgentUsername" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="email" id="exportAgentEmail" disabled="[[_working]]" value=""></paper-input>
              <paper-button raised on-click="exportAgent" disabled="[[_working]]">Export Agent</paper-button>
            </form>
          </iron-form>
        </iron-collapse>
        <h2 on-click="toggleUploadAgent" style="cursor: pointer">Upload Agent</h2>
        <iron-collapse id="collapseUploadAgent">
          <iron-form on-keypress="_keyPressedUploadAgent">
            <form>
              <paper-input label="agent xml file" id="uploadAgentFile" disabled="[[_working]]" type="file" required="true"></paper-input>
              <paper-input label="password (optional)" id="uploadAgentPassword" disabled="[[_working]]" value="" type="password"></paper-input>
              <paper-button raised on-click="uploadAgent" disabled="[[_working]]">Upload Agent</paper-button>
            </form>
          </iron-form>
          <div id="uploadAgentMsg" style="font-weight: bold"></div>
        </iron-collapse>

        <!-- FIXME: passphrase change currently breaks ETH key generation (needs a second layer, an "inner" password)
        <h2 on-click="toggleChangePassphrase" style="cursor: pointer">Change Passphrase</h2>
        <iron-collapse id="collapseChangePassphrase">
          <iron-form on-keypress="_keyPressedChangePassphrase">
            <form>
              <paper-input label="agentid" id="changePassphraseAgentid" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="old passphrase" id="changePassphrasePassphrase" disabled="[[_working]]" value="" type="password" required="true"></paper-input>
              <paper-input label="new passphrase" id="changePassphrasePassphraseNew" disabled="[[_working]]" value="" type="password" required="true"></paper-input>
              <paper-input label="new passphrase (repetition)" id="changePassphrasePassphraseNew2" disabled="[[_working]]" value="" type="password" required="true"></paper-input>
              <paper-button raised on-click="changePassphrase" disabled="[[_working]]">Change Passphrase</paper-button>
            </form>
          </iron-form>
          <div id="changePassphraseMsg" style="font-weight: bold"></div>
        </iron-collapse>
        -->

        <h2 on-click="toggleCreateGroup" style="cursor: pointer">Create Group</h2>
        <iron-collapse id="collapseCreateGroup">
          <h3>Members</h3>
          <table width="100%">
            <tr><th>Agentid</th><th>Username</th><th>Email</th></tr>
            <template is="dom-repeat" items="[[_memberAgents]]">
              <tr><td>[[item.shortid]]</td><td>[[item.username]]</td><td>[[item.email]]</td></tr>
            </template>
          </table>
          Add a member to the new group. Only one detail required.
          <iron-form on-keypress="_keyPressedAddMember">
            <form>
              <paper-input label="agentid" id="addMemberId" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="username" id="addMemberUsername" disabled="[[_working]]" value=""></paper-input>
              <paper-input label="email" id="addMemberEmail" disabled="[[_working]]" value=""></paper-input>
              <paper-button raised on-click="addGroupMember">Add Member</paper-button>
            </form>
          </iron-form>
          <iron-form on-keypress="_keyPressedCreateGroup">
            <form>
              <paper-button raised on-click="createGroup" disabled="[[_hasNoMemberAgents]]">Create Group</paper-button>
            </form>
          </iron-form>
          <div id="createGroupMsg" style="font-weight: bold"></div>
        </iron-collapse>
        <h2 on-click="toggleManageGroup" style="cursor: pointer">Manage Group</h2>
        <iron-collapse id="collapseManageGroup">
          <template is="dom-if" if="[[!_hasNoManageAgents]]">
            <h3>Members</h3>
            <table width="100%">
              <tr><th></th><th>Agentid</th><th>Username</th><th>Email</th></tr>
              <template is="dom-repeat" items="[[_manageAgents]]" as="agent">
                <tr><td><paper-icon-button raised icon="delete" title="remove member from group" on-click="removeManageMember"></paper-icon-button></td><td>[[agent.shortid]]</td><td>[[agent.username]]</td><td>[[agent.email]]</td></tr>
              </template>
            </table>
          </template>
          <iron-form>
            <form>
              <template is="dom-if" if="[[_hasNoManageAgents]]">
                <paper-input label="group agentid" disabled="[[_working]]" value="{{_manageGroupAgentId}}"></paper-input>
                <paper-button raised on-click="loadGroup">Load Group</paper-button>
              </template>
              <template is="dom-if" if="[[!_hasNoManageAgents]]">
                <paper-button raised on-click="saveGroup">Save Group</paper-button>
              </template>
            </form>
          </iron-form>
        </iron-collapse>

        <h2 on-click="toggleAgentList" style="cursor: pointer">List User Agents</h2>
        <iron-collapse id="collapseAgentList">
          <template is="dom-if" if="[[!_hasNoAgentsList]]">
            <h3>Members</h3>
            <table width="100%">
              <tr>
              	<th>Agentid</th>
              	<th>Adress</th>
              	<th>Username</th>
              	<th>Reputation</th>
              </tr>
              <template is="dom-repeat" items="[[_listAgents]]" as="agent">
                <tr>
                  <td>[[agent.shortid]]</td>
                  <td>[[agent.address]]</td>
                  <td>[[agent.username]]</td>
                  <td>
                  	<iron-star-rating value="[[agent.rating]]" on-rating-selected="rateAgent"></iron-star-rating>
                  </td>
                </tr>
              </template>
            </table>
          </template>
        </iron-collapse>
      </div>
      
              
      <!-- Toast Messages -->
      <paper-toast id="toast" horizontal-align="right"></paper-toast>
    `;
  }

  static get properties() {
    return {
      apiEndpoint: { type: String, notify: true },
      agentId: { type: String, notify: true },
      error: { type: Object, notify: true },
      _working: Boolean,
      _ethProfile: { type: Array, value: [] },
      _hasNoAgentsList: { type: Boolean, value: true },
      _hasNoEthProfile: { type: Boolean, value: true },
      _hasNoManageAgents: { type: Boolean, value: true },
      _hasNoMemberAgents: { type: Boolean, value: true },
      _isAjaxLoading: { type: Boolean, value: true },
      _listAgents: { type: Array, value: [] },
      _manageAgents: { type: Array, value: [] },
      _manageGroupAgentId: String,
      _memberAgents: { type: Array, value: [] },
    };
  }

  ready() {
    super.ready();
    let appThis = this;
    window.setTimeout(function() { appThis.refresh(); }, 5);
  }

  refresh() {
	//this.$.ajaxGetEthProfile.generateRequest();
    this.$.ajaxListAgents.generateRequest();
  }
  refreshEthProfile() { console.log(this.agentId); this.$.ajaxGetEthProfile.generateRequest(); }
  requestEthFaucet() { this.$.ajaxRequestFaucet.generateRequest(); }
  toggleCreateAgent() { this.$.collapseCreateAgent.toggle(); }
  toggleAgentList() { this.$.collapseAgentList.toggle(); }
  toggleEthProfile() { this.$.collapseEthProfile.toggle(); }
  
  _handleLoadAgentlistResponse(event) {
    console.log(event.detail.response);
    let response = event.detail.response;
    response.members.forEach(function(element) { element.shortid = element.agentid.substr(0, 15) + '...' });
    this._listAgents = response.members;
    this._hasNoAgentsList = false;
  }
  _handleRateAgentResponse(event) {
	  console.log(event);
	  let response = event.detail.response;
	  
	  this.$.toast.innerHTML = 'Rating (' + response.recipientname + ': '+ response.rating + ') successfully casted.';
	  this.$.toast.open();
  }
  rateAgent(event) {
	  console.log(event);
	  console.log(event.model.get('agent'));
	  console.log(event.model.get('agent.shortid'));
	  console.log(event.model.get('agent.username'));
	  
	  //let req = this.$.ajaxRateAgentMock;
	  let req = this.$.ajaxRateAgent;
      req.body = new FormData();
      req.body.append('agentid', event.model.get('agent.agentid'));
      req.body.append('rating', event.detail.rating);
      req.generateRequest();
      
      //this.$.toast.innerHTML = 'Rating (' + event.model.get('agent.username') + ': '+ event.detail.rating + ') successfully casted.';
	  //this.$.toast.open();
  }
  _handleGetEthProfileResponse(event) {
	  this._hasNoEthProfile = false;
    this._ethProfile = event.detail.response;
  }
  _handleRequestFaucetResponse(event) {
	  console.log(event.detail.response);
	  this.refreshEthProfile();
  }

  _keyPressedCreateAgent(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.createAgent();
      return false;
    }
    return true;
  }

  createAgent() {
    let req = this.$.ajaxCreateAgent;
    req.body = new FormData();
    req.body.append('username', this.$.createAgentUsername.value);
    req.body.append('email', this.$.createAgentEmail.value);
    req.body.append('mnemonic', this.$.createAgentEthereumMnemonic.value);
    req.body.append('password', this.$.createAgentPassword.value);
    req.generateRequest();
  }

  _handleCreateAgentResponse(event) {
    this.$.createAgentUsername.value = '';
    this.$.createAgentEmail.value = '';
    this.$.createAgentPassword.value = '';
    this.$.createAgentMsg.innerHTML = 'Agent successfully created, ID: ' + event.detail.response.agentid;
  }

  toggleExportAgent() { this.$.collapseExportAgent.toggle(); }

  _keyPressedExportAgent(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.exportAgent();
      return false;
    }
    return true;
  }

  exportAgent() {
    let req = this.$.ajaxExportAgent;
    req.body = new FormData();
    req.body.append('agentid', this.$.exportAgentId.value);
    req.body.append('username', this.$.exportAgentUsername.value);
    req.body.append('email', this.$.exportAgentEmail.value);
    req.generateRequest();
  }

  _handleExportAgentResponse(event) {
    this.$.exportAgentId.value = '';
    this.$.exportAgentUsername.value = '';
    this.$.exportAgentEmail.value = '';
    // pack response as download file
    let element = document.createElement('a');
    element.style.display = 'none';
    element.setAttribute('href', 'data:application/xml;charset=utf-8,' + encodeURIComponent(event.detail.response));
    element.setAttribute('download', 'agent.xml');
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }

  toggleUploadAgent() { this.$.collapseUploadAgent.toggle(); }

  _keyPressedUploadAgent(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.uploadAgent();
      return false;
    }
    return true;
  }

  uploadAgent(event) {
    let req = this.$.ajaxUploadAgent;
    req.body = new FormData();
    req.body.append('agentFile', this.$.uploadAgentFile.inputElement.inputElement.files[0]); // this is an input inside an iron-input inside a paper-input
    req.body.append('password', this.$.uploadAgentPassword.value);
    req.generateRequest();
  }

  _handleUploadAgentResponse(event) {
    this.$.uploadAgentFile.value = '';
    this.$.uploadAgentPassword.value = '';
    this.$.uploadAgentMsg.innerHTML = 'Agent successfully uploaded, ID: ' + event.detail.response.agentid;
  }

  toggleChangePassphrase() { this.$.collapseChangePassphrase.toggle(); }

  _keyPressedChangePassphrase(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.changePassphrase();
      return false;
    }
    return true;
  }

  changePassphrase(event) {
    let req = this.$.ajaxChangePassphrase;
    req.body = new FormData();
    req.body.append('agentid', this.$.changePassphraseAgentid.value);
    req.body.append('passphrase', this.$.passphrase.value);
    req.body.append('passphraseNew', this.$.passphraseNew.value);
    req.body.append('passphraseNew2', this.$.passphraseNew2.value);
    req.generateRequest();
  }

  _handleChangePassphraseResponse(event) {
    this.$.changePassphraseAgentid.value = '';
    this.$.passphrase.value = '';
    this.$.passphraseNew.value = '';
    this.$.passphraseNew2.value = '';
    this.$.changePassphraseMsg.innerHTML = 'Passphrase successfully changed, agentid: ' + event.detail.response.agentid;
  }

  toggleCreateGroup() { this.$.collapseCreateGroup.toggle(); }

  _keyPressedAddMember(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.addGroupMember();
      return false;
    }
    return true;
  }

  addGroupMember() {
    let req = this.$.ajaxAddMember;
    req.body = new FormData();
    req.body.append('agentid', this.$.addMemberId.value);
    req.body.append('username', this.$.addMemberUsername.value);
    req.body.append('email', this.$.addMemberEmail.value);
    req.generateRequest();
  }

  _handleAddMemberResponse(event) {
    this.$.addMemberId.value = '';
    this.$.addMemberUsername.value = '';
    this.$.addMemberEmail.value = '';
    let agent = event.detail.response;
    if (!(this._memberAgents.find(m => m.agentid === agent.agentid))) { // avoid duplicate membership
      agent.shortid = agent.agentid.substr(0, 15) + '...';
      this.push('_memberAgents', agent);
    }
    this._hasNoMemberAgents = false;
  }

  createGroup() {
    let req = this.$.ajaxCreateGroup;
    req.body = new FormData();
    req.body.append('members', JSON.stringify(this._memberAgents));
    req.generateRequest();
  }

  _handleCreateGroupResponse(event) {
    this.splice('_memberAgents', 0, this._memberAgents.length);
    this._hasNoMemberAgents = true;
    this.$.createGroupMsg.innerHTML = 'Group successfully created, ID: ' + event.detail.response.agentid;
  }

  toggleManageGroup() { this.$.collapseManageGroup.toggle(); }

  loadGroup() {
    let req = this.$.ajaxLoadGroup;
    req.body = new FormData();
    req.body.append('agentid', this._manageGroupAgentId);
    req.generateRequest();
  }

  _handleLoadGroupResponse(event) {
    let response = event.detail.response;
    response.members.forEach(function(element) { element.shortid = element.agentid.substr(0, 15) + '...' });
    this._manageAgents = response.members;
    this._hasNoManageAgents = false;
  }

  removeManageMember(event) {
    let agentid = event.model.get('agent.agentid');
    this._manageAgents = this._manageAgents.filter(function(obj) { return obj.agentid !== agentid; });
  }

  saveGroup() {
    let req = this.$.ajaxChangeGroup;
    req.body = new FormData();
    req.body.append('agentid', this._manageGroupAgentId);
    req.body.append('members', this._manageAgents);
    req.generateRequest();
  }

  _handleChangeGroupResponse(event) {
    let response = event.detail.response;
    this._manageAgents = [];
    this._hasNoManageAgents = true;
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
    this.error = { title: errorTitle, msg: errorMsg };
  }
}

window.customElements.define('agents-view', AgentsView);
