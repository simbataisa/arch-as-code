package com.techcombank.qe.sut.capability.authz;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link TokenExpiryTestController} tests (Task 19 follow-up addition). Same
 * {@code @WebMvcTest}/{@code @Import} shape as {@code AbstractAuthzTest} --
 * proves the one property Task 19's out-of-process {@code assert-authz.groovy}
 * clock-skew sweep depends on: a token minted here with a positive {@code
 * secondsPastExpiry} is rejected by the real, running validator once that
 * offset exceeds the declared tolerance, exactly like {@code
 * TokenLifecycleTest}'s in-process equivalent.
 */
@WebMvcTest({TokenExpiryTestController.class, ProtectedController.class})
@Import({SecurityConfig.class, JwtService.class})
class TokenExpiryTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mintsAnAlreadyExpiredTokenAcceptedWithinDeclaredSkew() throws Exception {
        // Declared tolerance defaults to 5s (app.authz.clock-skew-seconds); 1s past
        // exp is comfortably inside it.
        String token = mintExpired("reader", 1);
        MockHttpServletResponse r = mockMvc.perform(
                get("/protected/read").header("Authorization", "Bearer " + token))
            .andReturn().getResponse();
        assertEquals(200, r.getStatus());
    }

    @Test
    void rejectsATokenExpiredWellBeyondDeclaredSkew() throws Exception {
        String token = mintExpired("reader", 3600);
        MockHttpServletResponse r = mockMvc.perform(
                get("/protected/read").header("Authorization", "Bearer " + token))
            .andReturn().getResponse();
        assertEquals(401, r.getStatus());
    }

    @Test
    void unknownRoleReturns400() throws Exception {
        mockMvc.perform(post("/_test/token/expired")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"not-a-real-role\",\"secondsPastExpiry\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("not-a-real-role")));
    }

    /** A negative offset would mint an {@code exp} in the FUTURE -- a fully valid,
     *  non-expired token for any role -- which contradicts this endpoint's own
     *  documented contract ("mint an already-expired token"). Must be rejected with
     *  400, never silently accepted as a valid token. */
    @Test
    void negativeSecondsPastExpiryReturns400NotAValidToken() throws Exception {
        mockMvc.perform(post("/_test/token/expired")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"admin\",\"secondsPastExpiry\":-3600}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("secondsPastExpiry")));
    }

    private String mintExpired(String role, long secondsPastExpiry) throws Exception {
        String body = mockMvc.perform(post("/_test/token/expired")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"" + role + "\",\"secondsPastExpiry\":" + secondsPastExpiry + "}"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
