package com.aishare.knowledgerag.audit;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestIdProvider {

    public static final String MDC_KEY = "requestId";

    public String currentOrCreate() {
        String requestId = MDC.get(MDC_KEY);
        return requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
    }
}
