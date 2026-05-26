package com.astraNotes.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Manages encryption/decryption and HMAC computation for notes.
 * REQ-SEC-1: Encryption at rest
 * REQ-SEC-2: Integrity checks (HMAC)
 */
public class EncryptionManager {
    private static final String CIPHER_ALGORITHM = "AES";
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final int KEY_SIZE = 256;

    private SecretKey rootKey;
    private boolean unlocked;

    public EncryptionManager() {
        this.unlocked = false;
    }

    /**
     * Unlock the encryption manager with a password.
     * Derives a key from the password using PBKDF2-like approach.
     * REQ-SEC-3: Access control (password-based unlock)
     */
    public boolean unlock(String password) {
        try {
            byte[] salt = "AstraNotesSalt".getBytes(StandardCharsets.UTF_8);
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = deriveKey(passwordBytes, salt, 256);
            this.rootKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, CIPHER_ALGORITHM);
            this.unlocked = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lock the encryption manager by clearing the root key.
     */
    public void lock() {
        this.rootKey = null;
        this.unlocked = false;
    }

    /**
     * Encrypt plaintext using AES.
     */
    public byte[] encrypt(String plaintext) throws EncryptionException {
        if (!unlocked || rootKey == null) {
            throw new EncryptionException("Encryption manager not unlocked");
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, rootKey);
            return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt ciphertext using AES.
     */
    public String decrypt(byte[] ciphertext) throws EncryptionException {
        if (!unlocked || rootKey == null) {
            throw new EncryptionException("Encryption manager not unlocked");
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rootKey);
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Compute HMAC for note integrity verification.
     */
    public byte[] computeHMAC(String noteId, String body) throws EncryptionException {
        if (!unlocked || rootKey == null) {
            throw new EncryptionException("Encryption manager not unlocked");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            String combined = noteId + ":" + body;
            digest.update(combined.getBytes(StandardCharsets.UTF_8));
            digest.update(rootKey.getEncoded());
            return digest.digest();
        } catch (Exception e) {
            throw new EncryptionException("HMAC computation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify note integrity using HMAC.
     */
    public boolean verifyHMAC(String noteId, String body, byte[] expectedHmac) throws EncryptionException {
        byte[] computed = computeHMAC(noteId, body);
        return Arrays.equals(computed, expectedHmac);
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    /**
     * Simple key derivation function (simplified PBKDF2-like approach).
     * In production, use proper PBKDF2 library.
     */
    private byte[] deriveKey(byte[] password, byte[] salt, int keyLength) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        byte[] combined = new byte[password.length + salt.length];
        System.arraycopy(password, 0, combined, 0, password.length);
        System.arraycopy(salt, 0, combined, password.length, salt.length);

        byte[] result = new byte[keyLength / 8];
        byte[] hash = combined;
        for (int i = 0; i < 1000; i++) {
            digest.update(hash);
            hash = digest.digest();
            int copyLength = Math.min(hash.length, result.length - i * hash.length);
            if (copyLength > 0) {
                System.arraycopy(hash, 0, result, i * hash.length, copyLength);
            }
        }
        return result;
    }
}
