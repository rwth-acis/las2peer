/**
@license
Copyright (c) 2018 Advanced Community Information Systems (ACIS) Group, Chair of Computer Science 5 (Databases &
Information Systems), RWTH Aachen University, Germany. All rights reserved.
*/

import {LitElement} from '@polymer/lit-element';

import 'oidc-client';

class OpenIDConnectPopupSigninCallback extends LitElement {

  constructor() {
    super();
    new UserManager().signinPopupCallback();
  }

}

customElements.define('openidconnect-popup-signin-callback', OpenIDConnectPopupSigninCallback);
