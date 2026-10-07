#!/usr/bin/env bash
# Deploys the registry contracts to the local chain and writes the las2peer registry config.
# Expects a checkout of las2peer-registry-contracts (branch revival) next to this repo, or CONTRACTS_DIR.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONTRACTS_DIR=${CONTRACTS_DIR:-"$ROOT/../las2peer-registry-contracts"}
[[ -x "$CONTRACTS_DIR/scripts/deploy.sh" ]] || { echo "no contracts checkout at $CONTRACTS_DIR (set CONTRACTS_DIR)"; exit 1; }
# the contracts bring their own toolchain (Foundry), pinned in their mise.toml
run() { if command -v mise >/dev/null; then (cd "$CONTRACTS_DIR" && mise exec -- "$@"); else (cd "$CONTRACTS_DIR" && "$@"); fi; }
[[ -d "$CONTRACTS_DIR/lib/forge-std" ]] || run forge install --no-git foundry-rs/forge-std@v1.17.0
mkdir -p "$ROOT/etc"
export REGISTRY_CONFIG="$ROOT/etc/i5.las2peer.registry.data.RegistryConfiguration.properties"
run ./scripts/deploy.sh
