# iCastar Platform

A comprehensive talent management platform connecting artists and recruiters in the entertainment industry.

## 🎯 Overview

iCastar is a centralized platform that enables seamless job postings, applications, live auditions, subscriptions, messaging, and payment handling for artists and recruiters in the entertainment industry. The platform features a dynamic artist type system that supports multiple artist categories with type-specific fields.

## ✨ Key Features

### 🎭 Dynamic Artist & Recruiter System
- **Multiple Artist Types**: Actor, Dancer, Singer, Director, Writer, DJ/RJ, Band, Model, Photographer, Videographer
- **Multiple Recruiter Types**: Production House, Casting Director, Individual Recruiter
- **Custom Fields**: Each artist and recruiter type has unique fields (e.g., height for actors, dance styles for dancers)
- **Extensible**: Easy addition of new artist or recruiter types without code changes
- **Searchable**: Advanced search with type-specific filters

### 🔐 Authentication & Security
- OTP-based mobile login
- JWT token-based session management
- Role-based access control (Admin/Artist/Recruiter)
- Account verification and management

### 💼 Job Management
- Job posting with detailed requirements
- Application tracking and management
- Live audition scheduling
- Job boosting for visibility

### 💬 Communication
- Real-time messaging between artists and recruiters
- Paid message unlocking for verified artists
- File sharing and attachments
- Read receipts and timestamps

### 💳 Subscription & Payments
- Tiered subscription plans (Free, Premium, Enterprise) for Artists, Recruiters, and unified plans.
- In-app payment handling via Razorpay/Stripe
- Commission tracking and management
- Invoice generation and billing history

### 📊 Admin Panel
- User management and verification
- Job moderation and visibility control
- Analytics and reporting
- Audit logging and compliance

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8.0
- **Security**: Spring Security, JWT
- **Real-time**: WebSocket
- **File Storage**: AWS S3 (placeholder)
- **Payments**: Razorpay, Stripe (placeholders)
- **Notifications**: Firebase FCM (placeholder)
- **Email**: SMTP (placeholder)

### Project Structure
```
src/main/java/com/icastar/platform/
├── config/                 # Configuration classes
├── controller/             # REST controllers
├── dto/                   # Data Transfer Objects
├── entity/                # JPA entities
├── repository/            # Data access layer
├── service/               # Business logic
├── security/              # Security configuration
├── exception/             # Exception handling
└── util/                  # Utility classes
```

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+

### Quick Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd icastar-main
   ```

2. **Database Setup**
   ```bash
   # Create database and schema
   mysql -u root -p
   CREATE DATABASE icastar_db;
   USE icastar_db;
   source database-schema.sql;
   
   # Run setup scripts
   ./setup-artist-types-manual.sh
   ./setup-recruiter-system.sh
   ./setup-account-management.sh
   ./setup-subscription-system.sh
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8080/api`
   - Admin Panel: `http://localhost:8080/api/admin`
   - H2 Console (dev): `http://localhost:8080/api/h2-console`

## 🗄️ Database Schema

### Core Tables
- `users` - User accounts and authentication
- `artist_profiles` - Artist profile information
- `recruiter_profiles` - Recruiter/company profiles
- `job_posts` - Job postings
- `job_applications` - Job applications
- `auditions` - Audition scheduling and management
- `messages` - Real-time messaging
- `subscriptions` - Subscription management
- `payments` - Payment tracking

### Dynamic Artist System Tables
- `artist_types` - Artist type definitions (Actor, Dancer, etc.)
- `artist_type_fields` - Field definitions for each artist type
- `artist_profile_fields` - Dynamic field values for artist profiles

### Dynamic Recruiter System Tables
- `recruiter_categories` - Recruiter type definitions (Production House, etc.)
- `recruiter_category_fields` - Field definitions for each recruiter type
- `recruiter_profile_fields` - Dynamic field values for recruiter profiles

### Supporting Tables
- `subscription_plans` - Available subscription plans
- `otps` - OTP management for authentication
- `audit_logs` - System audit trail
- `notifications` - User notifications

## 🎨 Artist Types & Fields

The platform supports multiple artist types with their specific fields. The `Dancer` type is fully configured with sample data.

### 🎬 Actor
- Height, Weight, Body Type
- Hair Color, Eye Color, Skin Tone
- Languages, Acting Experience
- Special Skills, Demo Reel, Headshots

