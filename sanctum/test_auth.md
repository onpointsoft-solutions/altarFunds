# Authentication Test Guide

## Test Credentials

The Sanctum Church Management System now has proper credential validation:

### Valid Test Credentials:
- **Username**: `admin` **Password**: `admin123` → Admin Dashboard
- **Username**: `pastor` **Password**: `pastor123` → Pastor Dashboard  
- **Username**: `treasurer` **Password**: `treasurer123` → Treasurer Dashboard
- **Username**: `secretary` **Password**: `secretary123` → Secretary Dashboard
- **Username**: `usher` **Password**: `usher123` → Usher Dashboard
- **Username**: `test` **Password**: `test123` → Default Dashboard

### Invalid Credentials:
- Any other username/password combination will be rejected
- Empty username or password will be rejected
- Wrong password for valid username will be rejected

## Features Implemented:

✅ **API Authentication**: Proper validation against credentials
✅ **Session Management**: Persistent login state with "Remember Me"
✅ **Loading Indicator**: Shows progress during authentication
✅ **Error Handling**: Clear error messages for invalid credentials
✅ **Role-Based Navigation**: Redirects to appropriate dashboard based on user role
✅ **Session Persistence**: Remembers login if "Remember Me" is checked

## Testing Steps:

1. **Start the application**: `./run.sh`
2. **Try invalid credentials**: Enter wrong username/password → Should show "Invalid credentials"
3. **Try valid credentials**: Use any of the test credentials above → Should login and redirect
4. **Test Remember Me**: Check "Remember me" and restart application → Should auto-login

## Security Notes:

- Passwords are case-sensitive
- Session expires after 24 hours
- Tokens are stored securely in user home directory
- API calls use Bearer token authentication
