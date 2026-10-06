package com.tz.forensics.service;

import java.io.IOException;

public interface EvidenceStorage {
    void write(String key, byte[] data) throws IOException;
    byte[] read(String key) throws IOException;
    boolean exists(String key) throws IOException;
    void delete(String key) throws IOException;
}
