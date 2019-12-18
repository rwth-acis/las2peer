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
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-dropdown-menu/paper-dropdown-menu.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-item/paper-item.js';
import '@polymer/paper-listbox/paper-listbox.js';
import '@polymer/paper-spinner/paper-spinner.js';
import '@polymer/paper-tabs/paper-tabs.js';
//import '@cwmr/iron-star-rating/iron-star-rating.js';
import './custom-star-rating.js';
import './shared-styles.js';

class EthereumView extends PolymerElement {
  static get template() {
    return html`
      <iron-ajax id="ajaxListAgents"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/listAgents"
                 handle-as="json"
                 on-response="_handleLoadAgentlistResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxListProfiles"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/listProfiles"
                 handle-as="json"
                 on-response="_handleLoadProfilelistResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxRateAgent"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/rateAgent"
                 handle-as="json"
                 on-response="_handleRateAgentResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxGetCoinbaseBalance"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/getCoinbaseBalance"
                 handle-as="json"
                 on-response="_handleGetCoinbaseBalanceResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxGetEthWallet"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/getEthWallet"
                 handle-as="json"
                 on-response="_handleGetEthWalletResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxRequestFaucet"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/requestFaucet"
                 handle-as="json"
                 on-response="_handleRequestFaucetResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>
      <iron-ajax id="ajaxReputationProfile"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/registerProfile"
                 handle-as="json"
                 on-response="_handleRegisterProfileResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>  
      <iron-ajax id="ajaxGenericTransaction"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/addTransaction"
                 handle-as="json"
                 on-response="_handleGenericTransactionResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>   
        <iron-ajax id="ajaxGenericTxLog"
                 method="POST"
                 url$="[[apiEndpoint]]/eth/getGenericTxLog"
                 handle-as="json"
                 on-response="_handleGenericTxLogResponse"
                 on-error="_handleError"
                 loading="{{_working}}"></iron-ajax>    
                 
        <iron-ajax
                 id="ajaxGetGroups"
                 url='[[baseUrl]]/contactservice/groups'
                 params='{}'
                 handle-as="json"
                 on-response="_updateGroups"
                 on-error="_handleError"
                 loading="{{_working}}">
               </iron-ajax>
                 

      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }

        paper-tabs {
          background-color: var(--tabs-background-colour, mediumslateblue);
          --paper-tabs-selection-bar-color: var(--tabs-colour, white);
          color: var(--tabs-colour, white);
        }

        paper-button.red {
            --paper-button-ink-color: var(--paper-red-a200);
            --paper-button-flat-keyboard-focus: {
              background-color: var(--paper-red-a200) !important;
              color: white !important;
            };
            --paper-button-raised-keyboard-focus: {
              background-color: var(--paper-red-a200) !important;
              color: white !important;
            };
          }
          paper-button.red:hover {
            background-color: var(--paper-red-100);
          }

          paper-button.green {
            --paper-button-ink-color: var(--paper-green-a200);
            --paper-button-flat-keyboard-focus: {
              background-color: var(--paper-green-a200) !important;
              color: white !important;
            };
            --paper-button-raised-keyboard-focus: {
              background-color: var(--paper-green-a200) !important;
              color: white !important;
            };
          }
          paper-button.green:hover {
            background-color: var(--paper-green-100);
          }
      </style>

      <div class="card">
        <h1>Agents</h1>

        <paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>

        <!-- ETH WALLET -->
        <template is="dom-if" if="[[agentId.length>5]]">
          <h2 on-click="toggleEthWallet" style="cursor:point">
            Ethereum Wallet <paper-icon-button icon="refresh" title="Refresh Ethereum Wallet" on-click="refreshEthWallet" disabled="[[_working]]"></paper-button>
          </h2>
          <iron-collapse opened id="collapseEthWallet">
            <template is="dom-if" if="[[!_hasNoEthWallet]]">
              <p>Welcome, [[_EthWallet.username]] 
                <template is="dom-if" if="[[_hasEthProfile]]">
                  <custom-star-rating value="[[_EthWallet.ethRating]]" readonly></custom-star-rating>
                </template>
                <template is="dom-if" if="[[!_hasEthProfile]]">
                  <paper-button raised on-click="requestReputationProfile" disabled="[[_working]]">
                    <iron-icon icon="record-voice-over"></iron-icon> Request reputation profile
                  </paper-button>
                </template>
              </p>
              <p>
                <strong><iron-icon icon="fingerprint"></iron-icon> Eth Credentials Address</strong>: [[_EthWallet.ethAgentCredentialsAddress]] <br />
                <strong><iron-icon icon="verified-user"></iron-icon> Eth Mnemonic</strong>: [[_EthWallet.ethMnemonic]] <br />
                
                <template is="dom-if" if="[[_hasEthProfile]]">
                  <strong><iron-icon icon="stars"></iron-icon> Reputation No Transactions</strong> <small><em>[Rcvd | Sent]</em></small>: 
                    <iron-icon icon="cloud-download"></iron-icon> [[_EthWallet.ethNoTransactionsRcvd]] | 
                    <iron-icon icon="cloud-upload"></iron-icon> [[_EthWallet.ethNoTransactionsSent]]
                  <br />
                </template>
              </p>
            <p>
              <strong><iron-icon icon="account-balance"></iron-icon> Eth Balance</strong>: [[_EthWallet.ethAccBalance]]
              <paper-button raised on-click="requestEthFaucet" disabled="[[_working]]">
                <iron-icon icon="card-giftcard"></iron-icon> Request funds from faucet
              </paper-button> 
              <br />
              <strong>Select group agent for Success Modeling (agent must be in group):</strong>
              <paper-dropdown-menu label="Group Agent for Success Modeling" on-change="_updateGroupMemberlist" noink no-animations selected-item="{{_groupSelected}}">
                <paper-listbox slot="dropdown-content" class="dropdown-content" id="groupSelect">
                  <template is="dom-repeat" items="[[groups]]">
                  <paper-item value="{{item.groupID}}">{{item.groupName}}</paper-item>
                  </template>
                </paper-listbox>
              </paper-dropdown-menu-light>
            </p>
            </template>

            <template is="dom-if" if="[[!_hasNoTxLog]]">
              <paper-tabs selected="{{_selectedTab}}">
                <paper-tab><iron-icon icon="cloud-download"></iron-icon> TX Received</paper-tab>
                <paper-tab><iron-icon icon="cloud-upload"></iron-icon> TX Sent</paper-tab>
              </paper-tabs>

              <iron-pages selected="{{_selectedTab}}">
                <div>
                  <h2><iron-icon icon="cloud-download"></iron-icon> Transactions Received</paper-tab></h2>
                  <table width="100%">
                    <tr>
                      <th>Timestamp</th>
                      <th>Sender</th>
                      <th>TransactionType</th>
                      <th>Message</th>
                      <th>TransactionValue</th>
                      <th>TXHash</th>
                    </tr>
                    <template is="dom-repeat" items="[[_ethTxLog.rcvdJsonLog]]" as="tx">
                      <tr>
                        <td><iron-icon icon="update"></iron-icon> [[tx.txDateTime]]</td>
                        <td><iron-icon icon="face"></iron-icon> [[tx.txSender]]</td>
                        <td><iron-icon icon="class"></iron-icon> [[tx.txTransactionType]]</td>
                        <td><iron-icon icon="speaker-notes"></iron-icon> [[tx.txMessage]]</td>
                        <td><iron-icon icon="card-giftcard"></iron-icon> [[tx.txAmountInEth]] ETH</td>
                        <td><iron-icon icon="fingerprint"></iron-icon> [[tx.txTXHash]]</td>
                      </tr>
                    </template>
                  </table>
                </div>
                <div>
                  <h2><iron-icon icon="cloud-upload"></iron-icon> Transactions Sent</paper-tab></h2>
                  <table width="100%">
                    <tr>
                      <th>Timestamp</th>
                      <th>Receiver</th>
                      <th>TransactionType</th>
                      <th>Message</th>
                      <th>TransactionValue</th>
                      <th>TXHash</th>
                    </tr>
                    <template is="dom-repeat" items="[[_ethTxLog.sentJsonLog]]" as="tx">
                      <tr>
                        <td><iron-icon icon="update"></iron-icon> [[tx.txDateTime]]</td>
                        <td><iron-icon icon="face"></iron-icon> [[tx.txReceiver]]</td>
                        <td><iron-icon icon="class"></iron-icon> [[tx.txTransactionType]]</td>
                        <td><iron-icon icon="speaker-notes"></iron-icon> [[tx.txMessage]]</td>
                        <td><iron-icon icon="card-giftcard"></iron-icon> [[tx.txAmountInEth]] ETH</td>
                        <td><iron-icon icon="fingerprint"></iron-icon> [[tx.txTXHash]]</td>
                      </tr>
                    </template>
                  </table></div>
              </iron-pages>
            </template>
          </iron-collapse>
        </template>

        <!-- AGENTS LIST -->
        <h2 on-click="toggleAgentList" style="cursor: pointer">
          List User Agents <small>(ethereum agents registered in the network)</small>
          <paper-icon-button icon="refresh" title="Refresh Agents List" on-click="refreshAgentsList" disabled="[[_working]]"></paper-button>
        </h2>
        <paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>
        <iron-collapse id="collapseAgentList">
          <template is="dom-if" if="[[!_hasNoAgentsList]]">
            <h3>Members</h3>
            <table width="100%">
              <tr>
              	<th>Agentid</th>
              	<th>Adress</th>
              	<th>Username</th>
              	<th>Actions</th>
              </tr>
              <template is="dom-repeat" items="[[_listAgents]]" as="agent">
                <tr>
                  <td>[[agent.shortid]]</td>
                  <td>[[agent.address]]</td>
                  <td>[[agent.username]]</td>
                  <td>
                    <paper-icon-button icon="card-giftcard" title="Transfer ETH to Agent" on-click="openEthSendDialog" data-agentid$="[[agent.agentid]]" disabled="[[_working]]"></paper-button>
                  </td>
                </tr>
              </template>
            </table>
          </template>
        </iron-collapse>

        <!-- PROFILES LIST -->
        <h2 on-click="toggleProfileList" style="cursor: pointer">
          List User Profiles <small>(ethereum agents who have opted in to the Reputation System)</small>
          <paper-icon-button icon="refresh" title="Refresh Profiles List" on-click="refreshProfilesList" disabled="[[_working]]"></paper-button>
        </h2>
        <paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>
        <iron-collapse id="collapseProfileList">
          <template is="dom-if" if="[[!_hasNoProfilesList]]">
            <h3>Members</h3>
            <table width="100%">
              <tr>
                <th>Username</th>
                <th>Reputation</th>
                <th>Tx: [<iron-icon icon="cloud-download" title="Received"></iron-icon> | <iron-icon icon="cloud-upload" title="Sent"></iron-icon>]</th>
              	<th>Eth Adress</th>
              </tr>
              <template is="dom-repeat" items="[[_listProfiles]]" as="agent">
                <tr>
                  <td>[[agent.username]]</td>
                  <td>
                  	<custom-star-rating value="[[agent.rating]]" on-rating-selected="rateAgent"></custom-star-rating>
                  </td>
                  <td> 
                    [[agent.noOfTransactionsRcvd]] | 
                    [[agent.noOfTransactionsSent]]
                  </td>
                  <td><iron-icon icon="fingerprint"></iron-icon> [[agent.address]]</td>
                </tr>
              </template>
            </table>
          </template>
        </iron-collapse>
              
      <!-- Toast Messages -->
      <paper-toast id="toast" horizontal-align="right"></paper-toast>

      <!-- Dialog Boxes -->
      <paper-dialog id="ethFaucetDiaLog">
          <h1><iron-icon icon="receipt"></iron-icon> Faucet Transaction Log - transaction successful.</h1>
          <paper-dialog-scrollable>
            <iron-icon icon="face"></iron-icon>
            <strong>UserRating Score</strong>: [[_ethFaucetLog.rewardDetails.userRatingScore]] <br />
            <p>
              <iron-icon icon="important-devices"></iron-icon>
              <strong>HostingServices Score</strong>: [[_ethFaucetLog.rewardDetails.hostingServicesScore]]
              <template is="dom-if" if="[[_ethFaucetLog.rewardDetails.rewardedForServicesHosting]]">, rewarded for the following services: <br />
                <ul>
                <template is="dom-repeat" items="[[_ethFaucetLog.rewardDetails.rewardedForServicesHosting]]" as="service">
                  <li>[[service]]</li>
                </template>
                </ul>
              </template>
            </p>
            <p>
              <iron-icon icon="cloud-upload"></iron-icon>
              <strong>DevelopServices Score</strong>: [[_ethFaucetLog.rewardDetails.developServicesScore]]
              <template is="dom-if" if="[[_ethFaucetLog.rewardDetails.rewardedForServicesDevelop]]">, rewarded for the following services: <br />
                <ul>
                <template is="dom-repeat" items="[[_ethFaucetLog.rewardDetails.rewardedForServicesDevelop]]" as="service">
                  <li>[[service]]</li>
                </template>
                </ul>
              </template>
            </p>
            <p>
              <iron-icon icon="redeem"></iron-icon>
              <strong>Total Faucet Payout</strong>: [[_ethFaucetLog.ethFaucetAmount]] <br />
              <pre>= ([[_ethFaucetLog.rewardDetails.u]] * UserRating)   *   (     ( [[_ethFaucetLog.rewardDetails.h]] * HostingServices ) + ( [[_ethFaucetLog.rewardDetails.d]] * DevelopServices )     ) </pre> <br />
            </p>
          </paper-dialog-scrollable>
          <div class="buttons">
            <paper-button dialog-dismiss>
              <iron-icon icon="check"></iron-icon> 
              OK
            </paper-button>
          </div>
        </paper-dialog>

      <paper-dialog id="sendEthDialog">
        <h1>Transfer ETH</h1>
        <paper-dialog-scrollable>
          <div class="horizontal layout center-justified">
            <paper-spinner active="[[_working]]"></paper-spinner>
            <template is="dom-if" if="[[_ethTransactionSent]]">
              <iron-icon icon="done"></iron-icon>
            </template>
          </div>
          
          <iron-form on-keypress="_keyPressedSendETHTransaction">
            <form>
              <paper-input label="AgentID" id="SendETHTransactionAgentID" disabled="[[_working]]" value="[[_chosenAgentID]]"></paper-input>
              <paper-input label="Amount (in ETH)" id="SendETHTransactionWeiAmount" disabled="[[_working]]" value=""></paper-input>
              <paper-textarea label="Transaction Message" disabled="[[_working]]" id="SendETHTransactionMessage"></paper-textarea>
            </form>
          </iron-form>
        </paper-dialog-scrollable>
        <div class="buttons">
          <paper-button dialog-confirm autofocus raised class="red">
            <iron-icon icon="block"></iron-icon> Cancel
          </paper-button>
          <paper-button raised on-click="sendGenericTransaction" disabled="[[_working]]" class="green">
            <iron-icon icon="check"></iron-icon> Send ETH Transaction
          </paper-button>
        </div>
      </paper-dialog>
    `;
  }

