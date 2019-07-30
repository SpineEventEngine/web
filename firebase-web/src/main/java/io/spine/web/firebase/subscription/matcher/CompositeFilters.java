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

import com.google.protobuf.Message;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;

import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CompositeFilters {

    /**
     * Prevents the utility class instantiation.
     */
    private CompositeFilters() {
    }

    public static Predicate<Message> toPredicate(Collection<CompositeFilter> filters) {
        checkNotNull(filters);
        if (filters.isEmpty()) {
            return m -> true;
        }
        return filters.stream()
                      .map(CompositeFilters::forFilter)
                      .reduce(Predicate::and)
                      .orElseThrow(() -> new IllegalArgumentException("No filters provided."));
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases")
        // Default and fallback values are handled via the `default` clause.
    private static Predicate<Message> forFilter(CompositeFilter filter) {
        CompositeFilter.CompositeOperator operator = filter.getOperator();
        List<Filter> filters = filter.getFilterList();
        switch (operator) {
            case ALL:
                return compose(filters, Predicate::and);
            case EITHER:
                return compose(filters, Predicate::or);
            default:
                throw new IllegalArgumentException(operator.name());
        }
    }

    private static Predicate<Message>
    compose(List<Filter> matchers, BinaryOperator<Predicate<Message>> operator) {
        Predicate<Message> predicate = matchers
                .stream()
                .map(FieldMatcher::by)
                .reduce(operator)
                .orElse(m -> true);
        return predicate;
    }
}
