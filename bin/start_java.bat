cd ..
set BASE=%CD%
set CLASSPATH=%BASE%\tmp\classes\;%BASE%\tmp\junit\;%BASE%\lib\simpleXML-0.1.jar;%BASE%\lib\FreePastry-2.1.jar;%BASE%\lib\commonsCodec-1.7.jar;%BASE%\lib\xpp3-1.1.4.jar;%BASE%\lib\httpServer-0.2.jar;

java %1 %2 %3 %4 %5 %6 %7 %8 %9
pause