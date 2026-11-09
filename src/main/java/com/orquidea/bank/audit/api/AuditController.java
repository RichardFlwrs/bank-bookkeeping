package com.orquidea.bank.audit.api;

import com.orquidea.bank.audit.api.dto.AuditEventResponse;
import com.orquidea.bank.audit.application.AuditRecorder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "Audit rows filtered by traceId, purchaseId, or actor")
    public List<AuditEventResponse> list(
            @Parameter(description = "Groups one seed run or one purchase")
            @RequestParam(required = false) String traceId,
            @Parameter(description = "Purchase public id")
            @RequestParam(required = false) String purchaseId,
            @Parameter(example = "seeder:opening-deposit")
            @RequestParam(required = false) String actor
    ) {
        return audit.list(traceId, purchaseId, actor);
    }
}
