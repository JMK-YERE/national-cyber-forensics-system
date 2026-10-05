package com.tz.forensics.repository;

import com.tz.forensics.entity.CaseIoc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaseIocRepository extends JpaRepository<CaseIoc,Long> {
    List<CaseIoc> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}