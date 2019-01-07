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

import KnownTypes from '../../src/client/known-types';
import {types as webClientTypes} from 'spine-web-client-proto/known_types';
import {types as testTypes} from '../../proto/test/js/known_types';

describe('KnownTypes', () => {

  it('registers web-client types', () => {
    assertHasTypeUrls(webClientTypes);
  });

  it('registers test types', () => {
    assertHasTypeUrls(testTypes);
  });

  it('skips already registered types',() => {
    KnownTypes.with(webClientTypes);
    KnownTypes.with(testTypes);
    assertHasTypeUrls(webClientTypes);
    assertHasTypeUrls(testTypes);
  });

  function assertHasTypeUrls(knownTypesSubset) {
    for (let [typeUrl] of knownTypesSubset) {
      const hasTypeUrl = KnownTypes.hasTypeUrl(typeUrl);
      assert.ok(hasTypeUrl);
    }
  }
});
