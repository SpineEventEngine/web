# Spine Web JS client library
This module is a JS library that communicates with Spine Web server. It’s a facade for sending 
domain commands, querying data, and subscribing to entity states. The latest published version can 
be found on [NPM](https://www.npmjs.com/package/spine-web).

The NPM artifact provides the following:
* `spine-web` files along with used Protobuf definitions.
* types from [google-protobuf](https://www.npmjs.com/package/google-protobuf) NPM package.
 These types should be used since they are additionally processed by Spine's Protobuf plugin for JS.
* sources transpiled into ES5 along with their source maps.

It does **not** provide a bundled version assuming that library users perform bundling themselves.

## Environment installation

First, make sure that Node.js is installed and the NPM version is `v.6.1.0` or higher. 
See the [Downloading and installing Node.js and npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
section for detailed installation instructions.

## Usage

To use the library, execute the command:

```
npm i spine-web --save
```

Also, the library has [peer dependencies](https://docs.npmjs.com/files/package.json#peerdependencies)
that you need to install:

```
npm i rxjs --save
```

For a full list of peer dependencies, see [package.json](./package.json).

## Testing

Run the tests using the following command:
```bash
npm run test
```

You can also run tests from IntelliJ IDEA. To do so:
1. Install [NodeJs Plugin](https://plugins.jetbrains.com/plugin/6098-nodejs). The plugin adds `Mocha` configuration.
2. Update `Mocha` configuration template:
  * Add `--require @babel/register` to `node options`. It is required to support all ES6 features in Node.js environment.
  * Specify the path to `Mocha` package: `~\IdeaProjects\Spine\web\client-js\node_modules\mocha`.

## Publishing

To publish a new version to NPM:
1. Log in to NPM and generate a new access token.
2. Set the generated token to your `NPM_TOKEN` environment variable.
3. Execute Gradle `publishJs` task from a project root:
 ```bash
    ./gradlew publishJs
 ``` 
