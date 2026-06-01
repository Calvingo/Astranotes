package com.astraNotes.encryption;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Manages encryption/decryption and HMAC computation for notes.
 * REQ-SEC-1: Encryption at rest
 * REQ-SEC-2: Integrity checks (HMAC)
 */
public class EncryptionManager {
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_SIZE = 256;
    private static final int ITERATIONS = 65536;
    private static final byte[] SALT = "AstraNotesSalt".getBytes(StandardCharsets.UTF_8);
    private static final int GCM_IV_LENGTH = 12;

    private SecretKey rootKey;
    private boolean unlocked;

    public EncryptionManager() {
        this.unlocked = false;
    }

    /**
     * Unlock the encryption manager with a password.
     * Derives a key from the password using PBKDF2WithHmacSHA256.
     * REQ-SEC-3: Access control (password-based unlock)
     */
    public boolean unlock(String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.rootKey = new SecretKeySpec(keyBytes, "AES");
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
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, rootKey, spec);
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
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
            ByteBuffer buffer = ByteBuffer.wrap(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, rootKey, spec);
            byte[] decrypted = cipher.doFinal(cipherText);
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
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(rootKey.getEncoded(), HMAC_ALGORITHM);
            mac.init(keySpec);
            mac.update(noteId.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) ':');
            mac.update(body.getBytes(StandardCharsets.UTF_8));
            return mac.doFinal();
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
     * Return the root key bytes as a hex string for use with SQLCipher PRAGMA key (x'...').
     * Returns null if locked.
     */
    public String getRootKeyHex() {
        if (!unlocked || rootKey == null) return null;
        byte[] kb = rootKey.getEncoded();
        StringBuilder sb = new StringBuilder(kb.length * 2);
        for (byte b : kb) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
