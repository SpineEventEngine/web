# Integration tests

This module performs integration tests of the unpublished `spine-web` NPM library against
the Spine-based backend server. The `test-app` module contains a simplified Spine application.
The `web-tests` module defines tests which act as a client-side application where client-server
interactions are carried out by `spine-web` NPM library.

## `test-app`

The `web-tests` module contains a simplified Spine application. The `web-tests` tests use this
application as a test backend. The application uses the Spine `web` API (with the `firebase-web`
library). The application model contains a minimal set of commands, events, and projections that
allows to test functionality of the `spine-web` library.

The application uses Firebase application emulated locally with a [`firebase-server`](https://www.npmjs.com/package/firebase-server)
tool. This tool is executed from the `web-tests` module `node_modules` folder.

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


### Running integration test
