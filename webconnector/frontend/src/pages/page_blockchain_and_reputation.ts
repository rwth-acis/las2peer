import { PaperDialogElement } from '@polymer/paper-dialog/paper-dialog';
import { html, css, customElement, property } from 'lit-element';

import config from '../config.js';
import { PageElement } from '../helpers/page-element.js';
import '../components/custom_star_rating.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/notification-icons.js';
import '@polymer/iron-collapse/iron-collapse.js';
import '@polymer/iron-form/iron-form.js';
import '@polymer/paper-badge/paper-badge.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-dropdown-menu/paper-dropdown-menu.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-item/paper-item.js';
import '@polymer/paper-listbox/paper-listbox.js';
import '@polymer/paper-spinner/paper-spinner.js';
import '@polymer/paper-tabs/paper-tabs.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import '@polymer/paper-dialog/paper-dialog.js';
import '@polymer/iron-pages/iron-pages.js';
import { request, RequestResponse } from '../helpers/request_helper.js';

@customElement('page-eth-tools')
export class PageHome extends PageElement {
  static styles = css`
    section {
      padding: 1rem;
    }
    :host {
      display: block;
      padding: 10px;
    }
    .flex-horizontal {
      display: var(--layout-horizontal_-_display);
      -ms-flex-direction: var(--layout-horizontal_-_-ms-flex-direction);
      /* -webkit-flex-direction: var(--layout-horizontal_-_-webkit-flex-direction); */
      flex-direction: var(--layout-horizontal_-_flex-direction);
    }
    .agentList,
    .profileList {
      overflow-y: scroll;
      max-height: 350px;
    }

    pre {
      overflow-x: scroll;
      min-width: 200px;
      max-width: 300px;
      background: #f5f5f5;
      padding: 2px 5px;
      min-height: 2em;
      font-size: smaller;
    }

    .walletInfo {
      max-width: 30%;
    }

    dl {
      width: 100%;
      overflow: hidden;
      padding: 0;
      margin: 0;
    }
    dt {
      float: left;
      clear: left;
      width: 50%;
      padding: 0;
      margin: 0;
    }
    dd {
      float: left;
      width: 45%;
      padding: 0;
      margin: 0;
      padding-right: 5%;
      text-align: right;
    }

    th {
      background: #eff5ff;
      padding: 5px 2px;
      color: #16499c;
      text-align: left;
    }

    tr:nth-child(even) {
      background-color: #fafafa;
    }
  `;
  @property({ type: String })
  apiEndpoint: string | undefined;
  @property({ type: String })
  agentId = '';
  @property({ type: String })
  error: any;
  @property({ type: String })
  group = '';
  @property({ type: Array })
  groups: any = [];
  @property({ type: Object })
  _groupSelected: any;
  @property({ type: Boolean, attribute: true })
  _working = false;
  @property({ type: String })
  _chosenAgentID = '';
  @property({ type: String })
  _chosenUsername = '';
  @property({ type: Boolean })
  _ethTransactionSent = false;
  @property({ type: Object })
  _EthWallet = {
    agentid: '',
    email: '',
    ethAccBalance: 0,
    ethAgentAddress: '',
    ethAgentCredentialsAddress: '',
    ethCumulativeScore: ' 0',
    ethMnemonic: '',
    ethNoTransactionsRcvd: 0,
    ethNoTransactionsSent: 0,
    ethProfileOwner: '',
    ethRating: 0,
    username: '',
    rcvdTx: [],
  };
  @property({ type: Object })
  _ethFaucetLog = {
    ethFaucetAmount: 0,
    rewardDetails: {
      u: 0,
      h: 0,
      d: 0,
      userRatingScore: 0,
      hostingServicesScore: 0,
      rewardedForServicesHosting: [],
      developServicesScore: 0,
      rewardedForServicesDevelop: [],
    },
  };
  @property({ type: Object })
  _ethTxLog: any = {
    rcvdJsonLog: [],
    sentJsonLog: [],
  };
  @property({ type: Object })
  _ethCoinbaseInfo = {
    coinbaseAddress: '',
    coinbaseBalance: '',
  };
  @property({ type: Boolean })
  _hasEthProfile = false;
  @property({ type: Boolean })
  _hasNoAgentsList = true;
  @property({ type: Boolean })
  _hasNoEthWallet = true;
  @property({ type: Boolean })
  _hasNoProfilesList = true;
  @property({ type: Boolean })
  _hasNoDashboard = true;
  @property({ type: Boolean })
  _hasNoTxLog = true;
  @property({ type: Array })
  _listDashboard: any = [];
  @property({ type: Array })
  _listAgents = [];
  @property({ type: Array })
  _listProfiles = [];
  @property({ type: Number, attribute: true, reflect: true })
  _selectedTab = 0;
  timer: NodeJS.Timeout | undefined;

