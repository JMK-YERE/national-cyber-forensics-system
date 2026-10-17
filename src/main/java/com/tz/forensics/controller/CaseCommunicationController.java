package com.tz.forensics.controller;

import com.tz.forensics.entity.*;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/case-communications")
public class CaseCommunicationController{
 private final CaseCommunicationService communicationService;
 private final CaseFileService caseService;
 private final UserRepository users;
 private final AuditService audit;
 private final NotificationService notifications;
 private final IncidentService incidentService;
 private final com.tz.forensics.service.TenantAccessService tenantAccessService;

 public CaseCommunicationController(CaseCommunicationService c,CaseFileService cs,UserRepository u,AuditService a,
                                    NotificationService n,IncidentService incidentService,
                                    com.tz.forensics.service.TenantAccessService tenantAccessService){
  communicationService=c;caseService=cs;users=u;audit=a;notifications=n;this.incidentService=incidentService;
  this.tenantAccessService=tenantAccessService;
 }

 private User current(Authentication a){return a==null?null:users.findByUsername(a.getName()).orElse(null);}
 private boolean staff(User u){return u!=null&&(u.isAdmin()||u.isProfessional()||u.isForensics()||u.isAnalyst());}
 private boolean hasTenantAccess(Long tenantId,User u){
  if(tenantId==null)return false; // Unknown ownership is not authorization; resolve via approved mapping.
  if(u==null||u.getUsername()==null)return false;
  try{tenantAccessService.requireMembership(u.getUsername(),tenantId,java.util.Set.of());return true;}
  catch(org.springframework.web.server.ResponseStatusException denied){return false;}
 }
 private boolean canView(CaseFile c,User u){
  if(c==null||u==null||!hasTenantAccess(c.getTenantId(),u)||c.getIncidentId()==null)return false;
  Incident incident=incidentService.getById(c.getIncidentId());
  if(incident==null||!java.util.Objects.equals(c.getTenantId(),incident.getTenantId()))return false;
  if(u.isAdmin()||u.isProfessional()||u.isForensics())return true;
  if(!u.isAnalyst()||u.getId()==null)return false;
  boolean caseAccess=u.getId().equals(c.getCreatedBy())||u.getId().equals(c.getAssignedTo())||u.getId().equals(c.getLeadInvestigator());
  boolean incidentAccess=u.getId().equals(incident.getReporterUserId())||u.getId().equals(incident.getAssignedTo());
  return caseAccess&&incidentAccess;
 }

 private boolean closed(CaseFile c){
  String s=c==null||c.getStatus()==null?"OPEN":c.getStatus().trim().toUpperCase(Locale.ROOT);
  return "CLOSED".equals(s)||"ARCHIVED".equals(s);
 }

 @GetMapping("/{caseId}")
 public String view(@PathVariable Long caseId,Model m,Authentication a){
  User u=current(a);CaseFile c=caseService.getById(caseId);
  if(!staff(u)||!canView(c,u))return "redirect:/access-denied";
  m.addAttribute("caseFile",c);m.addAttribute("communications",communicationService.findByCaseId(caseId));
  audit.log("VIEW_CASE_COMMUNICATION","CaseFile",String.valueOf(caseId),"Viewed case communication");
  return "case-communications";
 }

 @PostMapping("/{caseId}/send")
 public String send(@PathVariable Long caseId,@RequestParam String message,@RequestParam(required=false) String tlp,Authentication a){
  User u=current(a);CaseFile c=caseService.getById(caseId);
  if(!staff(u)||!canView(c,u))return "redirect:/access-denied";
  if(closed(c))return "redirect:/case-communications/"+caseId;
  CaseCommunication item=communicationService.create(caseId,u.getId(),a.getName(),u.getRole(),message,tlp);
  if(item!=null){
   audit.log("CASE_MESSAGE_SENT","CaseCommunication",String.valueOf(item.getId()),"Case "+c.getCaseNumber()+" | TLP="+item.getTlp());
   caseService.addTimeline(caseId,"COMMUNICATION","Case message sent",item.getTlp()+" message by "+a.getName(),u.getId(),a.getName(),u.getRole());
   java.util.LinkedHashSet<Long> recipientIds=new java.util.LinkedHashSet<>();
   if(c.getAssignedTo()!=null) recipientIds.add(c.getAssignedTo());
   if(c.getLeadInvestigator()!=null) recipientIds.add(c.getLeadInvestigator());
   if(c.getCreatedBy()!=null) recipientIds.add(c.getCreatedBy());
   recipientIds.remove(u.getId());
   for(Long recipientId:recipientIds){
    User recipient=users.findById(recipientId).orElse(null);
    if(recipient!=null&&Boolean.TRUE.equals(recipient.getEnabled())&&recipient.isApproved()&&canView(c,recipient)){
     notifications.createNotification(recipientId,"Case communication","New "+item.getTlp()+" message in "+c.getCaseNumber(),
         "CASE_MESSAGE","/case-communications/"+caseId);
    }
   }
  }else audit.log("REJECT_CASE_MESSAGE","CaseCommunication",String.valueOf(caseId),"Invalid message or TLP");
  return "redirect:/case-communications/"+caseId;
 }

 @PostMapping("/{caseId}/escalate")
 public String escalate(@PathVariable Long caseId,Authentication a){
  User u=current(a);CaseFile c=caseService.getById(caseId);
  if(!staff(u)||!canView(c,u))return "redirect:/access-denied";
  if(closed(c)){audit.log("REJECT_CASE_ESCALATION","CaseFile",String.valueOf(caseId),"Escalation attempted after case closure");return "redirect:/case-communications/"+caseId;}
  String current=u.getRole()==null?"":u.getRole().toUpperCase(Locale.ROOT);
  String next=switch(current){case "ANALYST"->"FORENSICS";case "FORENSICS"->"CYBER_PRO";case "CYBER_PRO"->"ADMIN";default->null;};
  if(next!=null){
   // Escalation notices must not disclose a case to staff in another tenant.
   users.findAll().stream().filter(x->next.equalsIgnoreCase(x.getRole())&&Boolean.TRUE.equals(x.getEnabled())
       &&x.isApproved()&&!Boolean.TRUE.equals(x.getAccountLocked())&&hasTenantAccess(c.getTenantId(),x))
       .forEach(x->notifications.createNotification(x.getId(),"Case escalation","Case "+c.getCaseNumber()+" escalated by "+a.getName(),
           "CASE_ESCALATION","/cases/"+caseId));
   audit.log("ESCALATE_CASE","CaseFile",String.valueOf(caseId),current+" → "+next);
   caseService.addTimeline(caseId,"ESCALATION","Case escalated",current+" → "+next,u.getId(),a.getName(),u.getRole());
  }
  return "redirect:/case-communications/"+caseId;
 }
}