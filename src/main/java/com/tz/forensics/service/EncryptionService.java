package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class EncryptionService {

    private static final String NEW_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    @Value("${app.encryption.key}")
    private String encryptionKey;

    private SecretKeySpec getKey() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive encryption key", e);
        }
    }

    private SecretKeySpec getLegacyKey() {
        byte[] keyBytes = new byte[16];
        byte[] src = encryptionKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        return new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(byte[] data) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(NEW_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(data);
            byte[] output = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        try {
            if (encryptedData != null && encryptedData.length > IV_LENGTH + 16) {
                try {
                    byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
                    byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);
                    Cipher cipher = Cipher.getInstance(NEW_ALGORITHM);
                    cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(TAG_BITS, iv));
                    return cipher.doFinal(ciphertext);
                } catch (Exception ignored) {
                    // Legacy AES/ECB evidence remains readable during migration.
                }
            }
            Cipher legacy = Cipher.getInstance("AES/ECB/PKCS5Padding");
            legacy.init(Cipher.DECRYPT_MODE, getLegacyKey());
            return legacy.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
