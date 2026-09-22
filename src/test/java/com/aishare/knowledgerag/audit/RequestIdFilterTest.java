package com.aishare.knowledgerag.audit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    @Test
    void reusesSafeRequestIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "frontend-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(MDC.get(RequestIdProvider.MDC_KEY)).isEqualTo("frontend-request-123");

        new RequestIdFilter().doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME))
                .isEqualTo("frontend-request-123");
        assertThat(MDC.get(RequestIdProvider.MDC_KEY)).isNull();
    }
}
