# AltarFunds API Documentation

## Base URL
- **Production**: `https://altarfunds.pythonanywhere.com/api/`
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
  "phone_number": "+2348012345678"
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

### 2. Login
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

### 3. Refresh Token
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

### 4. Get User Profile
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
    "phone_number": "+2348012345678",
    "profile_picture": "https://...",
    "date_joined": "2024-01-15T10:30:00Z"
  }
}
```

### 5. Update Profile
**PUT** `/api/accounts/profile/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "first_name": "John",
  "last_name": "Doe",
  "phone_number": "+2348012345678",
  "address_line1": "123 Main St"
}
```

---

## ⛪ Church Endpoints

### 1. List Churches
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
        "location": "Lagos, Nigeria",
        "status": "verified",
        "member_count": 250,
        "pastor_name": "Rev. John Smith"
      }
    ]
  }
}
```

### 2. Get Church Details
**GET** `/api/churches/{id}/`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Grace Chapel",
    "church_type": "main",
    "status": "verified",
    "location": "Lagos, Nigeria",
    "address": "123 Church Street",
    "phone": "+2348012345678",
    "email": "info@gracechapel.org",
    "website": "https://gracechapel.org",
    "pastor_name": "Rev. John Smith",
    "member_count": 250,
    "founded_date": "2010-01-15",
    "description": "A vibrant community church..."
  }
}
```

### 3. Create Church
**POST** `/api/churches/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "New Life Church",
  "church_type": "main",
  "location": "Abuja, Nigeria",
  "address": "456 Faith Avenue",
  "phone": "+2348098765432",
  "email": "info@newlife.org",
  "pastor_name": "Rev. Jane Doe",
  "description": "A growing community..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "New Life Church",
    "status": "pending",
    "message": "Church registration submitted for approval"
  }
}
```

### 4. Join Church
**POST** `/api/churches/{id}/join/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "membership_type": "member",
  "reason": "Moving to this area"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully joined Grace Chapel"
}
```

### 5. Transfer Church
**POST** `/api/churches/transfer/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "from_church_id": 1,
  "to_church_id": 5,
  "reason": "Relocation",
  "request_transfer_letter": true
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "transfer_id": 123,
    "status": "pending",
    "message": "Transfer request submitted"
  }
}
```

### 6. List Pending Churches (Super Admin)
**GET** `/api/churches/pending-approval/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** System Admin only

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "name": "New Life Church",
      "pastor_name": "Rev. Jane Doe",
      "submitted_date": "2024-01-20T10:00:00Z",
      "status": "pending"
    }
  ]
}
```

### 7. Approve Church (Super Admin)
**POST** `/api/churches/{id}/approve/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** System Admin only

**Response:**
```json
{
  "success": true,
  "message": "Church approved successfully"
}
```

### 8. Reject Church (Super Admin)
**POST** `/api/churches/{id}/reject/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** System Admin only

**Request Body:**
```json
{
  "reason": "Incomplete documentation"
}
```

---

## 💰 Giving Endpoints

### 1. List Givings
**GET** `/api/giving/`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `church_id` - Filter by church
- `giving_type` - Filter by type (tithe, offering, donation)
- `start_date` - Filter from date
- `end_date` - Filter to date
- `status` - Filter by status (pending, completed, failed)

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 25,
    "results": [
      {
        "id": 100,
        "amount": 5000.00,
        "giving_type": "tithe",
        "church": {
          "id": 1,
          "name": "Grace Chapel"
        },
        "payment_date": "2024-01-20T14:30:00Z",
        "status": "completed",
        "payment_method": "paystack"
      }
    ]
  }
}
```

### 2. Create Giving
**POST** `/api/giving/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "amount": 5000.00,
  "giving_type": "tithe",
  "church_id": 1,
  "note": "Monthly tithe",
  "anonymous": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 100,
    "amount": 5000.00,
    "status": "pending",
    "message": "Giving record created. Proceed to payment."
  }
}
```

