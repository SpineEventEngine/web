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

package io.spine.web.firebase.query;

import com.google.common.testing.EqualsTester;
import io.spine.client.Query;
import io.spine.client.QueryFactory;
import io.spine.core.TenantId;
import io.spine.net.EmailAddress;
import io.spine.net.InternetDomain;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.RequestNodePath;
import io.spine.web.firebase.given.Author;
import io.spine.web.firebase.given.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.spine.time.ZoneIds.systemDefault;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestNodePath should")
class RequestNodePathTest {

    private static final QueryFactory queryFactory =
            new TestActorRequestFactory(RequestNodePathTest.class).query();

    @Test
    @DisplayName("construct self for a Query")
    void testConstruct() {
        Query firstQuery = queryFactory.all(Book.class);
        Query secondQuery = queryFactory.all(Author.class);

        NodePath firstPath = RequestNodePath.of(firstQuery);
        NodePath secondPath = RequestNodePath.of(secondQuery);

        assertNotNull(firstPath);
        assertNotNull(secondPath);
        assertNotEquals(firstPath.getValue(), secondPath.getValue());
    }

    @Test
    @DisplayName("be tenant-aware")
    void testTenantAware() {
        InternetDomain domain = InternetDomain
                .newBuilder()
                .setValue("spine.io")
                .vBuild();
        TenantId domainTenant = TenantId
                .newBuilder()
                .setDomain(domain)
                .vBuild();
        EmailAddress email = EmailAddress
                .newBuilder()
                .setValue("john@doe.org")
                .vBuild();
        TenantId emailTenant = TenantId
                .newBuilder()
                .setEmail(email)
                .vBuild();
        TenantId firstValueTenant = TenantId
                .newBuilder()
                .setValue("first tenant")
                .vBuild();
        TenantId secondValueTenant = TenantId
                .newBuilder()
                .setValue("second tenant")
                .vBuild();
        List<String> paths = Stream.of(domainTenant,
                                       emailTenant,
                                       firstValueTenant,
                                       secondValueTenant)
                                   .map(RequestNodePathTest::tenantAwareQuery)
                                   .map(RequestNodePath::of)
                                   .map(NodePath::getValue)
                                   .collect(toList());
        new EqualsTester()
                .addEqualityGroup(paths.get(0))
                .addEqualityGroup(paths.get(1))
                .addEqualityGroup(paths.get(2))
                .addEqualityGroup(paths.get(3))
                .testEquals();
    }

    @Test
    @DisplayName("construct into a valid path")
    void testEscaped() {
        TestActorRequestFactory requestFactory = new TestActorRequestFactory(
                "a.aa#@)?$0[abb-ab",
                systemDefault()
        );
        Query query = requestFactory.query()
                                    .all(Book.class);
        String path = RequestNodePath.of(query).getValue();
        assertFalse(path.contains("#"));
        assertFalse(path.contains("."));
        assertFalse(path.contains("["));

        assertTrue(path.contains("@"));
        assertTrue(path.contains("?"));
        assertTrue(path.contains(")"));
        assertTrue(path.contains("-"));
    }

    private static Query tenantAwareQuery(TenantId tenantId) {
        TestActorRequestFactory requestFactory =
                new TestActorRequestFactory(RequestNodePathTest.class, tenantId);
        Query query = requestFactory.query()
                                    .all(Book.class);
        return query;
    }
}
