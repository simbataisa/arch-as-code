package com.techcombank.qe.sut.capability.ratelimit;

import com.techcombank.qe.sut.capability.authz.JwtService;
import com.techcombank.qe.sut.capability.authz.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code RateLimitResetController} tests (Task 17 follow-up fix). Same
 * {@code @WebMvcTest}/{@code @Import} shape as {@code TokenBucketTest} and
 * {@code DefectControllerTest} -- {@link RateLimitFilter} is picked up
 * automatically as a {@code Filter} bean regardless of which controllers are
 * listed (see those classes' own javadoc), which is what lets this
 * controller's constructor-injected {@code RateLimitFilter} resolve inside
 * the slice.
 */
@WebMvcTest({RateLimitResetController.class, RateLimitedController.class})
@Import({SecurityConfig.class, JwtService.class})
class RateLimitResetControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void resetReturns204AndRestoresCapacityEvenAfterExhaustion() throws Exception {
        // Exhaust the shared, process-wide bucket first -- proves reset
        // actually restores capacity, not just that the endpoint responds.
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/rate-limited/ping"));
        }

        mvc.perform(post("/_test/reset/ratelimit")).andExpect(status().isNoContent());

        int admitted = 0;
        for (int i = 0; i < 15; i++) {
            int sc = mvc.perform(get("/rate-limited/ping")).andReturn().getResponse().getStatus();
            if (sc == 200) admitted++;
        }
        assertTrue(admitted >= 10,
            "reset must restore at least the configured rate's worth of admits, got " + admitted);
    }
}
