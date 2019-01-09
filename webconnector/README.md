# las2peer Webconnector / Node Front-end

## Structure

The actual front-end source code is located in `./frontend`.
The `./resources` directory contains symlinks, whose targets are bundled into the jar. In particular, it contains the built front-end code (from `./frontend/build/`).

Aside from that, the structure is the same as for the other las2peer modules.

## Development Workflow

When making changes to the front-end only, you probably don't want to wait for the entire Webconnector to build in order to test your changes in your browser.
Instead, you can use `polymer serve` (in `./frontend`) to serve the front-end only.
In order to use your (separately running) Webconnector back-end, change the `apiEndpoint` property in `node-frontend.js`.

As a concrete example, let's say your node runs the Webconnector at its default port, `8080`, then invoking `polymer serve` will automatically select a different port (`8081`). To use the running back-end change the `apiEndpoint`s value to `'http://localhost:8080/las2peer'`.
With `polymer serve` changes are applied immediately (after a page refresh), there is no need to build anything.

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

<details>
<summary>There a several ways to pass this token to las2peer's Webconnector.</summary>

Note: The authentication procedure was changed, so that these examples no longer work.
The access token is now only used during OIDC auto-registration (i.e., the first time an OIDC user tries to login), and still requires a password (which then becomes the new agent's password).

For details, see `AuthenticationManager#authenticateAgent`, but the gist is this: An access token alone is no longer sufficient for logging in (nor registering), instead, we always also require a password in the form of a basic authorization header.

You can still pass the access token in these ways:

1. as a query string parameter: ([HTTPie](https://httpie.org/) example) `http ":8080/las2peer/currentagent?access_token=<TOKEN>`
2. as a [standard Bearer Authorization](https://security.stackexchange.com/a/120244/60003) request header: `http :8080/las2peer/currentagent "Authorization:Bearer <TOKEN>"`
3. as a `access_token` request header: `http :8080/las2peer/currentagent "access_token: <TOKEN>"`

However, this will yield a 400 error saying that a basic auth header is needed as well.
This makes this a bit more complicated, but we can do this.
Here's one: `"Authorization:Basic VVNFUl9OQU1FLWFkYW06YWRhbXNwYXNz"` (that's base64 of `USER_NAME-adam:adamspass`).

With that header, everything works as expected. Well, except one thing: You can't send both a Bearer and Basic authorization header, so that option currently does not work. (Did anyone ever use it?)
</details>