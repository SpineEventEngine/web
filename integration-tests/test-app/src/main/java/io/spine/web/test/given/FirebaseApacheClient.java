/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.apache.ApacheHttpTransport;
import io.spine.logging.Logging;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.NodePath;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.rest.RestClient;
import io.spine.web.test.given.rest.RestBackOffCaller;
import io.spine.web.test.given.rest.RestBackOffRunner;

import java.util.Optional;

/**
 * Firebase Client over {@code HttpApacheTransport}.
 *
 * <p>{@code ApacheHttpTransport} could encounter issues on connecting to Firebase via REST.
 * This class wraps transport and adds backoff retry policies for the REST API calls. For more
 * details
 * about possible exceptions see <a href="http://hc.apache.org/httpclient-3.x/exception-handling.html">
 * Apache Http Client Exception Handling</a>
 *
 * @author Dmitry Kashcheiev
 */
public class FirebaseApacheClient implements FirebaseClient, Logging {

    private final RestClient restClient;

    public FirebaseApacheClient(DatabaseUrl url) {
        HttpRequestFactory requestFactory = new ApacheHttpTransport().createRequestFactory();
        this.restClient = RestClient.create(url, requestFactory);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public Optional<NodeValue> get(NodePath nodePath) {
        return RestBackOffCaller.<Optional<NodeValue>>create()
                .call(() -> restClient.get(nodePath));
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void merge(NodePath nodePath, NodeValue value) {
        RestBackOffRunner.create()
                         .run(() -> restClient.merge(nodePath, value));
    }
}
