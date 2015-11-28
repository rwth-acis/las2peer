cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/las2peer.jar;%BASE%/service/*"

cd %~dp0

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher %*
pause

