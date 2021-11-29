# Spine Web

[![Ubuntu build][ubuntu-build-badge]][gh-actions]

[gh-actions]: https://github.com/SpineEventEngine/web/actions
[ubuntu-build-badge]: https://github.com/SpineEventEngine/web/actions/workflows/build-on-ubuntu.yml/badge.svg

The Spine library is intended for interactions between a web server and a JavaScript client.

This repository contains both Spine web [API](./web/README.md) and its 
[implementation](./firebase-web/README.md) based on Firebase Realtime Database.

### Java Version

Starting version `2.0.0-SNAPSHOT.69`, the server-side modules of this library are built with Java 11
compilation target. Therefore, the consumer applications have to use Java 11 or higher.

Prior versions were build with Java 8.
