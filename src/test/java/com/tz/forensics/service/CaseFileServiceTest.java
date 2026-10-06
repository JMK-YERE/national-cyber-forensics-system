package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTimelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseFileServiceTest {
    @Mock CaseFileRepository cases;
    @Mock CaseTimelineRepository timeline;
    @InjectMocks CaseFileService service;

    @Test void lifecycleRejectsIllegalTransition() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("OPEN");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"INVESTIGATING",null,7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }

    @Test void assignedRequiresAssignee() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("TRIAGED");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"ASSIGNED",null,7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }

    @Test void closingRequiresReason() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("REVIEW");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"CLOSED","",7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }
}
