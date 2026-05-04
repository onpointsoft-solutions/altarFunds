# Sanctum Mobile App - Password Reset System

## 🔐 Overview

This document outlines the complete password reset functionality implemented for the Sanctum mobile app, providing a secure and user-friendly password recovery experience.

## 📱 System Architecture

### **Backend Components**
- **PasswordResetRequestView**: Handles password reset requests
- **PasswordResetConfirmView**: Handles password reset confirmation
- **PasswordResetToken Model**: Manages reset tokens with expiration
- **Email Service**: Sends reset links via email

### **Mobile App Components**
- **ForgotPasswordActivity**: Initial password reset request
- **ResetPasswordActivity**: Password reset confirmation
- **Deep Link Handling**: Processes email links
- **API Integration**: Communicates with backend endpoints

## 🔄 Password Reset Flow

### **Step 1: Request Password Reset**
1. User clicks "Forgot Password?" on login screen
2. User enters their email address
3. App validates email format and existence
4. App sends request to `/accounts/forgot-password/` endpoint
5. Backend generates UUID token (1-hour expiration)
6. Backend sends email with reset link
7. App shows success confirmation dialog

### **Step 2: Email Processing**
1. User receives email with reset link
2. Email contains: `https://altarfunds.co.ke/reset-password/{token}/`
3. User clicks the link (opens mobile app via deep link)

### **Step 3: Reset Password**
1. Mobile app opens ResetPasswordActivity via deep link
2. App extracts token from URL
3. User enters new password (12+ chars, complex requirements)
4. App validates password strength and confirmation
5. App sends request to `/accounts/password/reset/confirm/` endpoint
6. Backend validates token and updates password
7. App redirects to login screen with success message

## 🔧 Technical Implementation

### **API Endpoints**

#### **Request Password Reset**
```http
POST /accounts/forgot-password/
Content-Type: application/json

{
  "email": "user@example.com"
}

Response:
{
  "message": "Password reset email sent"
}
```

#### **Confirm Password Reset**
```http
POST /accounts/password/reset/confirm/
Content-Type: application/json

{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "new_password": "NewSecurePassword123!",
  "new_password_confirm": "NewSecurePassword123!"
}

Response:
{
  "message": "Password reset successful"
}
```

### **Mobile App Models**

#### **PasswordResetConfirmRequest**
```kotlin
data class PasswordResetConfirmRequest(
    val token: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("new_password_confirm") val newPasswordConfirm: String
)
```

### **Deep Link Configuration**
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" 
          android:host="altarfunds.co.ke" 
          android:pathPrefix="/reset-password/" />
</intent-filter>
```

## 🛡️ Security Features

### **Token Security**
- **UUID Generation**: Cryptographically secure random tokens
- **Expiration**: 1-hour token validity
- **Single Use**: Tokens marked as used after successful reset
- **Validation**: Backend validates token existence and expiration

### **Password Requirements**
- **Minimum Length**: 12 characters
- **Complexity**: Uppercase, lowercase, number, special character
- **Validation**: Both client-side and server-side validation

### **Rate Limiting**
- **Email Validation**: Prevents email enumeration attacks
- **Request Limiting**: Prevents spam reset requests
- **Token Limits**: One active token per user

## 🎨 User Interface

### **ForgotPasswordActivity Features**
- Clean, focused email input form
- Real-time email validation
- Loading states and error handling
- Success dialog with clear instructions

### **ResetPasswordActivity Features**
- Password visibility toggles
- Real-time password strength validation
- Clear password requirements display
- Progress indicators and error messages

### **Design Elements**
- **Material Design 3** components
- **Consistent Theming** with app colors
- **Accessibility** considerations
- **Responsive** layouts

## 📧 Email Template

### **Password Reset Email**
```
Subject: Password Reset - AltarFunds

Dear {User Name},

You requested a password reset for your AltarFunds account.

Click the link below to reset your password:
https://altarfunds.co.ke/reset-password/{token}/

This link will expire in 1 hour.

If you didn't request this password reset, please ignore this email.

Thank you,
Sanctum Team
```

## 🧪 Testing Scenarios

### **Test 1: Valid Email Reset**
1. Enter valid registered email
2. Verify success message appears
3. Check email received with reset link
4. Click link and verify app opens
5. Enter new password and confirm
6. Verify password reset success

### **Test 2: Invalid Email**
1. Enter unregistered email
2. Verify error message appears
3. Verify no email sent

### **Test 3: Expired Token**
1. Request password reset
2. Wait for token to expire (1 hour)
3. Try to use expired link
4. Verify "expired token" error

### **Test 4: Used Token**
1. Request password reset
2. Use reset link successfully
3. Try to use same link again
4. Verify "used token" error

### **Test 5: Password Validation**
1. Enter weak password (less than 12 chars)
2. Verify validation error
3. Enter password without special character
4. Verify validation error
5. Enter mismatched passwords
6. Verify confirmation error

## 🔍 Error Handling

### **Client-Side Errors**
- **Network Issues**: "Network error" message
- **Invalid Email**: "Invalid email address"
- **Password Weakness**: Detailed requirement messages
- **Token Missing**: "Invalid reset token"

### **Server-Side Errors**
- **Email Not Found**: "No account found with this email"
- **Rate Limiting**: "Too many reset attempts. Please try again later."
- **Invalid Token**: "Invalid or expired reset token"
- **Used Token**: "Reset token has already been used"

## 📊 Analytics & Logging

### **Security Events Logged**
- Password reset requests (with IP address)
- Successful password resets
- Failed reset attempts
- Token validation failures

### **User Experience Metrics**
- Password reset completion rate
- Average time to complete reset
- Common error occurrences
- Email delivery success rate

## 🚀 Deployment Checklist

### **Backend Verification**
- [ ] Email service configured correctly
- [ ] Token expiration working
- [ ] Rate limiting implemented
- [ ] Security logging enabled

### **Mobile App Verification**
- [ ] Deep links registered correctly
- [ ] API endpoints configured
- [ ] UI validation working
- [ ] Error handling complete

### **Integration Testing**
- [ ] End-to-end flow tested
- [ ] Email delivery verified
- [ ] Token validation tested
- [ ] Password reset confirmed

### **Security Testing**
- [ ] Token security verified
- [ ] Rate limiting tested
- [ ] Input validation confirmed
- [ ] Error message safety checked

## 🔧 Configuration

### **Environment Variables**
```
# Email Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USE_TLS=True
EMAIL_HOST_USER=noreply@altarfunds.co.ke
EMAIL_HOST_PASSWORD=your_email_password

# Frontend URL
FRONTEND_URL=https://altarfunds.co.ke
```

### **Token Settings**
```python
# In Django settings
PASSWORD_RESET_TIMEOUT = 3600  # 1 hour in seconds
PASSWORD_RESET_TOKEN_LENGTH = 32
```

## 📞 Support

### **Common Issues & Solutions**

**Issue: User doesn't receive email**
- Check email configuration
- Verify spam folder
- Check email address validity

**Issue: Deep link not working**
- Verify app is installed
- Check deep link configuration
- Test with Android App Links

**Issue: Password validation failing**
- Check password requirements
- Verify backend validation rules
- Update client-side validation

**Issue: Token expired immediately**
- Check server time synchronization
- Verify token expiration logic
- Check timezone settings

---

**Last Updated**: May 4, 2026  
**Version**: 1.0  
**Security Level**: High  
**Testing Status**: Ready for QA
