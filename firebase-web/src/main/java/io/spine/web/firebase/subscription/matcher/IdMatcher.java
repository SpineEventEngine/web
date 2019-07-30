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

package io.spine.web.firebase.subscription.matcher;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.IdFilter;
import io.spine.protobuf.TypeConverter;

import java.util.List;

@Internal
public final class IdMatcher extends FilterMatcher<IdFilter> {

    private final boolean empty;

    public IdMatcher(IdFilter filter) {
        super(filter);
        this.empty = filter.getIdList().isEmpty();
    }

    @Override
    public boolean test(Message message) {
        if (empty) {
            return true;
        } else {
            Object value = extractValue(message);
            return match(value);
        }
    }

    private boolean match(Object value) {
        Any idAny = TypeConverter.toAny(value);
        List<Any> expectedIds = filter().getIdList();
        return expectedIds.contains(idAny);
    }

    private static Object extractValue(Message input) {
        List<Descriptors.FieldDescriptor> fields = input.getDescriptorForType()
                                                        .getFields();
        Descriptors.FieldDescriptor idField = fields.get(0);
        Object fieldValue = input.getField(idField);
        return fieldValue;
    }
}