  render() {
    return html`
      <div class="card">
        <!-- Toast Messages -->
        <paper-toast id="toast" horizontal-align="right"></paper-toast>
        <!-- Dialog Boxes -->
        <paper-dialog id="ethFaucetDiaLog">
          <h1>
            <iron-icon icon="receipt"></iron-icon> Faucet Transaction Log -
            transaction successful.
          </h1>
          <paper-dialog-scrollable>
            <iron-icon icon="face"></iron-icon>
            <strong>UserRating Score</strong>:
            ${this._ethFaucetLog.rewardDetails.userRatingScore} <br />
            <p>
              <iron-icon icon="important-devices"></iron-icon>
              <strong>HostingServices Score</strong>:
              ${this._ethFaucetLog.rewardDetails.hostingServicesScore}
              ${this._ethFaucetLog.rewardDetails.rewardedForServicesHosting
                ? html`, rewarded for the following services: <br />
                    <ul>
                      ${this._ethFaucetLog.rewardDetails.rewardedForServicesHosting.map(
                        (service) => html` <li>${service}</li> `
                      )}
                    </ul>`
                : html``}
            </p>
            <p>
              <iron-icon icon="cloud-upload"></iron-icon>
              <strong>DevelopServices Score</strong>:
              ${this._ethFaucetLog.rewardDetails.developServicesScore}
              ${this._ethFaucetLog.rewardDetails.rewardedForServicesDevelop
                ? html`, rewarded for the following services: <br />
                    <ul>
                      ${this._ethFaucetLog.rewardDetails.rewardedForServicesDevelop.map(
                        (service) => html` <li>${service}</li> `
                      )}
                    </ul>`
                : html``}
            </p>
            <p>
              <iron-icon icon="redeem"></iron-icon>
              <strong>Total Faucet Payout</strong>:
              ${this._ethFaucetLog.ethFaucetAmount} <br />
              The reputation pay-out has been obtained as follows:
            </p>
            <p>
              UserRating
              <small>(* ${this._ethFaucetLog.rewardDetails.u})</small> * [
              &nbsp;HostingServices
              <small>(* ${this._ethFaucetLog.rewardDetails.h}&nbsp;%)</small>
              &nbsp;+&nbsp; DevelopServices
              <small>(* ${this._ethFaucetLog.rewardDetails.d}&nbsp;%)</small> ]
            </p>
            <p>
              <small>
                A UserRating of "1" in case there is no reputation profile or no
                incoming votes indicates a placeholder. <br />
                If no contributions are detected, the payout is still 0.
              </small>
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
          <h1>Transfer L2Pcoin to ${this._chosenUsername}</h1>
          <paper-dialog-scrollable>
            <div class="horizontal layout center-justified">
              <paper-spinner ?active=${this._working}></paper-spinner>
              ${this._ethTransactionSent
                ? html`<iron-icon icon="done"></iron-icon>`
                : html``}
            </div>
            <iron-form @keypress="_keyPressedSendETHTransaction">
              <form>
                <paper-input
                  label="AgentID"
                  id="SendETHTransactionAgentID"
                  hidden
                  readonly
                  value=${this._chosenAgentID}
                ></paper-input>
                <paper-input
                  label="Username"
                  id="SendETHTransactionUserName"
                  hidden
                  readonly
                  value=${this._chosenUsername}
                ></paper-input>
                <paper-input
                  label="Amount (in L2Pcoin)"
                  id="SendETHTransactionWeiAmount"
                  disabled=${this._working}
                  value=""
                ></paper-input>
                <paper-textarea
                  label="Transaction Message"
                  disabled=${this._working}
                  id="SendETHTransactionMessage"
                ></paper-textarea>
              </form>
            </iron-form>
          </paper-dialog-scrollable>
          <div class="buttons">
            <paper-button dialog-confirm raised class="red">
              <iron-icon icon="block"></iron-icon> Cancel
            </paper-button>
            <paper-button
              raised
              @click=${this.sendGenericTransaction}
              disabled=${this._working}
              class="green"
            >
              <iron-icon icon="check"></iron-icon> Send L2Pcoin Transaction
            </paper-button>
          </div>
        </paper-dialog>
        <h1>Blockchain and Reputation</h1>
        <paper-spinner
          ?active=${this._working}
          style="float:right;"
        ></paper-spinner>
        <div class="introText">
          <p class="description">
            To incentivize las2peer users to contribute to the community, a
            reputation system has been introduced. <br />
            Technically, this reputation is represented by a las2peer-internal
            cryptocurrency called <strong>L2Pcoin</strong>.
          </p>
          <p class="description">
            While user agents can be rated directly, the value a service
            provides to the different communities cannot be immediately
            evaluated and thus is based on the
            <a
              href="https://github.com/rwth-acis/mobsos-success-modeling/wiki/Manual"
              >MobSOS success model</a
            >. This means, that each community
            <small>(<em>represented by a las2peer group</em>)</small> can use a
            separate success model to easily rate the value a service is
            providing to them. For security reasons, the rewarded user agent
            also must be part of the group, otherwise they would not have access
            to the rating value assigned to the hosted/developed service.
          </p>
          <p class="description">
            To differentiate the effort related to
            <strong>service development</strong> and
            <strong>service hosting</strong>, two community service values have
            been introduced
            <iron-icon icon="help-outline" id="mobsosValues"></iron-icon>:
            <br />
          </p>
          <ol>
            <li>
              The agent responsible for <strong>hosting</strong> a service is
              the administrator of the las2peer node on which the service is
              running.
            </li>
            <li>
              The user who publishes a service is understood as its
              <strong>developer</strong> as they provide the jar binary
              containing the files necessary to deploy.
            </li>
          </ol>
          <p class="description">
            <strong>User rating</strong> is used to multiply the value gained by
            counting the services hosted and/or developed by the requesting
            agent and thus gives the community a more personal way of
            influencing the amount of reputation gained by hosting. <br />
            In other words,
            <em
              >an unpopular user will gain less for hosting a popular service
              than a popular user hosting an unpopular service.
            </em>
          </p>
          <paper-tooltip for="mobsosValues" offset="0">
            See class i5.las2peer.registry.data.RegistryConfiguration for
            details
          </paper-tooltip>
        </div>
        <hr />
        <!-- ETH WALLET -->
        <!-- <template
          ><template is="dom-if" if="[[agentId.length>5]]"> -->
        <h2>
          Reputation Wallet
          <paper-icon-button
            icon="refresh"
            title="Refresh Reputation Wallet"
            @click=${this.refreshEthWallet}
            ?disabled=${this._working}
          ></paper-icon-button>
        </h2>
        <paper-spinner
          ?active=${this._working}
          style="float:right;"
        ></paper-spinner>
        ${this._hasNoEthWallet == false
          ? html` <div class="flex-horizontal">
              <!-- LEFT HAND SIDE -->
              <div class="flexchild">
                <!--  WELCOME -->
                Welcome, ${this._EthWallet.username}
                <dl>
                  ${this._hasEthProfile == true
                    ? html` <!-- USER RATING -->
                        <dt>
                          <iron-icon icon="account-circle"></iron-icon> User
                          rating
                        </dt>
                        <dd>
                          <custom-star-rating
                            value=${this._EthWallet.ethRating}
                            readonly
                            single
                          ></custom-star-rating>
                        </dd>
                        <!-- NO. OF VOTES -->
                        <dt>
                          <iron-icon icon="assessment"></iron-icon> Number of
                          user ratings
                        </dt>
                        <dd>
                          <iron-icon
                            icon="cloud-download"
                            title="Incoming votes"
                          ></iron-icon>
                          ${this._ethTxLog.rcvdJsonLog.length} |
                          <iron-icon
                            icon="cloud-upload"
                            title="Outgoing votes"
                          ></iron-icon>
                          ${this._ethTxLog.sentJsonLog.length}
                        </dd>`
                    : html``}

                  <!-- ACCUMULATED REPUTATION -->
                  <dt>
                    <iron-icon icon="account-balance-wallet"></iron-icon>
                    Accumulated reputation
                  </dt>
                  <dd>${this._EthWallet.ethAccBalance} L2Pcoin</dd>
                  <!--  TOTAL REPUTATION -->
                  <dt>
                    <iron-icon icon="account-balance"></iron-icon> Total
                    reputation available for request
                    <iron-icon
                      id="totalReputation"
                      icon="help-outline"
                    ></iron-icon>
                    <paper-tooltip for="totalReputation" offset="0">
                      This value represents the total amount of reputation that
                      can be paid out to all users. <br />
                      Technically, it's the amount of Ether in the coinbase
                      account, <br />
                      i.e. the account which by default configuration is
                      rewarded the mined coins.
                    </paper-tooltip>
                  </dt>
                  <dd>${this._ethCoinbaseInfo.coinbaseBalance} L2Pcoin</dd>
                  <!-- REQUEST  PAY-OUT -->
                  <dt>
                    <paper-dropdown-menu
                      style="min-width: 250px"
                      label="Group to use for Success Modeling"
                      @change="_updateGroupMemberlist"
                      noink
                      no-animations
                      selected-item=${this._groupSelected}
                    >
                      <paper-listbox
                        slot="dropdown-content"
                        class="dropdown-content"
                        id="groupSelect"
                      >
                        ${this.groups.map(
                          (item: any) => html`
                            <paper-item value=${item.groupID}
                              >${item.groupName}</paper-item
                            >
                          `
                        )}
                      </paper-listbox>
                    </paper-dropdown-menu>
                  </dt>
                  <dd>
                    <paper-button
                      class="green"
                      style="margin-top:10px"
                      raised
                      @click=${this.requestEthFaucet}
                      ?disabled=${this._working}
                    >
                      <iron-icon icon="card-giftcard"></iron-icon> Request
                      reputation pay-out
                    </paper-button>
                  </dd>
                </dl>
                <!-- REQUEST REPUTATION PROFILE (OPT-IN) -->
                ${this._hasEthProfile == false
                  ? html`
                      ${this._EthWallet.ethAccBalance
                        ? html` <p class="description">
                              las2peer user reputation requires users to
                              <em>opt-in</em> to the system to rate others and,
                              most importantly, be rated by others. Each
                              transaction on the blockchain
                              <small
                                >(<em
                                  >which is the backing mechanism of las2peer
                                  reputation</em
                                >)</small
                              >
                              requires a small transaction fee. To welcome new
                              users to the community
                              <small
                                >(<em
                                  >through
                                  <abbr
                                    title="Legitimate peripheral participation"
                                    >LPP</abbr
                                  ></em
                                >)</small
                              >, a small amount of reputation is paid out on
                              their first pay-out request to allow them to
                              participate in the user rating system.
                            </p>
                            <paper-button
                              class="green"
                              id="reputationOptIn"
                              raised
                              @click=${this.requestReputationProfile}
                              ?disabled=${this._working}
                            >
                              <iron-icon icon="record-voice-over"></iron-icon>
                              Opt-in to reputation
                            </paper-button>`
                        : html``}
                    `
                  : html``}
              </div>
              <!-- END LEFT HAND SIDE -->
              <!-- RIGHT HAND SIDE -->
              <div class="walletInfo">
                <!-- ETH WALLET INFO -->
                <div class="ethInfo">
                  <h4 id="ethInfoTitle">
                    L2Pcoin Wallet Info
                    <iron-icon icon="help-outline"></iron-icon>
                  </h4>
                  <p class="description">
                    The las2peer (<small
                      ><em
                        >L2P, the namesake for the cryptocurrency L2Pcoin</em
                      ></small
                    >) reputation profile is implemented by means of an Ethereum
                    Wallet. <br />
                    The wallet address can be used to send and receive
                    transactions on the blockchain. <br />
                    The provided mnemonic is generated according to the
                    <a
                      href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki"
                      target="_blank"
                      >BIP-39</a
                    >
                    standard.
                  </p>
                  <strong
                    ><iron-icon icon="fingerprint"></iron-icon>Wallet
                    Address</strong
                  >:
                  <pre>${this._EthWallet.ethAgentCredentialsAddress}</pre>
                  <strong
                    ><iron-icon icon="verified-user"></iron-icon>Wallet
                    Mnemonic</strong
                  >:
                  <pre>${this._EthWallet.ethMnemonic}</pre>
                </div>
              </div>
              <!-- END RIGHT HAND SIDE -->
            </div>`
          : html``}

        <!-- END PROFILE -->
        <!-- </template> -->
        ${this._hasEthProfile == true
          ? html` <paper-tabs selected=${this._selectedTab}>
                <paper-tab @click=${() => (this._selectedTab = 0)}>
                  <span id="dashboard"
                    ><iron-icon icon="store"></iron-icon> Dashboard</span
                  >
                  <paper-spinner
                    ?active=${this._working}
                    style="float:right;"
                  ></paper-spinner>
                </paper-tab>
                <paper-tab @click=${() => (this._selectedTab = 1)}>
                  <span id="faucet-tx"
                    ><iron-icon icon="assignment"></iron-icon> Repuation pay-out
                    Log</span
                  >
                  <paper-badge
                    for="faucet-tx"
                    label=${this._EthWallet.rcvdTx.length}
                  ></paper-badge>
                </paper-tab>
                <paper-tab @click=${() => (this._selectedTab = 2)}>
                  <span id="rcvd-tx"
                    ><iron-icon icon="cloud-download"></iron-icon> Incoming
                    reputation</span
                  >
                  <paper-badge
                    for="rcvd-tx"
                    label=${this._ethTxLog.rcvdJsonLog.length}
                  ></paper-badge>
                </paper-tab>
                <paper-tab @click=${() => (this._selectedTab = 3)}>
                  <span id="sent-tx"
                    ><iron-icon icon="cloud-upload"></iron-icon> Outgoing
                    reputation</span
                  >
                  <paper-badge
                    for="sent-tx"
                    label=${this._ethTxLog.sentJsonLog.length}
                  ></paper-badge>
                </paper-tab>
              </paper-tabs>
              <iron-pages selected=${this._selectedTab}>
                <div class="dashboardList">
                  <!-- Reputation dashboard -->
                  <table width="100%">
                    <tr>
                      <th>
                        <iron-icon
                          icon="perm-identity"
                          title="Username"
                        ></iron-icon>
                        Username
                      </th>
                      <th>
                        <iron-icon
                          icon="account-circle"
                          title="Rate user"
                        ></iron-icon>
                        Rate user
                      </th>
                      <th>
                        <iron-icon
                          icon="assessment"
                          title="Reputation Statistics"
                        ></iron-icon>
                        Reputation Statistics
                      </th>
                      <th>
                        <iron-icon
                          icon="fingerprint"
                          title="Wallet address"
                        ></iron-icon>
                        Wallet address
                      </th>
                      <th>
                        <iron-icon icon="touch-app" title="Actions"></iron-icon>
                        Actions
                      </th>
                    </tr>

                    ${this._listDashboard.map(
                      (agent: {
                        username: unknown;
                        agentHasProfile: any;
                        ethRating: unknown;
                        noOfTransactionsRcvd: unknown;
                        noOfTransactionsSent: unknown;
                        address: unknown;
                        agentid: unknown;
                      }) => html` <tr>
                        <td>${agent.username}</td>
                        <td>
                          <!-- RATING -->
                          ${agent.agentHasProfile
                            ? html` <custom-star-rating
                                @rating-selected="rateAgent"
                                value=${agent.ethRating}
                              ></custom-star-rating>`
                            : html``}
                          ${!agent.agentHasProfile
                            ? html`
                                <custom-star-rating
                                  readonly
                                ></custom-star-rating>
                              `
                            : html``}
                        </td>
                        <td>
                          <!-- VOTING STATISTICS -->
                          <div
                            style="display: flex; align-content: center; align-items: center;"
                          >
                            ${!agent.agentHasProfile ? html` - ` : html``}
                            ${!agent.agentHasProfile
                              ? html`
                                  <iron-icon
                                    icon="notification:network-check"
                                    title="User reputation"
                                  ></iron-icon>
                                  <custom-star-rating
                                    value=${agent.ethRating}
                                    readonly=""
                                    single=""
                                  ></custom-star-rating>
                                  ( ${agent.ethRating} ) &nbsp;|&nbsp;
                                  <iron-icon
                                    icon="cloud-download"
                                    title="Incoming votes"
                                  ></iron-icon>
                                  &nbsp;${agent.noOfTransactionsRcvd}
                                  &nbsp;|&nbsp;
                                  <iron-icon
                                    icon="cloud-upload"
                                    title="Outgoing votes"
                                  ></iron-icon>
                                  &nbsp;${agent.noOfTransactionsSent}
                                `
                              : html``}
                          </div>
                        </td>
                        <td>
                          <pre class="address">${agent.address}</pre>
                        </td>
                        <td>
                          <paper-icon-button
                            icon="card-giftcard"
                            title="Transfer L2Pcoin to Agent"
                            @click=${this.openEthSendDialog}
                            data-username=${agent.username}
                            data-agentid=${agent.agentid}
                            disabled=${this._working}
                          ></paper-icon-button>
                        </td>
                      </tr>`
                    )}
                  </table>
                  <!-- End Reputation dashboard -->
                </div>
                <div>
                  <!-- Faucet Payout-Log -->
                  <table width="100%">
                    <tr>
                      <th>Timestamp</th>
                      <th>TransactionValue</th>
                    </tr>
                    ${this._EthWallet.rcvdTx.map(
                      (tx: {
                        blockDateTime: unknown;
                        value: unknown;
                      }) => html` <tr>
                        <td>
                          <iron-icon icon="update"></iron-icon>
                          ${tx.blockDateTime}
                        </td>
                        <td>
                          <iron-icon icon="card-giftcard"></iron-icon>
                          ${tx.value} L2Pcoin
                        </td>
                      </tr>`
                    )}
                  </table>
                </div>
                <!-- End Faucet Pay-out Log -->
                <div>
                  <!-- Incoming TX Log -->
                  <table width="100%">
                    <tr>
                      <th>Timestamp</th>
                      <th>Sender</th>
                      <th>TransactionType</th>
                      <th>Message</th>
                      <th>TransactionValue</th>
                    </tr>

                    ${this._ethTxLog.rcvdJsonLog.map(
                      () =>
                        html` ${this._ethTxLog.rcvdJsonLog.map(
                          (tx: {
                            sender: string;
                            txDateTime: unknown;
                            txSenderAddress: unknown;
                            txTransactionType: unknown;
                            txMessage: unknown;
                            transactionType: any;
                            txAmountInEth: unknown;
                          }) =>
                            html` ${tx.sender ==
                            this._ethCoinbaseInfo.coinbaseAddress
                              ? html` <tr>
                                  <td>
                                    <iron-icon icon="update"></iron-icon>
                                    ${tx.txDateTime}
                                  </td>
                                  <td>
                                    <iron-icon icon="face"></iron-icon>
                                    ${tx.txSenderAddress}
                                  </td>
                                  <td>
                                    <iron-icon icon="class"></iron-icon>
                                    ${tx.txTransactionType}
                                  </td>
                                  <td>
                                    <iron-icon icon="speaker-notes"></iron-icon>
                                    ${tx.txMessage}
                                  </td>
                                  <td>
                                    ${this._isUserRating(tx.transactionType)
                                      ? html` <iron-icon
                                            icon="record-voice-over"
                                          ></iron-icon>
                                          Rating`
                                      : html``}
                                    ${!this._isUserRating(tx.transactionType)
                                      ? html` ${tx.txAmountInEth
                                          ? html` <iron-icon
                                                icon="card-giftcard"
                                              ></iron-icon>
                                              ${tx.txAmountInEth} L2Pcoin`
                                          : html``}
                                        Rating`
                                      : html``}
                                  </td>
                                </tr>`
                              : html``}`
                        )}`
                    )}
                  </table>
                </div>
                <!-- End Incoming TX Log -->
                <div>
                  <!-- Outgoing TX Log -->
                  <table width="100%">
                    <tr>
                      <th>Timestamp</th>
                      <th>Receiver</th>
                      <th>TransactionType</th>
                      <th>Message</th>
                      <th>TransactionValue</th>
                    </tr>

                    ${this._ethTxLog.sentJsonLog.map(
                      (tx: {
                        txDateTime: unknown;
                        txReceiverAddress: unknown;
                        txTransactionType: unknown;
                        txMessage: unknown;
                        transactionType: string;
                        txAmountInEth: unknown;
                      }) => html` <tr>
                        <td>
                          <iron-icon icon="update"></iron-icon>
                          ${tx.txDateTime}
                        </td>
                        <td>
                          <iron-icon icon="face"></iron-icon>
                          ${tx.txReceiverAddress}
                        </td>
                        <td>
                          <iron-icon icon="class"></iron-icon>
                          ${tx.txTransactionType}
                        </td>
                        <td>
                          <iron-icon icon="speaker-notes"></iron-icon>
                          ${tx.txMessage}
                        </td>
                        <td>
                          ${this._isUserRating(tx.transactionType)
                            ? html` <iron-icon
                                  icon="record-voice-over"
                                ></iron-icon>
                                Rating`
                            : html``}
                          ${!this._isUserRating(tx.transactionType)
                            ? html`
                                ${tx.txAmountInEth
                                  ? html` <iron-icon
                                        icon="card-giftcard"
                                      ></iron-icon>
                                      ${tx.txAmountInEth} L2Pcoin`
                                  : html``}
                              `
                            : html``}
                        </td>
                      </tr>`
                    )}
                  </table>
                </div>
                <!-- End outgoing TX log -->
              </iron-pages>`
          : html``}

        <!-- END TX LOG -->
      </div>
    `;
  }
  firstUpdated() {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    this.refreshWallet();
    this.timer = setInterval(() => this.refreshWallet(), 5000);
  }
  async refreshWallet() {
    this.refreshAgentsList();
    this.refreshProfilesList();
    // if (this.agentId.length > 5) {
    this.refreshEthWallet();
    // }
  }
  async refreshEthWallet() {
    const response = await request(config.url + '/las2peer/eth/dashboardList', {
      method: 'POST',
    });
    this._handleDashboardListResponse(response);
    const responseWallet = await request(
      config.url + '/las2peer/eth/getEthWallet',
      {
        method: 'POST',
      }
    );
    this._handleGetEthWalletResponse(responseWallet);
    const responseLog = await request(
      config.url + '/las2peer/eth/getGenericTxLog',
      {
        method: 'POST',
      }
    );
    this._handleGenericTxLogResponse(responseLog);
    const responseBalance = await request(
      config.url + '/las2peer/eth/getCoinbaseBalance',
      {
        method: 'POST',
      }
    );
    this._handleGetCoinbaseBalanceResponse(responseBalance);
  }
  async requestEthFaucet() {
    if (this._groupSelected == null || this._groupSelected.innerHTML == null) {
      this.group = '';
    } else {
      this.group = this._findGroupIDByName(
        this._groupSelected.innerHTML.trim()
      );
    }
    console.log('faucet request for groupID: ' + this.group);
    const body = new FormData();
    body.append('groupID', this.group);
    const response = await request(config.url + '/las2peer/eth/requestFaucet', {
      method: 'POST',
      body: body,
    });
    this._handleRequestFaucetResponse(response);
  }
  async requestReputationProfile() {
    if (this._EthWallet.ethAccBalance < 0.01) {
      this.error = {
        title: 'Not enough funds',
        msg: 'Try requesting reputation (L2Pcoin) from the faucet. <br>\n Hosting or publishing services will positively influence Your reputation. <br>\n Interact with users and get them to rate You positively, the rating is factored in the reputation payout.',
      };
      return;
    } else {
      const response = await request(
        config.url + '/las2peer/eth/registerProfile',
        {
          method: 'POST',
        }
      );
      this._handleRegisterProfileResponse(response);
    }
  }
  async refreshAgentsList() {
    const response = await request(config.url + '/las2peer/eth/listAgents', {
      method: 'POST',
    });
    this._handleLoadAgentlistResponse(response);
  }
  async refreshProfilesList() {
    const response = await request(config.url + '/las2peer/eth/listProfiles', {
      method: 'POST',
    });
    this._handleLoadProfilelistResponse(response);
  }
  async toggleCreateAgent() {
    // this.$.collapseCreateAgent.toggle();
  }
  async toggleAgentList() {
    // this.$.collapseAgentList.toggle();
  }
  async toggleProfileList() {
    // this.$.collapseProfileList.toggle();
  }
  async toggleEthWallet() {
    // this.$.collapseEthWallet.toggle();
  }

