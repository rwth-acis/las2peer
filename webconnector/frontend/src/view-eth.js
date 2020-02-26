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
import '@polymer/paper-badge/paper-badge.js';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-dropdown-menu/paper-dropdown-menu.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-item/paper-item.js';
import '@polymer/paper-listbox/paper-listbox.js';
import '@polymer/paper-spinner/paper-spinner.js';
import '@polymer/paper-tabs/paper-tabs.js';
import '@polymer/paper-tooltip/paper-tooltip.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/notification-icons.js';
import './custom-star-rating.js';
import './shared-styles.js';

class EthereumView extends PolymerElement {
	static get template() {
		return html`
			<iron-ajax id="ajaxDashboardList"
								 method="POST"
								 url$="[[apiEndpoint]]/eth/dashboardList"
								 handle-as="json"
								 on-response="_handleDashboardListResponse"
								 on-error="_handleError"
								 loading="{{_working}}"></iron-ajax>
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
			<iron-ajax id="ajaxGetGroups"
								 url='[[baseUrl]]/contactservice/groups'
								 params='{}'
								 handle-as="json"
								 on-response="_updateGroups"
								 on-error="_handleError"
								 loading="{{_working}}"></iron-ajax>

			<style include="shared-styles">
				:host {
					display: block;
					padding: 10px;
				}

				.agentList, .profileList {
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

			</style>

			<div class="card">
						 
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
								<iron-icon icon="redeem"></iron-icon> <strong>Total Faucet Payout</strong>: [[_ethFaucetLog.ethFaucetAmount]] <br />
								The reputation pay-out has been obtained as follows: 
							</p>
							<p>
								UserRating <small>(* [[_ethFaucetLog.rewardDetails.u]])</small> * <br />
								 [ &nbsp;HostingServices <small>(* [[_ethFaucetLog.rewardDetails.h]])</small>&nbsp;+&nbsp;DevelopServices <small>(* [[_ethFaucetLog.rewardDetails.d]])</small> ]
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
					<h1>Transfer L2Pcoin to [[_chosenUsername]]</h1>
					<paper-dialog-scrollable>
						<div class="horizontal layout center-justified">
							<paper-spinner active="[[_working]]"></paper-spinner>
							<template is="dom-if" if="[[_ethTransactionSent]]">
								<iron-icon icon="done"></iron-icon>
							</template>
						</div>
						<iron-form on-keypress="_keyPressedSendETHTransaction">
							<form>
								<paper-input label="AgentID" id="SendETHTransactionAgentID" hidden readonly value="[[_chosenAgentID]]"></paper-input>
								<paper-input label="Username" id="SendETHTransactionUserName" hidden readonly value="[[_chosenUsername]]"></paper-input>
								<paper-input label="Amount (in L2Pcoin)" id="SendETHTransactionWeiAmount" disabled="[[_working]]" value=""></paper-input>
								<paper-textarea label="Transaction Message" disabled="[[_working]]" id="SendETHTransactionMessage"></paper-textarea>
							</form>
						</iron-form>
					</paper-dialog-scrollable>
					<div class="buttons">
						<paper-button dialog-confirm autofocus raised class="red">
							<iron-icon icon="block"></iron-icon> Cancel
						</paper-button>
						<paper-button raised on-click="sendGenericTransaction" disabled="[[_working]]" class="green">
							<iron-icon icon="check"></iron-icon> Send L2Pcoin Transaction
						</paper-button>
					</div>
				</paper-dialog>

				<h1>
					Blockchain and Reputation
				</h1>

				<paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>

				<div class="introText">
					<p class="description">
						To incentivize las2peer users to contribute to the community, a reputation system has been introduced. <br />
						Technically, this reputation is represented by a las2peer-internal cryptocurrency called <strong>L2Pcoin</strong>. 
					</p>
					<p class="description">
						While user agents can be rated directly, the value a service provides to the different communities cannot be immediately evaluated and thus is based on the <a href="https://github.com/rwth-acis/mobsos-success-modeling/wiki/Manual">MobSOS success model</a>.
						This means, that each community <small>(<em>represented by a las2peer group</em>)</small> can use a separate success model to easily rate the value a service is providing to them.
						For security reasons, the rewarded user agent also must be part of the group, otherwise they would not have access to the rating value assigned to the hosted/developed service.
					</p>
					<p class="description">
						To differentiate the effort related to <strong>service development</strong> and <strong>service hosting</strong>, two community service values have been introduced <iron-icon icon="help-outline" id="mobsosValues"></iron-icon>: <br />
					</p>
					<ol>
							<li>The agent responsible for <strong>hosting</strong> a service is the administrator of the las2peer node on which the service is running.</li>
							<li>The user who publishes a service is understood as its <strong>developer</strong> as they provide the jar binary containing the files necessary to deploy.</li>
					</ol>
					<p class="description">
						<strong>User rating</strong> is used to multiply the value gained by counting the services hosted and/or developed by the requesting agent and thus gives the community a more personal way of influencing the amount of reputation gained by hosting. <br />
						In other words, <em>an unpopular user will gain less for hosting a popular service than a popular user hosting an unpopular service. </em>
					</p>
					<paper-tooltip for="mobsosValues" offset="0">
						See class i5.las2peer.registry.data.RegistryConfiguration for details
					</paper-tooltip>
				</div>

				<hr />

				<!-- ETH WALLET -->
				<template is="dom-if" if="[[agentId.length>5]]">
					<h2>
						Reputation Wallet 
						<paper-icon-button icon="refresh" title="Refresh Reputation Wallet" on-click="refreshEthWallet" disabled="[[_working]]"></paper-icon-button>
					</h2>

					<paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>

					<template is="dom-if" if="[[!_hasNoEthWallet]]">

						<div class="flex-horizontal">

							<!-- LEFT HAND SIDE -->
							<div class="flexchild">

									<!--  WELCOME -->
									Welcome, [[_EthWallet.username]]

									<dl>
											<template is="dom-if" if="[[_hasEthProfile]]">
												<!-- USER RATING -->
												<dt>
													<iron-icon icon="account-circle"></iron-icon> User rating
												</dt>

												<dd>
													<custom-star-rating value="[[_EthWallet.ethRating]]" readonly single></custom-star-rating>
												</dd>


												<!-- NO. OF VOTES -->
												<dt>
													<iron-icon icon="assessment"></iron-icon> Number of user ratings
												</dt>

												<dd>
													<iron-icon icon="cloud-download" title="Incoming votes"></iron-icon> 
													[[_ethTxLog.rcvdJsonLog.length]] | 
													<iron-icon icon="cloud-upload" title="Outgoing votes"></iron-icon> 
													[[_ethTxLog.sentJsonLog.length]]
												</dd>

												</template> 

											<!-- ACCUMULATED REPUTATION -->
											<dt>
												<iron-icon icon="account-balance-wallet"></iron-icon> Accumulated reputation
											</dt>

											<dd>
												[[_EthWallet.ethAccBalance]] L2Pcoin
											</dd>

											<!--  TOTAL REPUTATION -->
											<dt>
												<iron-icon icon="account-balance"></iron-icon> Total reputation available for request
													<iron-icon id="totalReputation" icon="help-outline"></iron-icon>
													<paper-tooltip for="totalReputation" offset="0">
														This value represents the total amount of reputation that can be paid out to all users. <br />
														Technically, it's the amount of Ether in the coinbase account, <br />
														i.e. the account which by default configuration is rewarded the mined coins.
													</paper-tooltip>
											</dt>

											<dd>
												[[_ethCoinbaseInfo.coinbaseBalance]] L2Pcoin
											</dd>

											<!-- REQUEST  PAY-OUT -->
											<dt>
												<paper-dropdown-menu 
													style="min-width: 250px" 
													label="Group to use for Success Modeling" 
													on-change="_updateGroupMemberlist" 
													noink no-animations 
													selected-item="{{_groupSelected}}">
													<paper-listbox slot="dropdown-content" class="dropdown-content" id="groupSelect">
														<template is="dom-repeat" items="[[groups]]">
															<paper-item value="{{item.groupID}}">{{item.groupName}}</paper-item>
														</template>
													</paper-listbox>
												</paper-dropdown-menu>
											</dt>

											<dd>
												<paper-button class="green" style="margin-top:10px" raised on-click="requestEthFaucet" disabled="[[_working]]">
													<iron-icon icon="card-giftcard"></iron-icon> Request reputation pay-out
												</paper-button>
											</dd>
												
									</dl>

									<!-- REQUEST REPUTATION PROFILE (OPT-IN) -->
									<template is="dom-if" if="[[!_hasEthProfile]]">
										<template is="dom-if" if="[[_EthWallet.ethAccBalance]]"> 
											<p class="description">
												las2peer user reputation requires users to <em>opt-in</em> to the system to rate others and, most importantly, be rated by others. 
												Each transaction on the blockchain <small>(<em>which is the backing mechanism of las2peer reputation</em>)</small> requires a small transaction fee.
												To welcome new users to the community <small>(<em>through <abbr title="Legitimate peripheral participation">LPP</abbr></em>)</small>, a small amount of reputation is paid out on their first pay-out request to allow them to participate in the user rating system.
											</p>
											<paper-button class="green" id="reputationOptIn" raised on-click="requestReputationProfile" disabled="[[_working]]">
												<iron-icon icon="record-voice-over"></iron-icon> Opt-in to reputation
											</paper-button>
										</template>
									</template> 

							</div> <!-- END LEFT HAND SIDE -->

							<!-- RIGHT HAND SIDE -->
							<div class="walletInfo">
								<!-- ETH WALLET INFO -->
								<div class="ethInfo">
									<h4 id="ethInfoTitle">L2Pcoin Wallet Info <iron-icon icon="help-outline"></iron-icon></h4>
									<p class="description">
										The las2peer (<small><em>L2P, the namesake for the cryptocurrency L2Pcoin</em></small>) reputation profile is implemented by means of an Ethereum Wallet. <br />
										The wallet address can be used to send and receive transactions on the blockchain. <br />
										The provided mnemonic is generated according to the <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki" target="_blank">BIP-39</a> standard. 
									</p>
									<strong><iron-icon icon="fingerprint"></iron-icon>Wallet Address</strong>:
										<pre>[[_EthWallet.ethAgentCredentialsAddress]]</pre>
									<strong><iron-icon icon="verified-user"></iron-icon>Wallet Mnemonic</strong>:
										<pre>[[_EthWallet.ethMnemonic]]</pre>
							</div><!-- END RIGHT HAND SIDE -->
					</div>
				</template> <!-- END PROFILE -->

				<template is="dom-if" if="[[_hasEthProfile]]">
						<paper-tabs selected="{{_selectedTab}}">
							<paper-tab>
								<span id="dashboard"><iron-icon icon="store"></iron-icon> Dashboard</span>
								<paper-spinner active="[[_working]]" style="float:right;"></paper-spinner>
							</paper-tab>
							<paper-tab>
								<span id="faucet-tx"><iron-icon icon="assignment"></iron-icon> Repuation pay-out Log</span>
								<paper-badge for="faucet-tx" label="[[_EthWallet.rcvdTx.length]]"></paper-badge>
							</paper-tab>
							<paper-tab>
								<span id="rcvd-tx"><iron-icon icon="cloud-download"></iron-icon> Incoming reputation</span>
								<paper-badge for="rcvd-tx" label="[[_ethTxLog.rcvdJsonLog.length]]"></paper-badge>
							</paper-tab>
							<paper-tab>
								<span id="sent-tx"><iron-icon icon="cloud-upload"></iron-icon> Outgoing reputation</span>
								<paper-badge for="sent-tx" label="[[_ethTxLog.sentJsonLog.length]]"></paper-badge>
							</paper-tab>
						</paper-tabs>

						<iron-pages selected="{{_selectedTab}}">
							<div class="dashboardList"> <!-- Reputation dashboard -->
								<table width="100%">
										<tr>
											<th> <iron-icon icon="perm-identity" title="Username"></iron-icon> Username</th>
											<th> <iron-icon icon="account-circle" title="Rate user"></iron-icon> Rate user</th>
											<th> <iron-icon icon="assessment" title="Reputation Statistics"></iron-icon> Reputation Statistics</th>
											<th> <iron-icon icon="fingerprint" title="Wallet address"></iron-icon> Wallet address</th>
											<th> <iron-icon icon="touch-app" title="Actions"></iron-icon> Actions </th>
										</tr>

										<template is="dom-repeat" items="[[_listDashboard]]" as="agent">
											<tr>
												<td>
													[[agent.username]]
												</td>

												<td> <!-- RATING -->
													<template is="dom-if" if="[[agent.agentHasProfile]]">
														<custom-star-rating on-rating-selected="rateAgent" value$="[[agent.ethRating]]"></custom-star-rating>
														
													</template>
													<template is="dom-if" if="[[!agent.agentHasProfile]]">
														<custom-star-rating readonly></custom-star-rating>
													</template>
												</td>

												<td> <!-- VOTING STATISTICS -->
													<div style="display: flex; align-content: center; align-items: center;">
														<template is="dom-if" if="[[!agent.agentHasProfile]]">
															-
														</template>
														<template is="dom-if" if="[[agent.agentHasProfile]]">
															
															<iron-icon icon="notification:network-check" title="User reputation"></iron-icon> <custom-star-rating value="[[agent.ethRating]]" readonly="" single=""></custom-star-rating> ( [[agent.ethRating]] ) &nbsp;|&nbsp;
															
															<iron-icon icon="cloud-download" title="Incoming votes"></iron-icon> &nbsp;[[agent.noOfTransactionsRcvd]] &nbsp;|&nbsp; 
															
															<iron-icon icon="cloud-upload" title="Outgoing votes"></iron-icon> &nbsp;[[agent.noOfTransactionsSent]]
														</template>	
													</div>
												</td>

												<td> 
													<pre class="address">[[agent.address]]</pre> 
												</td>

												<td>
													<paper-icon-button icon="card-giftcard" title="Transfer L2Pcoin to Agent" on-click="openEthSendDialog" data-username$="[[agent.username]]" data-agentid$="[[agent.agentid]]" disabled="[[_working]]"></paper-icon-button>

												</td>
											</tr>
										</template>
								</table>
								<!-- End Reputation dashboard -->
							</div>
							<div> <!-- Faucet Payout-Log -->
								<table width="100%">
									<tr>
										<th>Timestamp</th>
										<th>TransactionValue</th>
									</tr>
									<template is="dom-repeat" items="[[_EthWallet.rcvdTx]]" as="tx">
										<tr>
											<td><iron-icon icon="update"></iron-icon> [[tx.blockDateTime]]</td>
											<td><iron-icon icon="card-giftcard"></iron-icon> [[tx.value]] L2Pcoin</td>
										</tr>
									</template>
								</table>
							</div> <!-- End Faucet Pay-out Log -->
							<div> <!-- Incoming TX Log -->
								<table width="100%">
									<tr>
										<th>Timestamp</th>
										<th>Sender</th>
										<th>TransactionType</th>
										<th>Message</th>
										<th>TransactionValue</th>
									</tr>
									<template is="dom-repeat" items="[[_ethTxLog.rcvdJsonLog]]" as="tx">
										<template is="dom-if" if="[[tx.sender == _ethCoinbaseInfo.coinbaseAddress]]">
										<tr>
											<td><iron-icon icon="update"></iron-icon> [[tx.txDateTime]]</td>
											<td><iron-icon icon="face"></iron-icon> [[tx.txSenderAddress]]</td>
											<td><iron-icon icon="class"></iron-icon> [[tx.txTransactionType]]</td>
											<td><iron-icon icon="speaker-notes"></iron-icon> [[tx.txMessage]]</td>
											<td>
											<template is="dom-if" if="[[_isUserRating(tx.transactionType)]]">
												<iron-icon icon="record-voice-over"></iron-icon> Rating
											</template>
											<template is="dom-if" if=[[!_isUserRating(tx.transactionType)]]">
												<template is="dom-if" if="[[tx.txAmountInEth]]">
													<iron-icon icon="card-giftcard"></iron-icon> [[tx.txAmountInEth]] L2Pcoin
												</template>
											</template>
											</td>
										</tr>
										</template>
									</template>
								</table>
							</div> <!-- End Incoming TX Log -->

							<div> <!-- Outgoing TX Log -->
								<table width="100%">
									<tr>
										<th>Timestamp</th>
										<th>Receiver</th>
										<th>TransactionType</th>
										<th>Message</th>
										<th>TransactionValue</th>
									</tr>
									<template is="dom-repeat" items="[[_ethTxLog.sentJsonLog]]" as="tx">
										<tr>
											<td><iron-icon icon="update"></iron-icon> [[tx.txDateTime]]</td>
											<td><iron-icon icon="face"></iron-icon> [[tx.txReceiverAddress]]</td>
											<td><iron-icon icon="class"></iron-icon> [[tx.txTransactionType]]</td>
											<td><iron-icon icon="speaker-notes"></iron-icon> [[tx.txMessage]]</td>
											<td>
											<template is="dom-if" if="[[tx.transactionType == 'L2P USER RATING']]">
												<iron-icon icon="record-voice-over"></iron-icon> Rating
											</template>
											<template is="dom-if" if="[[tx.txAmountInEth]]">
												<iron-icon icon="card-giftcard"></iron-icon> [[tx.txAmountInEth]] L2Pcoin
											</template>
											</td>
										</tr>
									</template>
								</table>
							</div> <!-- End outgoing TX log -->
						</iron-pages>
					</template> <!-- END TX LOG -->

		</div>
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
			_chosenUsername: { type: String, value: "" },
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
			_hasNoDashboard : { type: Boolean, value: true },
			_hasNoTxLog: { type: Boolean, value: true },
			_listDashboard: { type: Array, value: [] },
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
		let ethThis = this;
		window.ethThis = this;
		window.setTimeout(function() { ethThis.refreshWallet(); }, 5);
	}

	refreshWallet() {
		this.refreshAgentsList();
		this.refreshProfilesList();
		if ( this.agentId.length > 5 )
		{
			this.refreshEthWallet();
		}
	}
	refreshEthWallet() { 
		this.$.ajaxDashboardList.generateRequest();
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
		if (this._EthWallet.ethAccBalance < 0.01) {
			this.error = { title: "Not enough funds", msg: "Try requesting reputation (L2Pcoin) from the faucet. <br>\n Hosting or publishing services will positively influence Your reputation. <br>\n Interact with users and get them to rate You positively, the rating is factored in the reputation payout." };
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
		this.group = this._findGroupIDByName(this._groupSelected.innerHTML.trim());
	}

	_handleGenericTxLogResponse(event) {
		this._ethTxLog = event.detail.response;
		if (this._ethTxLog.rcvdJsonLog.length == 0 && this._ethTxLog.sentJsonLog.length == 0)
			this._hasNoTxLog = true;
		else
			this._hasNoTxLog = false;
	}
	
	_handleDashboardListResponse(event) {
		let response = event.detail.response;
		response.agentList.forEach(function(element) { element.shortid = element.agentid.substr(0, 15) + '...' });
		this._listDashboard = response.agentList;
		this._hasNoDashboard = false;
	}

	_handleLoadAgentlistResponse(event) {
		let response = event.detail.response;
		response.agents.forEach(function(element) { element.shortid = element.agentid.substr(0, 15) + '...' });
		this._listAgents = response.agents;
		this._hasNoAgentsList = false;
	}
	_handleLoadProfilelistResponse(event) {
		let response = event.detail.response;
		response.agents.forEach(function (element) { element.shortid = element.agentid.substr(0, 15) + '...' });
		this._listProfiles = response.agents;
		this._hasNoProfilesList = false;
	}  
	_handleRateAgentResponse(event) {
		let response = event.detail.response;
		
		this.$.toast.innerHTML = 'Rating (' + response.recipientname + ': '+ response.rating + ') successfully casted.';
		this.$.toast.open();
		this.refreshWallet();
	}
	rateAgent(event) {
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
		if ( this._EthWallet.rcvdTx.length == 0 )
		{
			this._hasNoTxLog = true;
		}
		else
		{
			this._hasNoTxLog = false;
		}
		this._EthWallet.ethAccBalance = parseFloat(this._EthWallet.ethAccBalance);
		this.$.ajaxGetGroups.generateRequest();
	}
	_handleRequestFaucetResponse(event) {
		this._ethFaucetLog = event.detail.response;
		this.$.ethFaucetDiaLog.open();
		this.refreshEthWallet();
	}
	_handleRegisterProfileResponse(event) {
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
		var username = event.target.getAttribute('data-username');
		this._chosenAgentID = agentid;
		this._chosenUsername = username;
		this.$.sendEthDialog.open();
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
		setTimeout(function () { ethThis.closeEthDialog(); }, 200);
	}

	_handleError(object, title, message) {
		window.rootThis._handleError(object, title, message)
	}

	_isUserRating(val)
	{
		return val == "L2P USER RATING";
	}
}

window.customElements.define('eth-view', EthereumView);