### 💃 Dancer ⭐ (Fully Configured)
- **Required**: Dance Styles, Training Background, Performance Experience, Performance Videos
- **Optional**: Choreography Skills, Teaching Experience, Flexibility Level, Performance Types
- **Additional**: Awards & Recognition, Availability, Travel Willingness, Costume Availability

... and so on for other artist types.

## 🏢 Recruiter Categories

### 1. Production House 🎬
- **Basic Information**: Name of Production House, Name of Recruiter, Location, Years in business
- **Advanced Information**: Mobile Number Verification, Email ID, ID Proof, Registration Certificate

### 2. Casting Director 🎭
- **Basic Information**: Name of Recruiter, Location, Name of the Production House
- **Advanced Information**: Mobile Number Verification, Casting Director Card/ID Proof, Face Verification

### 3. Individual Recruiter 👤
- **Basic Information**: Name, Location, Email ID, Contact Details
- **Advanced Information**: ID Proof, Face Verification

## 🔌 API Endpoints

### Public APIs
- `GET /api/public/artist-types` - Get all artist types
- `GET /api/public/artist-types/{id}/fields` - Get artist type fields

### Authentication
- `POST /api/auth/register/verification/request` - Start signup and send an email OTP
- `POST /api/auth/register/verification/verify` - Verify the signup email OTP and create the account
- `POST /api/auth/register/verification/resend` - Resend the signup email OTP (`channel: "email"`)
- `POST /api/auth/login/otp/request` - Check credentials and send email login OTP
- `POST /api/auth/login/otp/verify` - Verify login OTP and receive a JWT
- `POST /api/auth/login/otp/resend` - Resend login OTP

Legacy `/api/auth/login`, `/api/auth/register`, `/api/auth/otp/send`, and
`/api/auth/otp/verify` no longer issue credentials or create accounts; use the
verification flow above. Configure `MSG91_AUTH_KEY` and `MSG91_OTP_TEMPLATE_ID`
on the backend only if SMS delivery is needed for other application features.

### Artist Management
- `GET /api/artists/profile` - Get artist profile
- `POST /api/artists/profile` - Create artist profile
- `PUT /api/artists/profile` - Update artist profile
- `GET /api/artists/search` - Search artists

### Job Management
- `GET /api/jobs` - Get job posts
- `POST /api/jobs` - Create job post
- `GET /api/jobs/search` - Search jobs

### Admin APIs
- `GET /api/admin/users` - User management
- `GET /api/admin/jobs` - Job management
- `GET /api/admin/analytics` - Analytics

## 🔧 Configuration

### Environment Variables
```bash
# Database
DB_USERNAME=root
DB_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# AWS S3 (Placeholder)
AWS_S3_BUCKET=icastar-uploads
AWS_REGION=us-east-1

# Razorpay (Placeholder)
RAZORPAY_KEY_ID=your-key-id
RAZORPAY_KEY_SECRET=your-key-secret

# Stripe (Placeholder)
STRIPE_PUBLIC_KEY=your-public-key
STRIPE_SECRET_KEY=your-secret-key

# Firebase (Placeholder)
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_SERVICE_ACCOUNT_PATH=path-to-service-account.json
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ArtistServiceTest

# Run with coverage
mvn test jacoco:report
```

## 📈 Monitoring & Logging

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Logging**: Configured in `application.yml`
- **Audit Logs**: All admin actions are logged
- **Performance**: Database query optimization with indexes

## 🔒 Security Features

- JWT-based authentication
- Role-based authorization
- Input validation and sanitization
- SQL injection prevention
- XSS protection
- Rate limiting
- Audit logging

## 🚀 Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t icastar-platform .

# Run with Docker Compose
docker-compose up -d
```

### Production Considerations
- Use environment-specific configuration
- Set up proper database connection pooling
- Configure SSL/TLS certificates
- Set up monitoring and alerting
- Implement backup strategies
- Use CDN for static assets

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the API documentation
- Review the database schema

## 🔮 Future Enhancements

- Mobile app development
- Advanced analytics dashboard
- AI-powered job matching
- Video interview integration
- Multi-language support
- Advanced reporting features
- Integration with more payment gateways
- Enhanced notification system

---

**Note**: This implementation includes placeholders for third-party services (AWS S3, Razorpay, Stripe, Firebase, SMTP). These need to be configured with actual credentials and implementations for production use.
