import config from '../config.js';

import type { Route } from '@vaadin/router';

export const pageNotFoundMeta = {
  title: 'Error: Page not found',
  description: null,
  image: null,
};

export const routes: Route[] = [
  {
    path: '/',
    name: 'home',
    component: 'page-welcome',
    meta: {
      title: config.appName,
      titleTemplate: null,
      description: config.appDescription,
    },
    action: async () => {
      await import('../pages/page_welcome.js');
    },
  },
  {
    path: '/status',
    name: 'status',
    component: 'page-status',
    meta: {
      title: 'Status',
      description: 'About page description',
    },
    action: async () => {
      await import('../pages/page_status.js');
    },
  },
  {
    path: '/publish-service',
    name: 'publish-service',
    component: 'page-publish-service',
    meta: {
      title: 'Publish Service',
      description: 'About page description',
    },
    action: async () => {
      await import('../pages/page_publish_service.js');
    },
  },
  {
    path: '(.*)',
    name: 'not-found',
    component: 'page-not-found',
    meta: pageNotFoundMeta,
    action: async () => {
      await import('../pages/page_not_found.js');
    },
  },
];
