module.exports = {
    entry: './main/index.js',
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
    module: {
        rules: [{
            test: /\.jsx?$/,
            exclude: /(node_modules|bower_components)/,
            loader: 'babel-loader'
        }]
    }
};
