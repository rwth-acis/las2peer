#!/bin/bash

# this script is an example on how to launch a persistent network with multiple nodes at once
# it assumes that you have your las2peer.jar and all other dependencies in ./lib/

PIDFILE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/las2peer-network-screens.pid
echo "$PIDFILE"

# launch bootstrap node at first
screen -S 14501 -D -m java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher --port 14501 --node-id-seed=14501 interactive &
# save process id to be used in stop-network.sh later
echo $! > "$PIDFILE"

# launch other network nodes
for i in {14502..14510}; do
  screen -S $i -D -m java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher --port $i --bootstrap localhost:14501 --node-id-seed=$i interactive &
  echo $! >> "$PIDFILE"
done
