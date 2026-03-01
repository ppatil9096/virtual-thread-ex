package com.ppp.virtualthreadex;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

/**
 * Simple load generator using Virtual Threads.
 * Run AFTER starting VtHttpServer. Adjust REQUESTS as needed.
 */
public class LoadClient {
    public static void main(String[] args) throws Exception {
        final int REQUESTS = 2000;
        var client = HttpClient.newHttpClient();

        var exec = Executors.newVirtualThreadPerTaskExecutor();
        Instant start = Instant.now();

        for (int i = 0; i < REQUESTS; i++) {
            final int id = i;
            exec.submit(() -> {
                var req = HttpRequest.newBuilder(URI.create("http://localhost:8080/work?userId=" + id))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                try {
                    var res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println("res: " + res);
                    if (res.statusCode() != 200) {
                        System.err.println("Non-200: " + res.statusCode());
                    }
                } catch (Exception e) {
                    System.err.println("Err: " + e.getMessage());
                }
            });
        }

        exec.shutdown();
        while (!exec.isTerminated()) Thread.sleep(50);

        long tookMs = Duration.between(start, Instant.now()).toMillis();
        System.out.println("Sent " + REQUESTS + " requests in ~" + tookMs + " ms");
    }
}