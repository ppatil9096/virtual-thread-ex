package com.ppp.virtualthreadex;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;

/**
 * Minimal HTTP server using Virtual Threads (thread-per-request).
 * Endpoints:
 * GET /health
 * GET /work?userId=123
 * <p>
 * "work" simulates a DB call and an external service call in parallel using Structured Concurrency.
 * Use JFR to observe VirtualThread mount/unmount and blocking durations.
 */
public class VtHttpServer {
    static void main(String[] args) throws IOException, InterruptedException {
        JfrStarter.startRecording("vt-http-server");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executorService);
        server.createContext("/health", exchange -> respondJson(exchange, 200, "{\"status\":\"ok\"}"));
        server.createContext("/work", new WorkHandler());
        server.start();
        Thread.currentThread().join();
    }

    private static void respondJson(HttpExchange exchange, int statusCode, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final class WorkHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondJson(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String userId = query.getOrDefault("userId", "42");
            Instant start = Instant.now();
            try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {

                var userF = scope.fork(() -> SimulatedDb.fetchUser(userId));
                var extF = scope.fork(SimulatedExternal::call);

                scope.join();

                var user = userF.get();
                var ext = extF.get();


                Duration took = Duration.between(start, Instant.now());
                String json = """
                        {
                          "user": %s,
                          "external": %s,
                          "tookMs": %d
                        }
                        """.formatted(user, ext, took.toMillis());

                respondJson(exchange, 200, json);
                System.out.println( "user: "+ user +"& external: "+ ext);

            } catch (Exception e) {
                respondJson(exchange, 500, "{\"error\":\"internal server error\"}");
            }

        }

        private static Map<String, String> parseQuery(URI requestURI) {
            var q = new java.util.HashMap<String, String>();
            String raw = requestURI.getQuery();

            if (raw == null || raw.isBlank()) return q;
            for (String p : raw.split("&")) {
                int i = p.indexOf('=');
                if (i > 0) q.put(urlDecode(p.substring(0, i)), urlDecode(p.substring(i + 1)));
            }

            return q;
        }


        static String urlDecode(String s) {
            try {
                return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return s;
            }
        }

        static final class SimulatedDb {
            static String fetchUser(String id) {
                // Simulate JDBC latency; VT unmounts during sleep
                sleep(120);
                return "{\"id\":\"" + id + "\",\"name\":\"Ada\"}";
            }
        }

        static final class SimulatedExternal {
            static String call() {
                // Simulate remote API latency
                sleep(180);
                return "{\"feature\":\"ok\"}";
            }
        }

        static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
