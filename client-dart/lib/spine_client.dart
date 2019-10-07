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

/// The entry point for a Spine client.
///
/// This library provides the API for constructing and executing actor requests, such as commands
/// and queries.
///
/// The library provides an interface for a firebase client. Two implementations are included in
/// this package but not in the library.
///
/// Also, the package contains all the generated from Protobuf types, as well as many utilities
/// which make constructing requests easier.
///
/// Example:
/// ```dart
/// // Import `spine_client` and helper libraries.
/// import 'package:spine_client/spine_client.dart';
/// import 'package:spine_client/web_firebase_client.dart';
/// import 'package:spine_client/time.dart';
///
/// // Import the Firebase client library.
/// import 'package:firebase/firebase.dart' as fb;
///
/// // Import your model types, generated from Protobuf.
/// import 'example/ackme/task/commands.pb.dart';
/// import 'example/ackme/task/view.pb.dart';
///
/// // Import the type registry which contains all your model types.
/// // Generated by Spine Proto Dart Gradle plugin.
/// import 'types.dart' as exampleTypes;
///
/// void main() {
///     var actorId = UserId()
///             ..value = '$myUserId';
///     var requests = ActorRequestFactory(actorId);
///     var firebase = RestClient(
///             fb.Database.getInstance(myFirebaseDbJsObject()),
///             'https://example-org-42.firebaseio.com'
///     );
///     var client = BackendClient('https://example.org',
///                                firebase,
///                                typeRegistries: [myTypes.types()]);
///     // Fetch all `TaskView` projections and mark all tasks as done.
///     client.fetch(requests.query().all(TaskView())
///           .forEach((taskView) {
///               var markDone = MarkTaskDone()
///                       ..id = taskView.id
///                       ..who_completed = actorId;
///               client.post(requests.command().create(markDone));
///           });
/// }
/// ```
///
library spine_client;

export 'actor_request_factory.dart';
export 'backend_client.dart';
export 'command_factory.dart';
export 'firebase_client.dart';
export 'query_factory.dart';
export 'spine/core/tenant_id.pb.dart';
export 'spine/core/user_id.pb.dart';
export 'spine/time/time.pb.dart';
