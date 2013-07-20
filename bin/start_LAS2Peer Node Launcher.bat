cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/las2peer.jar"

mkdir log

java -cp %CLASSPATH% i5.las2peer.testing.L2pNodeLauncher -s 9011 - interactive
pause