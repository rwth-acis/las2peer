#! /bin/bash

BASE=$(dirname $0)/../

if [ ! -d ${BASE}tmp/classes ]; then
	echo "Classes directory does not exist - did you do an 'ant' or 'ant complile_all'?"
	echo
	exit
fi;


if [ $(uname -o) = "Cygwin" ]
then
    # we're in cygwin
	export COLOR_DISABLED=1
	export CLASSPATH="${BASE}tmp/classes/;${BASE}tmp/junit/;${BASE}lib/simpleXML-0.1.jar;${BASE}lib/FreePastry-2.1.jar;${BASE}lib/commonsCodec-1.7.jar;${BASE}lib/xpp3-1.1.4.jar;${BASE}lib/httpServer-0.2.jar"
else
	# we're somewhere else
	export CLASSPATH="${BASE}tmp/classes/:${BASE}tmp/junit/:${BASE}lib/simpleXML-0.1.jar:${BASE}lib/FreePastry-2.1.jar:${BASE}lib/commonsCodec-1.7.jar:${BASE}lib/xpp3-1.1.4.jar:${BASE}lib/httpServer-0.2.jar"
fi


java "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
