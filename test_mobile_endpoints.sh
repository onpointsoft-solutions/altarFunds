#!/bin/bash

# Test Mobile Endpoints for Announcements and Devotionals
# Replace YOUR_ACCESS_TOKEN with a valid JWT token

BASE_URL="https://backend.sanctum.co.ke/api"
ACCESS_TOKEN="YOUR_ACCESS_TOKEN"

echo "Testing Mobile Announcements Endpoint..."
curl -X GET "${BASE_URL}/mobile/announcements/?page=1" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "Testing Mobile Devotionals Endpoint..."
curl -X GET "${BASE_URL}/mobile/devotionals/?page=1" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "For comparison, testing original endpoints..."
echo "Original Announcements:"
curl -X GET "${BASE_URL}/announcements/?page=1" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "Original Devotionals:"
curl -X GET "${BASE_URL}/devotionals/?page=1" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "Testing with pagination..."
curl -X GET "${BASE_URL}/mobile/announcements/?page=1&page_size=5" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "Testing without authentication (should return 401)..."
curl -X GET "${BASE_URL}/mobile/announcements/?page=1" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"
