# AltarFunds Church Management System - Production Implementation Plan

## 🎯 Project Overview

Transform the AltarFunds church management system into a production-ready, full-stack application with:
- **Backend**: Django + Django REST Framework with role-based access control
- **Web Module**: Modern, responsive HTML/CSS/JS consuming backend APIs
- **Mobile App**: Android app with secure backend integration
- **Payment Gateway**: Paystack integration for offerings, tithes, and donations

---

## 📊 Current State Analysis

### ✅ Backend (Django)
- Django REST Framework configured
- JWT authentication with SimpleJWT
- Multiple apps: accounts, churches, giving, payments, members, etc.
- CORS configured for web/mobile clients
- Custom User model with roles: member, pastor, treasurer, auditor, denomination_admin, system_admin
- M-Pesa integration (needs Paystack addition)

### ⚠️ Areas Needing Work
1. **Role-Based Permissions**: Need to verify and strengthen permissions across all endpoints
2. **Paystack Integration**: Add alongside M-Pesa
3. **API Completeness**: Ensure all features have proper API endpoints
4. **Web Module**: Audit and update to consume backend APIs (no hard-coded data)
5. **Mobile App**: Verify complete backend integration

---

## 🏗️ Implementation Phases

### Phase 1: Backend API Audit & Enhancement

#### 1.1 Authentication & Authorization
- [x] JWT authentication configured
- [ ] Verify role-based permissions for all endpoints
- [ ] Implement custom permissions classes:
  - `IsMember`
  - `IsChurchAdmin` (pastor, treasurer, auditor)
  - `IsSuperAdmin` (system_admin)
- [ ] Add permission checks to all API views

#### 1.2 Core API Endpoints

**Authentication APIs** (`/api/accounts/`)
- [ ] POST `/register/` - Member registration
- [ ] POST `/login/` - JWT token generation
- [ ] POST `/refresh/` - Token refresh
- [ ] POST `/logout/` - Token invalidation
- [ ] GET `/profile/` - Get user profile
- [ ] PUT `/profile/` - Update user profile
- [ ] POST `/change-password/` - Password change
- [ ] POST `/forgot-password/` - Password reset request
- [ ] POST `/reset-password/` - Password reset confirmation

**Church APIs** (`/api/churches/`)
- [ ] GET `/` - List churches (with filters, search, pagination)
- [ ] POST `/` - Create church (requires approval)
- [ ] GET `/{id}/` - Church details
- [ ] PUT `/{id}/` - Update church (church admin only)
- [ ] DELETE `/{id}/` - Soft delete church (super admin only)
- [ ] GET `/{id}/members/` - List church members
- [ ] POST `/{id}/join/` - Join church
- [ ] POST `/transfer/` - Transfer between churches
- [ ] GET `/pending-approval/` - List pending churches (super admin)
- [ ] POST `/{id}/approve/` - Approve church (super admin)
- [ ] POST `/{id}/reject/` - Reject church (super admin)

**Giving APIs** (`/api/giving/`)
- [ ] GET `/` - List givings (filtered by user/church/date)
- [ ] POST `/` - Create giving record
- [ ] GET `/{id}/` - Giving details
- [ ] GET `/history/` - User's giving history
- [ ] GET `/church/{church_id}/` - Church giving records (church admin)
- [ ] GET `/summary/` - Giving summary/statistics

**Payment APIs** (`/api/payments/`)
- [ ] POST `/initialize/` - Initialize Paystack payment
- [ ] POST `/verify/` - Verify payment
- [ ] POST `/webhook/` - Paystack webhook handler
- [ ] GET `/` - List payments
- [ ] GET `/{id}/` - Payment details
- [ ] GET `/reconcile/` - Payment reconciliation report

**Member APIs** (`/api/members/`)
- [ ] GET `/` - List members (church admin only)
- [ ] GET `/{id}/` - Member details
- [ ] PUT `/{id}/` - Update member
- [ ] GET `/{id}/giving-history/` - Member giving history
- [ ] GET `/{id}/transfer-history/` - Member transfer history

