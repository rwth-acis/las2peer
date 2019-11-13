import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
/**
* `iron-star-rating`
* 5-star rating element (Polymer 3.x)
* based on https://github.com/chadweimer/iron-star-rating
*
* @customElement
* @polymer
*/
class CustomStarRating extends PolymerElement {
    static get template() {
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
                color: #eeeeee;
                float: right;
            }
            iron-icon.selected,
            iron-icon.selected ~ iron-icon {
                color: #fdd835;
            }
            :host(:not([readonly])) iron-icon:hover,
            :host(:not([readonly])) iron-icon:hover ~ iron-icon {
                color: #ffeb3b !important;
            }
        </style>
    
        <template is="dom-repeat" items="[[ratings]]">
            <iron-icon icon="[[icon]]" class\$="[[item.class]] [[_getSelected(item.selected)]]" on-click="_starClicked"></iron-icon>
        </template>
`;
    }

    static get is() { return 'custom-star-rating'; }
    static get properties() {
        return {
            value: {
                type: Number,
                value: 0,
                notify: true,
                observer: '_valueChanged'
            },
            icon: {
                type: String,
                value: 'icons:star'
            },
            disableAutoUpdate: {
                type: Boolean,
                value: false,
            },
            readonly: {
                type: Boolean,
                value: false,
                reflectToAttribute: true,
            }
        };
    }

    constructor() {
        super();

        this.ratings = [
            { value: 5, class: 'whole', selected: false },
            { value: 4, class: 'whole', selected: false },
            { value: 3, class: 'whole', selected: false },
            { value: 2, class: 'whole', selected: false },
            { value: 1, class: 'whole', selected: false },
        ];
    }

    _valueChanged(newValue, oldValue) {
        if (newValue !== 0 && !newValue) {
            return;
        }

        var self = this;
        this.ratings.forEach(function (rating, index) {
            if (rating.value === newValue) {
                rating.selected = true;
            } else {
                rating.selected = false;
            }
            self.notifyPath('ratings.' + index + '.selected')
        });
    }
    _getSelected(selected) {
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
        this.dispatchEvent(new CustomEvent('rating-selected', { detail: { rating: e.model.item.value } }));
    }
}

window.customElements.define(IronStarRating.is, IronStarRating);