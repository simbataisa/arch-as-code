package com.techcombank.qe.sut;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DefectController.class)
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
