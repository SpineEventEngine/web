package io.spine.web.firebase;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.IOUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.core.Path;
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

    public static String getContent(DatabaseReference reference) {
        try {
            GenericUrl nodeUrl = nodeUrl(reference);
            HttpRequest getRequest = httpRequestFactory().buildGetRequest(nodeUrl);
            log().warn("Executing GET request for node " + nodeUrl.getRawPath());
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

    static void addOrUpdate(DatabaseReference reference, String item) throws IOException {
        if (exists(reference)) {
            update(reference, item);
        } else {
            add(reference, item);
        }
    }

    public static void add(DatabaseReference reference, String item) throws IOException {
        GenericUrl nodeUrl = nodeUrl(reference);
        ByteArrayContent content = createRequestContent(item);
        log().warn("Adding item " + item + " under url " + nodeUrl.getRawPath());
        HttpTransport httpTransport = UrlFetchTransport.getDefaultInstance();
        HttpRequestFactory factory = httpTransport.createRequestFactory();
        HttpRequest request = factory.buildPutRequest(nodeUrl, content);
        HttpResponse firebaseResponse = request.execute();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(firebaseResponse.getContent(), outputStream);
        String firebaseResponseStr = outputStream.toString();
        log().warn("Firebase response to add: status - " + firebaseResponse.getStatusCode() + ", text: "
                + firebaseResponseStr);
    }

    public static void update(DatabaseReference reference, String item) throws IOException {
        GenericUrl nodeUrl = nodeUrl(reference);
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

    private static boolean exists(DatabaseReference reference) {
        return !isNull(reference);
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

    private static boolean isNull(DatabaseReference reference) {
        String responseContent = getContent(reference);
        boolean result = NULL_NODE.equals(responseContent);
        return result;
    }

    static GenericUrl nodeUrl(DatabaseReference reference) {
        Path path = reference.getPath();
        String firebaseNodeUrl = String.format("%s/%s.json", "https://spine-dev.firebaseio.com", path.wireFormat());
        return new GenericUrl(firebaseNodeUrl);
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
