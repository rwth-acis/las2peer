#! /bin/bash

BASE=$(dirname $0)/../

if [ ! -d ${BASE}../temp/classes ]; then
	echo "Classes directory does not exist - did you do an 'ant' or 'ant complile_all'?"
	echo
	exit
fi;


if [ $(uname -o) = "Cygwin" ]
then
    # we're in cygwin
	export COLOR_DISABLED=1
	export CLASSPATH="${BASE}../temp/classes/;${BASE}/../temp/junit/;${BASE}lib/simpleXML.jar;${BASE}lib/FreePastry-2.1.jar;${BASE}lib/commons-codec-1.7.jar;${BASE}lib/xpp3.jar;${BASE}lib/httpServer.jar;${BASE}/lib/qvs/guava-9.jar"
else
	# we're somewhere else
	export CLASSPATH="${BASE}../temp/classes/:${BASE}/../temp/junit/:${BASE}lib/simpleXML.jar:${BASE}lib/FreePastry-2.1.jar:${BASE}lib/commons-codec-1.7.jar:${BASE}lib/xpp3.jar:${BASE}lib/httpServer.jar:${BASE}lib/qvs/guava-9.jar"
fi


BASE=$(dirname ${0})/../

java "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
