import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/paper-icon-button/paper-icon-button.js';
import './shared-styles.js';

class WelcomeView extends PolymerElement {
  static get template() {
    return html`
      <style include="shared-styles">
        :host {
          display: block;

          padding: 10px;
        }
      </style>

      <div class="card">
        <h1>Welcome</h1>
        <p>Hi! las2peer is a peer-to-peer community service platform. Each network consists of nodes run by community members.</p>
        <p>
          Each participating node contributes storage space and computing power that allow you to store and run services.
          Data is encrypted and stored redundantly on multiple nodes. If a node goes offline, other nodes can take over its duties.
        </p>
        <p>
          Services are stored (as Java JAR files) in the network. You can <a href="[[rootPath]]view-services">view existing services</a> or <a href="[[rootPath]]publish-service">publish your own</a>.
          Many services are composed of several independent parts, called microservices.
          A service is ready to be used if all microservices run on at least one network node.
        </p>
        <p>
          This Web interface belongs to one specific node, operated by a community member.
          You can operate your own node.
          In addition to providing computing resources for other members, this has one distinct advantage:
          The authenticity of services started on your node is verified by comparing its signature against author data stored in a community blockchain shared by all nodes.
        </p>
        
        <h2>First Steps</h2>
        <ol>
          <li>Feel free to explore: Check out the <a href="[[rootPath]]view-services">existing services</a>.</li>
          <li>Register and log in: Create an account through <a href="https://api.learning-layers.eu/o/oauth2/">Learning Layers OpenID Connect</a> or <a href="[[rootPath]]agent-tools">register a las2peer network agent</a> (either way is fine!), then log in.</li>
          <li>Publish a service of your own: Once you are logged in, you can <a href="[[rootPath]]publish-service">publish services</a> to the network so that other community members can find and use them.</li>
        </ol>
        
        <p>For more information visit the <a href="https://las2peer.org/" target="_blank">las2peer project website</a>.</p>
      </div>
    `;
  }

  static get properties() {
    return {
      apiEndpoint: { type: String, notify: true },
      agentId: { type: String, notify: true },
      error: { type: Object, notify: true }
    };
  }

}

window.customElements.define('welcome-view', WelcomeView);
