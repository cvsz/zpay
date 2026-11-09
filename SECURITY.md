# ZWallet Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | ✅ Yes             |

## Reporting a Vulnerability

If you discover a security vulnerability in ZWallet, please report it responsibly:

1. **Email**: security@zeaz.dev
2. **GPG**: Use the public key available at https://zeaz.dev/security.gpg
3. **Do not** create public GitHub issues for security vulnerabilities

We aim to respond within 48 hours and provide a fix within 30 days for critical vulnerabilities.

## Security Architecture

### Private Key Management
- **Hardware-Backed Storage**: All private keys are encrypted using Android Keystore with hardware-backed AES-GCM
- **Never Plaintext**: Private key material is never stored in plaintext, SharedPreferences, or unencrypted databases
- **Secure Enclave**: Keys are generated and stored in the device's TrustZone/StrongBox
- **Biometric Gate**: All sensitive operations (decryption, transactions) require biometric authentication

### Encryption Standards
- **Algorithm**: AES-256-GCM for symmetric encryption
- **Key Derivation**: PBKDF2 with 100,000 iterations for password-based encryption
- **Random Generation**: SecureRandom for all nonces, IVs, and key material
- **Transport**: TLS 1.3 for all network communications

### Database Security
- **SQLCipher**: Room database encrypted with SQLCipher (256-bit AES)
- **Minor Units**: All financial values stored as Long minor units (satoshis/cents) to prevent floating-point errors
- **Idempotency**: All transactions use idempotency keys to prevent replay attacks
- **Schema Migrations**: Versioned migrations with data conversion, no destructive fallbacks

### Network Security
- **Certificate Pinning**: Network requests pinned to expected certificates
- **No Cleartext**: All API communication over HTTPS only
- **Request Signing**: Mutating requests signed with device-bound keys

### Biometric Authentication
- **Auth-Per-Use**: BiometricPrompt with CRYPTOGRAPHIC_OPERATION for each sensitive action
- **Invalidation**: Biometric keys invalidated on biometric enrollment changes
- **Fallback**: PIN/password fallback only after biometric failure, never as primary

### Cloud Backup (Encrypted)
- **E2EE**: End-to-end encryption with user-derived keys before cloud upload
- **KMS**: AWS KMS envelope encryption for additional protection
- **Zero-Knowledge**: Server cannot decrypt backup contents

### DApp Browser Security
- **Origin Isolation**: Each DApp runs in isolated WebView context
- **Permission Model**: Explicit user consent for each provider method
- **CSP**: Content Security Policy enforced on injected provider

### AI Trading Safety
- **Confirmation Required**: All automated trades require explicit per-trade user confirmation
- **Rate Limits**: Maximum trades per hour/day enforced
- **Amount Caps**: Configurable maximum trade amounts per rule

## Secure Development Practices

### Code Review
- All changes require security review for crypto/auth/storage modifications
- Static analysis (detekt, lint) enforced in CI/CD
- Dependency scanning for known vulnerabilities (OWASP Dependency Check)

### Testing
- Unit tests for all cryptographic operations
- Integration tests for biometric flows
- Penetration testing before major releases

### Secrets Management
- No secrets in source code
- Gradle Secrets Plugin for API keys
- Environment-specific configuration via .env files (gitignored)

### Supply Chain
- Pinned dependency versions in libs.versions.toml
- Verified checksums for all dependencies
- Minimal dependency footprint

## Compliance

### Google Play Store
- Financial app policy compliance
- No crypto mining or unauthorized financial services
- Transparent fee disclosure

### Apple App Store
- Guideline 3.1.1 compliance for in-app purchases
- No unauthorized financial products

### Regulatory
- No KYC/AML in wallet (non-custodial)
- User retains full control of private keys
- Tax documentation export for user compliance

## Incident Response

1. **Detection**: Automated alerts for anomalous transactions, failed biometrics, decryption failures
2. **Containment**: Remote key revocation, app update deployment
3. **Eradication**: Root cause analysis, patch deployment
4. **Recovery**: User notification, key rotation guidance
5. **Lessons Learned**: Post-incident review, policy updates

## Contact

Security Team: security@zeaz.dev
GPG Fingerprint: Available at https://zeaz.dev/security.gpg