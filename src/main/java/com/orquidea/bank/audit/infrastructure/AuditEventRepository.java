package com.orquidea.bank.audit.infrastructure;

import com.orquidea.bank.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Integer> {

    List<AuditEvent> findByTraceIdOrderByIdAsc(String traceId);

    List<AuditEvent> findByPurchaseIdOrderByIdAsc(String purchaseId);

    List<AuditEvent> findByActorOrderByIdAsc(String actor);
}
