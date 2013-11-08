#! /bin/sh

BINPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mkdir -p log

${BINPATH}/start_java.sh i5.las2peer.tools.L2pNodeLauncher "$@"

