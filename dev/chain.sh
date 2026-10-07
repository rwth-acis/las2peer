#!/usr/bin/env bash
# Local Ethereum dev chain for the las2peer registry (anvil, in-memory).
# dev/deploy-contracts.sh funds the las2peer dev accounts and deploys the registry.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONTRACTS_DIR=${CONTRACTS_DIR:-"$ROOT/../las2peer-registry-contracts"}
if command -v mise >/dev/null; then cd "$CONTRACTS_DIR" && exec mise exec -- anvil --chain-id 456719; fi
exec anvil --chain-id 456719
