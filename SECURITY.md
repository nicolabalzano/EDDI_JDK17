# Security Mitigations

This document outlines the security mitigations implemented in this project.

## CVE Mitigations

### CVE-2022-31129 - Moment.js ReDoS Vulnerability

**Status**: ✅ **MITIGATED**

- **Vulnerability**: Inefficient regular expression complexity in moment.js parsing
- **Affected versions**: moment.js >= 2.18.0, < 2.29.4
- **Current version**: moment.js 2.29.4 (patched)
- **Additional protection**: Implemented `eddi.safeMoment()` wrapper function that:
  - Limits input string length to 200 characters
  - Logs warnings for oversized inputs
  - Prevents ReDoS attacks on date parsing

**Location**: `src/main/resources/META-INF/resources/js/dashboard.js`

### CVE-2024-6531 - Bootstrap XSS Vulnerability

**Status**: ✅ **MITIGATED**

- **Vulnerability**: XSS vulnerability in Bootstrap carousel component
- **Affected versions**: Bootstrap >= 4.0.0, <= 4.6.2
- **Current version**: Bootstrap 5.3.6 (patched)
- **Migration**: Updated from Bootstrap 4.6.2 to 5.3.6

**Files updated**:
- `src/main/resources/META-INF/resources/css/bootstrap-5.3.6.min.css`
- `src/main/resources/META-INF/resources/js/bootstrap-5.3.6.bundle.min.js`
- All HTML templates updated to use new Bootstrap version

### AES-CBC Fixed IV Vulnerability

**Status**: ✅ **MITIGATED**

- **Vulnerability**: Use of fixed IV (Initialization Vector) in AES-CBC encryption
- **Risk**: Identical plaintexts produce identical ciphertexts, enabling pattern analysis
- **Solution**: Implemented random IV generation for each encryption operation
- **Backward compatibility**: Legacy data can still be decrypted using fallback method

**Implementation details**:
- Each encryption now generates a cryptographically secure random IV
- IV is prepended to ciphertext before Base64 encoding
- Legacy decryption method available for existing encrypted data
- Migration path documented in `CRYPTO_MIGRATION.md`

**Location**: `src/main/java/ai/labs/eddi/configs/backup/RestGitBackupStore.java`

**Environment Variables**:
- `EDDI_GIT_AES_KEY`: Base64-encoded 32-byte AES key (required)
- `EDDI_GIT_AES_IV`: Legacy IV for backward compatibility (temporary)

## Content Security Policy (CSP)

**Status**: ✅ **IMPLEMENTED**

- Removed `unsafe-inline` and `unsafe-eval` from CSP directives
- Externalized all inline scripts to separate JS files
- Implemented strict CSP headers in `application.properties`

**Configuration**: `src/main/resources/application.properties`

## Security Headers

The following security headers are implemented:

- `Content-Security-Policy`: Strict policy without unsafe directives
- `X-Content-Type-Options`: nosniff
- `X-Frame-Options`: DENY
- `X-XSS-Protection`: 1; mode=block
- `Referrer-Policy`: strict-origin-when-cross-origin
- `Permissions-Policy`: Restricted geolocation, microphone, camera

## Best Practices

1. **Input Validation**: All user inputs are validated and sanitized
2. **CSRF Protection**: CSRF tokens implemented for all forms
3. **HTML Escaping**: All dynamic content is HTML-escaped
4. **SQL Injection Prevention**: Prepared statements used for all database queries
5. **Date Input Sanitization**: Length-limited date strings to prevent ReDoS

## Monitoring

- Security headers are applied to all endpoints including health checks
- Input validation logging is implemented
- CSRF token validation is enforced

---

**Last Updated**: December 2024  
**Next Review**: June 2025
