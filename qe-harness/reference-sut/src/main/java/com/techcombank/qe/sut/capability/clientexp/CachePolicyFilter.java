package com.techcombank.qe.sut.capability.clientexp;

import com.techcombank.qe.sut.DefectFlags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

/**
 * TST-043 cache/ETag/compression enforcement point.
 *
 * <p>Guards the {@code /catalogue} surface only -- every other request path
 * passes straight through {@link #doFilterInternal}, same convention as
 * TST-031's {@code RateLimitFilter} guarding {@code /rate-limited/**}.
 *
 * <p>Wraps the response in a {@link ContentCachingResponseWrapper} so the
 * body {@link CatalogueController} writes can be hashed into a strong
 * {@code ETag} <em>before</em> anything reaches the client -- the wrapper
 * buffers writes internally and only forwards them to the real response once
 * {@link ContentCachingResponseWrapper#copyBodyToResponse()} is called, which
 * is exactly the hook this filter needs to inspect, and conditionally
 * suppress, the body.
 *
 * <p>Three outcomes once the body is known:
 * <ul>
 *   <li>The request's {@code If-None-Match} matches the computed ETag --
 *       responds {@code 304 Not Modified} with no body at all (the wrapper's
 *       buffered content is simply never copied to the real response).</li>
 *   <li>The request's {@code Accept-Encoding} contains {@code gzip} -- the
 *       buffered body is gzip-compressed and written directly to the real,
 *       un-wrapped response so the client sees a genuinely-compressed
 *       payload, not just a claimed one. This SUT does its own gzip rather
 *       than relying solely on Tomcat's {@code server.compression.enabled}
 *       connector-level compression, because MockMvc-based tests such as
 *       {@code CachePolicyTest#compressesWhenClientAcceptsGzip} dispatch
 *       in-process and never pass through the servlet container, so
 *       connector-level compression would never be exercised by them.
 *       {@code server.compression.enabled=true} stays set in {@code
 *       application.properties} regardless, for the real deployed
 *       container Task 14 builds; Tomcat's compression valve skips
 *       responses that already carry a {@code Content-Encoding} header, so
 *       the two mechanisms do not double-compress.</li>
 *   <li>Otherwise -- the buffered body is copied to the real response
 *       unchanged.</li>
 * </ul>
 *
 * <p>The {@code cache-headers-absent} defect disables this filter outright:
 * no ETag, no Cache-Control, no conditional-request handling, no
 * application-level compression -- proving the capability can fail for the
 * right reason.
 */
@Component
public class CachePolicyFilter extends OncePerRequestFilter {

    private static final String GUARDED_PATH = "/catalogue";
    private static final String DEFECT_FLAG = "cache-headers-absent";
    private static final String CACHE_CONTROL_VALUE = "public, max-age=60";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(GUARDED_PATH) || DefectFlags.isActive(DEFECT_FLAG)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapper);

        byte[] body = wrapper.getContentAsByteArray();
        String etag = strongETag(body);

        wrapper.setHeader(HttpHeaders.ETAG, etag);
        wrapper.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE);

        if (etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
            wrapper.setStatus(HttpStatus.NOT_MODIFIED.value());
            // Deliberately do not call copyBodyToResponse(): the wrapper's
            // buffered content never reaches the real response, so the
            // client sees no body at all.
            return;
        }

        String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            byte[] compressed = gzip(body);
            wrapper.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            wrapper.setContentLength(compressed.length);
            response.getOutputStream().write(compressed);
            response.getOutputStream().flush();
            return;
        }

        wrapper.copyBodyToResponse();
    }

    /** Strong ETag: a SHA-256 hash of the exact response body, quoted per
     *  RFC 9110 §8.8.3. Two identical bodies always hash to the same ETag,
     *  which is what makes {@code matchingIfNoneMatchYieldsNotModifiedWithNoBody}
     *  deterministic across separate requests. */
    private String strongETag(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "\"" + hex + "\"";
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-mandatory algorithm (see MessageDigest's own
            // javadoc); this branch is unreachable on any conforming JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private byte[] gzip(byte[] body) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(body);
        }
        return buffer.toByteArray();
    }
}
