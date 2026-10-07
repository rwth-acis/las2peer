#!/usr/bin/env bash
# Starts a las2peer node from the local source build with the Ethereum registry enabled.
# WALLET=N picks the N-th dev mnemonic (0-9), matching the funded keys in chain.sh.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MNEMONICS=(
  "differ employ cook sport clinic wedding melody column pave stuff oak price"
  "memory wrist half aunt shrug elbow upper anxiety maximum valve finish stay"
  "alert sword real code safe divorce firm detect donate cupboard forward other"
  "pair stem change april else stage resource accident will divert voyage lawn"
  "lamp elbow happy never cake very weird mix episode either chimney episode"
  "cool pioneer toe kiwi decline receive stamp write boy border check retire"
  "obvious lady prize shrimp taste position abstract promote market wink silver proof"
  "tired office manage bird scheme gorilla siren food abandon mansion field caution"
  "resemble cattle regret priority hen six century hungry rice grape patch family"
  "access crazy can job volume utility dial position shaft stadium soccer seven"
)
WALLET=${WALLET:-0}
PORT=${PORT:-9011}
cd "$ROOT"
# webconnector/lib holds all runtime dependencies (core/lib and restmapper/lib only add older duplicates)
CP="core/export/jars/*:restmapper/export/jars/*:webconnector/export/jars/*:webconnector/lib/*"
exec java -cp "$CP" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED \
  i5.las2peer.tools.L2pNodeLauncher --service-directory "$ROOT/services" --port "$PORT" --node-id-seed "${NODE_ID_SEED:-$WALLET}" \
  ${BOOTSTRAP:+--bootstrap "$BOOTSTRAP"} \
  --ethereum-mnemonic "${MNEMONICS[$WALLET]}" \
  startWebConnector "node=getNodeAsEthereumNode()" "registry=node.getRegistryClient()" "$@" interactive
