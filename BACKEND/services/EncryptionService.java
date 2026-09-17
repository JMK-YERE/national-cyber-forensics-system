package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${app.encryption.key}")
    private String encryptionKey;

    private Key getKey() {
        byte[] keyBytes = new byte[16];
        byte[] src = encryptionKey.getBytes();
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
