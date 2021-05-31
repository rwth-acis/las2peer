/**
 @license
 Copyright (c) 2018 Advanced Community Information Systems (ACIS) Group, Chair of Computer Science 5 (Databases &
 Information Systems), RWTH Aachen University, Germany. All rights reserved.
 */

import { LitElement, html } from '@polymer/lit-element';

import Static from '../src/static';
import Auth from '../src/util/auth';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import Common from '../src/util/common';

/**
 * Callback element which gets called by GitHub API, after settings called GitHub's /login/oauth/authorize endpoint.
 * GitHub redirects to /callbacks/github-callback.html?code=<...>.
 * We use the given code to request a GitHub access token.
 */
class GitHubCallback extends LitElement {
  render() {
    return html`
      <div style="display: flex; height: 100%">
        <paper-spinner-lite
          style="margin: auto auto auto auto"
          active
        ></paper-spinner-lite>
      </div>
    `;
  }

  constructor() {
    super();

    // GitHub puts query parameter "code" into URL
    // we need to get this code to request an access token
    const url_string = window.location.href;
    const url = new URL(url_string);
    // get code from query parameters
    const code = url.searchParams.get('code');
    if (url.searchParams.has('code')) {
      // request access token from Project Management Service
      // The Project Management Service uses the given code to request an access token.
      // This request cannot be done at client-side, because it needs the client_secret
      // which should not be public.
      fetch(Static.ProjectManagementServiceURL + '/users/githubcode/' + code, {
        method: 'POST',
        headers: Auth.getAuthHeader(),
      }).then((response) => {
        if (response.ok) {
          response.json().then((data) => {
            if (data.access_token && data.gitHubUsername) {
              // received both access token for GitHub as well as the GitHub username of the user

              // store access token to localStorage
              Common.storeUserInfoGitHubAccessToken(data.access_token);
              // store username to localStorage
              Common.storeGitHubUsername(data.gitHubUsername);
              // close window (then automatically return to settings page)
              window.close();
            }
          });
        }
      });
    }
  }
}

customElements.define('github-callback', GitHubCallback);