  static get properties() {
    return {
      apiEndpoint: { type: String, notify: true },
      agentId: { type: String, notify: true, observer: '_agentIdChanged' },
      error: { type: Object, notify: true },
      group: { type: String, value: "" },
      groups: { type: Array, value: [] },
      _groupSelected: {type: Object},
      _working: { type: Boolean, value: false },
      _chosenAgentID: { type: String, value: "" },
      _ethTransactionSent: { type: Boolean, value: false },
      _EthWallet: { type: Object, 
        value: { 
          agentid: "",
          email: "",
          ethAccBalance: 0,
          ethAgentAddress: "",
          ethAgentCredentialsAddress: "",
          ethCumulativeScore: 0,
          ethMnemonic: "",
          ethNoTransactionsRcvd: 0,
          ethNoTransactionsSent: 0,
          ethProfileOwner: "",
          ethRating: 0,
          username: ""
        } 
      },
      _ethFaucetLog: { type: Object, 
        value: {
          ethFaucetAmount: 0,
          rewardDetails: Object, value: {
            u: 0,
            h: 0,
            d: 0,
            userRatingScore: 0,
            hostingServicesScore: 0,
            rewardedForServicesHosting: [],
            developServicesScore: 0,
            rewardedForServicesDevelop: []
          }
        }
      },
      _ethTxLog: { type: Object, 
        value: {
          rcvdJsonLog: [],
          sentJsonLog: []
        } 
      },
      _ethCoinbaseInfo: { type: Object, 
        value: {
          coinbaseAddress: "",
          coinbaseBalance: ""
        }
      },
      _hasEthProfile: { type: Boolean, value: false },
      _hasNoAgentsList: { type: Boolean, value: true },
      _hasNoEthWallet: { type: Boolean, value: true },
      _hasNoProfilesList: { type: Boolean, value: true },
      _hasNoTxLog: { type: Boolean, value: true },
      _listAgents: { type: Array, value: [] },
      _listProfiles: { type: Array, value: [] },
      _selectedTab: {
        type: Number,
        value: 0,
        notify: true,
        reflectToAttribute: true
      }
    };
  }

