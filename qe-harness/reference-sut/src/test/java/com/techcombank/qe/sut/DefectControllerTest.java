package com.techcombank.qe.sut;

import com.techcombank.qe.sut.capability.authz.JwtService;
import com.techcombank.qe.sut.capability.authz.SecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @Import}s TST-040's {@link SecurityConfig} (Task 9): without it, this
 * slice would fall back to Spring Boot's own zero-config security default
 * (deny everything, generated password) now that spring-boot-starter-security
 * is on the classpath -- {@link SecurityConfig} only locks down
 * {@code /protected/**}, so importing it here restores this test's original,
 * unauthenticated-access behaviour for {@code /_test/defect/**}.
 */
@WebMvcTest(DefectController.class)
@Import({SecurityConfig.class, JwtService.class})
class DefectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearState() {
        DefectFlags.clear();
    }

    @Test
    void activatingKnownFlagReturns204AndActivatesIt() throws Exception {
        mockMvc.perform(post("/_test/defect/ledger-unbalanced"))
            .andExpect(status().isNoContent());
        assertTrue(DefectFlags.isActive("ledger-unbalanced"));
    }

    @Test
    void activatingUnknownFlagReturns400WithFlagNameInBody() throws Exception {
        mockMvc.perform(post("/_test/defect/not-a-real-flag"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(Matchers.containsString("not-a-real-flag")));
    }

    @Test
    void clearReturns204AndDeactivatesWhateverWasActive() throws Exception {
        DefectFlags.activate("schema-drift");
        mockMvc.perform(delete("/_test/defect"))
            .andExpect(status().isNoContent());
        assertFalse(DefectFlags.isActive("schema-drift"));
    }
}
