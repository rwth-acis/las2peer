# las2peer node frontend

React 19 + Vite + Tailwind 4 app served by the web connector at `/las2peer/webapp/`.
Gradle builds it as part of `:webconnector:jar` (`npm ci && npm run build`, output copied into the jar).

```bash
npm ci
LAS2PEER_URL=http://localhost:8080 npm run dev   # proxies /las2peer/* to a running node
```

Open http://localhost:5173/las2peer/webapp/. Code layout: `src/lib` (API client, auth, queries), `src/components` (UI kit), `src/pages`.
