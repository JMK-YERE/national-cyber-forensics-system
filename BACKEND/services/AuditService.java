package com.tz.forensics.service;

import com.tz.forensics.entity.AuditLog;
import com.tz.forensics.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.Servlet
