package com.tz.forensics.service;

import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.repository.CaseIocRepository;
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
    @InjectMocks CaseIocService service;

    @Test void crossCaseDeleteIsRejected() {
        CaseIoc ioc = new CaseIoc(); ioc.setId(5L); ioc.setCaseId(10L);
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(ioc));
        assertFalse(service.delete(5L,11L));
        verify(repository,never()).delete(any());
    }

    @Test void validCreateNormalizesType() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CaseIoc ioc=service.create(10L," ip ","1.2.3.4", "HIGH","sensor",null,null,null,7L,"analyst");
        assertNotNull(ioc);
        assertEquals("IP",ioc.getIocType());
        assertEquals("1.2.3.4",ioc.getValue());
    }
}
