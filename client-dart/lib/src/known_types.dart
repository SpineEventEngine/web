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

import 'package:protobuf/protobuf.dart';
import 'package:spine_client/types.dart' as standardTypes;

final theKnownTypes = KnownTypes._instance();

class KnownTypes {

    final Map<String, BuilderInfo> _typeUrlToBuilderInfo = Map();
    final Map<GeneratedMessage, String> _msgToTypeUrl = Map();

    KnownTypes._instance() {
        register(standardTypes.types());
    }

    BuilderInfo findBuilderInfo(String typeUrl) {
        return _typeUrlToBuilderInfo[typeUrl];
    }

    String typeUrlOf(GeneratedMessage message) {
        var defaultValue = message.createEmptyInstance();
        return _msgToTypeUrl[defaultValue];
    }

    TypeRegistry registry() {
        return TypeRegistry(_msgToTypeUrl.keys);
    }

    void register(dynamic types) {
        Map<String, BuilderInfo> typeUrlToBuilderInfo = types.typeUrlToInfo;
        Map<GeneratedMessage, String> msgToTypeUrl = types.defaultToTypeUrl;
        _typeUrlToBuilderInfo.addAll(typeUrlToBuilderInfo);
        _msgToTypeUrl.addAll(msgToTypeUrl);
    }
}
