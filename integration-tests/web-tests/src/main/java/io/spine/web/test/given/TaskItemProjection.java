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

import io.spine.core.Subscribe;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.projection.Projection;

/**
 * An projection with the state of type {@code spine.web.test.given.TaskItem}.
 *
 * <p>Exposes task name, description and description length as {@link EntityColumn} allowing
 * ordering and filtering when tasks are queried.
 */
public class TaskItemProjection extends Projection<TaskId, TaskItem, TaskItemVBuilder> {

    protected TaskItemProjection(TaskId id) {
        super(id);
    }

    @Subscribe
    void on(TaskCreated e) {
        builder().setId(e.getId())
                 .setName(e.getName())
                 .setDescription(e.getDescription());
    }

    @Subscribe
    void on(TaskRenamed e) {
        builder().setName(e.getName());
    }

    @Column
    public String getName() {
        return state().getName();
    }

    @Column
    public String getDescription() {
        return state().getDescription();
    }

    @Column
    public int getDescriptionLength() {
        return state().getDescription().length();
    }
}
