/* eslint-disable import/no-duplicates */
import { TextField } from '@material/mwc-textfield';
import { html, css, customElement, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

// import { request } from '../helpers/request_helper.js';
import '@material/mwc-button';
import '@material/mwc-textfield';

@customElement('page-publish-service')
export class PagePublishService extends PageElement {
  static styles = css`
    section {
      padding: 1rem;
    }
    .page-publish-service-fields {
      display: flex;
      flex-direction: column;
    }
  `;
  @property({ type: Object })
  publishServiceInfo: PublishServiceInfo = {
    description: '',
    frontendUrl: '',
    name: '',
    serviceClassesToStart: '',
    sourceCodeUrl: '',
  };
  render() {
    return html`
      <section>
        <h1>Publish a Service</h1>
        <p>
          Publish a service in the network by uploading its JAR file and
          providing some metadata.
        </p>
        <p>
          The service package name will automatically be registered to your
          name, if it isn’t already. Further releases can only be uploaded by
          you.
        </p>
        <p>
          The additional metadata will help users discover your service and its
          features. The name should be a human-readable variant of the package
          name. The description should consist of a few short sentences.
        </p>

        <div class="page-publish-service-fields">
          <!-- <mwc-button> -->
          <input
            type="file"
            id="jarfile"
            .multiple=${false}
            onChange=${this.handleshit}
          />
          <!-- </mwc-button> -->
          <mwc-textfield
            id="someiiiiiidd"
            placeholder="Service classes to start (comma-separated)"
            helper="Service classes to start (comma-separated)"
          ></mwc-textfield>
          <mwc-textfield helper="Name" placeholder="Name"></mwc-textfield>
          <mwc-textfield
            helper="Description"
            placeholder="Description"
          ></mwc-textfield>
          <mwc-textfield
            helper="Source code URL (e.g., GitHub project)"
            placeholder="Source code URL (e.g., GitHub project)"
          ></mwc-textfield>
          <mwc-textfield
            helper="Front-end URL"
            placeholder="Front-end URL"
          ></mwc-textfield>
          <mwc-button @click=${this.handleshit}
            >Publish your Service</mwc-button
          >
        </div>
      </section>
    `;
  }

  firstUpdated() {
    // const file = {
    //   dom: document.getElementById('theFile'),
    //   binary: null,
    // };
  }
  handleshit() {
    // const reader = new FileReader();
    console.log(
      (this.shadowRoot.getElementById('someiiiiiidd') as TextField).value
    );
    console.log('sdsds');
  }
  async onClickPublishButton() {
    const bodyToSend: PublishServiceInfoToPost = {
      jarfile: '',
      supplement: {
        class: this.publishServiceInfo.serviceClassesToStart,
        description: this.publishServiceInfo.description,
        frontendUrl: this.publishServiceInfo.frontendUrl,
        name: this.publishServiceInfo.name,
        vcsUrl: this.publishServiceInfo.sourceCodeUrl,
      },
    };
    const body = new FormData();
    body.append('jarfile', bodyToSend.jarfile);
    body.append('supplement', JSON.stringify(bodyToSend.supplement));
    // const response = await request(
    //   'https://las2peer.tech4comp.dbis.rwth-aachen.de/las2peer/services/upload',
    //   {
    //     method: 'POST',
    //     body: body,
    //   }
    // );
  }
}
interface PublishServiceInfo {
  serviceClassesToStart: string;
  name: string;
  description: string;
  sourceCodeUrl: string;
  frontendUrl: string;
}

interface PublishServiceInfoToPost {
  jarfile: string;
  supplement: {
    class: string;
    name: string;
    description: string;
    vcsUrl: string;
    frontendUrl: string;
  };
}
