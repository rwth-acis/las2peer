import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The web connector serves the built app under /las2peer/webapp/ (WebappHandler).
// In dev, API calls are proxied to a running node (LAS2PEER_URL, default http://localhost:8080).
const node = process.env.LAS2PEER_URL ?? 'http://localhost:8080'

export default defineConfig({
  base: '/las2peer/webapp/',
  plugins: [react(), tailwindcss()],
  build: { outDir: 'dist', emptyOutDir: true },
  server: {
    port: 5173,
    proxy: {
      '^/las2peer/(?!webapp).*': { target: node, changeOrigin: true },
    },
  },
})
