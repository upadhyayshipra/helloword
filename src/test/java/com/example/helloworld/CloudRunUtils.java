package com.example.helloworld;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CloudRunUtils {

  /**
   * Discovers the Cloud Run service URL.
   * Priority:
   * 1. SERVICE_URL environment variable.
   * 2. 'gcloud run services describe' command.
   */
  public static String getServiceUrl(String serviceName) throws Exception {
    String envUrl = System.getenv("SERVICE_URL");
    if (envUrl != null && !envUrl.isEmpty()) {
      return envUrl;
    }

    System.out.println("🔍 No SERVICE_URL provided. Attempting to discover Cloud Run service URL...");
    String region = System.getenv("GOOGLE_CLOUD_REGION");
    if (region == null) region = "us-central1";

    Process process = new ProcessBuilder(
        "gcloud", "run", "services", "describe", serviceName,
        "--platform", "managed",
        "--region", region,
        "--format", "value(status.url)"
    ).start();

    String url = readProcessOutput(process).trim();
    if (url.isEmpty()) {
      throw new RuntimeException("Could not find Cloud Run service URL for " + serviceName);
    }
    return url;
  }

  /**
   * Fetches the Google Cloud Identity Token using gcloud.
   * Returns null if the token cannot be fetched (e.g., unauthenticated local run).
   */
  public static String getIdentityToken() {
    try {
      Process process = new ProcessBuilder("gcloud", "auth", "print-identity-token").start();
      String token = readProcessOutput(process).trim();
      return token.isEmpty() ? null : token;
    } catch (Exception e) {
      return null;
    }
  }

  private static String readProcessOutput(Process process) throws Exception {
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