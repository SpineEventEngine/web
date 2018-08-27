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

package io.spine.web.test.given;

import io.spine.web.firebase.FirebaseSubscriptionBridge;
import io.spine.web.firebase.FirebaseSubscriptionKeepUpServlet;

import javax.servlet.annotation.WebServlet;

import static io.spine.web.test.given.FirebaseClient.database;
import static io.spine.web.test.given.Server.application;

/**
 * An endpoint for client requests to keep subscription running.
 *
 * @author Drachuk Mykhailo
 */
@WebServlet("/subscription/keep-up")
@SuppressWarnings("serial")
public class TestSubscriptionKeepUpServlet extends FirebaseSubscriptionKeepUpServlet {

    public TestSubscriptionKeepUpServlet() {
        super(FirebaseSubscriptionBridge.newBuilder()
                                        .setQueryService(application().getQueryService())
                                        .setDatabase(database())
                                        .build());
    }
}