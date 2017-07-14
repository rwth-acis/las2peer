#!/bin/bash

# this files kills all processes listed in the pid file
# it can be used to shutdown multiple nodes at once

PIDFILE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/las2peer-network-screens.pid

while read pid; do
  kill $pid
done < "$PIDFILE"

/bin/rm "$PIDFILE"
