import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@polymer/iron-icons/social-icons.js';
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
            iron-icon.color-1 {
                color: #d7191c;
            }
            iron-icon.color-2 {
                color: #fdae61;
            }
            iron-icon.color-3 {
                color: #bbb;
            }
            iron-icon.color-4 {
                color: #abd9e9;
            }
            iron-icon.color-5 {
                color: #2c7bb6;
            }
            :host(:not([readonly])) iron-icon.whole {
                opacity: 0.4;
            }
            iron-icon.selected {
                color: #999999;
            }
            :host[single] {
                opacity: 1.0;
            }
            :host(:not([readonly])) iron-icon:hover {
                opacity: 1.0;
            }
        </style>
    
        <template is="dom-repeat" items="[[ratings]]">
            <iron-icon icon="[[item.icon]]" class\$="[[item.class]] [[_getSelected(item.selected)]]" on-click="_starClicked"></iron-icon>
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
                value: 'social:sentiment-neutral'
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
                notify: true
            },
            single: {
                type: Boolean,
                value: false,
                notify: true,
                reflectToAttribute: true
            }
        };
    }

    constructor() {
        super();

        this.ratings = [
            { value: 5, class: 'whole', icon: 'social:sentiment-very-satisfied', selected: false },
            { value: 4, class: 'whole', icon: 'social:sentiment-satisfied', selected: false },
            { value: 3, class: 'whole', icon: 'social:sentiment-neutral', selected: false },
            { value: 2, class: 'whole', icon: 'social:sentiment-dissatisfied', selected: false },
            { value: 1, class: 'whole', icon: 'social:sentiment-very-dissatisfied', selected: false },
        ]; 
    }

    connectedCallback() {
        super.connectedCallback();


        if ( !this.value && this.readonly )
        {
            this.ratings = [
                { value: 3, class: 'whole', icon: 'social:sentiment-neutral', selected: false },
            ];
        } 
        else 
        if ( this.disabled )
        {
            this.readonly = true;
            this.ratings = [
                { value: 5, class: 'whole', icon: 'icons:remove-circle-outline', selected: false },
                { value: 4, class: 'whole', icon: 'icons:remove-circle-outline', selected: false },
                { value: 3, class: 'whole', icon: 'icons:remove-circle-outline', selected: false },
                { value: 2, class: 'whole', icon: 'icons:remove-circle-outline', selected: false },
                { value: 1, class: 'whole', icon: 'icons:remove-circle-outline', selected: false },
            ];
        }
        else
        if ( this.single )
        {
            var valueToFace = '';

            switch (this.value) {
                case 1:
                    valueToFace = 'remove-circle-outline';
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
                { value: this.value, class: 'whole color-'+this.value, icon: 'icons:' + valueToFace, selected: false }
            ];
        } 
        else
        {
            if ( this.value > 0 )
            {
                this.ratings = [
                    { value: 5, class: 'whole color-5', icon: 'social:sentiment-very-satisfied', selected: false },
                    { value: 4, class: 'whole color-4', icon: 'social:sentiment-satisfied', selected: false },
                    { value: 3, class: 'whole color-3', icon: 'social:sentiment-neutral', selected: false },
                    { value: 2, class: 'whole color-2', icon: 'social:sentiment-dissatisfied', selected: false },
                    { value: 1, class: 'whole color-1', icon: 'social:sentiment-very-dissatisfied', selected: false },
                ];
            }
            else
            {
                this.ratings = [
                    { value: 5, class: 'whole', icon: 'social:sentiment-very-satisfied', selected: false },
                    { value: 4, class: 'whole', icon: 'social:sentiment-satisfied', selected: false },
                    { value: 3, class: 'whole', icon: 'social:sentiment-neutral', selected: false },
                    { value: 2, class: 'whole', icon: 'social:sentiment-dissatisfied', selected: false },
                    { value: 1, class: 'whole', icon: 'social:sentiment-very-dissatisfied', selected: false },
                ];               
            }   
        }

        console.log(
            {
                1: (this.readonly) ? this.readonly : "?r?",
                2: (this.disabled) ? this.disabled : "?d?",
                3: (this.single) ? this.single : "?s?",
                4: (this.value) ? this.value : "?v?",
                5: this.ratings.length
            }
        );
    }

    _valueChanged(newValue, oldValue) {
        if (newValue !== 0 && !newValue && this.ratings.length > 0) {
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

window.customElements.define(CustomStarRating.is, CustomStarRating);