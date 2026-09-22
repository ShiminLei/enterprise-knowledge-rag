package com.aishare.knowledgerag.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditPersistenceService {

    private final RagRequestAuditRepository repository;

    public AuditPersistenceService(RagRequestAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(RagRequestAudit audit) {
        repository.insert(audit);
    }
}
