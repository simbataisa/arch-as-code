package com.techcombank.qe.sut.capability.clientexp;

import com.techcombank.qe.sut.DefectFlags;
import com.techcombank.qe.sut.capability.authz.JwtService;
import com.techcombank.qe.sut.capability.authz.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TST-043 cache/ETag/compression tests.
 *
 * <p>{@code @WebMvcTest(CatalogueController.class)} boots only the web layer,
 * same rationale as TST-031's {@code TokenBucketTest}: this capability is
 * pure HTTP caching/compression, no DataSource/Flyway involved. Spring
 * Boot's web-slice type filter picks up {@link CachePolicyFilter}
 * automatically since it is a {@code Filter} bean.
 *
 * <p>{@code @Import}s TST-040's {@link SecurityConfig}: without it this
 * slice falls back to Spring Boot's zero-config security default (deny
 * everything) now that spring-boot-starter-security is on the classpath.
 * {@link SecurityConfig} only locks down {@code /protected/**}, so importing
 * it here restores unauthenticated access to {@code /catalogue}.
 */
@WebMvcTest(CatalogueController.class)
@Import({SecurityConfig.class, JwtService.class})
class CachePolicyTest {

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clearDefectFlag() {
        DefectFlags.clear();
    }

    @Test
    void responseCarriesCacheControlAndETag() throws Exception {
        mvc.perform(get("/catalogue"))
           .andExpect(status().isOk())
           .andExpect(header().exists("Cache-Control"))
           .andExpect(header().exists("ETag"));
    }

    @Test
    void matchingIfNoneMatchYieldsNotModifiedWithNoBody() throws Exception {
        String etag = mvc.perform(get("/catalogue")).andReturn()
                         .getResponse().getHeader("ETag");
        mvc.perform(get("/catalogue").header("If-None-Match", etag))
           .andExpect(status().isNotModified())
           .andExpect(content().string(""));
    }

    @Test
    void compressesWhenClientAcceptsGzip() throws Exception {
        mvc.perform(get("/catalogue").header("Accept-Encoding", "gzip"))
           .andExpect(header().string("Content-Encoding", "gzip"));
    }

    @Test
    void defectFlagOmitsCacheHeaders() throws Exception {
        withDefect("cache-headers-absent", () ->
            mvc.perform(get("/catalogue"))
               .andExpect(header().doesNotExist("ETag"))
               .andExpect(header().doesNotExist("Cache-Control")));
    }

    /** Activates {@code flag} for the duration of {@code action}, always
     *  clearing it afterwards even if {@code action} throws -- same pattern
     *  as TST-031's {@code TokenBucketTest}. */
    private void withDefect(String flag, ThrowingRunnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DefectFlags.clear();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
