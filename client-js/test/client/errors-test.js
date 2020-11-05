/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import assert from 'assert';

import {ClientError, CommandHandlingError} from '@lib/client/errors';
import {Error as SpineBaseError} from '@proto/spine/base/error_pb';

describe('CommandHandlingError', () => {

    describe('assures a command is neglected with a', () => {
        it('`ClientError`', done => {
            const error = new CommandHandlingError(
                'Unable to process command', new ClientError('Test Client Error')
            );
            assert.ok(error.assuresCommandNeglected())
            done();
        });

        it('`SpineBaseError`', done => {
            const error = new CommandHandlingError(
                'Unable to process command', new SpineBaseError()
            );
            assert.ok(error.assuresCommandNeglected())
            done();
        });
    })

    describe('assures a command is not neglected when', () => {
        it('no cause set', done => {
            const error = new CommandHandlingError(
                'Unable to process command'
            );
            assert.ok(error.assuresCommandNeglected() === false);
            done();
        });

        it('a generic `Error` is the cause', done => {
            const error = new CommandHandlingError(
                'Unable to process command', new Error()
            );
            assert.ok(error.assuresCommandNeglected() === false);
            done();
        });
    })
});
