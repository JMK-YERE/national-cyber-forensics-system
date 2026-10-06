package com.tz.forensics.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedSecurityServiceTest {

    private final AdvancedSecurityService service = new AdvancedSecurityService();

    @Test
    void dnsLookupRejectsLocalHost() {
        Map<String, Object> result = service.dnsLookup("localhost");
        assertEquals(false, result.get("success"));
        assertTrue(String.valueOf(result.get("error")).toLowerCase().contains("private"));
    }

    @Test
    void domainAgeRejectsPrivateHost() {
        Map<String, Object> result = service.checkDomainAge("127.0.0.1");
        assertEquals(false, result.get("success"));
        assertTrue(String.valueOf(result.get("error")).toLowerCase().contains("private"));
    }

    @Test
    void ipReputationClassifiesLoopbackAsPrivate() {
        Map<String, Object> result = service.checkIPReputation("127.0.0.1");
        assertEquals(true, result.get("private"));
    }

    @Test
    void urlReputationRejectsLoopbackTarget() {
        Map<String, Object> result = service.checkUrlReputation("http://127.0.0.1");
        assertEquals(false, result.get("success"));
        assertEquals("INVALID URL", result.get("verdict"));
    }
}
