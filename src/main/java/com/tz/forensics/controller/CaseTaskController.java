package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseTask;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.CaseTaskService;
import com.tz.forensics.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/case-tasks")
public class CaseTaskController {
 private final CaseTaskService taskService; private final CaseFileService caseService; private final UserRepository users;
 private final AuditService audit; private final NotificationService notifications;
 public CaseTaskController(CaseTaskService t,CaseFileService c,UserRepository u,AuditService a,NotificationService n){taskService=t;caseService=c;users=u;audit=a;notifications=n;}
 private User current(Authentication a){return a==null?null:users.findByUsername(a.getName()).orElse(null);}
 private boolean staff(User u){return u!=null&&(u.isAdmin()||u.isProfessional()||u.isForensics()||"ANALYST".equalsIgnoreCase(u.getRole()));}
 private boolean canView(CaseFile c,User u){return c!=null&&u!=null&&(u.isAdmin()||u.isProfessional()||u.isForensics()||("ANALYST".equalsIgnoreCase(u.getRole())&&(u.getId().equals(c.getCreatedBy())||u.getId().equals(c.getAssignedTo())||u.getId().equals(c.getLeadInvestigator()))));}
 private boolean manager(User u){return u!=null&&(u.isAdmin()||u.isProfessional()||u.isForensics());}

 @GetMapping
 public String myTasks(Model m,Authentication a){User u=current(a);if(!staff(u))return "redirect:/access-denied";m.addAttribute("tasks",taskService.findMyTasks(u.getId()));return "my-tasks";}

 @PostMapping("/create")
 public String create(@RequestParam Long caseId,@RequestParam String title,@RequestParam(required=false)String description,
                      @RequestParam Long assignedTo,@RequestParam(required=false)String priority,@RequestParam(required=false)String dueDate,Authentication a){
  User actor=current(a);CaseFile c=caseService.getById(caseId);if(!manager(actor)||!canView(c,actor))return "redirect:/access-denied";
  User assignee=users.findById(assignedTo).orElse(null);
  if(assignee==null||!Boolean.TRUE.equals(assignee.getEnabled())||!assignee.isApproved()||!(assignee.isAnalyst()||assignee.isForensics()||assignee.isProfessional()))return "redirect:/cases/"+caseId;
  LocalDateTime due=null;try{if(dueDate!=null&&!dueDate.isBlank())due=LocalDateTime.parse(dueDate);}catch(Exception e){return "redirect:/cases/"+caseId;}
  if(due!=null&&due.isBefore(LocalDateTime.now()))return "redirect:/cases/"+caseId;
  CaseTask t=taskService.create(caseId,title,description,assignee.getId(),assignee.getFullName()!=null?assignee.getFullName():assignee.getUsername(),priority,due,actor.getId(),a.getName());
  if(t!=null){audit.log("CREATE_CASE_TASK","CaseTask",""+t.getId(),"Created for case "+c.getCaseNumber()+" by "+a.getName());
   caseService.addTimeline(caseId,"TASK_CREATED","Task created",t.getTitle()+" → "+t.getAssignedToName(),actor.getId(),a.getName(),actor.getRole());
   notifications.createNotification(assignee.getId(),"New case task","Case "+c.getCaseNumber()+" — "+t.getTitle(),"TASK_ASSIGNED","/case-tasks");}
  return "redirect:/cases/"+caseId;
 }

 @PostMapping("/{id}/status")
 public String status(@PathVariable Long id,@RequestParam String status,Authentication a){
  User actor=current(a);CaseTask t=taskService.get(id);CaseFile c=t==null?null:caseService.getById(t.getCaseId());
  if(!staff(actor)||t==null||c==null||!canView(c,actor))return "redirect:/access-denied";
  boolean assignee=actor.getId().equals(t.getAssignedTo());boolean privileged=manager(actor);
  if(!assignee&&!privileged)return "redirect:/access-denied";
  String from=t.getStatus()==null?"OPEN":t.getStatus().trim().toUpperCase();
  String requested=status==null?"":status.trim().toUpperCase();
  if(taskService.transition(t,requested,actor.getId(),a.getName())){
   audit.log("CHANGE_CASE_TASK_STATUS","CaseTask",""+id,from+" → "+t.getStatus()+" by "+a.getName());
   caseService.addTimeline(c.getId(),"TASK_STATUS_CHANGED","Task status changed",t.getTitle()+" | "+from+" → "+t.getStatus(),actor.getId(),a.getName(),actor.getRole());
   if(t.getAssignedTo()!=null){
    notifications.createNotification(t.getAssignedTo(),"Case task updated","Case "+c.getCaseNumber()+" — "+t.getTitle()+" | "+from+" → "+t.getStatus(),"TASK_STATUS","/case-tasks");
   }
  } else {
   audit.log("REJECT_CASE_TASK_STATUS","CaseTask",""+id,from+" → "+requested+" by "+a.getName());
  }
  return "redirect:/cases/"+c.getId();
 }
}