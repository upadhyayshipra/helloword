package com.example.helloworld;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class IntegrationTest {

  private static final String SERVICE_NAME = "helloworld";
  private static final int MAX_RETRIES = 10;

  @Test
  void testServiceIsUp() throws Exception {
    String serviceUrl = getServiceUrl();
    System.out.println("Targeting Service: " + serviceUrl);

    String token = getIdentityToken();
    if (token != null) {
      System.out.println("✅ Identity token acquired.");
    } else {
      System.out.println("⚠️ No identity token found. Attempting unauthenticated request.");
    }

    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    for (int i = 1; i <= MAX_RETRIES; i++) {
      try {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(serviceUrl))
            .GET();

        if (token != null) {
          requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && response.body().contains("Hello")) {
          System.out.println("✅ Service is UP! Response: " + response.body());
          return; // Test Passed
        }

        System.out.println("Attempt " + i + "/" + MAX_RETRIES + ": Status " + response.statusCode() + ". Waiting...");
      } catch (Exception e) {
        System.out.println("Attempt " + i + "/" + MAX_RETRIES + ": Failed to connect (" + e.getMessage() + "). Waiting...");
      }

      TimeUnit.SECONDS.sleep(3);
    }

    fail("❌ Service check FAILED after " + MAX_RETRIES + " attempts.");
  }

  private String getServiceUrl() throws Exception {
    String envUrl = System.getenv("SERVICE_URL");
    if (envUrl != null && !envUrl.isEmpty()) {
      return envUrl;
    }

    System.out.println("🔍 No SERVICE_URL provided. Attempting to discover Cloud Run service URL...");
    String region = System.getenv("GOOGLE_CLOUD_REGION");
    if (region == null) region = "us-central1";

    Process process = new ProcessBuilder(
        "gcloud", "run", "services", "describe", SERVICE_NAME,
        "--platform", "managed",
        "--region", region,
        "--format", "value(status.url)"
    ).start();

    String url = readProcessOutput(process).trim();
    if (url.isEmpty()) {
      throw new RuntimeException("Could not find Cloud Run service URL for " + SERVICE_NAME);
    }
    return url;
  }

  private String getIdentityToken() {
    try {
      Process process = new ProcessBuilder("gcloud", "auth", "print-identity-token").start();
      String token = readProcessOutput(process).trim();
      return token.isEmpty() ? null : token;
    } catch (Exception e) {
      return null;
    }
  }

  private String readProcessOutput(Process process) throws Exception {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    }
  }
}