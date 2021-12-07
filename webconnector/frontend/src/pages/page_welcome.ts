import { html, css, customElement } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

@customElement('page-welcome')
export class PageHome extends PageElement {
  static styles = css`
    html {
      scroll-behavior: smooth;
    }
    h1 {
      font-size: 3rem;
      margin: 0;
    }
    h2 {
      font-size: 2rem;
      margin: 0;
      margin-bottom: 1rem;
      font-weight: 100;
    }
    h3 {
      font-size: 1.5rem;
      margin: 1.5rem;
      padding-top: 2rem;
      font-weight: 100;
      text-align: center;
    }

    section {
      padding: 1rem;
    }
    .page-welcome-las2peer-description-title {
      text-align: center;
      font-size: 2rem;
      margin: 0;
      padding-top: 2rem;
      font-weight: 100;
    }
    @media screen and (min-width: 550px) {
      .page-welcome-las2peer-descriptions {
        display: grid;
        grid-template-columns: 50% 50%;
        justify-items: center;
      }
    }
    @media screen and (max-width: 550px) {
      .page-welcome-las2peer-descriptions {
        display: grid;
        grid-template-columns: 100%;
        justify-items: center;
      }
      .page-welcome-las2peer-image {
        display: none;
      }
    }
    img {
      object-fit: contain;
      width: 15rem;
    }
    .page-welcome-las2peer-description-item {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
  `;

  render() {
    return html`
      <section>
        <div class="page-welcome-text-and-button">
          <div>
            <div class="page-welcome-title" style="display: flex; flex-direction: column;">
              <h1 style="margin-left: auto; margin-right: auto">
                Welcome to this las2peer node!
              </h1>
              <h2 style="margin-left: auto; margin-right: auto">
                Here you can view and start services in this las2peer network.
              </h2>
              <img src="./images/las2peer.png" alt="las2peer logo" style="margin-left: auto; margin-right: auto"/>
            </div>
          </div>
        </div>
      </section>
      <hr>
      <div>
        <div
          id="page-welcome-las2peer-description"
          class="page-welcome-las2peer-description-title"
        >
          las2peer is a peer-to-peer community service platform
        </div>
        <div class="page-welcome-las2peer-descriptions">
          <div class="page-welcome-las2peer-description-item">
            <img src="./images/icon1.png" alt="" />
            <h3>
              Each participating node contributes storage space and computing
              power that allow you to store and run services.
            </h3>
          </div>
          <div class="page-welcome-las2peer-description-item">
            <img src="./images/icon2.png" alt="" />
            <h3>
              This Web interface belongs to one specific node, operated by a
              community member. You can operate your own node.
            </h3>
          </div>
          <div class="page-welcome-las2peer-description-item">
            <img src="./images/icon3.png" alt="" />
            <h3>
              The authenticity of services started on your node is verified in a
              community blockchain.
            </h3>
          </div>
          <div class="page-welcome-las2peer-description-item">
            <img src="./images/icon4.png" alt="" />
            <h3>
              Services are java programs running in the network. Discover
              existing services or publish your own.
            </h3>
          </div>
        </div>
      </div>
    `;
  }
}