  _agentIdChanged(agentid: any) {
    if (this.agentId == '') return;
    if (this.agentId.length > 5) {
      this.refreshEthWallet();
    }
  }

  _findGroupIDByName(name: any) {
    return this.groups.find((g: { groupName: any }) => g.groupName == name)
      .groupID;
  }

  _updateGroups(event: { detail: { response: any } }) {
    const res = event.detail.response;
    this.groups = [];
    const keys = Object.keys(res);
    for (let i = 0; i < keys.length; i++) {
      this.groups.push({
        groupID: keys[i],
        groupName: res[keys[i]],
      });
    }
    if (keys.length > 0) {
      if (this.$.groupSelect.value.length > 0) {
        this._updateGroupMemberlist();
      } else {
        this.group = keys[0];
      }
    }
  }
  _updateGroupMemberlist() {
    this.group = this._findGroupIDByName(this._groupSelected.innerHTML.trim());
  }

  _handleGenericTxLogResponse(event: any) {
    this._ethTxLog = event;
    if (
      this._ethTxLog.rcvdJsonLog.length == 0 &&
      this._ethTxLog.sentJsonLog.length == 0
    )
      this._hasNoTxLog = true;
    else this._hasNoTxLog = false;
  }

  _handleDashboardListResponse(event: any) {
    const response = event;
    response.agentList.forEach(function (element: {
      shortid: string;
      agentid: string;
    }) {
      element.shortid = element.agentid.substr(0, 15) + '...';
    });
    this._listDashboard = response.agentList;
    this._hasNoDashboard = false;
  }

