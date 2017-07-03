#![las2peer](https://rwth-acis.github.io/las2peer/logo/vector/las2peer-logo.svg)
[![Build Status](http://layers.dbis.rwth-aachen.de/jenkins/buildStatus/icon?job=las2peer Core)](http://layers.dbis.rwth-aachen.de/jenkins/job/las2peer%20Core/) [![Build Status](https://travis-ci.org/rwth-acis/las2peer.svg?branch=master)](https://travis-ci.org/rwth-acis/las2peer) [![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/rwth-acis/las2peer)

las2peer is a Java-based server framework for developing and deploying services in a distributed Peer-to-Peer (P2P) environment. las2peer was developed by the Advanced Community Information Systems (ACIS) group at the Chair of Computer Science 5 (Information Systems & Databases), RWTH Aachen University, Germany. It's main focus lies on providing developers with a tool to easily develop and test their services and deploy them in a P2P network without having to rely on a centralized infrastructure.

Developers can develop and test their services locally and then deploy them on any machine that has joined the network. For communication between nodes, the FreePastry (http://www.freepastry.org/) library is used.

## Service Development
This project contains las2peer itself. To develop a service for las2peer, please use the
[las2peer Template Project](https://github.com/rwth-acis/las2peer-Template-Project/) and follow the instructions of the project's ReadMe.  

If you want to learn more about las2peer, please visit the [las2peer Template Project's Wiki Page](https://github.com/rwth-acis/las2peer-Template-Project/wiki).

## Preparations

las2peer depends on strong encryption enabled in its Java Runtime Environment (JRE).
If you use an Oracle Java version, you have to enable strong encryption by replacing a set of policy files in subdirectory ./lib/security/ of your JRE installation.

Policy files for strong encryption can be downloaded via Oracle:

[JCE for Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html "JCE-8")

(If the JUnit-test "i5.las2peer.communication.MessageTest" runs successfully, you have enabled strong encryption correctly)

## Project structure

### Modules

This repository contains four las2peer modules:
* Core (`/core`)
* REST Mapper (`/restmapper`)
* Web Connector (`/webconnector`)
* Node Admin Connector (`/nodeadminconnector`)

Each of them lays in its own subfolder, containing a build file providing the following tasks:
* `ant` or `ant main_jar` will build the respective jar of the submodule. The jars will be stored in `/export/jars`.
* `ant junit_tests` will run the respective junit tests. Test reports will be stored in `/tmp/test_reports`.
* `ant javadoc` will create the javadocs for the respective submodule. The results will be stored in `/export/javadoc`.
* `ant all` runs all the tasks from above.
* `ant deploy-local` deploys the respective submodule to the local ivy repository. If you have set up  a project to resolve dependencies from the local repository, you can conventiently test your local changes to the code without pushing and publishing them to everyone.

Hint: If you want to build a module that depends on another module and want to test them together, run `ant deploy-local` on the dependency!

### Bundle

las2peer has a modular structure and many dependencies. However, most installations use all modules together, this is why we provide a bundle of all submodules and their dependencies in one single jar. The build script for the bundle can be found in `/bundle`.

The build script provides the following tasks:
* `ant` or `ant main_jar` will build the fat jar. The jar will be stored in `/export/jars`.
* `ant deploy-local` deploys the bundle to the local ivy repository.

If you want to bundle your local changes, run `ant deploy-local` on all modules.

### Super build script

To make life easier for us developers, this repository contains a build script for convenient building multiple submodules.

* `ant` or `ant build-only` will run `deploy-local` on each submodule
* `ant all` will run `ant all` and `deploy-local` on each submodule

## CI

All subprojects and bundles will be built by Jenkins and be published as snapshot.
