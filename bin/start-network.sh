#!/bin/bash

# this script is an example on how to launch a persistent network with multiple nodes at once
# it assumes that you have your las2peer.jar and all other dependencies in ./lib/

# launch bootstrap node at first
screen -S 9085 -d -m java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher --port 9085 --node-id-seed=9085 interactive

# launch other network nodes
for i in {9086..9089}; do
  screen -S $i -d -m java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher --port $i --bootstrap localhost:9085 --node-id-seed=$i interactive
done
