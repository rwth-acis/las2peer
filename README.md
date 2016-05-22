![las2peer](https://github.com/rwth-acis/las2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

las2peer-RESTMapper
======================

Jenkins: [![Build Status](http://layers.dbis.rwth-aachen.de/jenkins/buildStatus/icon?job=las2peer REST Mapper)](http://layers.dbis.rwth-aachen.de/jenkins/job/las2peer%20REST%20Mapper/)

Travis CI: [![Build Status](https://travis-ci.org/rwth-acis/las2peer-RESTMapper.svg?branch=master)](https://travis-ci.org/rwth-acis/las2peer-RESTMapper)

Maps HTTP methods and URI patterns to service methods.

Creates a XML based on annotations used in the service class.
From the XML (which can also come from an external source) a mapping tree is created.
For each request the matching services and methods is looked up using this tree by traversing the URI path.

The information from the request is used to assign values to the method parameters, so the method can be directly invoked.
