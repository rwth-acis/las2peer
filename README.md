![LAS2peer](https://raw.github.com/rwth-acis/LAS2peer/master/img/las2peer_logo.png)
=================
LAS2peer is a Java-based server framework for developing and deploying services in a distributed Peer-to-Peer (P2P) environment. LAS2peer was developed by the Advanced Community Information Systems (ACIS) group at the Chair of Computer Science 5 (Information Systems & Databases), RWTH Aachen University, Germany. Its main focus lies on providing developers with a tool to easily develop and test their services and deploy them in a P2P network without having to rely on a centralized infrastructure.

Developers can develop and test their services locally and then deploy them on any machine that has joined the network. For communication between nodes, the FreePastry (http://www.freepastry.org/) library is used.

Currently, connection to the outside is realized via a connector that uses the HTTP-Protocol.

Service Development
-----------------------
This project contains LAS2peer itself. To develop a service for LAS2peer, please use the 
[LAS2Peer-Sample-Service-Project](https://github.com/rwth-acis/LAS2peer-Sample-Service/) and follow the instructions of the project's ReadMe.

Preparations
-----------------------

LAS2peer depends on strong encryption enabled in its Java Runtime Environment (JRE).
If you use an Oracle Java version, you have to enable strong encryption by replacing a set of policy files in subdirectory ./lib/security/ of your JRE installation.

Policy files for strong encryption can be downloaded via Oracle:

[JCE for Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html "JCE-7")

(If the unit-test "i5.las2peer.communication.MessageTest" runs successfully, you have enabled strong encryption correctly)


Building Instructions [![Build Status](https://travis-ci.org/rwth-acis/LAS2peer.png?branch=master)](https://travis-ci.org/rwth-acis/LAS2peer)
----------------------

For building simply run:  
    ```ant compile_all```


Unit Tests
-----------

All JUnit tests are started with:  
    ```ant junit_tests```

Reports can be found in ../tmp/reports afterwards.


JavaDoc
----------

Simply build the standard java docs with:  
    ```ant java_doc```


Start a LAS2peer Instance
----------

You can use the start scripts located in the "bin/" folder.
These start scripts use the L2PNodeLauncher to start a LAS2peer instance.
For more information on how to use the Node-Launcher, please refer to the [LAS2Peer-Sample-Service-Project](https://github.com/rwth-acis/LAS2peer-Sample-Service/) 
