package com.orquidea.bank.audit.application;

import com.orquidea.bank.audit.api.dto.AuditEventResponse;
import com.orquidea.bank.audit.domain.AuditAction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditRecorder {

    public void record(
            String actor,
            AuditAction action,
            String aggregateType,
            String aggregateId,
            String purchaseId,
            String traceId,
            String beforeJson,
            String afterJson
    ) {
        throw new UnsupportedOperationException("AuditRecorder.record is not implemented yet");
    }

    public List<AuditEventResponse> list(String traceId, String purchaseId, String actor) {
        throw new UnsupportedOperationException("AuditRecorder.list is not implemented yet");
    }
}
