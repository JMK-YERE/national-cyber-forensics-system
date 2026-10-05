package com.tz.forensics.service;
import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.repository.CaseTaskRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class CaseTaskService{
 private final CaseTaskRepository repository;
 public CaseTaskService(CaseTaskRepository repository){this.repository=repository;}
 public List<CaseTask> findByCaseId(Long id){return id==null?List.of():repository.findByCaseIdOrderByCreatedAtDesc(id);}
 public List<CaseTask> findMyTasks(Long userId){return userId==null?List.of():repository.findByAssignedToOrderByCreatedAtDesc(userId);}
 public CaseTask get(Long id){return id==null?null:repository.findById(id).orElse(null);}
 public CaseTask create(Long caseId,String title,String description,Long assignedTo,String assignedName,String priority,LocalDateTime dueDate,Long creator,String creatorName){
  if(caseId==null||title==null||title.isBlank())return null;
  CaseTask t=new CaseTask();t.setCaseId(caseId);t.setTitle(title.trim());t.setDescription(description);t.setAssignedTo(assignedTo);t.setAssignedToName(assignedName);t.setPriority(priority==null||priority.isBlank()?"MEDIUM":priority.toUpperCase());t.setDueDate(dueDate);t.setCreatedBy(creator);t.setCreatedByName(creatorName);return repository.save(t);
 }
 public boolean transition(CaseTask t,String to,Long actor,String actorName){
  if(t==null||to==null)return false;String from=t.getStatus()==null?"OPEN":t.getStatus().toUpperCase(), next=to.toUpperCase();
  boolean valid=switch(from){case "OPEN"->"IN_PROGRESS".equals(next)||"CANCELLED".equals(next);case "IN_PROGRESS"->"BLOCKED".equals(next)||"COMPLETED".equals(next)||"CANCELLED".equals(next);case "BLOCKED"->"IN_PROGRESS".equals(next)||"CANCELLED".equals(next);case "COMPLETED","CANCELLED"->false;default->false;};
  if(!valid)return false;t.setStatus(next);t.setUpdatedAt(LocalDateTime.now());
  if("COMPLETED".equals(next)){t.setCompletedAt(LocalDateTime.now());t.setCompletedBy(actor);t.setCompletedByName(actorName);}
  repository.save(t);return true;
 }
}