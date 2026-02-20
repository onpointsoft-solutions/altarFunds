#!/bin/bash

echo "🔄 Restarting frontend to pick up new backend configuration..."

# Kill any existing processes on port 3000
echo "🛑 Stopping any existing processes on port 3000..."
lsof -ti:3000 | xargs kill -9 2>/dev/null || true

# Clear any cached environment variables
echo "🧹 Clearing environment cache..."
unset VITE_API_URL
unset VITE_ADMIN_API_URL

# Restart the frontend development server
echo "🚀 Starting frontend with new backend configuration..."
cd web
npm run dev

echo "✅ Frontend is now configured to use: https://sanctum.co.ke/backend/api"
echo "📝 Make sure your backend is deployed and accessible at this URL"
