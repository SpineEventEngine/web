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

package io.spine.web.firebase.given;

import io.spine.client.EntityStateWithVersion;
import io.spine.client.QueryResponse;
import io.spine.core.Response;
import io.spine.core.Version;
import io.spine.people.PersonName;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Responses.statusOk;
import static io.spine.protobuf.AnyPacker.pack;
import static java.util.Arrays.stream;

public final class FirebaseSubscriptionRecordTestEnv {

    /**
     * Prevents instantiation of this test environment.
     */
    private FirebaseSubscriptionRecordTestEnv() {
    }

    public static Book updateAuthors(Book designPatterns, List<Author> gangOfFour) {
        Book.Builder builder = Book.newBuilder();
        builder.mergeFrom(designPatterns);
        return builder.clearAuthors()
                      .addAllAuthors(gangOfFour)
                      .build();
    }

    public static QueryResponse mockQueryResponse(Book... books) {
        QueryResponse.Builder responseBuilder = QueryResponse
                .newBuilder()
                .setResponse(okResponse());
        stream(books)
                .map(FirebaseSubscriptionRecordTestEnv::toEntityState)
                .forEach(responseBuilder::addMessage);
        return responseBuilder.build();
    }

    private static EntityStateWithVersion toEntityState(Book book) {
        EntityStateWithVersion result = EntityStateWithVersion
                .newBuilder()
                .setState(pack(book))
                .setVersion(Version.getDefaultInstance())
                .build();
        return result;
    }

    public static final class Books {

        private Books() {
        }

        public static Book designPatterns() {
            return book(bookId(),
                        "Design Patterns: Elements of Reusable Object-Oriented Software",
                        Authors.literalGangOfFour());
        }

        public static Book donQuixote() {
            BookId bookId = bookId();
            return book(bookId, "Don Quixote", Authors.miguelDeCervantes());
        }

        public static Book aliceInWonderland() {
            BookId bookId = bookId();
            return book(bookId, "Alice’s Adventures in Wonderland", Authors.lewisCarrol());
        }

        public static Book guideToTheGalaxy() {
            return book(bookId(), "The Hitchhiker's Guide to the Galaxy", Authors.douglasAdams());
        }

        private static Book book(BookId id, String name, Author author) {
            return Book.newBuilder()
                       .setId(id)
                       .setName(name)
                       .addAuthors(author)
                       .build();
        }

        private static BookId bookId() {
            return BookId.newBuilder()
                         .setValue(newUuid())
                         .build();
        }
    }

    public static final class Authors {

        private Authors() {
        }

        private static Author erichGamma() {
            return author(name("Erich", "Gamma"));
        }

        private static Author richardHelm() {
            return author(name("Richard", "Helm"));
        }

        private static Author ralphJohnson() {
            return author(name("Ralph", "Johnson"));
        }

        private static Author johnVlissides() {
            return author(name("John", "Vlissides"));
        }

        private static Author literalGangOfFour() {
            PersonName name = PersonName
                    .newBuilder()
                    .setGivenName("Gang of Four")
                    .vBuild();
            return author(name);
        }

        public static List<Author> gangOfFour() {
            return newArrayList(erichGamma(), richardHelm(), ralphJohnson(),
                                johnVlissides());
        }

        private static Author lewisCarrol() {
            return author(name("Lewis", "Carrol"));
        }

        private static Author douglasAdams() {
            return author(name("Douglas", "Adams"));
        }

        private static Author miguelDeCervantes() {
            return author(name("Miguel", "de Cervantes"));
        }

        private static Author author(PersonName name) {
            return Author
                    .newBuilder()
                    .setName(name)
                    .vBuild();
        }

        private static PersonName name(String firstName, String lastName) {
            return PersonName
                    .newBuilder()
                    .setGivenName(firstName)
                    .setFamilyName(lastName)
                    .vBuild();
        }
    }

    private static Response okResponse() {
        return Response
                .newBuilder()
                .setStatus(statusOk())
                .vBuild();
    }
}
