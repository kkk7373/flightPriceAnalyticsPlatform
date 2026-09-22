package com.flightpriceanalytics.backend.handler;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.flightpriceanalytics.backend.controller.ApiController;
import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryException;
import com.flightpriceanalytics.backend.service.athena.AthenaResultException;
import com.flightpriceanalytics.backend.service.athena.AthenaQueryTimeoutException;
import com.flightpriceanalytics.backend.service.init.InitDashboardService;

import software.amazon.awssdk.services.athena.model.AthenaException;

class ApiExceptionHandlerTest {

    @Test
    void queryFailureReturnsBadGatewayWithoutInternalReason() throws Exception {
        MockMvc mvc = mvcThrowing(new AthenaQueryException("internal Athena failure reason"));

        mvc.perform(get("/api/init"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.code").value("ATHENA_QUERY_FAILED"))
                .andExpect(jsonPath("$.detail").value("集計データを取得できませんでした"))
                .andExpect(content().string(not(containsString("internal Athena failure reason"))));
    }

    @Test
    void timeoutReturnsGatewayTimeout() throws Exception {
        MockMvc mvc = mvcThrowing(new AthenaQueryTimeoutException("internal timeout reason"));

        mvc.perform(get("/api/init"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.code").value("ATHENA_TIMEOUT"))
                .andExpect(content().string(not(containsString("internal timeout reason"))));
    }

    @Test
    void sdkFailureReturnsBadGatewayWithoutSdkDetails() throws Exception {
        MockMvc mvc = mvcThrowing(AthenaException.builder().message("internal SDK detail").build());

        mvc.perform(get("/api/init"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ATHENA_QUERY_FAILED"))
                .andExpect(content().string(not(containsString("internal SDK detail"))));
    }

    @Test
    void invalidAthenaResultReturnsBadGateway() throws Exception {
        MockMvc mvc = mvcThrowing(new AthenaResultException("invalid result detail"));

        mvc.perform(get("/api/init"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ATHENA_QUERY_FAILED"))
                .andExpect(content().string(not(containsString("invalid result detail"))));
    }

    @Test
    void sqlLoadFailureReturnsInternalServerError() throws Exception {
        MockMvc mvc = mvcThrowing(new IOException("internal SQL path"));

        mvc.perform(get("/api/init"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SQL_RESOURCE_ERROR"))
                .andExpect(content().string(not(containsString("internal SQL path"))));
    }

    @Test
    void interruptionReturnsServiceUnavailable() throws Exception {
        MockMvc mvc = mvcThrowing(new InterruptedException("internal interrupt detail"));
        try {
            mvc.perform(get("/api/init"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("REQUEST_INTERRUPTED"))
                    .andExpect(content().string(not(containsString("internal interrupt detail"))));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void unexpectedFailureReturnsSafeInternalServerError() throws Exception {
        MockMvc mvc = mvcThrowing(new IllegalStateException("internal state detail"));

        mvc.perform(get("/api/init"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("internal state detail"))));
    }

    @Test
    void unsupportedMethodKeepsItsStandardStatus() throws Exception {
        MockMvc mvc = mvcThrowing(new IllegalStateException("should not be called"));

        mvc.perform(post("/api/init"))
                .andExpect(status().isMethodNotAllowed());
    }

    private static MockMvc mvcThrowing(Exception failure) {
        InitDashboardService service = new InitDashboardService(null, null) {
            @Override
            public InitResponse initDashboard(LocalDate date) throws IOException, InterruptedException {
                if (failure instanceof IOException ioFailure) {
                    throw ioFailure;
                }
                if (failure instanceof InterruptedException interruptedFailure) {
                    throw interruptedFailure;
                }
                if (failure instanceof RuntimeException runtimeFailure) {
                    throw runtimeFailure;
                }
                throw new AssertionError(failure);
            }
        };
        return MockMvcBuilders.standaloneSetup(new ApiController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
}
