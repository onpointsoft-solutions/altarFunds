# OnPoint Pay - Kenya Payment Gateway

A production-ready payment gateway built with Django and Django REST Framework, designed for the Kenyan market with M-Pesa integration and support for card payments.

## 🚀 Features

### Core Payment Features
- **M-Pesa Integration**: Full Daraja API support
  - STK Push payments
  - Transaction status queries
  - C2B confirmation & validation
  - Transaction reversals
- **Card Payments**: Ready for Paystack/Flutterwave integration
- **Unified API**: Single endpoint for multiple payment methods
- **Webhooks**: Reliable webhook delivery with retries
- **Security**: API key authentication, rate limiting, audit logs

### Merchant Features
- **Dashboard**: Complete merchant management interface
- **API Keys**: Secure key management with permissions
- **Transactions**: Full transaction history and reporting
- **Settlements**: Automated settlement processing
- **Compliance**: KYC document management

### Technical Features
- **JWT Authentication**: Secure token-based auth
- **Rate Limiting**: Configurable transaction limits
- **Audit Trail**: Complete audit logging
- **Background Tasks**: Celery for async processing
- **Docker Support**: Containerized deployment

## 📋 Prerequisites

- Python 3.11+
- PostgreSQL 12+
- Redis 6+
- Docker & Docker Compose (optional)

## 🛠️ Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd onpoint_pay
```

### 2. Environment Setup
```bash
# Copy environment file
cp env.example .env

# Edit with your credentials
nano .env
```

### 3. Docker Installation (Recommended)
```bash
# Build and start all services
docker-compose up --build

# Run migrations
docker-compose exec web python manage.py migrate

# Create superuser
docker-compose exec web python manage.py createsuperuser
```

### 4. Manual Installation
```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run migrations
python manage.py migrate

# Create superuser
python manage.py createsuperuser

# Start development server
python manage.py runserver
```

## 🔧 Configuration

### Environment Variables
```bash
# Django Settings
SECRET_KEY=your-secret-key
DEBUG=False
ALLOWED_HOSTS=yourdomain.com

# Database
DB_NAME=onpoint_pay
DB_USER=postgres
DB_PASSWORD=password
DB_HOST=localhost
DB_PORT=5432

# M-Pesa (Safaricom Daraja)
MPESA_CONSUMER_KEY=your-consumer-key
MPESA_CONSUMER_SECRET=your-consumer-secret
MPESA_PASSKEY=your-passkey
MPESA_SHORTCODE=174379

# Callback URLs
MPESA_CALLBACK_URL=https://yourdomain.com/api/v1/payments/mpesa/callback/
MPESA_CONFIRMATION_URL=https://yourdomain.com/api/v1/payments/mpesa/confirmation/
MPESA_VALIDATION_URL=https://yourdomain.com/api/v1/payments/mpesa/validation/

# Card Payments (Paystack)
PAYSTACK_PUBLIC_KEY=your-paystack-public-key
PAYSTACK_SECRET_KEY=your-paystack-secret-key
```

### M-Pesa Setup
1. **Get Daraja API Credentials**:
   - Visit [Safaricom Developer Portal](https://developer.safaricom.co.ke/)
   - Create a new app
   - Note down Consumer Key, Consumer Secret, and Passkey

2. **Configure Callback URLs**:
   - Set your callback URLs in the Daraja portal
   - Use the URLs provided in your `.env` file

3. **Test Integration**:
   - Use the provided Postman collection
   - Start with small test amounts

## 📚 API Documentation

### Base URL
```
Production: https://yourdomain.com/api/v1
Development: http://localhost:8000/api/v1
```

### Authentication
All API endpoints require JWT authentication or API key authentication.

#### JWT Authentication (Merchant Dashboard)
```bash
# Login
POST /auth/login/
{
  "email": "merchant@example.com",
  "password": "password"
}

# Response
{
  "tokens": {
    "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
  },
  "merchant": { ... }
}
```

#### API Key Authentication (Payment APIs)
```bash
# Include in headers
Authorization: Bearer sk_your_secret_key_here
```

### Payment Endpoints

#### Initiate Payment
```bash
POST /payments/initiate/
Content-Type: application/json
Authorization: Bearer sk_your_secret_key

{
  "amount": 100.00,
  "currency": "KES",
  "payment_method": "mpesa",
  "phone": "254712345678",
  "email": "customer@example.com",
  "reference": "ORDER-123",
  "callback_url": "https://yourdomain.com/webhook",
  "metadata": {
    "order_id": "123",
    "customer_name": "John Doe"
  }
}
```

#### Check Payment Status
```bash
POST /payments/status/
{
  "reference": "TXN20240116ABC123",
  "payment_method": "mpesa"
}
```

#### Transaction History
```bash
GET /payments/transactions/
Authorization: Bearer sk_your_secret_key