**Reports APIs** (`/api/reports/`)
- [ ] GET `/financial-summary/` - Financial dashboard data
- [ ] GET `/giving-trends/` - Giving trends analysis
- [ ] GET `/church-performance/` - Church performance metrics
- [ ] GET `/member-statistics/` - Member statistics
- [ ] GET `/export/` - Export reports (CSV/PDF)

#### 1.3 Paystack Integration

**Setup**
```python
# settings.py
PAYSTACK_SECRET_KEY = config('PAYSTACK_SECRET_KEY')
PAYSTACK_PUBLIC_KEY = config('PAYSTACK_PUBLIC_KEY')
PAYSTACK_CALLBACK_URL = config('PAYSTACK_CALLBACK_URL')
```

**Payment Flow**
1. Frontend initiates payment → POST `/api/payments/initialize/`
2. Backend creates payment record → Returns Paystack authorization URL
3. User completes payment on Paystack
4. Paystack sends webhook → POST `/api/payments/webhook/`
5. Backend verifies payment → Updates giving record
6. Frontend polls → GET `/api/payments/verify/{reference}/`

**Security**
- Verify webhook signature
- Prevent duplicate payments
- Link payment to member + church
- Store payment reference for reconciliation

---

### Phase 2: Mobile App Backend Integration

#### 2.1 API Service Configuration
- [x] Base URL configured: `https://altarfunds.pythonanywhere.com/api/`
- [x] JWT token authentication
- [ ] Verify all endpoints are correctly integrated
- [ ] Add proper error handling
- [ ] Implement offline caching for critical data

#### 2.2 Features to Verify
- [ ] User registration and login
- [ ] Church search and joining
- [ ] Church transfer
- [ ] Giving/donation with Paystack
- [ ] Giving history
- [ ] Profile management
- [ ] Push notifications

#### 2.3 Payment Integration
- [ ] Integrate Paystack Android SDK
- [ ] Implement payment initialization
- [ ] Handle payment callbacks
- [ ] Update UI with payment status

---

### Phase 3: Web Module Modernization

#### 3.1 Architecture
- Modern, responsive HTML/CSS/JS
- Bootstrap 5 for UI components
- Fetch API for backend communication
- No hard-coded data - all from APIs

#### 3.2 Pages to Modernize

**Public Pages**
- [ ] Landing page (`/`)
- [ ] About page
- [ ] Contact page
- [ ] Church directory (search/browse)

**Authentication Pages**
- [ ] Login (`/login/`)
- [ ] Register (`/register/`)
- [ ] Forgot password
- [ ] Reset password

**Member Dashboard** (`/dashboard/`)
- [ ] Overview with financial summary
- [ ] My givings
- [ ] My church
- [ ] Profile settings
- [ ] Transfer church

**Church Admin Dashboard** (`/dashboard/church/`)
- [ ] Church overview
- [ ] Members list
- [ ] Givings received
- [ ] Financial reports
- [ ] Church settings

**Super Admin Dashboard** (`/dashboard/admin/`)
- [ ] System overview
- [ ] Pending church approvals
- [ ] All churches management
- [ ] All users management
- [ ] System-wide reports
- [ ] Audit logs

#### 3.3 UI/UX Requirements
- Mobile-first responsive design
- Clean, modern interface
- Loading states for all async operations
- Error handling with user-friendly messages
- Form validation
- Success/error notifications
- Consistent spacing, colors, typography

---

### Phase 4: Role-Based Access Control

#### 4.1 Permission Classes

```python
# common/permissions.py

class IsMember(BasePermission):
    """Allow access to authenticated members"""
    def has_permission(self, request, view):
        return request.user.is_authenticated

class IsChurchAdmin(BasePermission):
    """Allow access to church admins (pastor, treasurer, auditor)"""
    def has_permission(self, request, view):
        return request.user.role in ['pastor', 'treasurer', 'auditor', 'denomination_admin']

class IsSuperAdmin(BasePermission):
    """Allow access to system administrators"""
    def has_permission(self, request, view):
        return request.user.role == 'system_admin' or request.user.is_superuser

class IsChurchMember(BasePermission):
    """Check if user belongs to the church"""
    def has_object_permission(self, request, view, obj):
        return obj.church == request.user.church
```

