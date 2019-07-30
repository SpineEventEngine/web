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
import com.google.protobuf.Message;
import io.spine.base.FieldPath;
import io.spine.base.FieldPaths;
import io.spine.client.Filter;
import io.spine.protobuf.TypeConverter;
import io.spine.server.storage.OperatorEvaluator;

import java.util.function.Predicate;

public final class FieldMatcher extends FilterMatcher<Filter> {

    private FieldMatcher(Filter filter) {
        super(filter);
    }

    static Predicate<Message> by(Filter filter) {
        return new FieldMatcher(filter);
    }

    @Override
    public boolean test(Message message) {
        Object value = extractValue(message);
        return match(value, message.getClass());
    }

    private boolean match(Object value, Class<? extends Message> holderClass) {
        Filter.Operator operator = filter().getOperator();
        Class<?> fieldClass = FieldPaths.typeOfFieldAt(holderClass, filter().getFieldPath());
        Any expectedAny = filter().getValue();
        Object expectedValue = TypeConverter.toObject(expectedAny, fieldClass);
        boolean result = OperatorEvaluator.eval(value, operator, expectedValue);
        return result;
    }

    private Object extractValue(Message input) {
        FieldPath path = filter().getFieldPath();
        Object value = FieldPaths.getValue(path, input);
        return value;
    }
}
