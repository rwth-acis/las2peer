import { html, css, customElement } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

@customElement('page-welcome')
export class PageHome extends PageElement {
  static styles = css`
    html {
      scroll-behavior: smooth;
    }
    .myButton {
      width: fit-content;
      background-color: #ffffff;
      border-radius: 28px;
      border: 1px solid #000000;
      display: inline-block;
      cursor: pointer;
      color: #171617;
      font-family: Arial;
      font-size: 20px;
      padding: 17px 31px;
      text-decoration: none;
      text-shadow: 0px 1px 0px #000000;
      margin: 2rem;
    }
    .myButton:hover {
      background-color: #19d2eb;
    }
    .myButton:active {
      position: relative;
      top: 1px;
    }

    .page-welcome-text-and-image {
      display: flex;
      padding-top: 5rem;
    }
    .page-welcome-text-and-button {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    h1 {
      color: white;
      font-size: 3rem;
      margin: 0;
    }
    h2 {
      color: #ececec;
      font-size: 2rem;
      margin: 0;
      padding-top: 2rem;
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
      background-color: cornflowerblue;
    }
    #las2peer-text {
      font-weight: bold;
      font-size: 4rem;
      display: inline;
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
        <div class="page-welcome-text-and-image">
          <div class="page-welcome-text-and-button">
            <div>
              <div class="page-welcome-title">
                <h1>
                  Welcome to this <br />
                  <div id="las2peer-text">las2peer</div>
                  Node
                </h1>
              </div>
              <div class="page-welcome-subtitle">
                <h2>
                  Here you can view and start services in this las2peer network
                </h2>
              </div>
            </div>
            <a href="#page-welcome-las2peer-description" class="myButton"
              >Get Started</a
            >
          </div>
          <div class="page-welcome-las2peer-image">
            <img src="./images/las2peer.png" alt="" />
          </div>
        </div>
      </section>
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
