package com.techcombank.qe.sut;

import com.techcombank.qe.sut.capability.authz.JwtService;
import com.techcombank.qe.sut.capability.authz.SecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @Import}s TST-040's {@link SecurityConfig} (Task 9): without it, this
 * slice would fall back to Spring Boot's own zero-config security default
 * (deny everything, generated password) now that spring-boot-starter-security
 * is on the classpath -- {@link SecurityConfig} only locks down
 * {@code /protected/**}, so importing it here restores this test's original,
 * unauthenticated-access behaviour for {@code /_capabilities} and
 * {@code /capability/*}.
 */
@WebMvcTest(CapabilityController.class)
@Import({SecurityConfig.class, JwtService.class})
class CapabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void capabilitiesReturns200WithAllTwentyFourKeys() throws Exception {
        mockMvc.perform(get("/_capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(24))
            .andExpect(jsonPath("$['TST-020']").value("declared"));
    }

    @Test
    void probeOfDeclaredArchetypeReturns501WithArchetypeInBody() throws Exception {
        mockMvc.perform(get("/capability/TST-022/probe"))
            .andExpect(status().isNotImplemented())
            .andExpect(content().string(Matchers.containsString("TST-022")));
    }

    @Test
    void probeOfUnknownArchetypeReturns400() throws Exception {
        mockMvc.perform(get("/capability/TST-999/probe"))
            .andExpect(status().isBadRequest());
    }
}
