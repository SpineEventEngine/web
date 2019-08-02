# Integration tests

This package contains modules for testing of the unpublished `spine-web` NPM library against
the Spine-based server. The `test-app` module contains a simplified Spine application.
The `web-tests` module defines tests acting as a client-side application where client-server
interactions are carried out by `spine-web` NPM library.

## `test-app`

The `test-app` module contains a simplified Spine application. The `web-tests` module uses this
application as a test backend. The application uses the Spine `web` API (with the `firebase-web`
library). The application model contains a minimal set of commands, events, and projections that
allows to test functionality of the `spine-web` library.

The application uses Firebase application emulated locally with a [`firebase-server`](https://www.npmjs.com/package/firebase-server)
tool. This tool is executed from the `web-tests` module `node_modules` folder.

### Dummy service account

The resources of this project contain a Google service account credential. The credential 
corresponds to the `dummy` service account. The account has **no permissions**. This credential
allows us to initialize the Firebase Admin SDK without the need to maintain an encrypted credential
for Travis and AppVeyor.

### Running the application locally

The application can be run locally by Gretty and Firebase emulators. To run the
application do the following:
1. Assemble the application:
    ```bash
    ./gradlew clean assemble
    ```
    
2. Start the local server:

    The following command runs the server on `localhost:8080`. It also runs
    the the local Firebase server on `localhost:5000`:
    ```bash
    ./gradlew appStart
    ```

After the command is executed, the server is available on `localhost:8080`.
To debug the local server, create a new Remote configuration and run it in the debug mode.
The configuration should connect to `localhost:5005`.

### Stopping the application
 
The local server should be stopped with `./gradlew appStop` command or just by terminating a
console process. When stopping the local server with a respective command, server and
Firebase emulators are stopped. When terminating a console process, Firebase emulator stays serving.

## `web-tests`

The `web-tests` module performs tests of the unpublished artifact of the `spine-web` library. It
defines tests acting as a client-side application that uses `spine-web` library for interactions
with a backend.

Performs tests using the unpublished artifact of the `spine-web` library. This approach allows to
test not only the library functionality but also ensure it is properly built.

### Running integration test

To run the integration tests do the following:
1. Assemble the application:
    ```bash
    ./gradlew clean assemble
    ```
    
2. Run tests:
    ```bash
    ./gradlew web-tests:integrationTest
    ```
    This command does the following:
     - installs the unpublished artifact of the `spine-web` library form the `client-js` module 
       as module NPM dependency by call to `npm link spine-web` command. For details about
       two-step package linking see `npm-link` [documentation](https://docs.npmjs.com/cli/link);
     - runs the local backend server from the `test-app` module;
     - performs integration tests;
     - stops the local server when test complete or fail.
