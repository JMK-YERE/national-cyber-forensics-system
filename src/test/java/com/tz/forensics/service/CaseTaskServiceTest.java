package com.tz.forensics.service;

import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.repository.CaseTaskRepository;
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
class CaseTaskServiceTest {
    @Mock CaseTaskRepository repository;
    @Mock CaseFileRepository caseFileRepository;
    @InjectMocks CaseTaskService service;

    @Test void illegalTaskTransitionIsRejected() {
        CaseTask task = new CaseTask(); task.setId(1L); task.setCaseId(10L); task.setStatus("OPEN");
        CaseFile cf = new CaseFile(); cf.setId(10L); cf.setStatus("OPEN");
        when(caseFileRepository.findById(10L)).thenReturn(java.util.Optional.of(cf));
        assertFalse(service.transition(task,"COMPLETED",1L,"tester"));
        verify(repository,never()).save(any());
    }

    @Test void completionRecordsActor() {
        CaseTask task = new CaseTask(); task.setId(1L); task.setCaseId(10L); task.setStatus("IN_PROGRESS");
        CaseFile cf = new CaseFile(); cf.setId(10L); cf.setStatus("OPEN");
        when(caseFileRepository.findById(10L)).thenReturn(java.util.Optional.of(cf));
        assertTrue(service.transition(task,"COMPLETED",9L,"forensics"));
        assertEquals("COMPLETED",task.getStatus());
        assertEquals(9L,task.getCompletedBy());
        assertEquals("forensics",task.getCompletedByName());
        verify(repository).save(task);
    }
}
