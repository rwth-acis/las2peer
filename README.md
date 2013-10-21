Welcome to LAS2peer!
=================

This project contains LAS2peer itself. To develop a service for LAS2peer, please use the 
[LAS2Peer-Sample-Project](https://github.com/rwth-acis/LAS2peer-Sample-Service/) and follow the instructions of the project's ReadMe.

What is LAS2peer?
-----------------------
LAS2peer is a Java-based framework for distributing community services in a peer-to-peer network infrastructure. LAS2peer was developed by the Advanced Community Information Systems (ACIS) group at the Chair of Computer Science 5 (Information Systems & Databases), RWTH Aachen University, Germany. 

Preparations
-----------------------

If you use an Oracle Java version, you have to enable strong encryption for this software.

Please put the files to [...]/lib/security/ of your java runtime installation (replacing the existing files).

The policy files can be downloaded via Oracle:

[JCE for Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html "JCE-7")

(If the unit-test "i5.las2peer.communication.MessageTest" runs successfully, you have enabled strong encryption correctly)


Building Instructions [![Build Status](https://travis-ci.org/rwth-acis/LAS2peer.png?branch=master)](https://travis-ci.org/rwth-acis/LAS2peer)
----------------------

For building simply run
    ant compile_all


Unit Tests
-----------

All JUnit tests are started with
    ant junit_tests

Reports can be found in ../tmp/reports afterwards.


JavaDoc
----------

Simply build the standard java docs with
    ant java_doc
