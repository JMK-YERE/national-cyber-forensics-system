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
    @Test
    void rejectsMissingHashWithoutThrowing() {
        Map<String, Object> result = service.analyzeHash(null);
        assertEquals(false, result.get("valid"));
    }

    @Test
    void rejectsOversizedHashWithoutThrowing() {
        Map<String, Object> result = service.analyzeHash("a".repeat(129));
        assertEquals(false, result.get("valid"));
    }

    @Test
    void rejectsNullPasswordBreachCheck() {
        Map<String, Object> result = service.checkPasswordBreach(null);
        assertEquals(false, result.get("success"));
    }

    @Test
    void rejectsOversizedPasswordStrengthInput() {
        Map<String, Object> result = service.checkPasswordStrength("a".repeat(1001));
        assertEquals("VERY WEAK", result.get("rating"));
    }

    @Test
    void rejectsOversizedUrlBeforeNetworkValidation() {
        Map<String, Object> result = service.checkUrlReputation("https://example.com/" + "a".repeat(2050));
        assertEquals(false, result.get("success"));
        assertEquals("INVALID URL", result.get("verdict"));
    }

}
