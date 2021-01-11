/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import assert from 'assert';
import TestEnvironment from '../given/test-environment';
import {AnyPacker} from '@lib/client/any-packer';
import {Type} from '@lib/client/typed-message';
import {UserInfoAdded} from '@testProto/spine/web/test/given/user_events_pb';
import {UserInfoView} from '@testProto/spine/web/test/given/user_info_pb';
import {initClient} from './given/firebase-client';
import {fail} from "../test-helpers";

const singleTenantClient = initClient(TestEnvironment.ENDPOINT);
const userInfoAddedType = Type.forClass(UserInfoAdded);

describe('Single-tenant client', function () {

  // Big timeout allows to receive model state changes during tests.
  this.timeout(10000);

  it('sends a command', done => {
    const fullName = 'John Smith';
    const cmd = TestEnvironment.addUserInfoCommand(fullName);
    singleTenantClient
        .command(cmd)
        .onError(fail(done))
        .onImmediateRejection(fail(done))
        .observe(UserInfoAdded, ({subscribe, unsubscribe}) =>
            subscribe(event => {
              const eventMessage = AnyPacker.unpack(event.getMessage()).as(userInfoAddedType);
              assert.strictEqual(eventMessage.getFullName(), fullName);
              unsubscribe();
              done();
            }))
        .post();
  });

  it('performs a query', done => {
    const fullName = 'John Smith 2';
    const cmd = TestEnvironment.addUserInfoCommand(fullName);
    const queryTimeoutMs = 1000;
    singleTenantClient
      .command(cmd)
      // Allow the model to receive the updates.
      .onOk(() => setTimeout(() => {
        singleTenantClient
          .select(UserInfoView)
          .byId(cmd.getId())
          .run()
          .then(messages => {
             assert.strictEqual(messages.length, 1);
             assert.strictEqual(messages[0].getFullName(), fullName);
             done();
          })
          .catch((e) => {
             console.error("Failed the single-tenant client query: " + e);
             fail(done);
          });
      }, queryTimeoutMs))
      .post();
  });

  it('subscribes to an entity state', done => {
    const fullName = 'John Smith 3';
    singleTenantClient
        .subscribeTo(UserInfoView)
        .post()
        .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
          itemAdded.subscribe({
            next: item => {
              assert.strictEqual(item.getFullName(), fullName);
              done();
            }
          });
        });
    const cmd = TestEnvironment.addUserInfoCommand(fullName);
    singleTenantClient
        .command(cmd)
        .post();
  });
});
