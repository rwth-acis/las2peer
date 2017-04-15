cd %CD%\..
set BASE=%CD%
set CLASSPATH=%BASE%\tmp\classes;%BASE%\tmp\junit;%BASE%\lib\simpleXML.jar;%BASE%\lib\FreePastry-2.1.jar;%BASE%\lib\commons-codec-1.7.jar;%BASE%\lib\xpp3.jar;%BASE%\lib\httpServer.jar;%BASE%\lib\qvs\guava-9.jar;%CLASSPATH%

java %1 %2 %3 %4
