![las2peer](https://github.com/rwth-acis/LAS2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

las2peer is a Java-based server framework for developing and deploying services in a distributed Peer-to-Peer (P2P) environment. las2peer was developed by the Advanced Community Information Systems (ACIS) group at the Chair of Computer Science 5 (Information Systems & Databases), RWTH Aachen University, Germany. It's main focus lies on providing developers with a tool to easily develop and test their services and deploy them in a P2P network without having to rely on a centralized infrastructure.

Developers can develop and test their services locally and then deploy them on any machine that has joined the network. For communication between nodes, the FreePastry (http://www.freepastry.org/) library is used.

Currently, connection to the outside is realized via the [HTTP-Connector](https://github.com/rwth-acis/LAS2peer-HttpConnector/) or the [Web-Connector](https://github.com/rwth-acis/LAS2peer-WebConnector/).

Service Development
-----------------------
This project contains las2peer itself. To develop a service for las2peer, please use the 
[las2peer Template Project](https://github.com/rwth-acis/LAS2peer-Template-Project/) and follow the instructions of the project's ReadMe.  

If you want to learn more about las2peer, please visit the [las2peer Template Project's Wiki Page](https://github.com/rwth-acis/LAS2peer-Template-Project/wiki).

Preparations
-----------------------

las2peer depends on strong encryption enabled in its Java Runtime Environment (JRE).
If you use an Oracle Java version, you have to enable strong encryption by replacing a set of policy files in subdirectory ./lib/security/ of your JRE installation.

Policy files for strong encryption can be downloaded via Oracle:

[JCE for Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html "JCE-8")

(If the JUnit-test "i5.las2peer.communication.MessageTest" runs successfully, you have enabled strong encryption correctly)


Building Instructions
----------------------

To build the las2peer jar file simply run default target:
    ```ant```
    or directly
    ```ant jars```

Jenkins: [![Build Status](http://layers.dbis.rwth-aachen.de/jenkins/buildStatus/icon?job=LAS2peer Core)](http://layers.dbis.rwth-aachen.de/jenkins/job/LAS2peer%20Core/)

Travis CI: [![Build Status](https://travis-ci.org/rwth-acis/LAS2peer.svg?branch=master)](https://travis-ci.org/rwth-acis/LAS2peer)

JUnit Tests
-----------

All JUnit tests are started with:  
    ```ant junit_tests```

Reports can be found in ./tmp/test_reports afterwards.


Javadoc
----------

Simply build the standard Javadocs with:  
    ```ant javadoc```

Javadoc can be found in ./export/javadoc afterwards.
