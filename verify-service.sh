#!/bin/bash
# Usage: ./verify-service.sh [SERVICE_URL]

URL="${1:-${SERVICE_URL}}"

# --- NEW: Dynamic URL Discovery ---
if [ -z "$URL" ]; then
  echo "🔍 No SERVICE_URL provided. Attempting to discover Cloud Run service URL..."

  # Default to us-central1 if not set
  REGION="${GOOGLE_CLOUD_REGION:-us-central1}"
  SERVICE_NAME="helloworld"

  # Fetch the URL using gcloud
  URL=$(gcloud run services describe $SERVICE_NAME \
    --platform managed \
    --region $REGION \
    --format 'value(status.url)')

  if [ -z "$URL" ]; then
    echo "❌ Error: Could not find Cloud Run service URL for '$SERVICE_NAME' in '$REGION'."
    exit 1
  fi
fi
# ----------------------------------

echo "Targeting Service: $URL"

# 2. Acquire Identity Token
# We can now use gcloud since we updated the Docker image
TOKEN=$(gcloud auth print-identity-token 2>/dev/null)

if [ -n "$TOKEN" ]; then
  echo "✅ Identity token acquired."
else
  echo "⚠️  No identity token found. Attempting unauthenticated request."
fi

# 3. Retry Loop
MAX_RETRIES=10
for ((i=1; i<=MAX_RETRIES; i++)); do
  if [ -n "$TOKEN" ]; then
    RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$URL")
  else
    RESPONSE=$(curl -s "$URL")
  fi

  if [[ "$RESPONSE" == *"Hello"* ]]; then
    echo "✅ Service is UP! Response: $RESPONSE"
    exit 0
  fi

  echo "Attempt $i/$MAX_RETRIES: Waiting for service... (Last response: $RESPONSE)"
  sleep 3
done

echo "❌ Service check FAILED."
exit 1