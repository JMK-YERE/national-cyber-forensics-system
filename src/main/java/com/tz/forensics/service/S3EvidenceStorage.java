package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3EvidenceStorage implements EvidenceStorage {
    private final S3Client client;
    private final String bucket;
    private final String prefix;

    public S3EvidenceStorage(@Value("${app.storage.s3.bucket:}") String bucket,
                             @Value("${app.storage.s3.region:auto}") String region,
                             @Value("${app.storage.s3.endpoint:}") String endpoint,
                             @Value("${app.storage.s3.access-key:}") String accessKey,
                             @Value("${app.storage.s3.secret-key:}") String secretKey,
                             @Value("${app.storage.s3.prefix:cyber-forensics/}") String prefix) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("EVIDENCE_STORAGE_BUCKET is required when EVIDENCE_STORAGE_PROVIDER=s3.");
        }
        this.bucket = bucket.trim();
        this.prefix = normalizePrefix(prefix);
        String effectiveRegion = (region == null || region.isBlank() || "auto".equalsIgnoreCase(region)) ? "us-east-1" : region.trim();
        var builder = S3Client.builder().region(Region.of(effectiveRegion));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim())).forcePathStyle(true);
        }
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        this.client = builder.build();
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) return "";
        String p = value.trim().replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        return p.endsWith("/") ? p : p + "/";
    }

    private String key(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) throw new IOException("Evidence storage key is required.");
        if (storageKey.indexOf('/') >= 0 || storageKey.indexOf('\\') >= 0) throw new IOException("Invalid evidence storage key.");
        String normalized = Paths.get(storageKey).getFileName().toString();
        if (!normalized.equals(storageKey)) throw new IOException("Invalid evidence storage key.");
        if (normalized.isBlank() || ".".equals(normalized) || "..".equals(normalized)) throw new IOException("Invalid evidence storage key.");
        return prefix + normalized;
    }

    public void write(String storageKey, byte[] data) throws IOException {
        if (data == null) throw new IOException("Evidence storage data is required.");
        try { client.putObject(PutObjectRequest.builder().bucket(bucket).key(key(storageKey)).build(), RequestBody.fromBytes(data)); }
        catch (RuntimeException e) { throw new IOException("Unable to write evidence object.", e); }
    }

    public byte[] read(String storageKey) throws IOException {
        try { return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key(storageKey)).build()).asByteArray(); }
        catch (NoSuchKeyException e) { throw new IOException("Stored evidence object not found.", e); }
        catch (RuntimeException e) { throw new IOException("Unable to read evidence object.", e); }
    }

    public boolean exists(String storageKey) throws IOException {
        String objectKey = key(storageKey);
        try {
            client.headObject(b -> b.bucket(bucket).key(objectKey));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw new IOException("Unable to check evidence object.", e);
        } catch (RuntimeException e) {
            throw new IOException("Unable to check evidence object.", e);
        }
    }

    public void delete(String storageKey) throws IOException {
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key(storageKey)).build()); }
        catch (RuntimeException e) { throw new IOException("Unable to delete evidence object.", e); }
    }
}