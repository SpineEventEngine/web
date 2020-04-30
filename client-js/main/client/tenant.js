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

import {TenantId} from "../proto/spine/core/tenant_id_pb";

export class TenantIds {

  /**
   * Constructs a `TenantId` which is a plain string value.
   *
   * @param {!string} tenantIdValue the tenant ID
   * @return {TenantId} a new `TenantId` instance
   */
  static plainString(tenantIdValue) {
    if (!tenantIdValue) {
      throw new Error('Expected a valid tenant ID value.');
    }
    const result = new TenantId();
    result.setValue(tenantIdValue);
    return result;
  }
}

export class TenantProvider {

  /**
   * Creates a new `TenantProvider` configured with the passed tenant ID.
   *
   * The argument may be omitted but until the `_tenantId` is assigned some non-default value, the
   * application is considered single-tenant.
   *
   * @param {?TenantId} tenantId the ID of the currently active tenant
   */
  constructor(tenantId) {
    if (tenantId) {
      this._tenantId = tenantId;
    }
  }

  /**
   * @param {!TenantId} tenantId the ID of the currently active tenant
   */
  update(tenantId) {
    if (!tenantId) {
      throw new Error('Expected a valid tenant ID.');
    }
    this._tenantId = tenantId;
  }

  /**
   * Returns the currently active tenant ID.
   *
   * @return {TenantId}
   */
  tenantId() {
    return this._tenantId;
  }
}

