#!/usr/bin/env bash
# Deploys the registry contracts to the local chain and writes the las2peer registry config.
# Expects a checkout of las2peer-registry-contracts (branch revival) next to this repo, or CONTRACTS_DIR.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONTRACTS_DIR=${CONTRACTS_DIR:-"$ROOT/../las2peer-registry-contracts"}
[[ -x "$CONTRACTS_DIR/scripts/deploy.sh" ]] || { echo "no contracts checkout at $CONTRACTS_DIR (set CONTRACTS_DIR)"; exit 1; }
[[ -d "$CONTRACTS_DIR/node_modules" ]] || (cd "$CONTRACTS_DIR" && npm ci --no-audit --no-fund)
mkdir -p "$ROOT/etc"
REGISTRY_CONFIG="$ROOT/etc/i5.las2peer.registry.data.RegistryConfiguration.properties" \
  exec "$CONTRACTS_DIR/scripts/deploy.sh"