#### 4.2 Apply Permissions to Views
- Member endpoints: `IsMember`
- Church management: `IsChurchAdmin`
- Church approval: `IsSuperAdmin`
- Giving records: Owner or church admin
- Reports: Church admin for church data, super admin for system-wide

---

### Phase 5: Data Flow & Consistency

#### 5.1 Member Journey
1. Register → Create User + Member profile
2. Search churches → GET `/api/churches/`
3. Join church → POST `/api/churches/{id}/join/`
4. Make giving → POST `/api/giving/` + Paystack payment
5. View history → GET `/api/giving/history/`
6. Transfer church → POST `/api/churches/transfer/`

#### 5.2 Church Admin Journey
1. Create church → POST `/api/churches/` (pending approval)
2. Super admin approves → POST `/api/churches/{id}/approve/`
3. Manage members → GET `/api/churches/{id}/members/`
4. View givings → GET `/api/giving/church/{id}/`
5. Generate reports → GET `/api/reports/financial-summary/`

#### 5.3 Super Admin Journey
1. View pending churches → GET `/api/churches/pending-approval/`
2. Approve/reject churches
3. View all churches → GET `/api/churches/`
4. View system reports → GET `/api/reports/`
5. Manage users → Admin panel

---

### Phase 6: Security & Production Readiness

#### 6.1 Security Checklist
- [ ] HTTPS enforced in production
- [ ] JWT tokens with proper expiration
- [ ] Rate limiting on sensitive endpoints
- [ ] Input validation on all forms
- [ ] SQL injection prevention (use ORM)
- [ ] XSS prevention (escape user input)
- [ ] CSRF protection enabled
- [ ] Secure password hashing
- [ ] Payment webhook signature verification
- [ ] Audit logging for sensitive operations

#### 6.2 Performance
- [ ] Database indexing on frequently queried fields
- [ ] Query optimization (select_related, prefetch_related)
- [ ] API response caching where appropriate
- [ ] Pagination on list endpoints
- [ ] Image optimization
- [ ] Static file compression

#### 6.3 Testing
- [ ] Unit tests for models
- [ ] API endpoint tests
- [ ] Permission tests
- [ ] Payment flow tests
- [ ] End-to-end user journey tests

---

## 🚀 Execution Priority

### Week 1: Backend Foundation
1. Audit and strengthen role-based permissions
2. Complete missing API endpoints
3. Implement Paystack integration
4. Add comprehensive error handling

### Week 2: Mobile App Integration
1. Verify all API integrations
2. Implement Paystack in mobile app
3. Add offline support
4. Test all user flows

### Week 3: Web Module Modernization
1. Modernize authentication pages
2. Build member dashboard
3. Build church admin dashboard
4. Build super admin dashboard

### Week 4: Testing & Production
1. Security audit
2. Performance optimization
3. End-to-end testing
4. Production deployment

---

## 📝 Development Standards

### Code Quality
- Clean, readable, well-commented code
- Follow Django best practices
- Use serializers for all API responses
- Consistent error response format
- Proper logging

### API Response Format
```json
{
  "success": true,
  "data": {},
  "message": "Success message",
  "errors": null
}
```

### Error Response Format
```json
{
  "success": false,
  "data": null,
  "message": "Error message",
  "errors": {
    "field": ["Error detail"]
  }
}
```

---

## 🔗 Integration Points

### Web ↔ Backend
- All pages consume REST APIs
- JWT token stored in localStorage
- Axios/Fetch for HTTP requests
- Handle 401 (redirect to login)
- Handle 403 (show permission error)

### Mobile ↔ Backend
- Retrofit for HTTP requests
- JWT token in shared preferences
- Proper error handling
- Offline data caching
- Push notification integration

### Paystack Integration
- Initialize payment from frontend
- Process payment on Paystack
- Webhook updates backend
- Frontend polls for confirmation
- Reconciliation report for admins

---

## 📊 Success Metrics

- All API endpoints documented and tested
- Zero hard-coded data in frontend
- Role-based access working correctly
- Payment flow tested end-to-end
- Mobile and web consuming same APIs
- Responsive design on all devices
- Production-ready security measures
- Clean, maintainable codebase

---

*This document will be updated as implementation progresses.*