# Query Parameters
?status=completed&payment_method=mpesa&date_from=2024-01-01&date_to=2024-01-31
```

#### Refund Payment
```bash
POST /payments/refund/
{
  "reference": "TXN20240116ABC123",
  "amount": 50.00,
  "reason": "Customer requested refund"
}
```

### M-Pesa Webhooks

#### STK Push Callback
```bash
POST /payments/mpesa/callback/
# M-Pesa will send transaction results here
```

#### C2B Confirmation
```bash
POST /payments/mpesa/confirmation/
# Receive C2B payment confirmations
```

#### C2B Validation
```bash
POST /payments/mpesa/validation/
# Validate incoming C2B payments
```

## 🔒 Security Features

### API Security
- **API Key Authentication**: Secure key-based authentication
- **IP Whitelisting**: Restrict API access by IP
- **Rate Limiting**: Configurable transaction limits
- **Request Signing**: HMAC signature verification for webhooks
- **Encryption**: All sensitive data encrypted at rest

### Compliance
- **Audit Logging**: Complete audit trail for all operations
- **KYC Management**: Document storage and verification
- **PCI Compliance**: Secure card payment handling
- **Data Protection**: GDPR-compliant data handling

## 🧪 Testing

### Development Testing
```bash
# Run tests
python manage.py test

# Run specific app tests
python manage.py test payments
python manage.py test merchants
```

### M-Pesa Sandbox
Use sandbox credentials for testing:
- Consumer Key: sandbox key
- Consumer Secret: sandbox secret
- Passkey: sandbox passkey
- Shortcode: 174379

## 📊 Monitoring & Logging

### Application Logs
```bash
# View logs
tail -f logs/onpoint_pay.log

# Docker logs
docker-compose logs -f web
```

### Database Monitoring
```bash
# Connect to database
docker-compose exec db psql -U postgres -d onpoint_pay

# Check transactions
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10;
```

## 🚀 Deployment

### Production Deployment
1. **Environment Setup**:
   ```bash
   # Set production variables
   DEBUG=False
   SECURE_SSL_REDIRECT=True
   ALLOWED_HOSTS=yourdomain.com
   ```

2. **Database Migration**:
   ```bash
   python manage.py migrate
   python manage.py collectstatic --noinput
   ```

3. **Process Management**:
   ```bash
   # Start with Gunicorn
   gunicorn --bind 0.0.0.0:8000 --workers 4 onpoint_pay.wsgi:application
   
   # Start Celery workers
   celery -A onpoint_pay worker -l info
   celery -A onpoint_pay beat -l info
   ```

### Docker Production
```bash
# Build production image
docker build -t onpoint-pay:latest .

# Run with environment file
docker run -d --env-file .env -p 8000:8000 onpoint-pay:latest
```

## 📈 Scalability

### Horizontal Scaling
- **Load Balancing**: Multiple web instances behind load balancer
- **Database Replication**: Read replicas for scaling reads
- **Redis Cluster**: Distributed task queue
- **CDN Integration**: Static asset delivery

### Performance Optimization
- **Database Indexing**: Optimized queries with proper indexes
- **Caching Strategy**: Redis for frequently accessed data
- **Async Processing**: Background tasks for heavy operations
- **Connection Pooling**: Database connection management

## 🔧 Maintenance

### Database Maintenance
```bash
# Backup database
pg_dump -U postgres -h localhost onpoint_pay > backup.sql

# Clean old logs
find logs/ -name "*.log" -mtime +30 -delete

# Archive old transactions
python manage.py shell
>>> from payments.models import Transaction
>>> Transaction.objects.filter(created_at__lt=timezone.now()-timezone.timedelta(days=365)).count()
```

### Security Updates
```bash
# Update dependencies
pip install -r requirements.txt --upgrade

# Security scan
pip install safety
safety check -r requirements.txt
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

- **Documentation**: [API Docs](https://docs.onpointpay.com)
- **Support Email**: support@onpointpay.com
- **Status Page**: [status.onpointpay.com](https://status.onpointpay.com)

## 🔄 Changelog

### v1.0.0 (2024-01-16)
- ✅ Initial release
- ✅ M-Pesa STK Push integration
- ✅ JWT authentication system
- ✅ Merchant management
- ✅ Webhook system
- ✅ Audit logging
- ✅ Docker support

---

**Built with ❤️ for the Kenyan fintech ecosystem**
