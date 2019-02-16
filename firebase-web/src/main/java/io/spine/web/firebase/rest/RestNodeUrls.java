package io.spine.web.firebase.rest;

import com.google.api.client.http.GenericUrl;
import io.spine.net.Url;
import io.spine.net.Urls;
import io.spine.web.firebase.DatabaseUrl;
import io.spine.web.firebase.DatabaseUrls;
import io.spine.web.firebase.NodePath;

import static java.lang.String.format;

/**
 * A factory creating {@linkplain RestNodeUrl REST Node URLs} of the Firebase database at
 * specified {@linkplain DatabaseUrl url}.
 */
final class RestNodeUrls {

    private final String template;

    RestNodeUrls(DatabaseUrl url) {
        DatabaseUrls.checkSpec(url);
        this.template = Urls.toString(url.getUrl()) + "/%s.json";
    }

    /**
     * Creates a new {@link RestNodeUrl} for a node at the specified {@link NodePath path}.
     */
    RestNodeUrl with(NodePath path) {
        Url url = Urls.create(format(template, path.getValue()));
        RestNodeUrl node = RestNodeUrlVBuilder
                .newBuilder()
                .setUrl(url)
                .build();
        return node;
    }

    static GenericUrl asGenericUrl(RestNodeUrl node) {
        GenericUrl url = new GenericUrl(Urls.toString(node.getUrl()));
        return url;
    }
}
