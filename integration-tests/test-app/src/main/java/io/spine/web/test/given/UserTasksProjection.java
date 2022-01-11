/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.web.test.given;

import io.spine.base.Time;
import io.spine.core.Subscribe;
import io.spine.core.UserId;
import io.spine.server.projection.Projection;

import static io.spine.web.test.given.UserTasks.Load.EXTREME;
import static io.spine.web.test.given.UserTasks.Load.HIGH;
import static io.spine.web.test.given.UserTasks.Load.LOW;
import static io.spine.web.test.given.UserTasks.Load.VERY_HIGH;

/**
 * A projection representing a user and a list of {@link TaskId tasks} assigned to him.
 *
 * <p>Assigned tasks count and indication of several tasks assigned are exposed as columns
 * allowing ordering and filtering when user tasks are queried.
 */
final class UserTasksProjection
        extends Projection<UserId, UserTasks, UserTasks.Builder> {

    @Subscribe
    void on(TaskCreated event) {
        builder().setId(event.getAssignee())
                 .addTasks(event.getId())
                 .setLastUpdated(event.getWhen());
    }

    @SuppressWarnings("CheckReturnValue")
    @Subscribe
    void on(TaskReassigned event) {
        if (reassignedFromThisUser(event)) {
            var tasks = state().getTasksList();
            var reassigned = tasks.indexOf(event.getId());
            builder().removeTasks(reassigned);
        } else if (reassignedToThisUser(event)) {
            builder().setId(event.getTo())
                     .addTasks(event.getId());
        }
        builder().setLastUpdated(event.getWhen());
    }

    @Override
    protected void onBeforeCommit() {
        var taskTotal = countTasks();
        builder().setTaskCount(taskTotal)
                 .setLastUpdated(Time.currentTime())
                 .setIsOverloaded(taskTotal > 1)
                 .setLoad(loadWith(taskTotal));
    }

    private int countTasks() {
        var tasks = builder().getTasksList();
        return tasks.size();
    }

    private static UserTasks.Load loadWith(int taskTotal) {
        if (taskTotal == 0) {
            return LOW;
        } else if (taskTotal == 1) {
            return HIGH;
        } else if (taskTotal == 2) {
            return VERY_HIGH;
        } else {
            return EXTREME;
        }
    }

    private boolean reassignedFromThisUser(TaskReassigned event) {
        return event.hasFrom() && event.getFrom()
                                       .equals(id());
    }

    private boolean reassignedToThisUser(TaskReassigned event) {
        return event.hasTo() && event.getTo()
                                     .equals(id());
    }
}
