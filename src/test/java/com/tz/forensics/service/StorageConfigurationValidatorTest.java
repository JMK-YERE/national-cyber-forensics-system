package com.tz.forensics.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigurationValidatorTest {

    private StorageConfigurationValidator validator(String provider, boolean productionRequired,
                                                    String bucket, String encryptionKey) {
        StorageConfigurationValidator validator = new StorageConfigurationValidator();
        ReflectionTestUtils.setField(validator, "provider", provider);
        ReflectionTestUtils.setField(validator, "productionRequired", productionRequired);
        ReflectionTestUtils.setField(validator, "bucket", bucket);
        ReflectionTestUtils.setField(validator, "encryptionKey", encryptionKey);
        return validator;
    }

    @Test
    void permitsLocalStorageForNonProductionDevelopment() {
        assertDoesNotThrow(() -> validator("local", false, "", "").validate());
    }

    @Test
    void rejectsLocalStorageWhenProductionEvidenceVaultIsRequired() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> validator("local", true, "evidence-bucket", "x".repeat(32)).validate());
        assertTrue(error.getMessage().contains("EVIDENCE_STORAGE_PROVIDER=local"));
    }

    @Test
    void productionS3RequiresBucketAndStrongEncryptionKey() {
        IllegalStateException missingBucket = assertThrows(IllegalStateException.class,
            () -> validator("s3", true, " ", "x".repeat(32)).validate());
        assertTrue(missingBucket.getMessage().contains("EVIDENCE_STORAGE_BUCKET"));

        IllegalStateException weakKey = assertThrows(IllegalStateException.class,
            () -> validator("s3", true, "evidence-bucket", "short").validate());
        assertTrue(weakKey.getMessage().contains("ENCRYPTION_KEY"));
    }

    @Test
    void acceptsConfiguredS3ProductionStorage() {
        assertDoesNotThrow(() ->
            validator("s3", true, "evidence-bucket", "k".repeat(32)).validate());
    }

    @Test
    void rejectsUnknownStorageProviderEvenOutsideProduction() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> validator("filesystem", false, "", "").validate());
        assertTrue(error.getMessage().contains("Unsupported EVIDENCE_STORAGE_PROVIDER"));
    }
}
