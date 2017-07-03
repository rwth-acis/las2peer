cd %~dp0
cd ..
mkdir XMLOutput
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;%BASE%/export/jars/*"

cd %~dp0

java -cp %CLASSPATH% i5.las2peer.restMapper.tools.CommandLine  create i5.las2peer.restMapper.tools.ExampleClass %BASE%/XMLOutput/xml1.xml
java -cp %CLASSPATH% i5.las2peer.restMapper.tools.CommandLine  validate %BASE%/XMLOutput/xml1.xml
pause