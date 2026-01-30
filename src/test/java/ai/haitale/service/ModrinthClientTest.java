package ai.haitale.service;

import ai.haitale.model.Mod;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@MicronautTest
public class ModrinthClientTest {
    @Inject
    ObjectMapper objectMapper;

    static MockWebServer server;

    @BeforeAll
    public static void start() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    public static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    public void testProjectToModExtractsChecksumAndUrl() throws Exception {
        // Mock /search to return one project hit
        String searchResponse = "{\"hits\":[{" +
            "\"id\":\"proj-1\",\"slug\":\"proj-slug\",\"title\":\"Test Mod\",\"description\":\"A test mod\",\"versions\":[\"v1\"],\"latest_version\":\"v1\",\"authors\":[{\"username\":\"dev\"}]" +
            "}]}";

        server.enqueue(new MockResponse().setResponseCode(200).setBody(searchResponse));

        // Mock /version/v1 to return two files (one with sha256)
        String versionResponse = "{" +
            "\"id\":\"v1\"," +
            "\"version_number\":\"1.2.3\"," +
            "\"files\":[{" +
            "\"url\":\"https://cdn.example.com/file1.jar\",\"size\":12345,\"hashes\":{} ,\"filename\":\"file1.jar\"},{" +
            "\"url\":\"https://cdn.example.com/file2.jar\",\"size\":54321,\"hashes\":{\"sha256\":\"abcd1234\"},\"filename\":\"file2.jar\"}]" +
            "}";

        server.enqueue(new MockResponse().setResponseCode(200).setBody(versionResponse));

        ModrinthClient client = new ModrinthClient(objectMapper, server.url("/").toString());

        List<Mod> mods = client.search("Test Mod", 10);

        // Take recorded requests with timeout to avoid blocking forever
        RecordedRequest r1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest r2 = server.takeRequest(1, TimeUnit.SECONDS);

        if (r1 == null) {
            Assertions.fail("No search request recorded; requestCount=" + server.getRequestCount());
            return;
        }
        if (r2 == null) {
            Assertions.fail("No version request recorded; requestCount=" + server.getRequestCount() + ", path1=" + r1.getPath());
            return;
        }

        System.out.println("Recorded request 1: " + r1.getMethod() + " " + r1.getPath());
        System.out.println("Recorded request 2: " + r2.getMethod() + " " + r2.getPath());

        Assertions.assertEquals(1, mods.size(), "Expected one mod returned by search");
        Mod mod = mods.get(0);
        Assertions.assertEquals("proj-1", mod.getId());
        Assertions.assertEquals("Test Mod", mod.getName());
        Assertions.assertEquals("A test mod", mod.getDescription());
        Assertions.assertEquals("dev", mod.getAuthor());
        Assertions.assertEquals("https://cdn.example.com/file2.jar", mod.getDownloadUrl());
        Assertions.assertEquals("abcd1234", mod.getChecksum());
    }
}
