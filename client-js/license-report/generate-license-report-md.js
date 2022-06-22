/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * A Node program printing all the NPM dependencies of the project to a file in Markdown.
 *
 * The resulting file is located in
 * `<module-root>/build/reports/dependency-license/dependency/license-report.md`.
 * The file is appended. So in case Java dependencies were written to the same file,
 * they aren't overwritten.
 *
 * See `https://github.com/davglass/license-checker`.
 */

const fs = require('fs');
const checker = require('license-checker');

/**
 * Prints the dependencies to the given `stream` in Markdown format.
 *
 * @param report a license report in JSON format
 * @param stream destination
 */
function printDependencies(report, stream) {
    for (let key in report) {
        const item = report[key];
        stream.write("1. **" + key + "**\n");
        stream.write("     * Licenses: " + item.licenses + "\n");
        stream.write("     * Repository: " + (item.repository 
                                              ? ("[" + item.repository + "](" + item.repository + ")") 
                                              : "unknown") + "\n");
    }
}


// A folder in which the resulting file is written.
const folder = './build/reports/dependency-license/dependency/';
const libraryName = process.env.npm_package_name;
const libraryVersion = process.env.npm_package_version;

// Open the file for appending.
const appending = {'flags': 'a'};
const stream = fs.createWriteStream(folder + "license-report.md", appending);
stream.once('open', function () {

    checker.init({
        start: './',
        production: true
    }, function (err, packages) {
        if (err) {
            console.log("Error searching for production dependencies: " + err);
        } else {
            stream.write("\n\n\n#NPM dependencies of `" + libraryName + "@" + libraryVersion + "`");
            stream.write("\n\n## `Production` dependencies:\n\n");
            printDependencies(packages, stream);

            checker.init({
                start: './',
                development: true
            }, function (err, packages) {
                if (err) {
                    console.log("Error searching for development dependencies: " + err);
                } else {
                    stream.write("\n\n\n## `Development` dependencies:\n\n");
                    printDependencies(packages, stream);

                    const now = new Date();
                    stream.write("\n\nThis report was generated on **" + now + "** using " +
                        "[NPM License Checker](https://github.com/davglass/license-checker) " +
                        "library.");

                    stream.end();
                }
            });
        }
    });
});
