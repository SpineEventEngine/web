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

package io.spine.web.firebase.subscription;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.EntityStateUpdate;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.web.firebase.NodeValue;
import io.spine.web.firebase.StoredJson;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;
import java.util.stream.Collector;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.hash.Hashing.murmur3_128;
import static io.spine.client.SubscriptionUpdate.UpdateCase.ENTITY_UPDATES;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Data of a single subscription update.
 *
 * <p>Consists of entities or events, which ought to be written into the Firebase database under
 * a certain path.
 */
final class UpdatePayload {

    /**
     * A hash function for the entity IDs and event IDs.
     *
     * <p>The ID hashes are used as keys for storing the entities and events in the Firestore.
     *
     * @implNote The 128-but murmur3 function is recommended by Guava as a quick and consistent
     *         between launches. It is not cryptographically safe. However, accidental collisions
     *         should be quite rare.
     * @see com.google.common.hash.Hashing#murmur3_128()
     */
    private static final HashFunction keyHashFunction = murmur3_128();

    private final ImmutableMap<String, @Nullable Message> messages;

    private UpdatePayload(ImmutableMap<String, @Nullable Message> messages) {
        this.messages = messages;
    }

    /**
     * Creates a new {@code UpdatePayload} from the given subscription update.
     */
    static UpdatePayload from(SubscriptionUpdate update) {
        return update.getUpdateCase() == ENTITY_UPDATES
               ? entityUpdates(update)
               : eventUpdates(update);
    }

    private static UpdatePayload entityUpdates(SubscriptionUpdate update) {
        ImmutableMap<String, @Nullable Message> messages = update
                .getEntityUpdates()
                .getUpdateList()
                .stream()
                .collect(toHashTable(EntityStateUpdate::getId, UpdatePayload::messageOrNull));
        checkArgument(!messages.isEmpty(), "Empty subscription update: %s", update);
        return new UpdatePayload(messages);
    }

    private static @Nullable Message messageOrNull(EntityStateUpdate update) {
        if (update.hasState()) {
            System.out.println("Update state: " + unpack(update.getState()));
            return unpack(update.getState());
        } else {
            Preconditions.checkState(update.getNoLongerMatching());
            System.out.println("No longer matching");
            return null;
        }
    }

    private static UpdatePayload eventUpdates(SubscriptionUpdate update) {
        ImmutableMap<String, Message> messages = update
                .getEventUpdates()
                .getEventList()
                .stream()
                .collect(toHashTable(Event::id, Event::enclosedMessage));
        return new UpdatePayload(messages);
    }

    private static <T> Collector<T, ?, ImmutableMap<String, Message>>
    toHashTable(Function<T, Message> keyMapper, Function<T, Message> valueMapper) {
        return toImmutableMap(keyMapper.andThen(UpdatePayload::key), valueMapper);
    }

    /**
     * Converts the given ID message into a key for persistence.
     *
     * <p>The output key string consists of hexadecimal lower case characters (0-9 and a-f).
     *
     * @param id
     *         the ID to convert into a key
     * @return database key
     * @implSpec The returned value is a murmur3 hash code of the given key with respect to its
     *           Protobuf type.
     * @implNote It is important for the implementation that this function is deterministic, i.e.
     *           returns the same string (in terms of {@link String#equals(Object) equals} method)
     *           for the same input message.
     */
    private static String key(Message id) {
        Any packedId = AnyPacker.pack(id);
        HashCode code = keyHashFunction.newHasher()
                                       .putString(packedId.getTypeUrl(), UTF_8)
                                       .putBytes(packedId.getValue().toByteArray())
                                       .hash();
        return code.toString();
    }

    /**
     * Obtains this update as a database node value.
     */
    NodeValue asNodeValue() {
        NodeValue node = NodeValue.empty();
        messages.forEach((id, message) -> node.addChild(id, StoredJson.encodeOrNull(message)));
        return node;
    }
}
