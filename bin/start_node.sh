#! /bin/sh

BINPATH=$(dirname $0)/

mkdir -p log

${BINPATH}start_java.sh i5.las2peer.testing.L2pNodeLauncher $1 $2 $3 $4 $5 $6 $7 $8 $9