  _handleLoadAgentlistResponse(event: any) {
    const response = event;
    response.agents.forEach(function (element: {
      shortid: string;
      agentid: string;
    }) {
      element.shortid = element.agentid.substr(0, 15) + '...';
    });
    this._listAgents = response.agents;
    this._hasNoAgentsList = false;
  }
  _handleLoadProfilelistResponse(event: any) {
    const response = event;
    response.agents.forEach(function (element: {
      shortid: string;
      agentid: string;
    }) {
      element.shortid = element.agentid.substr(0, 15) + '...';
    });
    this._listProfiles = response.agents;
    this._hasNoProfilesList = false;
  }
  _handleRateAgentResponse(event: { detail: { response: any } }) {
    const response = event.detail.response;

    // this.$.toast.innerHTML =
    //   'Rating (' +
    //   response.recipientname +
    //   ': ' +
    //   response.rating +
    //   ') successfully casted.';
    // this.$.toast.open();
    // this.refreshWallet();
  }
  rateAgent(event: {
    model: { get: (arg0: string) => any };
    detail: { rating: any };
  }) {
    //let req = this.$.ajaxRateAgentMock;
    // const req = this.$.ajaxRateAgent;
    // req.body = new FormData();
    // req.body.append('agentid', event.model.get('agent.agentid'));
    // req.body.append('rating', event.detail.rating);
    // req.generateRequest();
    //this.$.toast.innerHTML = 'Rating (' + event.model.get('agent.username') + ': '+ event.detail.rating + ') successfully casted.';
    //this.$.toast.open();
  }
  _handleGetCoinbaseBalanceResponse(event: any) {
    this._ethCoinbaseInfo = event;
  }
  _handleGetEthWalletResponse(event: any) {
    this._hasNoEthWallet = false;
    this._EthWallet = event;

    if (this._EthWallet.ethCumulativeScore !== '???') {
      this._hasEthProfile = true;
    }
    if (this._EthWallet.rcvdTx.length == 0) {
      this._hasNoTxLog = true;
    } else {
      this._hasNoTxLog = false;
    }
    this._EthWallet.ethAccBalance = parseFloat(this._EthWallet.ethAccBalance);
    // this.$.ajaxGetGroups.headers = window.rootThis.$.ajaxLogin.headers;
    // this.$.ajaxGetGroups.generateRequest();
  }
  _handleRequestFaucetResponse(event: any) {
    this._ethFaucetLog = event;
    (
      this.shadowRoot?.getElementById('ethFaucetDiaLog') as PaperDialogElement
    ).open();
    this.refreshEthWallet();
  }
  _handleRegisterProfileResponse(event: RequestResponse) {
    this.refreshEthWallet();
    this.refreshProfilesList();
  }

