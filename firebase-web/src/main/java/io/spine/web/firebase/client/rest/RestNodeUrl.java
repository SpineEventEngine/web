package io.spine.web.firebase.client.rest;

import com.google.api.client.http.GenericUrl;
import io.spine.web.firebase.client.DatabaseUrl;
import io.spine.web.firebase.client.DatabaseUrls;
import io.spine.web.firebase.client.NodePath;

import static java.lang.String.format;

/**
 * A URL of a single Firebase node accessed via REST.
 * 
 * <p>Instances of REST node URL are created from it’s {@linkplain Template template}.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // instantiated via Template 
class RestNodeUrl {

    private final String url;

    private RestNodeUrl(Template template, NodePath path) {
        this.url = format(template.template, path.getValue());
    }

    GenericUrl asGenericUrl() {
        return new GenericUrl(url);
    }

    /**
     * A template to create {@linkplain RestNodeUrl REST Node URLs} in the Firebase database at
     * specified {@linkplain DatabaseUrls url}.
     */
    static class Template {

        private final String template;

        Template(DatabaseUrl url) {
            DatabaseUrls.checkSpec(url);
            this.template = url.getUrl()
                               .getSpec() + "/%s.json";
        }

        /**
         * Creates a new {@link RestNodeUrl} for a node at the specified {@link NodePath path}.
         */
        RestNodeUrl with(NodePath path) {
            return new RestNodeUrl(this, path);
        }
    }
}
