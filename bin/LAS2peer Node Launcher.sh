#! /bin/sh

mkdir -p ../log
BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/../

if [ ! -e "${BASE}export/jars/las2peer.jar" ]; then
	echo "Do 'ant jars' before running this!"
	exit
fi;

if [ $(uname -o) = "Cygwin" ]
then
    # we're in cygwin
	export COLOR_DISABLED=1
	export CLASSPATH="${BASE}lib/*;${BASE}export/jars/las2peer.jar;"
else
	# we're somewhere else
	export CLASSPATH="${BASE}lib/*:${BASE}export/jars/las2peer.jar"
fi

java -cp "${CLASSPATH}" i5.las2peer.tools.L2pNodeLauncher "$@"
