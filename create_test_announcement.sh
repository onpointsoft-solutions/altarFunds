#!/bin/bash

# Create a test announcement to verify endpoints work
# Replace YOUR_ACCESS_TOKEN with a valid JWT token

BASE_URL="https://backend.sanctum.co.ke/api"
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc0NjIwNjk5LCJpYXQiOjE3NzQ2MTcwOTksImp0aSI6IjZlYzc3MzJiOGIzZDRmZjZiYThkZjc5ZmY2ZWI3NWM2IiwidXNlcl9pZCI6MjN9.S0tY5W6rlrIIJ7RKfkc2yVN0I0ct7tuV0PuEOjgFT-M"

echo "Creating test announcement..."
curl -X POST "${BASE_URL}/announcements/" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Announcement for Mobile App",
    "content": "This is a test announcement to verify that the mobile endpoints are working correctly. If you can see this, the announcements are loading properly!",
    "priority": "normal",
    "is_active": true,
    "target_audience": "all"
  }' \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"

echo "Testing announcements fetch after creation..."
curl -X GET "${BASE_URL}/announcements/?page=1" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n\n"
