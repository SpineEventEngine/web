const path = require('path');

module.exports = {
    entry: './src/index.js',
    mode: 'none', // "production" | "development" | "none"
    output: {
        library: 'spine-web-client', // the name of the exported library
        libraryTarget: 'umd', // the type of the exported library,
        umdNamedDefine: true,
        globalObject: "typeof window !== 'undefined' ? window : this" // https://github.com/webpack/webpack/issues/6522
    },
    devtool: 'source-map',
    externals: [
        {
            "isomorphic-fetch": {
                root: 'isomorphic-fetch',
                commonjs2: 'isomorphic-fetch',
                commonjs: 'isomorphic-fetch',
                amd: 'isomorphic-fetch'
            }
        }
    ],
    resolve: {
        alias: {
            // Use Webpack aliasing when bundling files and
            // use `babel-plugin-module-resolver` aliasing when running tests.
            "spine-web-client": path.resolve(__dirname, "./src"),
            "spine-web-client-proto": path.resolve(__dirname, "./proto/main/js")
        }
    }
};
