#!/usr/bin/env bash
# Configures and starts a las2peer node. All settings come from environment variables:
#   LAS2PEER_PORT            P2P port (default 9011)
#   LAS2PEER_BOOTSTRAP       host:port of a node to join; empty starts a new network
#   LAS2PEER_ETH_MNEMONIC    node operator's BIP39 mnemonic; set to enable the Ethereum registry
#   REGISTRY_CONFIG          registry properties file written by the contract deployer
#   NODE_ADMIN_NAME/EMAIL    node operator shown in the frontend (used for admin checks)
#   OIDC_PROVIDERS           comma-separated OpenID Connect issuers for the frontend login
#   NODE_ID_SEED             seed for a stable node ID
set -euo pipefail
cd /app
LAS2PEER_PORT=${LAS2PEER_PORT:-9011}

if [[ -n "${LAS2PEER_ETH_MNEMONIC:-}" ]]; then
  REGISTRY_CONFIG=${REGISTRY_CONFIG:-/config/registry.properties}
  until [[ -s "$REGISTRY_CONFIG" ]]; do echo "waiting for registry config at $REGISTRY_CONFIG ..."; sleep 1; done
  cp "$REGISTRY_CONFIG" etc/i5.las2peer.registry.data.RegistryConfiguration.properties
fi

cat > etc/i5.las2peer.connectors.webConnector.WebConnector.properties <<PROPS
httpPort = 8080
startHttp = TRUE
startHttps = FALSE
enableCrossOriginResourceSharing = TRUE
crossOriginResourceDomain = *
crossOriginResourceMaxAge = 60
onlyLocalServices = FALSE
oidcProviders = ${OIDC_PROVIDERS:-https://accounts.google.com}
PROPS

cat > etc/nodeInfo.xml <<XML
<las2peerNode>
	<adminName>${NODE_ADMIN_NAME:-admin}</adminName>
	<adminEmail>${NODE_ADMIN_EMAIL:-admin@example.org}</adminEmail>
	<organization>${NODE_ORGANIZATION:-}</organization>
	<description>${NODE_DESCRIPTION:-A las2peer node.}</description>
</las2peerNode>
XML

ARGS=(--service-directory service --port "$LAS2PEER_PORT" --node-id-seed "${NODE_ID_SEED:-$RANDOM}")
[[ -n "${LAS2PEER_BOOTSTRAP:-}" ]] && ARGS+=(--bootstrap "$LAS2PEER_BOOTSTRAP")
CMDS=(startWebConnector)
if [[ -n "${LAS2PEER_ETH_MNEMONIC:-}" ]]; then
  ARGS+=(--ethereum-mnemonic "$LAS2PEER_ETH_MNEMONIC")
  CMDS+=("node=getNodeAsEthereumNode()" "registry=node.getRegistryClient()")
fi

CP="core/export/jars/*:restmapper/export/jars/*:webconnector/export/jars/*:core/lib/*:restmapper/lib/*:webconnector/lib/*"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -cp "$CP" \
  --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED \
  i5.las2peer.tools.L2pNodeLauncher "${ARGS[@]}" "${CMDS[@]}" interactive
