Welcome to LAS2peer!
=================

This project contains LAS2peer itself. To develop a service for LAS2peer, please use the 
[LAS2Peer-Sample-Project](https://github.com/rwth-acis/LAS2peer-Sample-Service/archive/master.zip) and follow the instructions of the projects readme.


PREPARATIONS
-----------------------

If you use an Oracle Java version, you have to enable strong encryption for this software.

Just put the files to [...]/lib/security/local-policy.jar of your java runtime. (You have to replace the existing one.)

The policy files can be downloaded via the Oracle webpage:

[JCE for Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html "JCE-7")

(If the unit-test "i5.las2peer.communication.MessageTest" runs successfully, you have enabled strong encryption correctly)

Building instructions [![Build Status](https://travis-ci.org/rwth-acis/las2peer.png?branch=master)](https://travis-ci.org/rwth-acis/las2peer)
----------------------

For building simply run
    ant compile_all

Unit tests
-----------

All JUnit tests are started with
    ant junit_tests

Reports can be found in ../tmp/reports afterwards.

JavaDoc
----------

Simply build the standard java docs with
    ant java_doc