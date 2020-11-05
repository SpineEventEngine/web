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
import {TenantIds} from "../../main/client/tenant";

describe('TenantIds', function () {

  it('create a tenant ID which represents an internet domain', done => {
    const internetDomain = "en.wikipedia.org";
    const tenantId = TenantIds.internetDomain(internetDomain);
    assert.strictEqual(tenantId.getDomain(), internetDomain);
    done();
  });

  it('throws an `Error` when the passed internet domain name is not defined', () => {
    assert.throws(
        () => TenantIds.internetDomain(undefined)
    );
  });

  it('throws an `Error` when the passed internet domain name is empty', () => {
    assert.throws(
        () => TenantIds.internetDomain('')
    );
  });

  it('create a tenant ID which represents an email address', done => {
    const emailAddress = "user@test.com";
    const tenantId = TenantIds.emailAddress(emailAddress);
    assert.strictEqual(tenantId.getEmail(), emailAddress);
    done();
  });

  it('throws an `Error` when the passed email address value is not defined', () => {
    assert.throws(
        () => TenantIds.emailAddress(undefined)
    );
  });

  it('throws an `Error` when the passed email address value is empty', () => {
    assert.throws(
        () => TenantIds.emailAddress('')
    );
  });

  it('create a plain string tenant ID', done => {
    const tenantIdValue = "some-tenant-ID";
    const tenantId = TenantIds.plainString(tenantIdValue);
    assert.strictEqual(tenantId.getValue(), tenantIdValue);
    done();
  });

  it('throws an `Error` when the passed string is not defined', () => {
    assert.throws(
        () => TenantIds.plainString(undefined)
    );
  });

  it('throws an `Error` when the passed string is empty', () => {
    assert.throws(
        () => TenantIds.plainString('')
    );
  });
});
