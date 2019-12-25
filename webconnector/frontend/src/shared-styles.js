/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */

import '@polymer/polymer/polymer-element.js';

const $_documentContainer = document.createElement('template');
$_documentContainer.innerHTML = `<dom-module id="shared-styles">
  <template>
    <style>
      .card {
        margin: 24px;
        padding: 16px;
        color: #757575;
        border-radius: 5px;
        background-color: #fff;
        box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.14), 0 1px 5px 0 rgba(0, 0, 0, 0.12), 0 3px 1px -2px rgba(0, 0, 0, 0.2);
      }

      .circle {
        display: inline-block;
        width: 64px;
        height: 64px;
        text-align: center;
        color: #555;
        border-radius: 50%;
        background: #ddd;
        font-size: 30px;
        line-height: 64px;
      }

      h1 {
        margin: 16px 0;
        color: #212121;
        font-size: 22px;
      }

      paper-tooltip {
        --paper-tooltip: {
          font-size: 14px;
        }
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

      .flex-horizontal {
        @apply --layout-horizontal;
      }
      .flexchild {
        @apply --layout-flex;
      }
    </style>
  </template>
</dom-module>`;

document.head.appendChild($_documentContainer.content);
