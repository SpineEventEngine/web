/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:spine_client/firebase_client.dart';
import 'package:spine_client/src/proto/main/dart/spine/client/query.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/core/ack.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/core/command.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/web/firebase/query/response.pb.dart';

const _base64 = Base64Codec();

/// A client of a Spine-based web server.
///
/// Communicates with the backend via the Spine Firebase-web protocol.
///
/// For read operations, the client sends a request to the Spine-based server, receives a path to
/// a node in Firebase Realtime Database in response, and fetches the data under that node.
///
class BackendClient {

    final String _baseUrl;
    final FirebaseClient _database;

    BackendClient(this._baseUrl, this._database);

    /// Posts a given [Command] to the server.
    Future<Ack> post(Command command) {
        var body = command.writeToBuffer();
        return http
            .post('$_baseUrl/command', body: _base64.encode(body))
            .then(_parseAck);
    }

    /// Obtains entities matching the given query from the server.
    ///
    /// Sends a [Query] to the server and receives a path to a node in Firebase Realtime Database.
    /// The node's children represent the entities matching the query.
    ///
    /// Throws an exception if the query is invalid or if any kind of network or server error
    /// occurs.
    ///
    Stream<T> fetch<T>(Query query, T parse(String json)) async* {
        var body = query.writeToBuffer();
        var qr = await http.post('$_baseUrl/query', body: _base64.encode(body))
            .then(_parseQueryResponse);
        yield* _database
            .get(qr.path)
            .take(qr.count.toInt())
            .map(parse);
    }

    Ack _parseAck(http.Response response) {
        var bytes = _base64.decode(response.body);
        var ack = Ack.fromBuffer(bytes);
        return ack;
    }

    FirebaseQueryResponse _parseQueryResponse(http.Response response) {
        var bytes = _base64.decode(response.body);
        var queryResponse = FirebaseQueryResponse.fromBuffer(bytes);
        return queryResponse;
    }
}
