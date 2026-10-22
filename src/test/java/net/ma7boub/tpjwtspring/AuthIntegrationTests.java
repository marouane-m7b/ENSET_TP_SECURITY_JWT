package net.ma7boub.tpjwtspring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTests {
    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String credentials(String email, String field, String password) {
        return "{\"email\":\"" + email + "\",\"" + field + "\":\"" + password + "\"}";
    }

    @Test
    void registeredUsersCanLoginWithEitherPasswordFieldAndUseTheirToken() throws Exception {
        for (String field : new String[]{"password", "pass"}) {
            String email = UUID.randomUUID() + "@example.com";
            String registration = credentials(email, field, "password");
            assertEquals(201, post("/auth/register", registration).statusCode());
            assertEquals(409, post("/auth/register", registration).statusCode());
            for (String loginField : new String[]{"password", "pass"}) {
                var login = post("/auth/login", credentials(email, loginField, "password"));
                assertEquals(200, login.statusCode());
                assertTrue(login.body().matches("\\{\"token\":\"[^\"]+\"}"));
                String token = login.body().split("\"")[3];
                var profile = client.send(HttpRequest.newBuilder(
                                URI.create("http://localhost:" + port + "/api/profile"))
                        .header("Authorization", "Bearer " + token).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, profile.statusCode());
                assertTrue(profile.body().contains(email));
            }
            assertEquals(401, post("/auth/login", credentials(email, "pass", "wrong")).statusCode());
        }
    }

    @Test
    void missingCredentialsAreRejectedAndUnknownUsersAreUnauthorized() throws Exception {
        assertEquals(400, post("/auth/register", "{\"email\":\"missing@example.com\"}").statusCode());
        assertEquals(400, post("/auth/login", "{\"email\":\"missing@example.com\"}").statusCode());
        assertEquals(400, post("/auth/login", credentials("a@example.com", "pass", " ")).statusCode());
        assertEquals(401, post("/auth/login", credentials(UUID.randomUUID() + "@example.com", "pass", "wrong")).statusCode());
    }
    @Test
    void corsPreflightAllowsFrontendAndRejectsOtherOrigins() throws Exception {
        for (String path : new String[]{"/auth/login", "/api/profile"}) {
            for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173"}) {
                var response = preflight(path, origin);
                assertEquals(200, response.statusCode());
                assertEquals(origin, response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
                String headers = response.headers().firstValue("Access-Control-Allow-Headers").orElseThrow().toLowerCase();
                assertTrue(headers.contains("authorization"));
                assertTrue(headers.contains("content-type"));
            }
        }
        var rejected = preflight("/auth/login", "https://untrusted.example");
        assertEquals(403, rejected.statusCode());
        assertTrue(rejected.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    @Test
    void loginErrorsIncludeCorsHeadersForFrontend() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/login"))
                .header("Origin", "http://localhost:5173")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(credentials(UUID.randomUUID() + "@example.com", "password", "wrong")))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertEquals("http://localhost:5173", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    private HttpResponse<String> preflight(String path, String origin) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", path.startsWith("/auth/") ? "POST" : "GET")
                .header("Access-Control-Request-Headers", "content-type,authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
}
