# Attendance Management API

This module provides comprehensive attendance tracking functionality for churches, with special permissions for ushers to manage member attendance.

## Features

- **Usher Access**: Ushers can mark attendance for church members
- **Service-based Tracking**: Track attendance per service type and date
- **Individual Member Tracking**: Track individual member attendance patterns
- **Quick Attendance Bulk Marking**: Mark multiple members present at once
- **Attendance Statistics**: Generate attendance reports and summaries
- **Configurable Settings**: Church-specific attendance settings

## API Endpoints

### Attendance Records (`/api/attendance/records/`)

#### GET `/api/attendance/records/`
- **Description**: List all attendance records for the user's church
- **Permissions**: Authenticated users (church members and staff)
- **Response**: List of attendance records with basic information

#### GET `/api/attendance/records/{id}/`
- **Description**: Get detailed attendance record including member attendances
- **Permissions**: Authenticated users (church members and staff)
- **Response**: Detailed attendance record with individual member attendance

#### POST `/api/attendance/records/`
- **Description**: Create a new attendance record
- **Permissions**: Church admin, pastor, usher
- **Request Body**: Attendance record data

#### POST `/api/attendance/records/{id}/attendance_summary/`
- **Description**: Get attendance summary for a specific record
- **Permissions**: Authenticated users
- **Response**: Attendance statistics and summary

### Quick Attendance Actions

#### POST `/api/attendance/records/quick_mark_attendance/`
- **Description**: Quick bulk attendance marking for ushers
- **Permissions**: Usher, admin, pastor
- **Request Body**:
```json
{
    "member_ids": ["uuid1", "uuid2", "uuid3"],
    "service_date": "2024-01-15",
    "service_id": "service-uuid",
    "notes": "Sunday service"
}
```

#### POST `/api/attendance/records/{id}/mark_member_present/`
- **Description**: Mark a specific member as present
- **Permissions**: Usher, admin, pastor
- **Request Body**:
```json
{
    "member_id": "member-uuid",
    "notes": "Arrived on time"
}
```

### Member Attendance (`/api/attendance/members/`)

#### GET `/api/attendance/members/`
- **Description**: List all member attendance records
- **Permissions**: Authenticated users (church members and staff)

#### POST `/api/attendance/members/`
- **Description**: Create a member attendance record
- **Permissions**: Church admin, pastor, usher

### Service Types (`/api/attendance/service-types/`)

#### GET `/api/attendance/service-types/`
- **Description**: List service types for the church
- **Permissions**: Church admin

#### POST `/api/attendance/service-types/`
- **Description**: Create a new service type
- **Permissions**: Church admin
- **Request Body**:
```json
{
    "name": "Sunday Service",
    "default_start_time": "09:00:00",
    "typical_duration_minutes": 120,
    "is_active": true
}
```

### Attendance Settings (`/api/attendance/settings/`)

#### GET `/api/attendance/settings/`
- **Description**: Get attendance settings for the church
- **Permissions**: Church admin

#### POST `/api/attendance/settings/`
- **Description**: Update attendance settings
- **Permissions**: Church admin
- **Request Body**:
```json
{
    "enable_individual_tracking": true,
    "enable_arrival_departure_times": false,
    "require_usher_approval": true,
    "allow_self_checkin": false,
    "auto_mark_present_threshold_minutes": 15,
    "checkin_window_minutes_before": 30,
    "checkin_window_minutes_after": 60
}
```

## User Roles and Permissions

### Usher Role
- ✅ Can view attendance records for their church
- ✅ Can mark members present for services
- ✅ Can create and manage attendance records
- ✅ Can use quick attendance marking features
- ❌ Cannot modify attendance settings
- ❌ Cannot manage service types

### Admin/Pastor Roles
- ✅ All usher permissions
- ✅ Can modify attendance settings
- ✅ Can manage service types
- ✅ Full access to attendance reports

### Member Role
- ✅ Can view attendance records for their church
- ❌ Cannot modify attendance data
- ❌ Cannot access attendance settings

## Usage Examples

### Quick Attendance Marking (Usher)
```bash
POST /api/attendance/records/quick_mark_attendance/
Authorization: Bearer <usher-token>

{
    "member_ids": ["123e4567-e89b-12d3-a456-426614174000"],
    "service_date": "2024-01-15",
    "service_id": "service-uuid",
    "notes": "Regular Sunday service"
}
```

### Get Attendance Summary
```bash
GET /api/attendance/records/{record-id}/attendance_summary/
Authorization: Bearer <user-token>

Response:
{
    "total_registered": 150,
    "present_count": 142,
    "visitor_count": 8,
    "recorded_totals": {
        "total_attendance": 150,
        "male_attendance": 65,
        "female_attendance": 70,
        "children_attendance": 10,
        "youth_attendance": 5,
        "visitors_count": 8,
        "new_converts": 2
    }
}
```

## Database Models

### AttendanceRecord
- Main attendance record for a specific service
- Tracks total counts by category (male, female, children, youth)
- Links to church, service, and recorded by user

### MemberAttendance
- Individual member attendance tracking
- Tracks presence, arrival/departure times, visitor status
- Links to attendance record and member

### ServiceType
- Configurable service types per church
- Default start times and typical duration

### AttendanceSettings
- Church-specific attendance configuration
- Check-in windows, approval requirements, etc.

## Notes

- All attendance operations are scoped to the user's church
- Ushers can only manage attendance for their assigned church
- Attendance records are unique per church, service, and date
- Member attendance is tracked individually for detailed reporting
