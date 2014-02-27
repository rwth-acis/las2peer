cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/las2peer.jar;%BASE%/service/*"

java -cp %CLASSPATH% %*

pause