  ready() {
    super.ready();
    let appThis = this;
    window.appThis = this;
    window.setTimeout(function() { appThis.refresh(); }, 5);
  }

  refresh() {
    this.refreshAgentsList();
    if ( this.agentId.length > 5 )
    {
      this.refreshEthWallet();
    }
  }
  refreshEthWallet() { 
    this.$.ajaxGetEthWallet.generateRequest(); 
    this.$.ajaxGenericTxLog.generateRequest();
    this.$.ajaxGetCoinbaseBalance.generateRequest();
  }
  requestEthFaucet() { 
    if ( this._groupSelected == null || this._groupSelected.innerHTML == null )
    {
      this.group = "";
    }
    else
    {
      this.group = this._findGroupIDByName(this._groupSelected.innerHTML.trim());
    }
    console.log("faucet request for groupID: " + this.group);
    let req = this.$.ajaxRequestFaucet;
      req.body = new FormData();
      req.body.append('groupID', this.group);
      req.generateRequest();
  }
  requestReputationProfile() { 
    if (this._EthWallet.ethAccBalance < 0.15) {
      this.error = { title: "Not enough funds", message: "Try requesting eth from the faucet?" };
      return;
    } else {
      this.$.ajaxReputationProfile.generateRequest(); 
    }
  }
  refreshAgentsList() { this.$.ajaxListAgents.generateRequest(); }
  refreshProfilesList() { this.$.ajaxListProfiles.generateRequest(); }
  toggleCreateAgent() { this.$.collapseCreateAgent.toggle(); }
  toggleAgentList() { this.$.collapseAgentList.toggle(); }
  toggleProfileList() { this.$.collapseProfileList.toggle(); }
  toggleEthWallet() { this.$.collapseEthWallet.toggle(); }

