# Spine Web JS client library
This module is a JS library that communicates with Spine Web server. It’s a facade for sending 
domain commands, querying data and subscribing to entity states.  

The latest published version can be found on [NPM](https://www.npmjs.com/package/spine-web-client).

## Publishing

To publish a new version to NPM:
1. Login to NPM and generate a new access token.
2. Set the generated token to your `NPM_TOKEN` environment variable.
3. Execute Gradle `publishJs` task from project root:
 ```bash
    ./gradlew publishJs
 ``` 
