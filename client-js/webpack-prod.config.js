const merge = require('webpack-merge');
const common = require('./webpack-common.config');

module.exports = merge(common, {
    mode: 'production', // "production" | "development" | "none"
    output: {
        filename: 'bundle.umd.min.js', // the filename template for entry chunks
    },
});
