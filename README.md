VaultMind is a private digital vault designed to keep your notes, passwords, and expenses in one secure place. It is built for users who want a clean app experience without giving up strong protection for sensitive information.

All stored vault data is encrypted locally before it reaches the database. The app uses an encrypted Room-backed storage layer, with AES-GCM protecting the actual data and Android Keystore handling the encryption key securely on the device.

Access to the app is protected with a password-based lock screen. The password is verified with PBKDF2 hashing, so the app does not store plain-text credentials, and it requires authentication again whenever the app is reopened or returned from the background.

VaultMind combines security and simplicity in a lightweight, modern interface. It is meant to give you quick access to your personal data while keeping everything encrypted, isolated, and difficult to access without the correct credentials.
