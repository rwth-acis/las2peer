![las2peer](img/logo/bitmap/las2peer-logo-128x128.png)

# [![Build Status](https://jenkins.dbis.rwth-aachen.de/buildStatus/icon?job=las2peer%20Core)](https://jenkins.dbis.rwth-aachen.de/job/las2peer%20Core/) [![Build Status](https://travis-ci.org/rwth-acis/las2peer.svg?branch=master)](https://travis-ci.org/rwth-acis/las2peer) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/rwth-acis/las2peer)

las2peer is a Java-based server framework for developing and deploying microservices in a distributed Peer-to-Peer (P2P) environment.
It is developed by the Advanced Community Information Systems (ACIS) group at the Chair of Computer Science 5 (Information Systems & Databases), RWTH Aachen University, Germany.
It's main focus lies on providing developers with a tool to easily develop and test their microservices and deploy them in a P2P network without having to rely on a centralized infrastructure.
Communication between nodes is realized using the [FreePastry](http://www.freepastry.org/ "FreePastry") library.

For more information on the core concepts of las2peer, please visit [las2peer.org](https://las2peer.org "las2peer.org") or read the [las2peer Primer](https://dx.doi.org/10.13140/RG.2.2.31456.48645 "las2peer Primer").

## Service Development

This project contains las2peer itself.
To develop a service for las2peer, please use the [las2peer Template Project](https://github.com/rwth-acis/las2peer-template-project/) and follow the instructions of the project's ReadMe.

If you want to learn more about las2peer, please visit the [las2peer Template Project's Wiki Page](https://github.com/rwth-acis/las2peer-template-project/wiki).

## Preparations

### Java

las2peer uses **Java 8**.

If you use an Oracle Java version, please make sure you have **Java 8u162** or later installed, so that the Java Cryptography Extension (JCE) is enabled.
Otherwise, you have to enable it manually.
Each las2peer node performs an encryption self-test on startup.

### Build Dependencies

* Apache ant
* Node 6 / npm

## Project Structure

### Modules

This repository contains three las2peer modules:

* Core (`/core`)
* REST Mapper (`/restmapper`)
* Web Connector (`/webconnector`)

Each of them resides in its own subfolder, containing a build file providing the following tasks:

* `ant` or `ant main_jar` will build the respective jar of the submodule. The jars will be stored in `/export/jars`.
* `ant junit_tests` will run the respective JUnit tests. Test reports will be stored in `/tmp/test_reports`.
* `ant javadoc` will create the JavaDocs for the respective submodule. The results will be stored in `/export/javadoc`.
* `ant all` runs all the tasks from above.
* `ant deploy-local` deploys the respective submodule to the local Ivy repository. If you have set up a project to resolve dependencies from the local repository, you can conveniently test your local changes without pushing and publishing them.

Hint: If you want to build a module that depends on another module and you want to test them together, run `ant deploy-local` on the dependency!

### Bundle

las2peer has a modular structure and many dependencies.
However, most installations use all modules together, this is why we provide a bundle of all submodules and their dependencies in one single jar.
The build script for the bundle can be found in `/bundle`.

The build script provides the following tasks:

* `ant` or `ant main_jar` will build the fat jar. The jar will be stored in `/export/jars`.
* `ant deploy-local` deploys the bundle to the local Ivy repository.

If you want to bundle your local changes, run `ant deploy-local` on all modules.

### Super Build Script

To make life easier for developers, this repository contains a build script for conveniently building multiple submodules.

* `ant` or `ant build-only` will run `deploy-local` on each submodule
* `ant all` will run `ant all` and `deploy-local` on each submodule

## Documentation

JavaDocs of the latest release can be found online:

* [Core](http://rwth-acis.github.io/las2peer/core/ "Core")
* [Web Connector](http://rwth-acis.github.io/las2peer/webconnector/ "Web Connector")
* [Rest Mapper](http://rwth-acis.github.io/las2peer/restmapper/ "Rest Mapper")

## CI

All subprojects and bundles will be built by Jenkins and be published as snapshot.
