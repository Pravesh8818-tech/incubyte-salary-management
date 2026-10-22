package com.incubyte.salarymanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WriteApiBearerTokenFilterTest {

    @Test
    void unconfiguredTokenFailsClosedEvenWhenRequestSuppliesBearerHeader() throws Exception {
        WriteApiBearerTokenFilter filter = new WriteApiBearerTokenFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/employees");
        request.addHeader("Authorization", "Bearer some-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedApplication = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> reachedApplication.set(true));

        assertEquals(401, response.getStatus());
        assertFalse(reachedApplication.get());
    }
}
