@echo off
echo 🔄 Restarting frontend to pick up new backend configuration...

REM Kill any existing processes on port 3000
echo 🛑 Stopping any existing processes on port 3000...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":3000" ^| find "LISTENING"') do (
    echo Killing process %%a...
    taskkill /f /pid %%a >nul 2>&1
)

REM Clear any cached environment variables
echo 🧹 Clearing environment cache...
set VITE_API_URL=
set VITE_ADMIN_API_URL=

REM Restart the frontend development server
echo 🚀 Starting frontend with new backend configuration...
cd web
npm run dev

echo ✅ Frontend is now configured to use: https://sanctum.co.ke/backend/api
echo 📝 Make sure your backend is deployed and accessible at this URL
pause
