# AltarFunds API Documentation

## Base URL
- **Production**: `https://backend.sanctum.co.ke/api/`
- **Development**: `http://127.0.0.1:8000/api/`

## Authentication
All authenticated endpoints require a JWT Bearer token in the Authorization header:
```
Authorization: Bearer <access_token>
```

---

## 🔐 Authentication Endpoints

### 1. Register User
**POST** `/api/accounts/register/`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "first_name": "John",
  "last_name": "Doe",
  "phone_number": "+254724740854"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "first_name": "John",
      "last_name": "Doe",
      "role": "member"
    },
    "tokens": {
      "access": "eyJ0eXAiOiJKV1QiLCJhbGc...",
      "refresh": "eyJ0eXAiOiJKV1QiLCJhbGc..."
    }
  },
  "message": "Registration successful"
}
```

### 2. Register Staff
**POST** `/api/accounts/register/staff/`

### 3. Login
**POST** `/api/auth/token/`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "access": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

### 4. Refresh Token
**POST** `/api/auth/token/refresh/`

**Request Body:**
```json
{
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

**Response:**
```json
{
  "access": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

### 5. Get User Profile
**GET** `/api/accounts/profile/`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "role": "member",
    "church": {
      "id": 5,
      "name": "Grace Chapel"
    },
    "phone_number": "+254724740854",
    "profile_picture": "https://...",
    "date_joined": "2024-01-15T10:30:00Z"
  }
}
```

### 6. Update Profile
**PUT** `/api/accounts/profile/`

**Headers:** `Authorization: Bearer <token>`

### 7. Change Password
**POST** `/api/accounts/password/change/`

### 8. Reset Password Request
**POST** `/api/accounts/password/reset/`

### 9. Reset Password Confirm
**POST** `/api/accounts/password/reset/confirm/`

### 10. User Sessions
**GET** `/api/accounts/sessions/`

### 11. Revoke Session
**DELETE** `/api/accounts/sessions/<id>/revoke/`

### 12. User List
**GET** `/api/accounts/users/`

### 13. User Detail
**GET** `/api/accounts/users/<id>/`

---

## ⛪ Churches Management

### 1. Church List
**GET** `/api/churches/`

**Query Parameters:**
- `search` - Search by name
- `status` - Filter by status (verified, pending, suspended)
- `page` - Page number
- `page_size` - Results per page

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 50,
    "next": "https://.../api/churches/?page=2",
    "previous": null,
    "results": [
      {
        "id": 1,
        "name": "Grace Chapel",
        "location": "Nakuru, Kenya",
        "status": "verified",
        "member_count": 250,
        "pastor_name": "Rev. John Smith"
      }
    ]
  }
}
```

### 2. Church Detail
**GET** `/api/churches/<id>/`

### 3. Create Church
**POST** `/api/churches/register/`

### 4. Join Church
**POST** `/api/churches/join/`

### 5. Join Church by ID
**POST** `/api/churches/<id>/join/`

### 6. Verify Church
**POST** `/api/churches/<id>/verify/`

### 7. Update Church Status
**PUT** `/api/churches/<id>/status/`

### 8. Approve Church
**POST** `/api/churches/<id>/approve/`

### 9. Reject Church
**POST** `/api/churches/<id>/reject/`

### 10. Church Members
**GET** `/api/churches/<id>/members/`

### 11. Pending Churches
**GET** `/api/churches/pending-approval/`

### 12. Transfer Church
**POST** `/api/churches/transfer/`

### 13. Search Churches
**GET** `/api/churches/search/`

### 14. Church Summary
**GET** `/api/churches/<id>/summary/`

### 15. Church Options
**GET** `/api/churches/options/churches/`

### 16. Department Options
**GET** `/api/churches/options/departments/`

### 17. Small Group Options
**GET** `/api/churches/options/small-groups/`

### 18. Mobile Payment Details
**GET** `/api/churches/mobile/payment-details/`

### 19. Mobile Theme Colors
**GET** `/api/churches/mobile/theme-colors/`

---

## Church Sub-Resources

### Denominations
```
GET  /api/churches/denominations/             # Denomination List
POST /api/churches/denominations/             # Create Denomination
GET  /api/churches/denominations/<id>/        # Denomination Detail
```

### Campuses
```
GET  /api/churches/campuses/                  # Campus List
POST /api/churches/campuses/                  # Create Campus
GET  /api/churches/campuses/<id>/             # Campus Detail
```

### Departments
```
GET  /api/churches/departments/               # Department List
POST /api/churches/departments/               # Create Department
GET  /api/churches/departments/<id>/          # Department Detail
```

### Small Groups
```
GET  /api/churches/small-groups/              # Small Group List
POST /api/churches/small-groups/              # Create Small Group
GET  /api/churches/small-groups/<id>/         # Small Group Detail
```

### Bank Accounts
```
GET  /api/churches/bank-accounts/             # Bank Account List
POST /api/churches/bank-accounts/             # Create Bank Account
GET  /api/churches/bank-accounts/<id>/        # Bank Account Detail
```

### M-Pesa Accounts
```
GET  /api/churches/mpesa-accounts/            # M-Pesa Account List
POST /api/churches/mpesa-accounts/            # Create M-Pesa Account
GET  /api/churches/mpesa-accounts/<id>/       # M-Pesa Account Detail
```

---

## 💰 Giving & Donations

### Giving Endpoints
```
GET  /api/giving/categories/                  # Giving Categories
POST /api/giving/transactions/                # Create Giving Transaction
GET  /api/giving/categories-list/             # Category List (ViewSet)
GET  /api/giving/transactions-list/           # Transaction List (ViewSet)
GET  /api/giving/recurring/                    # Recurring Giving
GET  /api/giving/pledges/                      # Pledges
GET  /api/giving/campaigns/                    # Giving Campaigns
GET  /api/giving/church/<church_id>/          # Church Givings
```

### Donation Endpoints
```
GET  /api/donations/                           # Donation List
POST /api/donations/                          # Create Donation
```

---

## 💸 Expenses Management

```
GET  /api/expenses/                           # Expense List
POST /api/expenses/                           # Create Expense
GET  /api/expenses/<id>/                      # Expense Detail
PUT  /api/expenses/<id>/                      # Update Expense
DELETE /api/expenses/<id>/                    # Delete Expense
POST /api/expenses/<id>/approve/              # Approve Expense
POST /api/expenses/<id>/reject/               # Reject Expense
```

---

## 📊 Budgets Management

```
GET  /api/budgets/                            # Budget List
POST /api/budgets/                            # Create Budget
GET  /api/budgets/<id>/                       # Budget Detail
PUT  /api/budgets/<id>/                       # Update Budget
DELETE /api/budgets/<id>/                     # Delete Budget
```

---

## 👥 Members Management

```
GET  /api/members/                            # Member List
POST /api/members/                            # Create Member
GET  /api/members/<id>/                       # Member Detail
PUT  /api/members/<id>/                       # Update Member
DELETE /api/members/<id>/                     # Delete Member
```

---

## 📈 Dashboard & Reports

### Dashboard Endpoints
```
GET  /api/dashboard/                          # Dashboard View
GET  /api/dashboard/financial-summary/        # Financial Summary
GET  /api/dashboard/monthly-trend/             # Monthly Trend
GET  /api/dashboard/income-breakdown/         # Income Breakdown
GET  /api/dashboard/expense-breakdown/        # Expense Breakdown
GET  /api/dashboard/comprehensive/            # Comprehensive Dashboard
```

### Report Endpoints
```
GET  /api/reports/                            # Reports List
POST /api/reports/                            # Generate Report
```

---

## 📱 Mobile API

```
GET  /api/mobile/                             # Mobile Endpoints
```

---

## 💳 Payments

```
GET  /api/payments/                           # Payment Methods
POST /api/payments/                           # Process Payment
```

---

## 📋 Other Modules

### Accounting
```
GET  /api/accounting/                         # Accounting Data
```

### Admin Management
```
GET  /api/admin/                              # Admin Endpoints
```

### Audit
```
GET  /api/audit/                              # Audit Logs
```

### Notifications
```
GET  /api/notifications/                      # Notifications
```

### Devotionals
```
GET  /api/devotionals/                        # Devotionals
```

### Notices
```
GET  /api/notices/                            # Notices
```

### Announcements
```
GET  /api/announcements/                      # Announcements
```

### Suggestions
```
GET  /api/suggestions/                        # Suggestions
```

### Attendance
```
GET  /api/attendance/                         # Attendance Data
```

---

## 🏥 Health Check

```
GET  /api/health/                             # Health Check
```

---

## 🔗 API Root

```
GET  /                                        # API Root (Returns endpoint list)
```

---

## 📝 Integration Notes

1. **Authentication**: Most endpoints require JWT token in Authorization header: `Bearer <token>`
2. **Base URL**: `https://backend.sanctum.co.ke/api`
3. **Content-Type**: `application/json`
4. **Pagination**: List endpoints support pagination parameters
5. **CORS**: Configured for frontend integration
6. **Rate Limiting**: Applied to prevent abuse

