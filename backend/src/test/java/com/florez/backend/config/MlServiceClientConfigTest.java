package com.florez.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Prueba el cliente real contra un servidor HTTP real: MockRestServiceServer no abre conexiones y
 * no habría detectado que el cliente negociaba HTTP/2 (h2c), que uvicorn no soporta.
 */
class MlServiceClientConfigTest {

    private HttpServer server;
    private final AtomicReference<String> upgradeHeader = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/score", exchange -> {
            upgradeHeader.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void elClienteDelServicioDeMlUsaHttp11YEntregaElCuerpoDelPost() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        RestClient client = new MlServiceClientConfig()
                .mlServiceRestClient(new MlServiceProperties(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(5)));

        client.post().uri("/score").contentType(MediaType.APPLICATION_JSON).body(Map.of("age", 36)).retrieve().toBodilessEntity();

        assertThat(upgradeHeader.get()).as("no debe intentar negociar HTTP/2 (h2c)").isNull();
        assertThat(receivedBody.get()).isEqualTo("{\"age\":36}");
    }
}
