# AltarFunds Custom Admin Guide

## Overview

The AltarFunds custom admin interface provides a modern, professional dashboard for super administrators to manage the church giving platform. This guide covers all features and functionality.

## Access

### URLs
- **Standard Django Admin**: `/admin/` (for development)
- **Custom AltarFunds Admin**: `/altar-admin/` (recommended for production)

### Authentication
- Uses Django's built-in authentication system
- Super admin users can access all features
- Role-based permissions for different user types

## Dashboard Features

### 1. Real-time Statistics
- **Total Churches**: Number of registered churches
- **Total Users**: Active user accounts
- **Monthly Giving**: Current month's donation totals
- **Total Transactions**: All-time transaction count

### 2. Growth Metrics
- **Church Growth**: Month-over-month percentage change
- **User Growth**: New user acquisition trends
- **Giving Growth**: Donation volume changes

### 3. Interactive Charts
- **Giving Trends**: Line chart showing monthly donation patterns
- **User Growth**: Bar chart displaying new user registrations
- **Real-time Updates**: Auto-refresh functionality

### 4. Top Performing Churches
- Ranked by total giving amount
- Shows transaction counts
- Growth indicators

### 5. Recent Transactions
- Latest donation activities
- User and church information
- Transaction status and amounts

### 6. System Health Monitoring
- **CPU Usage**: Server processor utilization
- **Memory Usage**: RAM consumption
- **Disk Usage**: Storage utilization
- **Active Sessions**: Current logged-in users

## Model Management

### Churches
- **Basic Info**: Name, code, type, description
- **Contact Details**: Pastor info, email, phone
- **Location**: Address, city, county
- **Status**: Verification, active status
- **Statistics**: Member count, attendance data

### Users
- **Personal Info**: Name, email, phone
- **Permissions**: Role, staff status, groups
- **Activity**: Last login, registration date
- **Church Association**: Linked church information

### Giving Transactions
- **Transaction Details**: Amount, category, method
- **User Info**: Donor information
- **Church Info**: Recipient church
- **Status**: Completed, pending, failed
- **Payment Gateway**: Transaction IDs and responses

### Giving Categories
- **Category Management**: Name, description
- **Tax Settings**: Deductible status
- **Church Assignment**: Church-specific categories
- **Statistics**: Total amounts and transaction counts

### Payments
- **Payment Processing**: Gateway integration
- **Transaction Tracking**: IDs and references
- **Fee Management**: Gateway fees and net amounts
- **Status Monitoring**: Payment completion status

### Devices
- **Device Registration**: Mobile app devices
- **User Association**: Linked user accounts
- **App Versions**: Software version tracking
- **Activity Monitoring**: Last seen and usage

## Advanced Features

### 1. Search Functionality
- **Global Search**: Ctrl/Cmd + K shortcut
- **Real-time Results**: Instant search feedback
- **Multiple Models**: Search across all registered models
- **Smart Filtering**: Type-ahead suggestions

### 2. Data Export
- **Multiple Formats**: CSV, Excel, JSON
- **Custom Filters**: Export filtered datasets
- **Scheduled Reports**: Automated export options
- **Email Delivery**: Send reports via email

### 3. Notifications
- **Real-time Alerts**: System notifications
- **Badge Indicators**: Unread notification counts
- **Auto-refresh**: Periodic updates
- **Sound Alerts**: Optional audio notifications

### 4. Keyboard Shortcuts
- **Ctrl/Cmd + K**: Open search
- **Escape**: Close modals/search
- **Ctrl/Cmd + S**: Save current form
- **Ctrl/Cmd + R**: Refresh dashboard

## Customization

### CSS Variables
The admin interface uses CSS variables for easy theming:

```css
:root {
    --primary-color: #2563eb;
    --secondary-color: #64748b;
    --success-color: #10b981;
    --warning-color: #f59e0b;
    --danger-color: #ef4444;
}
```

### Custom Components
- **Stat Cards**: Animated metric displays
- **Charts**: Interactive data visualizations
- **Tables**: Sortable and filterable data tables
- **Modals**: Custom dialog components

### JavaScript Features
- **Auto-refresh**: Configurable update intervals
- **Data Animation**: Smooth number transitions
- **Tooltips**: Contextual help text
- **Form Validation**: Real-time validation feedback

## Security

### Access Control
- **Role-based Permissions**: Different access levels
- **IP Restrictions**: Optional IP whitelisting
- **Session Management**: Secure session handling
- **Audit Logging**: Complete activity tracking

### Data Protection
- **Field Encryption**: Sensitive data protection
- **Secure Headers**: Security best practices
- **CSRF Protection**: Cross-site request forgery prevention
- **SQL Injection Prevention**: Parameterized queries

## Performance

### Optimization
- **Lazy Loading**: On-demand data loading
- **Caching**: Redis integration for performance
- **Database Indexing**: Optimized queries
- **Asset Compression**: Minified CSS/JS

### Monitoring
- **Performance Metrics**: Response time tracking
- **Error Logging**: Comprehensive error tracking
- **Health Checks**: System status monitoring
- **Alert System**: Performance alerts

## Troubleshooting

### Common Issues

1. **Dashboard Not Loading**
   - Check static file configuration
   - Verify template paths
   - Check browser console for errors

2. **Charts Not Displaying**
   - Ensure Chart.js is loaded
   - Check data format
   - Verify canvas elements

3. **Search Not Working**
   - Check search endpoint configuration
   - Verify URL patterns
   - Check JavaScript console

4. **Permissions Issues**
   - Verify user roles
   - Check admin site registration
   - Review permission assignments

### Debug Mode
Enable debug mode for development:

```python
DEBUG = True
ADMIN_TOOLBAR = True
```

### Logging
Configure logging for troubleshooting:

```python
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'handlers': {
        'file': {
            'level': 'INFO',
            'class': 'logging.FileHandler',
            'filename': 'admin.log',
        },
    },
    'loggers': {
        'admin_management': {
            'handlers': ['file'],
            'level': 'INFO',
            'propagate': True,
        },
    },
}
```

## Deployment

### Production Settings
1. Set `DEBUG = False`
2. Configure `ALLOWED_HOSTS`
3. Set up static file serving
4. Configure database settings
5. Set up SSL certificates

### Static Files
```bash
python manage.py collectstatic --noinput
```

### Database Migrations
```bash
python manage.py migrate
```

### Create Super Admin
```bash
python manage.py createsuperuser
```

## Support

### Documentation
- **API Documentation**: `/api/docs/`
- **Admin Guide**: This document
- **Code Comments**: Inline documentation

### Contact
- **Technical Support**: admin@altarfunds.com
- **Feature Requests**: features@altarfunds.com
- **Bug Reports**: bugs@altarfunds.com

## Updates

### Version History
- **v2.0.0**: Complete redesign with modern UI
- **v1.5.0**: Added dashboard analytics
- **v1.0.0**: Initial release

### Future Features
- **Advanced Analytics**: AI-powered insights
- **Mobile Admin**: Responsive mobile interface
- **API Integration**: Third-party service connections
- **Workflow Automation**: Automated admin tasks

---

*Last updated: January 2024*
