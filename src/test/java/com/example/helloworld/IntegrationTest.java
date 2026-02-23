package com.example.helloworld;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

  private static final String SERVICE_NAME = "helloworld";
  private static final int MAX_RETRIES = 10;

  private String serviceUrl;
  private String identityToken;
  private HttpClient client;

  @BeforeAll
  void setup() throws Exception {
    // 1. Discover URL
    serviceUrl = getServiceUrl();
    System.out.println("Targeting Service: " + serviceUrl);

    // 2. Get Token
    identityToken = getIdentityToken();
    if (identityToken != null) {
      System.out.println("✅ Identity token acquired.");
    } else {
      System.out.println("⚠️ No identity token found. Attempting unauthenticated request.");
    }

    // 3. Initialize Client
    client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // 4. Warm up: Wait for the service to be ready before running any tests
    waitForServiceReady();
  }

  @Test
  @Tag("smoke")
  void smokeTest() throws Exception {
    HttpResponse<String> response = sendRequest();
    assertEquals(200, response.statusCode(), "Smoke Test Failed: Service did not return 200 OK");
    System.out.println("✅ Smoke Test Passed: Service is reachable (200 OK).");
  }

  @Test
  @Tag("regression")
  void contentValidationTest() throws Exception {
    HttpResponse<String> response = sendRequest();
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Hello"), "Regression Test Failed: Response body did not contain 'Hello'");
    System.out.println("✅ Regression Test Passed: Response contains expected content.");
  }

  // --- Helper Methods ---

  private void waitForServiceReady() throws Exception {
    System.out.println("Waiting for service to become ready...");
    for (int i = 1; i <= MAX_RETRIES; i++) {
      try {
        HttpResponse<String> response = sendRequest();
        if (response.statusCode() == 200) {
          System.out.println("Service is ready!");
          return;
        }
        System.out.println("Attempt " + i + "/" + MAX_RETRIES + ": Status " + response.statusCode() + ". Waiting...");
      } catch (Exception e) {
        System.out.println("Attempt " + i + "/" + MAX_RETRIES + ": Failed to connect (" + e.getMessage() + "). Waiting...");
      }
      TimeUnit.SECONDS.sleep(3);
    }
    fail("❌ Service failed to become ready after " + MAX_RETRIES + " attempts.");
  }

  private HttpResponse<String> sendRequest() throws Exception {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(serviceUrl))
        .GET();

    if (identityToken != null) {
      requestBuilder.header("Authorization", "Bearer " + identityToken);
    }

    return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
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