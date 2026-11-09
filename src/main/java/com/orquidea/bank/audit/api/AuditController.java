package com.orquidea.bank.audit.api;

import com.orquidea.bank.audit.api.dto.AuditEventResponse;
import com.orquidea.bank.audit.application.AuditRecorder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Audit")
public class AuditController {

    private final AuditRecorder audit;

    public AuditController(AuditRecorder audit) {
        this.audit = audit;
    }

    @GetMapping("/api/audit")
    public List<AuditEventResponse> list(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String purchaseId,
            @RequestParam(required = false) String actor
    ) {
        return audit.list(traceId, purchaseId, actor);
    }
}
