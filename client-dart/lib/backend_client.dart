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

import 'package:firebase/firebase.dart' as firebase;
import 'package:http/http.dart' as http;
import 'package:spine_client/src/proto/main/dart/spine/client/query.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/core/ack.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/core/command.pb.dart';
import 'package:spine_client/src/proto/main/dart/spine/web/firebase/query/response.pb.dart';

var _base64 = Base64Codec();

class BackendClient {

    final String _baseUrl;
    final firebase.Database _database;

    BackendClient(this._baseUrl, this._database);

    Future<Ack> post(Command command) {
        var body = command.writeToBuffer();
        return http
            .post('$_baseUrl/command', body: _base64.encode(body))
            .then(_parseAck);
    }

    Stream<T> fetch<T>(Query query, T parse(String json)) async* {
        var body = query.writeToBuffer();
        var qr = await http.post('$_baseUrl/query', body: _base64.encode(body))
            .then(_parseQueryResponse);
        yield* _database
            .ref(qr.path)
            .onChildAdded
            .map((event) => _parseValue(event, parse));
    }

    T _parseValue<T>(firebase.QueryEvent event, T parse(String json)) {
        var json = event.snapshot.toJson();
        return parse(json.toString());
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
