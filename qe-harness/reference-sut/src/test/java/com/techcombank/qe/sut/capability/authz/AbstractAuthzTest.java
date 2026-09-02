package com.techcombank.qe.sut.capability.authz;

import com.jayway.jsonpath.JsonPath;
import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared MockMvc fixture for the TST-040 authz capability's test suite --
 * same "abstract base carries the shared setup" pattern as the ledger
 * capability's {@code AbstractLedgerIntegrationTest}, but a
 * {@code @WebMvcTest} slice rather than a full {@code @SpringBootTest}: this
 * capability is pure in-memory JWT state, so it needs no
 * DataSource/Flyway/Postgres.
 *
 * <p>{@link SecurityConfig} and {@link JwtService} are not themselves
 * {@code @Controller}/{@code @Filter} types, so the slice's default component
 * scan would otherwise skip them -- {@code @Import} pulls both in explicitly,
 * which is also how {@link SecurityConfig}'s {@code SecurityFilterChain} bean
 * gets a real {@link JwtService} to construct its filters with.
 */
@WebMvcTest({TokenController.class, ProtectedController.class})
@Import({SecurityConfig.class, JwtService.class})
abstract class AbstractAuthzTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @BeforeEach
    void clearDefectFlag() {
        DefectFlags.clear();
    }

    /** GET {@code ep} as {@code role} -- {@code "anonymous"} sends no token at
     *  all; any other value issues a fresh access token for that role first. */
    protected MockHttpServletResponse call(String role, String ep) throws Exception {
        if ("anonymous".equals(role)) {
            return mockMvc.perform(get(ep)).andReturn().getResponse();
        }
        return callWithToken(issue(role), ep);
    }

    protected MockHttpServletResponse callWithToken(String token, String ep) throws Exception {
        return mockMvc.perform(get(ep).header("Authorization", "Bearer " + token)).andReturn().getResponse();
    }

    /** POST /auth/token for {@code role}; returns the issued access token string. */
    protected String issue(String role) throws Exception {
        String body = mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"" + role + "\"}"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** POST /auth/revoke for {@code token}. */
    protected void revoke(String token) throws Exception {
        mockMvc.perform(post("/auth/revoke")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\"}"));
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Activates {@code flag} for the duration of {@code action}, always
     *  clearing it afterwards even if {@code action} throws -- same pattern as
     *  the ledger and rate-limiter capabilities' own {@code withDefect}
     *  helpers. */
    protected void withDefect(String flag, ThrowingRunnable action) throws Exception {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
