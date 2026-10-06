package com.tz.forensics.service;

import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.repository.CaseIocRepository;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.entity.CaseFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseIocServiceTest {
    @Mock CaseIocRepository repository;
    @Mock CaseFileRepository caseFileRepository;
    @InjectMocks CaseIocService service;

    @Test void crossCaseDeleteIsRejected() {
        CaseIoc ioc = new CaseIoc(); ioc.setCaseId(10L);
        CaseFile cf = new CaseFile(); cf.setId(11L); cf.setStatus("OPEN");
        when(caseFileRepository.findById(11L)).thenReturn(java.util.Optional.of(cf));
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(ioc));
        assertFalse(service.delete(5L,11L));
        verify(repository,never()).delete(any());
    }

    @Test void validCreateNormalizesType() {
        CaseFile cf = new CaseFile(); cf.setId(10L); cf.setStatus("OPEN");
        when(caseFileRepository.findById(10L)).thenReturn(java.util.Optional.of(cf));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CaseIoc ioc=service.create(10L," ip ","1.2.3.4", "HIGH","sensor",null,null,null,7L,"analyst");
        assertNotNull(ioc);
        assertEquals("IP",ioc.getIocType());
        assertEquals("1.2.3.4",ioc.getValue());
    }
}
