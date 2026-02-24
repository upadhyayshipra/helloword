package com.example.helloworld;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ServiceClient {

  private final String serviceUrl;
  private final String identityToken;
  private final HttpClient client;

  public ServiceClient(String serviceUrl, String identityToken) {
    this.serviceUrl = serviceUrl;
    this.identityToken = identityToken;
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  public HttpResponse<String> sendRequest() throws Exception {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(serviceUrl))
        .GET();

    if (identityToken != null) {
      requestBuilder.header("Authorization", "Bearer " + identityToken);
    }

    return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
  }

  public void waitForServiceReady(int maxRetries) {
    System.out.println("Waiting for service to become ready...");
    for (int i = 1; i <= maxRetries; i++) {
      try {
        HttpResponse<String> response = sendRequest();
        if (response.statusCode() == 200) {
          System.out.println("Service is ready!");
          return;
        }
        System.out.println("Attempt " + i + "/" + maxRetries + ": Status " + response.statusCode() + ". Waiting...");
      } catch (Exception e) {
        System.out.println("Attempt " + i + "/" + maxRetries + ": Failed to connect (" + e.getMessage() + "). Waiting...");
      }
      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException ignored) {}
    }
    throw new RuntimeException("❌ Service failed to become ready after " + maxRetries + " attempts.");
  }
}