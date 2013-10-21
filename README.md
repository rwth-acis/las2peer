![LAS2peer](https://raw.github.com/rwth-acis/LAS2peer/master/img/las2peer_logo.png)
=================
LAS2peer is a server framework for developing and deploying services in a distributed Peer-to-Peer environment written in Java. Its main focus lies on providing developers with a tool to easily develop and test their services and deploy them in the network without having to rely on a centralized infrastructure.

Developers can develop and test their services locally and then deploy them on any machine that has joined the network. For communication between nodes, the FreePastry (http://www.freepastry.org/) library is used.

Currently, connection to the outside is realized via a connector that uses the HTTP-Protocol.

Service Development
-----------------------
This project contains LAS2peer itself. To develop a service for LAS2peer, please use the 
[LAS2Peer-Sample-Project](https://github.com/rwth-acis/LAS2peer-Sample-Service/) and follow the instructions of the project's ReadMe.


Preparations
-----------------------

If you use an Oracle Java version, you have to enable strong encryption for this software.

Please put the files to [...]/lib/security/ of your java runtime installation (replacing the existing files).

The policy files can be downloaded via Oracle:

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
