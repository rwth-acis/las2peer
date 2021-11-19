import config from '../config.js';

import type { Route } from '@vaadin/router';

export const pageNotFoundMeta = {
  title: 'Error: Page not found',
  description: null,
  image: null,
};

export const routes: Route[] = [
  {
    path: '/welcome',
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
      description: 'Status',
    },
    action: async () => {
      await import('../pages/page_status.js');
    },
  },
  {
    path: '/view-services',
    name: 'view-services',
    component: 'page-view-services',
    meta: {
      title: 'View Services',
      description: 'View Services',
    },
    action: async () => {
      await import('../pages/page_view_services.js');
    },
  },
  {
    path: '/publish-service',
    name: 'publish-service',
    component: 'page-publish-service',
    meta: {
      title: 'Publish Service',
      description: 'Publish Service',
    },
    action: async () => {
      await import('../pages/page_publish_service.js');
    },
  },
  {
    path: '/agent-tools',
    name: 'agent-tools',
    component: 'page-agent-tools',
    meta: {
      title: 'Agent Tools',
      description: 'Agent Tools',
    },
    action: async () => {
      await import('../pages/page_agent_tools.js');
    },
  },
  {
    path: '/eth-tools',
    name: 'page-eth',
    component: 'page-eth-tools',
    meta: {
      title: 'ETH Tools',
      description: 'ETH Tools',
    },
    action: async () => {
      await import('../pages/page_blockchain_and_reputation.js');
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
