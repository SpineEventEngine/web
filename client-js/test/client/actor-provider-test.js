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

import assert from 'assert';

import {Message} from 'google-protobuf';
import {Duration} from '@lib/client/time-utils';
import {ActorProvider} from '@lib/client/actor-request-factory';
import {UserId} from '@testProto/spine/core/user_id_pb';
import {fail} from './test-helpers';

class Given {
  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  static assertProvidesActor(actorProvider, expectedActor) {
    const providedActor = actorProvider.getActor();

    assert.ok(Message.equals(providedActor, expectedActor),
      `Expected actor with ID '${expectedActor.getValue()}', actual ${providedActor.getValue()}`);
  }

  static assertProvidesAnonymousActor(actorProvider) {
    Given.assertProvidesActor(actorProvider, ActorProvider.ANONYMOUS_ACTOR);
  }
}

Given.ACTOR_1 = function() {
  const actor = new UserId();
  actor.setValue('test-spine-web-actor');
  return actor;
}();

Given.ACTOR_2 = function() {
  const actor = new UserId();
  actor.setValue('actor-web-spine-test');
  return actor;
}();

describe('ActorProvider', function () {

  const timeoutDuration = new Duration({seconds: 5});
  this.timeout(timeoutDuration.inMs());

  it('provides anonymous actor', () => {
    const actorProvider = new ActorProvider();
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('provides custom actor', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_1);
  });

  it('provides anonymous actor when `null` actor set', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    actorProvider.setActor(null);
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('provides anonymous actor when undefined actor set', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    actorProvider.setActor();
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('provides correct actor when actor value changed', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    actorProvider.setActor(Given.ACTOR_2);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_2);
  });

  it('provides correct actor when actor hasn`t changed', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    actorProvider.setActor(Given.ACTOR_1);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_1);
  });

  it('provides anonymous actor when cleared', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_1);
    actorProvider.clearActor();
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('throws an error when the object of not `UserId` type is passed', done => {
    const actorProvider = new ActorProvider();
    try {
      actorProvider.setActor({value: 'invalid-identifier'});
    } catch (e) {
      done();
    }

    fail(done);
  });
});