  _agentIdChanged(agentid) {
    if (this.agentId == '' )
      return;
    if (this.agentId.length > 5 ) {
      this.refreshEthWallet();
    }
  }

  _findGroupIDByName(name) {
    return this.groups.find(g => g.groupName == name).groupID;
  }

  _updateGroups(event) {
    var res = event.detail.response;
    this.groups = [];
    let keys = Object.keys(res);
    for (var i = 0; i < keys.length; i++) {
        this.groups.push(
          {
            groupID: keys[i],
            groupName: res[keys[i]]
          }
        );
        console.log({
          groupID: keys[i],
          groupName: res[keys[i]]
        });
    }
    if (keys > 0) {
        if (this.$.groupSelect.value.length > 0) {
            this._updateGroupMemberlist();
        } else {
            this.group = keys[0];
        }
    }
  }
  _updateGroupMemberlist(e) {
    console.log([e.selectedItem, e]);

    console.log(this._groupSelected);
    console.log(this._findGroupIDByName(this._groupSelected.innerHTML.trim()));
    this.group = this._findGroupIDByName(this._groupSelected.innerHTML.trim());
  }

  _handleGenericTxLogResponse(event) {
    this._ethTxLog = event.detail.response;
    console.log(this._ethTxLog);
    if (this._ethTxLog.rcvdJsonLog.length == 0 && this._ethTxLog.sentJsonLog.length == 0)
      this._hasNoTxLog = true;
    else
      this._hasNoTxLog = false;
  }
  
