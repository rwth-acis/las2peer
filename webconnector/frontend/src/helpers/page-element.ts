import { LitElement, property } from 'lit-element';

import config from '../config.js';
import { updateMeta } from './html-meta-manager/index.js';

import type { MetaOptions } from './html-meta-manager/index.js';
import type { Route, RouterLocation } from '@vaadin/router';
import type { PropertyValues } from 'lit-element';

declare module '@vaadin/router/dist/vaadin-router' {
  export interface BaseRoute {
    meta?: MetaOptions;
  }
}

export class PageElement extends LitElement {
  @property({ type: Object })
  location?: RouterLocation;

  private defaultTitleTemplate = `%s | ${config.appName}`;

  protected get defaultMeta() {
    return {
      url: window.location.href,
      titleTemplate: this.defaultTitleTemplate,
    };
  }

  protected meta(route: Route) {
    return route.meta;
  }

  updated(changedProperties: PropertyValues<this>) {
    super.updated(changedProperties);

    if (this.location?.route) {
      const meta = this.meta(this.location.route);

      if (meta) {
        updateMeta({
          ...this.defaultMeta,
          ...(meta.titleTemplate && { titleTemplate: meta.titleTemplate }),
          ...meta,
        });
      }
    }
  }
}