  _keyPressedSendETHTransaction(event: {
    which: number;
    keyCode: number;
    preventDefault: () => void;
  }) {
    if (event.which == 13 || event.keyCode == 13) {
      event.preventDefault();
      this.sendGenericTransaction();
      return false;
    }
    return true;
  }
  openEthSendDialog(event: {
    target: { getAttribute: (arg0: string) => any };
  }) {
    this._ethTransactionSent = false;
    const agentid = event.target.getAttribute('data-agentid');
    const username = event.target.getAttribute('data-username');
    this._chosenAgentID = agentid;
    this._chosenUsername = username;
    (
      this.shadowRoot?.getElementById('sendEthDialog') as PaperDialogElement
    ).open();
  }
  closeEthDialog() {
    (
      this.shadowRoot?.getElementById('sendEthDialog') as PaperDialogElement
    ).close();
    this._ethTransactionSent = false;
  }
  sendGenericTransaction() {
    console.log(
      'sending transaction: ' + this.$.SendETHTransactionWeiAmount.value
    );
    const req = this.$.ajaxGenericTransaction;
    req.body = new FormData();
    req.body.append('agentid', this.$.SendETHTransactionAgentID.value);
    req.body.append('weiAmount', this.$.SendETHTransactionWeiAmount.value);
    req.body.append('message', this.$.SendETHTransactionMessage.value);
    req.generateRequest();
  }
  _handleGenericTransactionResponse(event: any) {
    this._ethTransactionSent = true;
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ethThis = this;
    setTimeout(function () {
      ethThis.closeEthDialog();
    }, 200);
  }

  _handleError(object: any, title: any, message: any) {
    window.rootThis._handleError(object, title, message);
  }

  _isUserRating(val: string) {
    return val == 'L2P USER RATING';
  }
}
