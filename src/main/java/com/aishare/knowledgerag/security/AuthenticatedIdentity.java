package com.aishare.knowledgerag.security;

import java.util.UUID;

public record AuthenticatedIdentity(UUID tenantId, String userId) {
}
