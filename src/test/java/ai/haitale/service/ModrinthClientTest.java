package ai.haitale.service;

import ai.haitale.model.Mod;
import ai.haitale.service.modrinth.dto.ModrinthProject;
import ai.haitale.service.modrinth.dto.ModrinthProject.ModrinthAuthor;
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

        // Construct a project that refers to version v1
        ModrinthProject proj = new ModrinthProject();
        proj.id = "proj-1";
        proj.slug = "proj-slug";
        proj.title = "Test Mod";
        proj.description = "A test mod";
        proj.latest_version = "v1";
        proj.versions = List.of("v1");
        ModrinthAuthor a = new ModrinthAuthor();
        a.username = "dev";
        proj.authors = List.of(a);

        Mod mod = client.projectToMod(proj, null);

        // Take the request to ensure version endpoint was called
        RecordedRequest r = server.takeRequest(1, TimeUnit.SECONDS);
        if (r == null) Assertions.fail("Expected a request to version endpoint");

        Assertions.assertNotNull(mod);
        Assertions.assertEquals("proj-1", mod.getId());
        Assertions.assertEquals("Test Mod", mod.getName());
        Assertions.assertEquals("A test mod", mod.getDescription());
        Assertions.assertEquals("dev", mod.getAuthor());
        Assertions.assertEquals("https://cdn.example.com/file2.jar", mod.getDownloadUrl());
        Assertions.assertEquals("abcd1234", mod.getChecksum());
    }
}
