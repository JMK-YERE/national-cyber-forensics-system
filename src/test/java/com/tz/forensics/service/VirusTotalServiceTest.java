package com.tz.forensics.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VirusTotalServiceTest {

    private VirusTotalService service() {
        return new VirusTotalService();
    }

    @Test
    void rejectsPrivateUrlBeforeCallingVirusTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> service().checkUrl("http://127.0.0.1/admin"));
    }

    @Test
    void rejectsNonStandardUrlPort() {
        assertThrows(IllegalArgumentException.class,
                () -> service().checkUrl("https://example.com:8443"));
    }

    @Test
    void rejectsMalformedHash() {
        assertThrows(IllegalArgumentException.class,
                () -> service().checkFileHash("not-a-hash"));
    }

    @Test
    void acceptsValidHashWithoutCallingExternalApiWhenUnconfigured() {
        Map<String, Object> result = service().checkFileHash(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertEquals(Boolean.FALSE, result.get("success"));
        assertEquals("VirusTotal API key haipo", result.get("error"));
    }

    @Test
    void rejectsPrivateIp() {
        assertThrows(IllegalArgumentException.class,
                () -> service().checkIP("10.0.0.1"));
    }

    @Test
    void rejectsMalformedDomain() {
        assertThrows(IllegalArgumentException.class,
                () -> service().checkDomain("localhost"));
    }
}
