package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.CaseIocService;
import com.tz.forensics.service.IncidentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Controller
@RequestMapping("/cases")
public class CaseIocController {
 private final CaseFileService caseFileService;
 private final CaseIocService iocService;
 private final UserRepository userRepository;
 private final AuditService auditService;
 private final IncidentService incidentService;
 private final com.tz.forensics.service.TenantAccessService tenantAccessService;

 public CaseIocController(CaseFileService c, CaseIocService i, UserRepository u,
                          AuditService a, IncidentService incidentService,
                          com.tz.forensics.service.TenantAccessService tenantAccessService) {
  caseFileService=c;iocService=i;userRepository=u;auditService=a;this.incidentService=incidentService;
  this.tenantAccessService=tenantAccessService;
 }
 private User user(Authentication a){return a==null||a.getName()==null?null:userRepository.findByUsername(a.getName()).orElse(null);}
 private boolean closed(CaseFile c){
  String s=c==null||c.getStatus()==null?"OPEN":c.getStatus().trim().toUpperCase(Locale.ROOT);
  return "CLOSED".equals(s)||"ARCHIVED".equals(s);
 }
 private boolean hasTenantAccess(Long tenantId,User u){
  if(tenantId==null)return true; // Legacy records stay readable pending approved ownership mapping.
  if(u==null||u.getUsername()==null)return false;
  try{tenantAccessService.requireMembership(u.getUsername(),tenantId,java.util.Set.of());return true;}
  catch(org.springframework.web.server.ResponseStatusException denied){return false;}
 }
 private boolean allowed(User u,CaseFile c){
  if(u==null||c==null||!hasTenantAccess(c.getTenantId(),u)||c.getIncidentId()==null)return false;
  Incident incident=incidentService.getById(c.getIncidentId());
  if(incident==null||!java.util.Objects.equals(c.getTenantId(),incident.getTenantId()))return false;
  if(u.isAdmin()||u.isProfessional()||u.isForensics())return true;
  if(!"ANALYST".equalsIgnoreCase(u.getRole())||u.getId()==null)return false;
  boolean caseAccess=u.getId().equals(c.getCreatedBy())||u.getId().equals(c.getAssignedTo())||u.getId().equals(c.getLeadInvestigator());
  boolean incidentAccess=u.getId().equals(incident.getReporterUserId())||u.getId().equals(incident.getAssignedTo());
  return caseAccess&&incidentAccess;
 }

 @PostMapping("/{caseId}/iocs")
 public String add(@PathVariable Long caseId,@RequestParam String iocType,@RequestParam String value,
                   @RequestParam(required=false)String confidence,@RequestParam(required=false)String source,
                   @RequestParam(required=false)String firstSeen,@RequestParam(required=false)String lastSeen,
                   @RequestParam(required=false)String notes,Authentication auth){
  User u=user(auth);CaseFile c=caseFileService.getById(caseId);
  if(!allowed(u,c))return "redirect:/access-denied";
  if(closed(c)){auditService.log("REJECT_IOC","CaseFile",c.getCaseNumber(),"IOC add attempted after case closure");return "redirect:/cases/"+caseId;}
  LocalDateTime f=parse(firstSeen),l=parse(lastSeen);
  CaseIoc i=iocService.create(caseId,iocType,value,confidence,source,f,l,notes,u.getId(),auth.getName());
  auditService.log(i!=null?"ADD_IOC":"REJECT_IOC","CaseFile",c.getCaseNumber(),i!=null?"IOC added: "+iocType:"Invalid IOC");
  if(i!=null)caseFileService.addTimeline(caseId,"IOC_ADDED","Indicator added",iocType+" | "+value,u.getId(),auth.getName(),u.getRole());
  return "redirect:/cases/"+caseId;
 }
 @PostMapping("/{caseId}/iocs/{iocId}/delete")
 public String delete(@PathVariable Long caseId,@PathVariable Long iocId,Authentication auth){
  User u=user(auth);CaseFile c=caseFileService.getById(caseId);
  if(!allowed(u,c))return "redirect:/access-denied";
  if(closed(c)){auditService.log("REJECT_IOC","CaseFile",c.getCaseNumber(),"IOC delete attempted after case closure");return "redirect:/cases/"+caseId;}
  if(iocService.delete(iocId,caseId)){
   auditService.log("DELETE_IOC","CaseFile",c.getCaseNumber(),"IOC "+iocId+" deleted");
   caseFileService.addTimeline(caseId,"IOC_REMOVED","Indicator removed","IOC ID "+iocId,u.getId(),auth.getName(),u.getRole());
  }
  return "redirect:/cases/"+caseId;
 }
 private LocalDateTime parse(String s){try{return s==null||s.isBlank()?null:LocalDateTime.parse(s);}catch(Exception e){return null;}}
}