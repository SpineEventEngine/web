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

package io.spine.web.firebase;

import com.google.api.client.http.ByteArrayContent;
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.google.api.client.http.ByteArrayContent.fromString;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;

class NodeContent {

    private final JsonObject content;

    private NodeContent(JsonObject content) {
        this.content = content;
    }

    NodeContent() {
        this.content = new JsonObject();
    }

    static NodeContent from(String json) {
        JsonParser parser = new JsonParser();
        JsonObject content = parser.parse(json).getAsJsonObject();
        return new NodeContent(content);
    }

    static NodeContent withSingleChild(String childContent) {
        NodeContent nodeContent = new NodeContent();
        nodeContent.pushData(childContent);
        return nodeContent;
    }

    ByteArrayContent toByteArray() {
        String contentString = content.toString();
        ByteArrayContent result = fromString(JSON_UTF_8.toString(), contentString);
        return result;
    }

    void pushData(String data) {
        String key = ChildKeyGenerator.newKey();
        content.addProperty(key, data);
    }

    void addChild(String key, String data) {
        content.addProperty(key, data);
    }

    JsonObject content() {
        return content;
    }

    private static class ChildKeyGenerator {

        private static final Clock CLOCK = new OffsetClock(new DefaultClock(), 0);

        private static String newKey() {
            return generatePushChildName(CLOCK.millis());
        }
    }
}
