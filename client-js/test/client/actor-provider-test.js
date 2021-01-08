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

import {Message} from 'google-protobuf';
import {Duration} from '@lib/client/time-utils';
import {ActorProvider} from '@lib/client/actor-request-factory';
import {UserId} from '@proto/spine/core/user_id_pb';
import {fail} from './test-helpers';

class Given {
  constructor() {
    throw new Error('A utility Given class cannot be instantiated.');
  }

  static assertProvidesActor(actorProvider, expectedActor) {
    const providedActor = actorProvider.get();

    assert.ok(Message.equals(providedActor, expectedActor),
      `Expected actor with ID '${expectedActor.getValue()}', actual ${providedActor.getValue()}`);
  }

  static assertProvidesAnonymousActor(actorProvider) {
    Given.assertProvidesActor(actorProvider, ActorProvider.ANONYMOUS);
  }
}

Given.ACTOR_BOB = function() {
  const actor = new UserId();
  actor.setValue('bob@example.com');
  return actor;
}();

Given.ACTOR_MIKE = function() {
  const actor = new UserId();
  actor.setValue('mike@acme.org');
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
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_BOB);
  });

  it('provides anonymous actor when `null` actor set', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    actorProvider.update(null);
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('provides anonymous actor when undefined actor set', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    actorProvider.update();
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('provides correct actor when actor value changed', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    actorProvider.update(Given.ACTOR_MIKE);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_MIKE);
  });

  it('provides correct actor when actor hasn`t changed', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    actorProvider.update(Given.ACTOR_BOB);
    Given.assertProvidesActor(actorProvider, Given.ACTOR_BOB);
  });

  it('provides anonymous actor when cleared', () => {
    const actorProvider = new ActorProvider(Given.ACTOR_BOB);
    actorProvider.clear();
    Given.assertProvidesAnonymousActor(actorProvider);
  });

  it('throws an error when the object of not `UserId` type is passed', done => {
    const actorProvider = new ActorProvider();
    try {
      actorProvider.update({value: 'invalid-identifier'});
    } catch (e) {
      done();
    }

    fail(done);
  });
});
