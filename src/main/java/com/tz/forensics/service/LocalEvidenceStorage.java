package com.tz.forensics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalEvidenceStorage implements EvidenceStorage {

    private final Path basePath;

    public LocalEvidenceStorage(@Value("${app.upload.dir}") String uploadDir) {
        this.basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path resolve(String key) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IOException("Evidence storage key is required.");
        }
        Path path = basePath.resolve(key).normalize();
        if (!path.startsWith(basePath)) {
            throw new IOException("Invalid evidence storage path.");
        }
        return path;
    }

    private void ensureBaseDirectory() throws IOException {
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }
        if (!Files.isDirectory(basePath)) {
            throw new IOException("Evidence storage path is not a directory.");
        }
    }

    @Override
    public void write(String key, byte[] data) throws IOException {
        if (data == null) throw new IOException("Evidence storage data is required.");
        ensureBaseDirectory();
        Path path = resolve(key);
        Files.write(path, data);
    }

    @Override
    public byte[] read(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Stored evidence file not found.");
        }
        return Files.readAllBytes(path);
    }

    @Override
    public boolean exists(String key) throws IOException {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }
}
