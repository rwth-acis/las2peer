cd %~dp0
cd ..
mkdir log

cd %~dp0
start_java.bat i5.las2peer.testing.L2pNodeLauncher %*
pause

