/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import {UserInfoAdded} from '@testProto/spine/web/test/given/user_events_pb';
import {UserInfo} from '@testProto/spine/web/test/given/user_info_pb';
import {initClient} from './given/firebase-client';

const singleTenantClient = initClient(TestEnvironment.ENDPOINT);

xdescribe('Single-tenant client', function () {

  // Big timeout allows to receive model state changes during tests.
  this.timeout(5000);

  it('sends a command', done => {
    const fullName = 'John Smith';
    const cmd = TestEnvironment.addUserInfoCommand(fullName);
    singleTenantClient.command(cmd)
        .observe(UserInfoAdded, ({subscribe, unsubscribe}) =>
            subscribe(event => {
              assert.equal(event.getFullName(), fullName);
              unsubscribe();
              done();
            }))
        .post();
  });

  it('performs a query', done => {
    const fullName = 'John Smith 2';
    const cmd = TestEnvironment.addUserInfoCommand(fullName);
    singleTenantClient.command(cmd)
        .observe(UserInfoAdded, ({subscribe, unsubscribe}) =>
            subscribe(event => {
              singleTenantClient
                  .select(UserInfo)
                  .byId(event.getId())
                  .run()
                  .then(messages => {
                    assert.equal(messages.length, 1);
                    assert.equal(messages[0].getFullName(), fullName);
                    done();
                  })
              unsubscribe();
            }))
        .post();
  });

  it('subscribes to an entity state', done => {
    const fullName = 'John Smith 3';
    singleTenantClient
        .subscribeTo(UserInfo)
        .post()
        .then(({itemAdded, itemChanged, itemRemoved, unsubscribe}) => {
          itemAdded.subscribe({
            next: item => {
              assert.equal(item.getFullName(), fullName);
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
