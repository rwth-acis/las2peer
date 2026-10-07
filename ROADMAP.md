# las2peer revival roadmap

Goal: las2peer runs anywhere with one command, is production grade, has a modern UI, and becomes a base for new features (other chains, IPFS storage, …).

## Phase 0 — Runs again ✅ (2026-10-07)

- Full stack (chain → contracts → node + frontend) from source via `docker compose up` in this repo, plus a native dev loop with `mise` tasks (`mise.toml`, `dev/`).
- End-to-end smoke test (`mise run smoke`): users, on-chain groups, service publishing, faucet, reputation.
- Bugs fixed on the `revival` branches:
  - **contracts** `migrations/2_deploy_registry_contracts.js`: `ServiceRegistry` was redeployed with the *GroupRegistry* address as its user registry, so it checked ownership against the wrong contract.
  - **contracts** `ReputationRegistry.addTransaction`: range check used `&&` (`amount > max && amount < min` is never true), so any rating value was accepted.
  - **las2peer** `StaticNonceManager.incStaticNonce`: NPE on a freshly started node whose stored nonce was ahead of the chain (broke the faucet).
  - **las2peer** `EthereumHandler` / `EthereumNode`: NPE in faucet/admin checks when the node has no admin email configured.
- Added: `Dockerfile` + `docker/entrypoint.sh` (las2peer, env-configured), `Dockerfile` + `scripts/deploy.sh` + `scripts/export-registry-config.js` (contracts; replaces grepping truffle logs).

## Phase 1 — Clean base

1. **Branching.** Work happens on `revival` in `las2peer` and `las2peer-registry-contracts` (and `las2peer-template-project`); nothing goes to `master` directly. Merge to `master` via PR once a phase is stable. The compose file builds the contracts from their `revival` branch — switch it to a tag when they are released.
2. **Thesis branches** `nonce-dev`, `userupdate`, `feature/addGroupsToBlockchain`, contracts `ba-erdzan`: diverged by ~750–890 changed lines per branch in the same files because master was reworked later; most thesis work landed via the 51 merged commits. Kept as archive; cherry-pick individual features if something turns out to be missing.
3. **CI** (GitHub Actions): build, unit tests, `docker compose up` + smoke test on every PR; publish images to GHCR.
4. **Test status (2026-10-07)**: las2peer 256 tests, 0 failures (19 skipped). Contracts 17 passing, **5 failing on master too**: the `ReputationContract` tests pass a username where `createProfile` now takes an address, so the tests are stale, not the contract.
5. **Known issues to fix**: template `GET /template/get` returns 500 for anonymous users; startup logs `Error creating agent … pastStorage is null` (nonce agent created before storage is up); three duplicate SLF4J bindings; references to dead Archiva in build files.

## Phase 2 — Modern toolchain

| Area | Now | Target | Notes |
|---|---|---|---|
| Java | ~~17~~ **21 ✅** | 25 LTS | The optional `--sandbox` (`L2pSecurityManager`) needs `-Djava.security.manager=allow` on 21 and cannot work on 24+ (JEP 486): redesign it (one process/container per service, or classloader + module boundaries) before moving to 25. Also: Mockito 1.9.5 in tests. |
| Build | ~~Gradle 7.3~~ **Gradle 9.8 ✅**, local jars in `jars/` | version catalog | Publish the FreePastry fork as a proper artifact or include it as a module. HTTPS certs now via BouncyCastle instead of JDK-internal `sun.security.x509`. |
| REST | Jersey 2.35 (javax) + Grizzly 2.4 | Jersey 3 / Jakarta EE 10, or Javalin/Helidon | javax → jakarta rename touches every service. |
| Ethereum client | web3j 4.5.18 | web3j 4.14 / 6.x | Still open. The static nonce manager is ✅ replaced by `NonceManager` (chain pending nonce + per-account lock, EIP-155 signing); 4.5 already works on anvil's current hardfork. |
| Contracts | ~~Solidity 0.5, Truffle 5.0, ganache 7~~ **Solidity 0.8.30, Foundry, anvil ✅** | — | ABIs unchanged; Foundry tests cover delegated (signed-consent) calls; dev chain runs the latest hardfork. |
| Frontend | lit-element 2, Polymer paper-*, rollup 2, TS 4.2, Node 16 | Lit 3 + Vite + TS 5 (or React), Node 22 | Polymer is EOL; the frontend is the main reason Node 16 is still pinned. |

## Phase 3 — Production grade

- **Key custody (security, do early):** the node generates users' Ethereum mnemonics server-side and returns them in API responses (`ethMnemonic` in `AgentsHandler` / `L2P_JSONUtil`). Move to user-held keys: browser wallet (MetaMask / WalletConnect / passkey smart accounts) signs, the node only verifies.
- Config: one documented config file or env vars (started in `docker/entrypoint.sh`); secrets via env/secret files, never in compose.
- Persistence: volumes for node storage + chain; restart without losing the network.
- HTTPS + real OIDC (Keycloak service in compose for local auth instead of Google/RWTH issuers).
- Multi-node compose profile (bootstrap + peers) to actually exercise the P2P part.
- Observability: health/readiness endpoints (basic healthcheck exists), Prometheus metrics, structured JSON logs.
- Deployment: Helm chart (the old K8s files in `las2peer-ethereum-cluster` are a starting point).
- Security review of the web connector (CORS `*`, session handling, faucet abuse limits).

## Phase 4 — UI/UX

**Started (2026-10-07):** new node frontend in `webconnector/ui` (React 19 + Vite + Tailwind 4, TanStack Query), replacing the Polymer/lit-element app and the Node 16 pin. Same pages, rebuilt: status (live meters, local services with OpenAPI links, known nodes), services (registry, versions, start/stop, deployments), publish (drag-and-drop), agents & groups (create/manage groups, export/import agents), wallet & reputation (pay-out breakdown, opt-in, rating, send L2Pcoin, activity). Light/dark mode, responsive, username/password sign-in and registration. Fixes the old UI's dead group selector, never-rendering incoming log, errors shown as success, leaking pollers. Backend: `/services/start|stop` now require a session; `/services/services` no longer fails when one peer is unreachable.

Next: OIDC sign-in (oidc-client-ts, Keycloak in compose), service detail pages, live updates instead of polling, component tests (Vitest + Testing Library).

- Redesign the node frontend: design system (tokens, light/dark), responsive layout, accessible components (WCAG 2.2 AA).
- Clear flows for the main jobs: sign up / connect wallet → browse services → start/stop → publish a service → groups → reputation & wallet.
- Live updates (chain events and node status via SSE/WebSocket instead of manual refresh buttons).
- Replace the console-only admin tasks with UI.

## Phase 5 — New features

- **Pluggable registry backend**: extract an interface from `ReadOnlyRegistryClient`/`ReadWriteRegistryClient`. Any EVM chain is then mostly configuration (L2 testnets such as Base/Optimism Sepolia, or Polygon); non-EVM chains get their own adapter.
- **IPFS storage**: service packages and supplements are already content-hashed artifacts, a natural fit for IPFS (Kubo node or Helia). Start with service package distribution, then optionally envelopes (encrypted at rest already).
- Service marketplace features on top of the reputation registry.
