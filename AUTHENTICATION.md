# E.D.D.I Authentication System

This document describes the comprehensive authentication system implemented for the E.D.D.I project with MongoDB persistence and modern security practices.

## Overview

A complete authentication system with database persistence, CSRF protection, and modern password hashing has been implemented in E.D.D.I. The system provides:

- **User Registration** with email support and form validation
- **User Login** with secure password verification
- **MongoDB Persistence** for user data storage
- **BCrypt Password Hashing** with automatic salt generation
- **CSRF Token Protection** to mitigate CSRF attacks
- **Session Management** for authenticated users
- **Security Filter** to protect endpoints
- **Modern UI** with Bootstrap and responsive design

## Security Architecture

### Password Security
- **Algorithm**: BCrypt with cost factor 12
- **Salt Management**: Automatic unique salt generation per password
- **Format**: `$2a$12$saltAndHashedPassword` (all in one field)
- **Verification**: Secure password verification with BCrypt.checkpw()

### CSRF Protection
- **Purpose**: Prevents Cross-Site Request Forgery attacks
- **Implementation**: Unique CSRF tokens for each form submission
- **Token Lifecycle**: 30-minute expiration with single-use validation
- **Coverage**: All login and registration forms protected

### Session Management
- **Storage**: In-memory session store (consider Redis for production)
- **Expiration**: 1-hour inactivity timeout
- **Cleanup**: Automatic cleanup of expired sessions
- **Security**: Session invalidation on logout

## Database Schema

### User Collection (MongoDB)
```javascript
{
  "_id": ObjectId,
  "username": "string (unique)",
  "passwordHash": "string (BCrypt format: $2a$12$...)",
  "email": "string",
  "createdAt": Date,
  "lastLoginAt": Date,
  "active": boolean
}
```

### Indexes
- **username**: Unique index for fast lookups and constraint enforcement
- **email**: Optional index for email-based queries

## Architecture Components

### Core Authentication
1. **IUserStore** - Interface for user persistence operations
2. **UserStore** - MongoDB implementation following project patterns
3. **User** - Entity model with BCrypt password hash
4. **AuthenticationService** - Enhanced with MongoDB persistence and BCrypt
5. **SecurityUtilities** - Updated with BCrypt methods and legacy compatibility

### Web Endpoints
1. **ILoginEndpoint** - REST interface for authentication
2. **RestLoginEndpoint** - Login implementation with CSRF protection
3. **ISignupEndpoint** - REST interface for user registration
4. **RestSignupEndpoint** - Registration implementation with email support
5. **RestLogoutEndpoint** - Logout with session cleanup
6. **CsrfTokenService** - CSRF token generation and validation

### Frontend
1. **login.html** - Responsive login page with CSRF protection
2. **signup.html** - User registration page with email field
3. **auth-check.js** - Authentication state management
4. **SessionAuthenticationFilter** - JAX-RS security filter

### Data Transfer Objects
1. **SignupRequest** - Registration data with username, password, email
2. **SessionInfo** - Session data with timestamps and expiration

## Database Integration

### MongoDB Pattern
The implementation follows the established MongoDB pattern used throughout E.D.D.I:

```java
@ApplicationScoped
public class UserStore implements IUserStore {
    
    @Inject
    private MongoDatabase database;
    
    @Inject
    private IJsonSerialization jsonSerialization;
    
    @Inject
    private IDocumentBuilder documentBuilder;
    
    private UserResourceStore userResourceStore;
    
    // Inner ResourceStore class following project pattern
    private class UserResourceStore extends ResourceStore<User> {
        // CRUD operations implementation
    }
}
```

### Dependency Injection
```xml
<!-- BCrypt dependency in pom.xml -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

## Password Security Implementation

### BCrypt Usage
```java
// Password hashing (registration/password change)
String hashedPassword = SecurityUtilities.hashPassword(plainPassword);
user.setPasswordHash(hashedPassword); // Stores: $2a$12$salt+hash

// Password verification (login)
boolean isValid = SecurityUtilities.verifyPassword(plainPassword, storedHash);
```

## Configuration

### application.properties

```properties
# Enable custom authentication system
authorization.enabled=true

