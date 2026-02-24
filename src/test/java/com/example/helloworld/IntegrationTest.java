package com.example.helloworld;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

  private static final String SERVICE_NAME = "helloworld";
  private ServiceClient serviceClient;

  @BeforeAll
  void setup() throws Exception {
    // 1. Configuration
    String serviceUrl = CloudRunUtils.getServiceUrl(SERVICE_NAME);
    String identityToken = CloudRunUtils.getIdentityToken();

    System.out.println("Targeting Service: " + serviceUrl);
    if (identityToken != null) {
      System.out.println("✅ Identity token acquired.");
    } else {
      System.out.println("⚠️ No identity token found. Attempting unauthenticated request.");
    }

    // 2. Initialize Client
    serviceClient = new ServiceClient(serviceUrl, identityToken);

    // 3. Warm up
    serviceClient.waitForServiceReady(10);
  }

  @Test
  @Tag("smoke")
  void smokeTest() throws Exception {
    HttpResponse<String> response = serviceClient.sendRequest();
    assertEquals(200, response.statusCode(), "Smoke Test Failed: Service did not return 200 OK");
    System.out.println("✅ Smoke Test Passed: Service is reachable (200 OK).");
  }

  @Test
  @Tag("regression")
  void contentValidationTest() throws Exception {
    HttpResponse<String> response = serviceClient.sendRequest();
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Hello"), "Regression Test Failed: Response body did not contain 'Hello'");
    System.out.println("✅ Regression Test Passed: Response contains expected content.");
  }
}