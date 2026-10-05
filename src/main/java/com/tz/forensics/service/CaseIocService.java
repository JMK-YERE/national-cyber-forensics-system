package com.tz.forensics.service;

import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.repository.CaseIocRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CaseIocService {
    private final CaseIocRepository repository;
    public CaseIocService(CaseIocRepository repository){this.repository=repository;}
    public List<CaseIoc> findByCaseId(Long caseId){return repository.findByCaseIdOrderByCreatedAtDesc(caseId);}
    public CaseIoc create(Long caseId,String type,String value,String confidence,String source,
                          LocalDateTime firstSeen,LocalDateTime lastSeen,String notes,Long userId,String userName){
        if(caseId==null||type==null||value==null||type.isBlank()||value.isBlank()) return null;
        CaseIoc i=new CaseIoc(); i.setCaseId(caseId); i.setIocType(type.trim().toUpperCase());
        i.setValue(value.trim()); i.setConfidence(confidence); i.setSource(source);
        i.setFirstSeen(firstSeen); i.setLastSeen(lastSeen); i.setNotes(notes);
        i.setCreatedBy(userId); i.setCreatedByName(userName); return repository.save(i);
    }
    public boolean delete(Long id){if(!repository.existsById(id))return false; repository.deleteById(id); return true;}
}