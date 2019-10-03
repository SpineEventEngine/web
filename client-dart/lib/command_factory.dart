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
import 'package:spine_client/actor_request_factory.dart';
import 'package:spine_client/spine/core/command.pb.dart';
import 'package:spine_client/src/known_types.dart';
import 'package:spine_client/uuids.dart';

/// A factory of commands to send to the server.
class CommandFactory {

    final ActorProvider _context;

    CommandFactory(this._context);

    /// Creates a command with the given message.
    Command create(GeneratedMessage message) {
        var packer = theKnownTypes.anyPacker();
        var cmd = Command();
        cmd
            ..id = _newId()
            ..message = packer.pack(message)
            ..context = _buildContext();
        return cmd;
    }

    CommandContext _buildContext() {
        var ctx = CommandContext();
        ctx.actorContext = _context();
        return ctx;
    }

    CommandId _newId() {
        var id = CommandId();
        id.uuid = newUuid();
        return id;
    }
}
