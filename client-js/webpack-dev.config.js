const merge = require('webpack-merge');
const common = require('./webpack-common.config');

module.exports = merge(common, {
    mode: 'development', // "production" | "development" | "none"
    output: {
        filename: 'bundle.umd.js', // the filename template for entry chunks
    },
});
