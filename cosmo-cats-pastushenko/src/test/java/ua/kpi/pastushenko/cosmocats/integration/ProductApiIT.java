package ua.kpi.pastushenko.cosmocats.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.context-path=/cosmo")
@Import(PostgresTestConfiguration.class)
class ProductApiIT {
    private static final String API = "/cosmo/api/products";
    private static final String VALID = """
            {"name":"Stellar Treats","description":"Space food","price":12.500,"categoryId":1,"stockQuantity":10}
            """;
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void completeCrudLifecyclePreservesLocationAndPagination() throws Exception {
        long initialCount = body(send("GET", API, null)).get("totalElements").asLong();
        var created = send("POST", API, VALID);
        assertThat(created.statusCode()).isEqualTo(201);
        long id = body(created).get("id").asLong();
        String location = created.headers().firstValue("Location").orElseThrow();
        try {
            assertThat(location).isEqualTo(API + "/" + id);
            var fetched = send("GET", location, null);
            assertThat(fetched.statusCode()).isEqualTo(200);
            assertThat(body(fetched).get("name").asText()).isEqualTo("Stellar Treats");
            assertThat(new BigDecimal(body(fetched).get("price").asText())).isEqualByComparingTo("12.50");
            assertThat(body(send("GET", API + "?size=1", null)).get("totalElements").asLong()).isEqualTo(initialCount + 1);

            String replacement = """
                    {"name":"New Collar","description":null,"price":19.99,"categoryId":3,"stockQuantity":0}
                    """;
            var updated = send("PUT", location, replacement);
            assertThat(updated.statusCode()).isEqualTo(200);
            assertThat(body(updated).get("id").asLong()).isEqualTo(id);
            var stored = body(send("GET", location, null));
            assertThat(stored.get("name").asText()).isEqualTo("New Collar");
            assertThat(stored.get("description").isNull()).isTrue();
            assertThat(stored.get("categoryId").asLong()).isEqualTo(3);
            assertThat(stored.get("stockQuantity").asInt()).isZero();
            assertThat(new BigDecimal(stored.get("price").asText())).isEqualByComparingTo("19.99");

            var deleted = send("DELETE", location, null);
            assertThat(deleted.statusCode()).isEqualTo(204);
            assertThat(deleted.body()).isEmpty();
            assertProblem(send("GET", location, null), 404, location);
            assertProblem(send("DELETE", location, null), 404, location);
            assertThat(body(send("GET", API, null)).get("totalElements").asLong()).isEqualTo(initialCount);
        } finally {
            send("DELETE", API + "/" + id, null);
        }
    }

    @Test
    void rejectedWritesLeaveStoredProductAndCollectionUnchanged() throws Exception {
        var created = send("POST", API, VALID);
        assertThat(created.statusCode()).isEqualTo(201);
        String location = API + "/" + body(created).get("id").asLong();
        try {
            var original = body(send("GET", location, null));
            long count = body(send("GET", API, null)).get("totalElements").asLong();
            String missingCategory = VALID.replace("\"categoryId\":1", "\"categoryId\":99");
            assertProblem(send("PUT", location, missingCategory), 404, location);
            assertThat(body(send("GET", location, null))).isEqualTo(original);
            assertProblem(send("POST", API, missingCategory), 404, API);
            assertProblem(send("POST", API, VALID.replace("12.500", "12.345")), 400, API);
            assertProblem(send("PUT", API + "/999999", VALID), 404, API + "/999999");
            assertThat(body(send("GET", API, null)).get("totalElements").asLong()).isEqualTo(count);
        } finally {
            send("DELETE", location, null);
        }
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response) throws Exception {
        return json.readTree(response.body());
    }

    private void assertProblem(HttpResponse<String> response, int status, String instance) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
        assertThat(body(response).get("status").asInt()).isEqualTo(status);
        assertThat(body(response).get("instance").asText()).isEqualTo(instance);
    }
}
