# las2peer Webconnector / Node Front-end

## Structure

The actual front-end source code is located in `./frontend`.
The `./resources` directory contains symlinks, whose targets are bundled into the jar. In particular, it contains the built front-end code (from `./frontend/build/`).

Aside from that, the structure is the same as for the other las2peer modules.

## Building the front-end (separately)

When you build the las2peer / the Webconnector module via ant, the front-end is automatically built and included in the jar.
But if you want to build it separately (e.g., during development), you can do so by running `polymer build` in `./frontend`. (See the README there. `npm run build` is equivalent.)
You can also run `polymer serve`, which reflects your changes without needing to rebuild (but of course this serves just the front-end, it does not run las2peer's Webconnector).

## HTTP Server

This uses Jersey. Various handlers are defined (see `i5.las2peer.connectors.webConnector.handler`) and registered with the server in `WebConnector#start`.
There’s a whole lot of URI processing to pass the requests to the right services but also serve some static files.

## History

Once upon a time, this front-end was originally created by Thomas Cujé based on the [Polymer Starter Kit](https://github.com/Polymer/polymer-starter-kit) for Polymer 2.
 
In late 2018, the components and in fact the entire build process was pretty outdated – such is the life in web frontend development.
Implementing new features without disturbing with the old dependencies was daunting. The browser console spews various deprecation warnings. NPM strongly suggests fixing a gazillion vulnerabilities.

So, let’s just upgrade, right? [There’s even a tool that does everything for you.](https://polymer-library.polymer-project.org/3.0/docs/upgrade) Almost. Well, okay, it’s all broken.  

Yeah, no, we’re starting over.

## Random notes

### Using OIDC with las2peer

The `oidcconnect-signin` component (the button) emits a `'signed-in'` event, whose `.detail` contains the `access_token` and the user's `profile`.
That token can be passed to las2peer in three ways:

1. as a query string parameter: ([HTTPie](https://httpie.org/) example) `http ":8080/las2peer/currentagent?access_token=<TOKEN>`
2. as a [standard Bearer Authorization](https://security.stackexchange.com/a/120244/60003) request header: `http :8080/las2peer/currentagent "Authorization:Bearer <TOKEN>"`
3. as a `access_token` request header: `http :8080/las2peer/currentagent "access_token: <TOKEN>"`

This token is only used to *log-in* to las2peer (which will give you a session cookie – *that* is the actual auth method).
