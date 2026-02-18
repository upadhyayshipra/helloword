#!/bin/bash
# Simple script to verify service is up and running
# Usage: ./verify-service.sh [SERVICE_URL]

URL="${1:-${SERVICE_URL:-http://localhost:8080}}"

echo "Verifying service at $URL..."
RESPONSE=$(curl -s "$URL")

if [[ "$RESPONSE" == *"Hello"* ]]; then
  echo "✅ Service is UP! Response: $RESPONSE"
  exit 0
else
  echo "❌ Service check FAILED. Response: $RESPONSE"
  exit 1
fi
