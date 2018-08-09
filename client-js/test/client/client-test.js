/*
 * Copyright 2018, TeamDev. All rights reserved.
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

// noinspection NodeJsCodingAssistanceForCoreModules
import assert from "assert";

import {ActorRequestFactory} from "../../src/client/actor-request-factory";
import {FirebaseClient} from "../../src/client/firebase-client";
import {devFirebaseApp as firebase} from "./test-firebase-app";
import {TypedMessage, TypeUrl} from "../../src/client/typed-message";

import commands from "../../proto/test/js/spine/web/test/commands_pb";
import task from "../../proto/test/js/spine/web/test/task_pb";
import {HttpClient} from "../../src/client/http-client";
import {BackendClient} from "../../src/client/backend-client";

const backendClient = newBackendClient();

describe("Client should", function () {

  // Big timeout due to remote calls during tests.
  this.timeout(2 * MINUTES);

  it("send commands successfully", done => {
    const productId = randomId("spine-web-test-1-");
    const command = creteTaskCommand(productId, "Write tests", "client-js needs tests; write'em");

    backendClient.sendCommand(command, function () {

      const type = new TypeUrl("type.spine.io/spine.web.test.Task");
      const idType = new TypeUrl("type.spine.io/spine.web.test.TaskId");
      const typedId = new TypedMessage(productId, idType);

      backendClient.fetchById(type, typedId, data => {
        assert.equal(data.name, command.message.getName());
        assert.equal(data.description, command.message.getDescription());
        done();
      }, done);

    }, fail, fail);
  });

  it("fetch all the existing entities of given type one by one", done => {
    const productId = randomId("spine-web-test-2-");
    const command = creteTaskCommand(productId, "Run tests", "client-js has tests; run'em");

    backendClient.sendCommand(command, function () {

      const type = new TypeUrl("type.spine.io/spine.web.test.Task");
      backendClient.fetchAll({ofType: type}).oneByOne().subscribe({
        next(data) {
          // Ordering is not guaranteed by fetch and 
          // the list of entities cannot be cleaned for tests,
          // thus at least one of entities should match the target one.
          if (data.id.value === productId.getValue()) {
            done();
          }
        }
      });

    }, fail, fail);
  });

  it("fetch all the existing entities of given type at once", done => {
    const productId = randomId("spine-web-test-2-");
    const command = creteTaskCommand(productId, "Run tests", "client-js has tests; run'em");

    backendClient.sendCommand(command, function () {

      const type = new TypeUrl("type.spine.io/spine.web.test.Task");
      backendClient.fetchAll({ofType: type}).atOnce()
        .then(data => {
          const targetObject = data.find(item => item.id.value === productId.getValue());
          assert.ok(targetObject);
          done();
        });

    }, fail, fail);
  });
});


const MILLISECONDS = 1;
const SECONDS = 1000 * MILLISECONDS;
const MINUTES = 60 * SECONDS;

function creteTaskCommand(id, name, description) {
  let command = new commands.CreateTask();
  command.setId(id);
  command.setName(name);
  command.setDescription(description);

  let commandType = new TypeUrl("type.spine.io/spine.web.test.CreateTask");

  return new TypedMessage(command, commandType);
}

function randomId(prefix) {
  let id = prefix + Math.round(Math.random() * 1000);
  let productId = new task.TaskId();
  productId.setValue(id);
  return productId;
}

function newBackendClient() {
  return new BackendClient(newHttpClient(), newFirebaseClient(), newRequestFactory());
}

function newHttpClient() {
  return new HttpClient("https://spine-dev.appspot.com");
}

function newFirebaseClient() {
  return new FirebaseClient(firebase);
}

function newRequestFactory() {
  return new ActorRequestFactory("web-test-actor");
}

const fail = () => assert.notOk(true);
