# las2peer Webconnector / Node Front-end

## Structure

The actual front-end code is located in `./frontend`.
The `./resources` directory contains symlinks, whose targets are bundled into the jar. In particular, it contains the built front-end code.

Aside from that, the structure is the same as for the other las2peer modules.

## HTTP Server

This uses Jersey. Various handlers are defined (see `i5.las2peer.connectors.webConnector.handler`) and registered with the server in `WebConnector#start`.
There’s a whole lot of URI processing to pass the requests to the right services but also serve some static files.

## History

Once upon a time, this front-end was originally created by Thomas Cujé based on the [Polymer Starter Kit](https://github.com/Polymer/polymer-starter-kit) for Polymer 2.
 
In late 2018, the components and in fact the entire build process was pretty outdated – such is the life in web frontend development.
Implementing new features without disturbing with the old dependencies was daunting. The browser console spews various deprecation warnings. NPM strongly suggests fixing a gazillion vulnerabilities.

So, let’s just upgrade, right? [There’s even a tool that does everything for you.](https://polymer-library.polymer-project.org/3.0/docs/upgrade) Almost. Well, okay, it’s all broken.  

Yeah, no, we’re starting over.
