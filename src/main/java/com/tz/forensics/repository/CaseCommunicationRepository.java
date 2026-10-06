package com.tz.forensics.repository;
import com.tz.forensics.entity.CaseCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CaseCommunicationRepository extends JpaRepository<CaseCommunication,Long>{
 List<CaseCommunication> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}