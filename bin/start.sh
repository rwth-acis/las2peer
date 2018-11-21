#!/bin/bash
set -e

LAS2PEER_BOOTSTRAP=${LAS2PEER_BOOTSTRAP:-"137.226.232.53:9000"}
LAS2PEER_CONFIG_ENDPOINT=${LAS2PEER_CONFIG_ENDPOINT:-"137.226.232.53:8001"}
LAS2PEER_ETH_BOOTSTRAP=${LAS2PEER_ETH_BOOTSTRAP:-"137.226.232.53:10303"}
LAS2PEER_ETH_HOST=${LAS2PEER_ETH_HOST:-"127.0.0.1"}
LAS2PEER_PORT=${LAS2PEER_PORT:-"9000"}

echo "Starting Ethereum node in Docker container (uses ports 8545 and 30303) ..."
docker run --rm -p 8545:8545 -p 30303:30303 -e ETHEREUM_BOOTSTRAP=${LAS2PEER_ETH_BOOTSTRAP} -e MINE=${MINE:-"yes"} -it --name eth-peer tjanson/go-ethereum:monitored-client

ETH_PROPS_DIR=./etc/
ETH_PROPS=i5.las2peer.registryGateway.Registry.properties

echo Attempting to autoconfigure registry blockchain parameters ...
if ./bin/wait-for-it.sh ${LAS2PEER_CONFIG_ENDPOINT} --timeout=300; then
    echo Downloading ...
    wget "http://${LAS2PEER_CONFIG_ENDPOINT}/${ETH_PROPS}" -O "${ETH_PROPS_DIR}${ETH_PROPS}"
    echo done.
else
    echo Registry configuration endpoint specified but not accessible. Aborting.
    exit 1
fi

echo Replacing Ethereum client host in config files ...
sed -i "s|^endpoint.*$|endpoint = http://${LAS2PEER_ETH_HOST}:8545|" "${ETH_PROPS_DIR}${ETH_PROPS}"
echo Replacing wallets path ...
sed -i "s|/root/keystore|./bin/keystore|" "${ETH_PROPS_DIR}${ETH_PROPS}"
echo done.

if ./bin/wait-for-it.sh ${LAS2PEER_BOOTSTRAP} --timeout=300; then
    echo Las2peer bootstrap available, continuing.
else
    echo Las2peer bootstrap specified but not accessible. Aborting.
    exit 3
fi

echo Starting las2peer node ...
java -cp "registrygateway/src/main/resources/:core/export/jars/*:registrygateway/export/jars/*:restmapper/export/jars/*:webconnector/export/jars/*:core/lib/*:registrygateway/lib/*:restmapper/lib/*:webconnector/lib/*" i5.las2peer.tools.L2pNodeLauncher --port $LAS2PEER_PORT $([ -n "$LAS2PEER_BOOTSTRAP" ] && echo "--bootstrap $LAS2PEER_BOOTSTRAP") --node-id-seed $RANDOM startWebConnector "node=getNode()" "registry=node.getRegistry()" "n=getNode" "r=n.getRegistry()" interactive