# MongoDB configuration (required for user persistence)
quarkus.mongodb.database=eddi
quarkus.mongodb.connection-string=mongodb://localhost:27017

# Public paths that don't require authentication
quarkus.http.auth.permission.permit1.paths=/q/metrics/*,/q/health/*,/chat/unrestricted/*,/bots/unrestricted/*,/managedbots/*,/css/*,/js/*,/img/*,/auth/*,/signup/*,/openapi/*,/q/swagger-ui/*
```

### Default Users

The system automatically creates default users on first startup:
- **admin/admin** - Administrator account (admin@eddi.ai)
- **user/user** - Regular user account (user@eddi.ai)

**⚠️ Important**: These are created only if they don't exist in the database. Change default passwords in production.

## API Endpoints

### Authentication Endpoints (`/auth`)

- `GET /auth/login` - Display login page
- `POST /auth/login` - Authenticate user (form data: username, password, csrfToken)
- `GET /auth/csrf-token` - Generate CSRF token

### Registration Endpoints (`/signup`)

- `GET /signup` - Display registration page
- `POST /signup` - Register new user (form data: username, password, email, csrfToken)

### Logout Endpoints (`/logout`)

- `GET /logout/userAuthenticated` - Check if user is authenticated
- `GET /logout/securityType` - Get security configuration type
- `POST /logout` - Logout user and invalidate session

## CSRF Attack Mitigation

### What is CSRF?
Cross-Site Request Forgery (CSRF) is an attack where malicious websites trick users into performing unwanted actions on authenticated websites. For example, a malicious site could submit a form to change a user's password without their knowledge.

### Our CSRF Protection Implementation

1. **Token Generation**: Each form receives a unique, unpredictable CSRF token
2. **Token Validation**: Server validates token before processing any state-changing request
3. **Token Expiration**: Tokens expire after 30 minutes to limit attack window
4. **Single Use**: Each token can only be used once to prevent replay attacks

### CSRF Token Flow
```
1. User visits login/signup page
2. Server generates unique CSRF token
3. Token embedded in hidden form field
4. User submits form with token
5. Server validates token before processing
6. Token is invalidated after use
```

### Protected Operations
- User login
- User registration
- Any form-based data submission
- Session-changing operations

## User Management

### User Registration Process
1. User fills registration form (username, password, email)
2. CSRF token validation
3. Username uniqueness check
4. Password hashed with BCrypt
5. User stored in MongoDB
6. Success confirmation

### Password Requirements
- Minimum length enforced by frontend validation
- BCrypt hashing with cost factor 12 (industry standard)
- Automatic salt generation (unique per password)
- Secure verification without exposing hash details

### Email Integration
- Email field added to user registration
- Stored in user profile for future features
- Can be used for password recovery (future enhancement)
- Optional validation can be added

## Database Operations

### User Store Interface
```java
public interface IUserStore {
    User findUserByUsername(String username) throws ResourceStoreException;
    void createUser(User user) throws ResourceAlreadyExistsException, ResourceStoreException;
    void updateUser(User user) throws ResourceStoreException, ResourceNotFoundException;
    void deleteUser(String username) throws ResourceStoreException;
    void updateLastLogin(String username) throws ResourceStoreException, ResourceNotFoundException;
}
```

### MongoDB Collections
- **Collection Name**: `users`
- **Database**: Configured in application.properties
- **Indexes**: Unique index on username field
- **Connection**: Standard MongoDB connection string

## API Endpoints

### Authentication Endpoints (`/auth`)

- `GET /auth/login` - Display login page
- `POST /auth/login` - Authenticate user (form data: username, password, csrfToken)
- `GET /auth/csrf-token` - Generate CSRF token

### Logout Endpoints (`/logout`)

- `GET /logout/userAuthenticated` - Check if user is authenticated
- `GET /logout/securityType` - Get security configuration type
- `POST /logout` - Logout user and invalidate session

## Frontend Integration

### Modern UI Features
- **Bootstrap 4.6.2** for responsive design
- **Form validation** with HTML5 and JavaScript
- **Error handling** with user-friendly messages
- **Responsive layout** for mobile and desktop
- **CSRF token management** automatic inclusion in forms

### Authentication Check

The `auth-check.js` script automatically:
- Checks authentication status on page load
- Redirects to login if unauthenticated
- Handles logout functionality
- Manages redirect URLs after successful login
- Provides authentication state for frontend applications

### Usage in HTML Pages

```html
<script src="/js/auth-check.js"></script>
```

The script automatically binds to logout buttons with:
- `id="logoutBtn"`
- `class="logout-btn"`
- `data-action="logout"`

### Registration Form Features
```html
<!-- Email field added to registration -->
<input type="email" name="email" required>

<!-- CSRF protection -->
<input type="hidden" name="csrfToken" id="csrfToken">

<!-- Form validation -->
<script>
document.getElementById('signupForm').addEventListener('submit', function(e) {
    // Client-side validation
    // CSRF token injection
    // Error handling
});
</script>
```

## Security Model

### Multi-layered Security

1. **Database Layer**: MongoDB with indexes and constraints
2. **Application Layer**: BCrypt password hashing and validation
3. **Session Layer**: Secure session management with expiration
4. **Request Layer**: CSRF token validation
5. **Transport Layer**: HTTPS recommended for production

### Authentication Flow

```
1. User visits protected resource
2. SessionAuthenticationFilter checks session
3. If not authenticated, redirect to login
4. User submits credentials + CSRF token
5. Server validates CSRF token
6. Server verifies password with BCrypt
7. Session created with authentication state
8. User redirected to original resource
```

### Registration Flow

```
1. User visits signup page
2. CSRF token generated and embedded
3. User fills form (username, password, email)
4. Client-side validation
5. Form submitted with CSRF token
6. Server validates CSRF token
7. Username uniqueness check
8. Password hashed with BCrypt
9. User stored in MongoDB
10. Success response or error handling
```

## Testing

### Unit Tests
The system includes comprehensive unit tests:

```java
@Test
void testAuthenticationService() {
    // Test valid credentials with BCrypt
    assertTrue(authenticationService.authenticateUser("admin", "admin"));
    
    // Test invalid credentials
    assertFalse(authenticationService.authenticateUser("admin", "wrong"));
}

@Test
void testUserRegistration() {
    // Test user creation with email
    authenticationService.addUser("testuser", "testpass", "test@example.com");
    assertTrue(authenticationService.userExists("testuser"));
}

@Test
void testPasswordMigration() {
    // Test automatic migration from SHA-512 to BCrypt
    // Legacy users automatically upgraded on login
}
```

### Integration Testing
- MongoDB connection testing
- CSRF token generation and validation
- Session management and expiration
- Password hashing and verification
- User registration and login flows

## Migration from Legacy System

### Automatic Password Migration
- Existing SHA-512 passwords automatically detected
- Migration occurs during successful login
- No user action required
- Backward compatibility maintained
- Gradual transition to BCrypt

### Database Migration
- New User collection created automatically
- Existing users (if any) can be migrated
- Index creation on first startup
- No downtime required for migration

## Customization

### Adding New Protected Paths

Update `SessionAuthenticationFilter.PUBLIC_PATHS` to modify which paths require authentication:

```java
private static final Set<String> PUBLIC_PATHS = Set.of(
    "/auth/", "/signup/", "/css/", "/js/", "/img/",
    "/q/metrics/", "/q/health/", "/openapi/", "/q/swagger-ui/"
);
```

### Custom User Store Implementation

The `IUserStore` interface allows custom implementations:

```java
@ApplicationScoped
public class CustomUserStore implements IUserStore {
    // Implement with your preferred backend:
    // - LDAP/Active Directory
    // - External database
    // - REST API integration
    // - OAuth providers
}
```

### BCrypt Configuration

Adjust BCrypt cost factor for security vs. performance:

```java
public static String hashPassword(String password) {
    // Cost factor 12 = good security/performance balance
    // Higher = more secure but slower
    // Lower = faster but less secure
    return BCrypt.hashpw(password, BCrypt.gensalt(12));
}
```

### UI Customization

The authentication pages use Bootstrap and can be customized:

- **login.html**: `/META-INF/resources/login.html`
- **signup.html**: `/META-INF/resources/signup.html`
- **CSS**: Embedded styles or external stylesheets
- **JavaScript**: Form validation and AJAX handling
- **Branding**: Logo, colors, and messaging

### Database Configuration

MongoDB configuration can be customized:

```properties
# Database name
quarkus.mongodb.database=your_database_name

# Connection string (production example)
quarkus.mongodb.connection-string=mongodb://username:password@host:port/database

# Connection pool settings
quarkus.mongodb.max-pool-size=20
quarkus.mongodb.min-pool-size=5
```

## Production Deployment

### Security Checklist

1. **Change Default Users**
   ```bash
   # Remove or change default admin/user accounts
   # Use strong passwords
   # Consider removing default users entirely
   ```

2. **HTTPS Configuration**
   ```properties
   # Force HTTPS in production
   quarkus.http.insecure-requests=redirect
   quarkus.http.ssl-port=8443
   ```

3. **MongoDB Security**
   ```properties
   # Use authentication
   quarkus.mongodb.connection-string=mongodb://user:pass@host:port/db?authSource=admin
   
   # Use SSL/TLS
   quarkus.mongodb.connection-string=mongodb://host:port/db?ssl=true
   ```

4. **Session Configuration**
   ```properties
   # Session timeout (production values)
   quarkus.http.session.timeout=PT30M
   
   # Secure session cookies
   quarkus.http.session.cookie.secure=true
   quarkus.http.session.cookie.http-only=true
   ```

5. **Password Policy**
   - Implement minimum password length
   - Require password complexity
   - Consider password expiration
   - Add account lockout after failed attempts

### Performance Considerations

- **Session Storage**: Consider Redis for distributed sessions
- **Database Indexing**: Ensure proper MongoDB indexes
- **Connection Pooling**: Configure appropriate pool sizes
- **BCrypt Cost**: Balance security vs. performance (cost factor 10-12)
- **CSRF Token Cleanup**: Implement token cleanup for memory management

### Monitoring and Logging

```properties
# Enable authentication logging
quarkus.log.category."ai.labs.eddi.engine.security".level=INFO

# Monitor failed login attempts
quarkus.log.category."ai.labs.eddi.engine.security.AuthenticationService".level=DEBUG
```

### Backup and Recovery

- **User Data**: Regular MongoDB backups
- **Session Data**: Consider persistence for session recovery
- **Configuration**: Version control for configuration changes
- **Disaster Recovery**: Test authentication system recovery procedures

## Development Workflow

### Local Development Setup

1. **Start MongoDB**
   ```bash
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

2. **Configure Application**
   ```properties
   quarkus.mongodb.connection-string=mongodb://localhost:27017
   quarkus.mongodb.database=eddi_dev
   authorization.enabled=true
   ```

3. **Run Application**
   ```bash
   ./mvnw quarkus:dev
   ```

4. **Test Authentication**
   - Visit `http://localhost:7070`
   - Should redirect to login page
   - Use admin/admin or user/user
   - Test registration with new users

### Testing Authentication Features

```bash
# Test login endpoint
curl -X POST http://localhost:7070/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&csrfToken=TOKEN"

# Test registration endpoint  
curl -X POST http://localhost:7070/signup \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=newuser&password=newpass&email=user@example.com&csrfToken=TOKEN"

# Test CSRF token generation
curl http://localhost:7070/auth/csrf-token
```

## Troubleshooting

### Common Issues

1. **MongoDB Connection Errors**
   ```
   Error: Failed to connect to MongoDB
   Solution: Check MongoDB is running and connection string is correct
   ```

2. **CSRF Validation Failed**
   ```
   Error: CSRF token validation failed
   Solution: Ensure CSRF token is properly included in forms and not expired
   ```

3. **BCrypt Hashing Errors**
   ```
   Error: Password hashing failed
   Solution: Check BCrypt dependency is included and cost factor is valid (4-31)
   ```

4. **User Already Exists**
   ```
   Error: User already exists during registration
   Solution: Username must be unique. Check database for existing users
   ```

5. **Session Not Found**
   ```
   Error: Session expired or invalid
   Solution: Check session timeout settings and ensure cookies are enabled
   ```

6. **Redirect Loop**
   ```
   Error: Infinite redirects between login and protected pages
   Solution: Verify `/auth/*` and `/signup/*` paths are in PUBLIC_PATHS
   ```

### Debug Logging

Enable detailed logging for troubleshooting:

```properties
# Authentication service debugging
quarkus.log.category."ai.labs.eddi.engine.security.AuthenticationService".level=DEBUG

# User store debugging  
quarkus.log.category."ai.labs.eddi.engine.security.mongo.UserStore".level=DEBUG

# CSRF token debugging
quarkus.log.category."ai.labs.eddi.engine.security.CsrfTokenService".level=DEBUG

# Session filter debugging
quarkus.log.category."ai.labs.eddi.engine.security.SessionAuthenticationFilter".level=DEBUG

# MongoDB operations
quarkus.log.category."org.mongodb.driver".level=INFO
```

### Database Debugging

```javascript
// MongoDB queries for debugging

// Check users collection
db.users.find({})

// Check for duplicate usernames
db.users.find({username: "admin"})

// Verify indexes
db.users.getIndexes()

// Check password hash format
db.users.findOne({username: "admin"}, {passwordHash: 1})
```

### Password Migration Issues

```java
// If legacy password migration fails:
// 1. Check log for migration errors
// 2. Verify old password format (salt:hash)
// 3. Ensure BCrypt dependency is available
// 4. Test password manually:

String legacyPassword = "salt:hashedPassword";
String[] parts = legacyPassword.split(":", 2);
String salt = parts[0];
String hash = parts[1];
// Verify legacy format works before migration
```

## Performance Monitoring

### Key Metrics to Monitor

1. **Authentication Response Time**
   - Login endpoint latency
   - Password hashing duration
   - Database query performance

2. **Session Management**
   - Active session count
   - Session cleanup frequency
   - Memory usage for session storage

3. **Database Performance**
   - MongoDB connection pool usage
   - Query execution time
   - Index effectiveness

4. **Security Metrics**
   - Failed login attempt rate
   - CSRF token validation failures
   - Session expiration rate

### Application Health Checks

```java
// Custom health check for authentication system
@ApplicationScoped
public class AuthHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Test database connectivity
            userStore.findUserByUsername("admin");
            
            // Test password hashing
            SecurityUtilities.hashPassword("test");
            
            return HealthCheckResponse.up("Authentication system healthy");
        } catch (Exception e) {
            return HealthCheckResponse.down("Authentication system error: " + e.getMessage());
        }
    }
}
```

## Future Enhancements

### Planned Features

1. **Email Verification**
   - Email confirmation during registration
   - Email-based password recovery
   - Email change verification

2. **Two-Factor Authentication**
   - TOTP (Time-based One-Time Password)
   - SMS verification
   - Backup codes

3. **Advanced Password Policies**
   - Configurable password requirements
   - Password history tracking
   - Account lockout after failed attempts

4. **Audit Logging**
   - Authentication event logging
   - User activity tracking
   - Security incident monitoring

5. **Social Login Integration**
   - Google OAuth
   - GitHub authentication
   - Microsoft Azure AD

6. **API Token Authentication**
   - JWT token support
   - API key management
   - Token refresh mechanisms

### Migration Considerations

- **Session Storage**: Move from in-memory to Redis
- **Password Policies**: Add configurable validation rules
- **Distributed Systems**: Handle authentication across microservices
- **Compliance**: GDPR, SOX, and other regulatory requirements

## License and Credits

This authentication system is part of the E.D.D.I project and follows the Apache 2.0 license.

### Dependencies
- **BCrypt**: Password hashing (jbcrypt 0.4)
- **MongoDB**: User data persistence
- **Bootstrap**: Frontend UI framework
- **Quarkus**: Application framework
- **JAX-RS**: REST API implementation

### Security Best Practices
This implementation follows OWASP guidelines for:
- Password storage
- Session management  
- CSRF protection
- Input validation
- Error handling