### 3. Get Giving History
**GET** `/api/giving/history/`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "data": {
    "total_given": 50000.00,
    "givings": [
      {
        "id": 100,
        "amount": 5000.00,
        "giving_type": "tithe",
        "date": "2024-01-20T14:30:00Z",
        "status": "completed"
      }
    ]
  }
}
```

### 4. Get Church Givings (Church Admin)
**GET** `/api/giving/church/{church_id}/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** Church Admin or System Admin

**Response:**
```json
{
  "success": true,
  "data": {
    "total_received": 500000.00,
    "givings": [
      {
        "id": 100,
        "amount": 5000.00,
        "member": "John Doe",
        "giving_type": "tithe",
        "date": "2024-01-20T14:30:00Z"
      }
    ]
  }
}
```

---

## 💳 Payment Endpoints

### 1. Initialize Paystack Payment
**POST** `/api/payments/payments/initialize_paystack/`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "amount": 5000.00,
  "giving_type": "tithe",
  "church_id": 1,
  "callback_url": "https://yourapp.com/payment/callback"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "authorization_url": "https://checkout.paystack.com/...",
    "access_code": "abc123xyz",
    "reference": "AF-1234567890AB"
  }
}
```

### 2. Verify Payment
**GET** `/api/payments/payments/verify_payment/?reference=AF-1234567890AB`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "success",
    "amount": 5000.00,
    "reference": "AF-1234567890AB",
    "paid_at": "2024-01-20T14:35:00Z"
  }
}
```

### 3. Paystack Webhook (Internal)
**POST** `/api/payments/paystack/webhook/`

**Headers:** `X-Paystack-Signature: <signature>`

**Note:** This endpoint is called by Paystack, not by your frontend.

---

## 📊 Reports Endpoints

### 1. Financial Summary
**GET** `/api/reports/financial-summary/`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `church_id` - Filter by church (church admin sees only their church)
- `start_date` - Period start
- `end_date` - Period end

**Response:**
```json
{
  "success": true,
  "data": {
    "total_income": 500000.00,
    "total_expenses": 300000.00,
    "net_income": 200000.00,
    "total_givings": 450000.00,
    "budget_utilization": 75.5,
    "period": "January 2024"
  }
}
```

### 2. Giving Trends
**GET** `/api/reports/giving-trends/`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "data": {
    "monthly_trends": [
      {
        "month": "2024-01",
        "total": 50000.00,
        "count": 25
      }
    ],
    "by_type": {
      "tithe": 30000.00,
      "offering": 15000.00,
      "donation": 5000.00
    }
  }
}
```

### 3. Member Statistics
**GET** `/api/reports/member-statistics/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** Church Admin or System Admin

**Response:**
```json
{
  "success": true,
  "data": {
    "total_members": 250,
    "active_members": 200,
    "new_members_this_month": 5,
    "tithe_payers": 150
  }
}
```

---

## 👥 Member Endpoints

### 1. List Members (Church Admin)
**GET** `/api/members/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** Church Admin or System Admin

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 250,
    "results": [
      {
        "id": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "phone": "+2348012345678",
        "membership_status": "member",
        "joined_date": "2023-01-15"
      }
    ]
  }
}
```

### 2. Get Member Details
**GET** `/api/members/{id}/`

**Headers:** `Authorization: Bearer <token>`

**Permissions:** Church Admin or System Admin

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+2348012345678",
    "membership_number": "GC-2023-001",
    "membership_status": "member",
    "joined_date": "2023-01-15",
    "total_givings": 50000.00,
    "departments": ["Choir", "Ushering"]
  }
}
```

---

## 🔔 Notification Endpoints

### 1. List Notifications
**GET** `/api/notifications/`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Payment Successful",
      "message": "Your tithe of ₦5,000 was received",
      "type": "payment",
      "is_read": false,
      "created_at": "2024-01-20T14:35:00Z"
    }
  ]
}
```

### 2. Mark as Read
**POST** `/api/notifications/{id}/mark-read/`

**Headers:** `Authorization: Bearer <token>`

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

*For support, contact: support@altarfunds.com*
