import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/social-icons.js';
import { html, property } from 'lit-element';

import { PageElement } from '../helpers/page-element.js';

class CustomStarRating extends PageElement {
  @property({ type: Object })
  ratings: { value: number; class: string; icon: string; selected: boolean }[];
  disabled: any;
  readonly: boolean | undefined;
  single: any;
  value: number | undefined;
  disableAutoUpdate: any;
  render() {
    return html`
        <style>
            :host {
                display: inline-block;
                --iron-icon-width: var(--star-rating-size, 24px);
                --iron-icon-height: var(--star-rating-size, 24px);
            }
            :host(:not([readonly])) {
                cursor: pointer;
            }
            :host[hidden] {
                display: none !important;
            }
            iron-icon {
                color: #bbb;
                float: right;
            }
            iron-icon.color-1, :host([single]) > iron-icon.color-1 {
                color: #d7191c;
            }
            iron-icon.color-2, :host([single]) > iron-icon.color-2 {
                color: #fdae61;
            }
            iron-icon.color-3, :host([single]) > iron-icon.color-3 {
                color: #aaa;
            }
            iron-icon.color-4, :host([single]) > iron-icon.color-4 {
                color: #5ac4e8;
            }
            iron-icon.color-5, :host([single]) > iron-icon.color-5 {
                color: #2c7bb6;
            }
            :host(:not([readonly])) iron-icon.whole {
                opacity: 0.5;
            }
            :host(:not([readonly])) iron-icon:hover {
                opacity: 1.0;
            }
            :host([single]) {
                opacity: 1.0;
            }
            iron-icon.selected {
                opacity: 0.95;
            }
        </style>
    
        <template is="dom-repeat" items="[[ratings]]">
            <iron-icon icon="[[item.icon]]" class\="[[item.class]] [[_getSelected(item.selected)]]" @click=${this._starClicked}"></iron-icon>
        </template>
`;
  }

  static get is() {
    return 'custom-star-rating';
  }
  static get properties() {
    return {
      value: {
        type: Number,
        value: 0,
        notify: true,
        observer: '_valueChanged',
      },
      icon: {
        type: String,
        value: 'social:sentiment-neutral',
      },
      disableAutoUpdate: {
        type: Boolean,
        value: false,
      },
      readonly: {
        type: Boolean,
        value: false,
        reflectToAttribute: true,
      },
      disableRating: {
        type: Boolean,
        value: false,
        notify: true,
      },
      single: {
        type: Boolean,
        value: false,
        notify: true,
        reflectToAttribute: true,
      },
    };
  }

  constructor() {
    super();

    this.ratings = [
      {
        value: 5,
        class: 'whole',
        icon: 'social:sentiment-very-satisfied',
        selected: false,
      },
      {
        value: 4,
        class: 'whole',
        icon: 'social:sentiment-satisfied',
        selected: false,
      },
      {
        value: 3,
        class: 'whole',
        icon: 'social:sentiment-neutral',
        selected: false,
      },
      {
        value: 2,
        class: 'whole',
        icon: 'social:sentiment-dissatisfied',
        selected: false,
      },
      {
        value: 1,
        class: 'whole',
        icon: 'social:sentiment-very-dissatisfied',
        selected: false,
      },
    ];
  }

  connectedCallback() {
    // super.connectedCallback();

    if (this.disabled) {
      this.readonly = true;
      this.ratings = [
        {
          value: 5,
          class: 'whole',
          icon: 'icons:remove-circle-outline',
          selected: false,
        },
        {
          value: 4,
          class: 'whole',
          icon: 'icons:remove-circle-outline',
          selected: false,
        },
        {
          value: 3,
          class: 'whole',
          icon: 'icons:remove-circle-outline',
          selected: false,
        },
        {
          value: 2,
          class: 'whole',
          icon: 'icons:remove-circle-outline',
          selected: false,
        },
        {
          value: 1,
          class: 'whole',
          icon: 'icons:remove-circle-outline',
          selected: false,
        },
      ];
    } else if (this.single) {
      this._updateSingleFace();
    } else if (this.value <= 0 && this.readonly) {
      this.ratings = [
        {
          value: 3,
          class: 'whole',
          icon: 'social:sentiment-neutral',
          selected: false,
        },
      ];
    } else {
      this.ratings = [
        {
          value: 5,
          class: 'whole color-5',
          icon: 'social:sentiment-very-satisfied',
          selected: false,
        },
        {
          value: 4,
          class: 'whole color-4',
          icon: 'social:sentiment-satisfied',
          selected: false,
        },
        {
          value: 3,
          class: 'whole color-3',
          icon: 'social:sentiment-neutral',
          selected: false,
        },
        {
          value: 2,
          class: 'whole color-2',
          icon: 'social:sentiment-dissatisfied',
          selected: false,
        },
        {
          value: 1,
          class: 'whole color-1',
          icon: 'social:sentiment-very-dissatisfied',
          selected: false,
        },
      ];
    }

    /*
        console.log(
            {
                "readonly": (this.readonly) ? this.readonly : "r; nope",
                "disabled": (this.disabled) ? this.disabled : "d; nope",
                "single": (this.single) ? this.single : "s; nope",
                "rounded value": (this.value) ? Math.round(this.value) : "v; nope",
                "value": (this.value) ? Math.round(this.value) : "v; nope",
                "no. elements": this.ratings.length,
                "ratings": this.ratings
            }
        );
        */
  }

  _updateSingleFace() {
    let valueToFace = '';
    switch (Math.round(this.value)) {
      case 1:
        valueToFace = 'sentiment-very-dissatisfied';
        break;
      case 2:
        valueToFace = 'sentiment-dissatisfied';
        break;
      default:
      case 3:
        valueToFace = 'sentiment-neutral';
        break;
      case 4:
        valueToFace = 'sentiment-satisfied';
        break;
      case 5:
        valueToFace = 'sentiment-very-satisfied';
        break;
    }
    this.ratings = [
      {
        value: Math.round(this.value),
        class: 'whole color-' + Math.round(this.value),
        icon: 'social:' + valueToFace,
        selected: false,
      },
    ];
  }

  _valueChanged(newValue: number, oldValue: any) {
    if (this.single) {
      this._updateSingleFace();
    }
    if (newValue !== 0 && !newValue && this.ratings.length > 0) {
      return;
    }

    let self = this;
    this.ratings.forEach(function (rating, index) {
      if (Math.round(rating.value) === newValue) {
        rating.selected = true;
      } else {
        rating.selected = false;
      }
      self.notifyPath('ratings.' + index + '.selected');
    });
  }
  _getSelected(selected: any) {
    return selected ? 'selected' : '';
  }

  _starClicked(e) {
    e.preventDefault();

    if (this.readonly) {
      return;
    }

    if (!this.disableAutoUpdate) {
      this.value = e.model.item.value;
    }
    this.dispatchEvent(
      new CustomEvent('rating-selected', {
        detail: { rating: e.model.item.value },
      })
    );
  }
}

window.customElements.define(CustomStarRating.is, CustomStarRating);
