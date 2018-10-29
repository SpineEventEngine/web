package io.spine.web.firebase;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.IOUtils;
import com.google.firebase.database.utilities.Clock;
import com.google.firebase.database.utilities.DefaultClock;
import com.google.firebase.database.utilities.OffsetClock;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.firebase.database.utilities.PushIdGenerator.generatePushChildName;

public class FirebaseRest {

    private static final Clock CLOCK = new OffsetClock(new DefaultClock(), 0);
    public static final String NULL_NODE = "null";

    private static final HttpRequestFactory requestFactory = createRequestFactory();

    private FirebaseRest() {
    }

    static String getContent(String nodeUrl) {
        try {
            GenericUrl genericUrl = genericUrl(nodeUrl);
            HttpRequest getRequest = httpRequestFactory().buildGetRequest(genericUrl);
            log().warn("Executing GET request for node " + genericUrl.getRawPath());
            HttpResponse getResponse = getRequest.execute();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(getResponse.getContent(), outputStream);
            String result = outputStream.toString();
            log().warn("Result of GET request for node: status - " + getResponse.getStatusCode() + ", message: " +
                    result);
            return result;
        } catch (Throwable e) {
            log().error("I/O error when working with Firebase: " + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Adds the value to the referenced Firebase array path.
     *
     * @param nodeUrl a Firebase array reference which can be appended an object.
     * @param item      a String value to add to an Array inside of Firebase
     * @return a {@code Future} of an item being added
     */
    static void addOrUpdate(String nodeUrl, String item) throws IOException {
        if (exists(nodeUrl)) {
            update(nodeUrl, item);
        } else {
            add(nodeUrl, item);
        }
    }

    public static void add(String nodeUrl, String item) throws IOException {
        GenericUrl genericUrl = genericUrl(nodeUrl);
        ByteArrayContent content = createRequestContent(item);
        log().warn("Adding item " + item + " under url " + genericUrl.getRawPath());
        HttpTransport httpTransport = UrlFetchTransport.getDefaultInstance();
        HttpRequestFactory factory = httpTransport.createRequestFactory();
        HttpRequest request = factory.buildPutRequest(genericUrl, content);
        HttpResponse firebaseResponse = request.execute();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(firebaseResponse.getContent(), outputStream);
        String firebaseResponseStr = outputStream.toString();
        log().warn("Firebase response to add: status - " + firebaseResponse.getStatusCode() + ", text: "
                + firebaseResponseStr);
    }

    public static void update(String reference, String item) throws IOException {
        GenericUrl nodeUrl = genericUrl(reference);
        ByteArrayContent content = createRequestContent(item);
        log().warn("Updating item " + item + " under url " + nodeUrl.getRawPath());
        HttpRequest request = httpRequestFactory().buildPatchRequest(nodeUrl, content);
        HttpResponse firebaseResponse = request.execute();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(firebaseResponse.getContent(), outputStream);
        String firebaseResponseStr = outputStream.toString();
        log().warn("Firebase response to update: status - " + firebaseResponse.getStatusCode() + ", text: "
                + firebaseResponseStr);
    }

    private static ByteArrayContent createRequestContent(String item) {
        String generatedKey = newChildKey();
        ByteArrayContent result = requestContent(generatedKey, item);
        return result;
    }

    private static boolean exists(String nodeUrl) {
        return !isNull(nodeUrl);
    }

    private static String newChildKey() {
        return generatePushChildName(CLOCK.millis());
    }

    static ByteArrayContent byteArrayContent(JsonObject jsonObject) {
        String jsonString = jsonObject.toString();
        return ByteArrayContent.fromString(JSON_UTF_8.toString(), jsonString);
    }

    private static ByteArrayContent requestContent(String generatedKey, String item) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(generatedKey, item);
        ByteArrayContent result = byteArrayContent(jsonObject);
        return result;
    }

    private static boolean isNull(String nodeUrl) {
        String responseContent = getContent(nodeUrl);
        boolean result = NULL_NODE.equals(responseContent);
        return result;
    }

    static GenericUrl genericUrl(String nodeUrl) {
        return new GenericUrl(nodeUrl);
    }

    static HttpRequestFactory httpRequestFactory() {
        return requestFactory;
    }

    private static HttpRequestFactory createRequestFactory() {
        HttpTransport httpTransport = new UrlFetchTransport();
        HttpRequestFactory result = httpTransport.createRequestFactory();
        return result;
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FirebaseRest.class);
    }
}
