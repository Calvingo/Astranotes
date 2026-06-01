SQLCipher integration

This project supports an optional SQLCipher-enabled SQLite JDBC driver for full
database file encryption at rest. SQLCipher requires native libraries and a
JDBC build that integrates SQLCipher; these builds are platform-specific.

How to enable (non-destructive):

1. Choose a SQLCipher-enabled JDBC driver distribution matching your OS/arch.
   Examples (non-exhaustive):
   - A vendor-provided sqlite-jdbc build that bundles SQLCipher
   - A platform-specific package that exposes JDBC + SQLCipher native libs

2. Build with the `sqlcipher` profile and pass the artifact coordinates and the
   activation property. Example (replace coordinates with your chosen artifact):

```bash
mvn clean package -Psqlcipher -DuseSqlCipher=true \
  -Dsqlcipher.groupId=com.example \
  -Dsqlcipher.artifactId=sqlite-jdbc-sqlcipher \
  -Dsqlcipher.version=1.0.0
```

3. At runtime, the application will attempt to set the SQLCipher key via:

   PRAGMA key = x'<hex-key>';

   This uses the derived root key from `EncryptionManager`. If SQLCipher is not
   available or the PRAGMA fails, the application will fall back to the
   application-level AES encryption already implemented.

Notes and recommendations:
- Test the chosen driver on your target OS. SQLCipher often requires matching
  native binaries (OpenSSL, etc.).
- For production, prefer packaging a tested SQLCipher-enabled JDBC with your
  installer, or provide clear platform-specific installation instructions.
- Keep key management and user prompts secure. The current `EncryptionManager`
  derives a root key from the user password; consider replacing the KDF with
  PBKDF2/Argon2 for stronger resistance.

If you want, I can:
- Add a concrete tested driver dependency for a specific platform (macOS, Linux,
  Windows) and verify the build, or
- Draft platform-specific packaging steps for SQLCipher native libs.