---

## 📋 Error Response Format

All errors follow this format:

```json
{
  "success": false,
  "message": "Error description",
  "errors": {
    "field_name": ["Error detail"]
  }
}
```

### Common HTTP Status Codes
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## 🔒 Role-Based Access Control

### Member
- View own profile
- Join churches
- Make givings
- View own giving history
- Transfer churches

### Church Admin (Pastor, Treasurer, Auditor)
- All member permissions
- View church members
- View church givings
- Generate church reports
- Manage church details

### System Admin
- All permissions
- Approve/reject churches
- View all churches
- View all members
- System-wide reports
- Manage all users

---

## 🚀 Integration Guide

### Web Frontend
```javascript
// Initialize payment
const response = await fetch('/api/payments/payments/initialize_paystack/', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    amount: 5000,
    giving_type: 'tithe',
    church_id: 1
  })
});

const data = await response.json();
if (data.success) {
  // Redirect to Paystack
  window.location.href = data.data.authorization_url;
}
```

### Mobile App (Android)
```kotlin
// Initialize payment
val request = PaymentRequest(
    amount = 5000.0,
    givingType = "tithe",
    churchId = 1
)

val response = apiService.initializePaystackPayment(request)
if (response.success) {
    // Open Paystack checkout
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.data.authorizationUrl))
    startActivity(intent)
}
```

---

*For support, contact: support@sanctum.co.ke*