  _handleLoadAgentlistResponse(event) {
    console.log(event.detail.response);
    let response = event.detail.response;
    response.agents.forEach(function(element) { element.shortid = element.agentid.substr(0, 15) + '...' });
    this._listAgents = response.members;
    this._hasNoAgentsList = false;
  }
  _handleLoadProfilelistResponse(event) {
    console.log(event.detail.response);
    let response = event.detail.response;
    response.agents.forEach(function (element) { element.shortid = element.agentid.substr(0, 15) + '...' });
    this._listProfiles = response.members;
    this._hasNoProfilesList = false;
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
  _handleGetCoinbaseBalanceResponse(event) {
    this._ethCoinbaseInfo = event.detail.response;
    
  }
  _handleGetEthWalletResponse(event) {
	  this._hasNoEthWallet = false;
    this._EthWallet = event.detail.response;
    if (this._EthWallet.ethCumulativeScore !== "???" )
    {
      this._hasEthProfile = true;
    }
    this.$.ajaxGetGroups.generateRequest();
  }
  _handleRequestFaucetResponse(event) {
    this._ethFaucetLog = event.detail.response;
    console.log(this._ethFaucetLog);
    this.$.ethFaucetDiaLog.open();
	  this.refreshEthWallet();
  }
  _handleRegisterProfileResponse(event) {
    console.log(event.detail.response);
    this.refreshEthWallet();
    this.refreshProfilesList();
  }

  _keyPressedSendETHTransaction(event) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.sendGenericTransaction();
      return false;
    }
    return true;
  }
  openEthSendDialog(event) {
    this._ethTransactionSent = false;
    var agentid = event.target.getAttribute('data-agentid');
    console.log("agentid chosen: " + agentid);
    this._chosenAgentID = agentid;
    this.$.sendEthDialog.open();
    console.log("modal opened");
  }
  closeEthDialog() {
    this.$.sendEthDialog.close();
    this._ethTransactionSent = false;
  }
  sendGenericTransaction() {
    console.log("sending transaction: " + this.$.SendETHTransactionWeiAmount.value);
    let req = this.$.ajaxGenericTransaction;
    req.body = new FormData();
    req.body.append('agentid', this.$.SendETHTransactionAgentID.value);
    req.body.append('weiAmount', this.$.SendETHTransactionWeiAmount.value);
    req.body.append('message', this.$.SendETHTransactionMessage.value);
    req.generateRequest();
  }
  _handleGenericTransactionResponse(event)
  {
    this._ethTransactionSent = true;
    setTimeout(function () { appThis.closeEthDialog(); }, 200);
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

window.customElements.define('eth-view', EthereumView);
