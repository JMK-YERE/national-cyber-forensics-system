package com.tz.forensics.service;
import com.tz.forensics.entity.CaseCommunication;
import com.tz.forensics.repository.CaseCommunicationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
@Service
public class CaseCommunicationService{
 private final CaseCommunicationRepository repository;
 private static final Set<String> TLP=Set.of("CLEAR","GREEN","AMBER","RED");
 public CaseCommunicationService(CaseCommunicationRepository repository){this.repository=repository;}
 public List<CaseCommunication> findByCaseId(Long caseId){return caseId==null?List.of():repository.findByCaseIdOrderByCreatedAtAsc(caseId);}
 public CaseCommunication create(Long caseId,Long authorId,String authorName,String authorRole,String message,String tlp){
  if(caseId==null||authorId==null||message==null||message.isBlank())return null;
  String level=tlp==null?"CLEAR":tlp.trim().toUpperCase();
  if(!TLP.contains(level))return null;
  String body=message.trim();
  if(body.length()>10000)return null;
  return repository.save(new CaseCommunication(caseId,authorId,authorName,authorRole,body,level));
 }
}