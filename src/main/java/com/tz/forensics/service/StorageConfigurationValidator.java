package com.tz.forensics.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageConfigurationValidator {
    @Value("${app.storage.provider:local}") private String provider;
    @Value("${app.storage.production-required:${PRODUCTION_STORAGE_REQUIRED:false}}") private boolean productionRequired;
    @Value("${app.storage.s3.bucket:}") private String bucket;
    @Value("${app.encryption.key:}") private String encryptionKey;

    @PostConstruct
    public void validate() {
        String normalized = provider == null ? "local" : provider.trim().toLowerCase();
        if (!"local".equals(normalized) && !"s3".equals(normalized)) {
            throw new IllegalStateException("Unsupported EVIDENCE_STORAGE_PROVIDER. Use 'local' or 's3'.");
        }
        if (!productionRequired) return;

        if ("local".equals(normalized)) {
            throw new IllegalStateException(
                    "Production evidence storage is enabled, but EVIDENCE_STORAGE_PROVIDER=local. "
                    + "Configure S3-compatible object storage before enabling production mode.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Production evidence storage requires EVIDENCE_STORAGE_BUCKET.");
        }
        if (encryptionKey == null || encryptionKey.isBlank()
                || encryptionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Production evidence storage requires ENCRYPTION_KEY with at least 32 UTF-8 bytes.");
        }
    }
}
