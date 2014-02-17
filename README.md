LAS2peer-RESTMapper
======================

Maps HTTP methods and URI patterns to service methods.

Creates a XML based on annotations used in the service class.
From the XML (which can also come from an external source) a mapping tree is created.
For each request the matching services and methods is looked up using this tree by traversing the URI path.

The information from the request is used to assign values to the method parameters, so the method can be directly invoked.
