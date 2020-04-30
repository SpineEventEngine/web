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
import {TenantId} from "../proto/spine/core/tenant_id_pb";
import {TenantProvider} from "../../main/client/tenant";

describe('TenantProvider', function () {

  class Given {

    static tenantId() {
      const tenantId = new TenantId();
      tenantId.setValue("some-tenant");
      return tenantId;
    }
  }

  const tenantId = Given.tenantId();

  it('can be constructed with custom tenant ID', done => {
    const tenantProvider = new TenantProvider(tenantId);
    assert.equal(tenantProvider.tenantId(), tenantId);
    done();
  });

  it('returns `undefined` tenant ID when constructed with no arguments', done => {
    const tenantProvider = new TenantProvider();
    assert.equal(tenantProvider.tenantId(), undefined);
    done();
  });

  it('allows to update the current tenant ID', done => {
    const tenantProvider = new TenantProvider();
    assert.equal(tenantProvider.tenantId(), undefined);
    tenantProvider.update(tenantId);
    assert.equal(tenantProvider.tenantId(), tenantId);
    done();
  });

  it('throws an `Error` when updating with tenant ID which is not defined', () => {
    const tenantProvider = new TenantProvider();
    assert.throws(() => tenantProvider.update(undefined));
  });
});
