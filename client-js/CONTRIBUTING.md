# Spine Web JS client library
This module is a JS library that communicates with Spine Web server. It’s a facade for sending 
domain commands, querying data and subscribing to entity states.  

The latest published version can be found on [NPM](https://www.npmjs.com/package/spine-web-client).

## Usage

To use the library install following modules:

```
npm i spine-web-client --save
npm i isomorphic-fetch --save
```

## Testing

You can run tests as follows:
```bash
npm run test
```

You can also run tests from Intellij IDEA. To do it:
* Install [NodeJs Plugin](https://plugins.jetbrains.com/plugin/6098-nodejs). The plugin adds `Mocha` configuration.
* Update `Mocha` configuration template:
  * Add `--require babel-register` to `node options`. It is required to support all ES6 features in Node.js environment.
  * Specify the path to `Mocha` package: `~\IdeaProjects\Spine\web\client-js\node_modules\mocha`.

## Publishing

To publish a new version to NPM:
1. Login to NPM and generate a new access token.
2. Set the generated token to your `NPM_TOKEN` environment variable.
3. Execute Gradle `publishJs` task from project root:
 ```bash
    ./gradlew publishJs
 ``` 
