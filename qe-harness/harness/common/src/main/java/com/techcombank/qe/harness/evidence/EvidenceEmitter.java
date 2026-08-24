package com.techcombank.qe.harness.evidence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class EvidenceEmitter {
    private final Path outputDir;
    private final ObjectMapper mapper;

    public EvidenceEmitter(Path outputDir) {
        this.outputDir = outputDir;
        this.mapper = new ObjectMapper();

        // Omit null fields from JSON output (invariants, thresholds, reason, sut_defect)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Register JSR310 module for LocalDate serialization
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure DefaultPrettyPrinter with proper indentation
        // DefaultPrettyPrinter uses ": " (colon + space) by default for field separators
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        this.mapper.setDefaultPrettyPrinter(printer);
    }

    public Path emit(RunFragment fragment) throws Exception {
        // Create filename: <ISO-instant>-<archetype>.json
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        String filename = timestamp + "-" + fragment.archetype() + ".json";
        Path outputPath = outputDir.resolve(filename);

        // Compute repo-relative path by walking up directory tree to find "qe-harness"
        String reportPath = computeRepoRelativePath(outputPath);

        // Create wrapper that includes report_path for serialization
        EvidenceWrapper wrapper = new EvidenceWrapper(fragment, reportPath);

        // Write JSON using ObjectMapper with proper pretty printing
        Files.createDirectories(outputPath.getParent());
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(outputPath.toFile(), wrapper);

        return outputPath;
    }

    /**
     * Walk up from the output path looking for a parent directory named "qe-harness".
     * If found, return the path relative to qe-harness's parent (the repo root).
     * If not found (e.g., @TempDir outside repo), return bare filename.
     * All paths are normalized to absolute before comparison to avoid Path.relativize() errors.
     */
    private String computeRepoRelativePath(Path outputPath) {
        Path absolute = outputPath.toAbsolutePath().normalize();
        Path current = absolute.getParent();

        while (current != null) {
            Path fileName = current.getFileName();
            if (fileName != null && fileName.toString().equals("qe-harness")) {
                // Found qe-harness; repo root is its parent
                Path repoRoot = current.getParent();
                if (repoRoot != null) {
                    try {
                        return repoRoot.relativize(absolute).toString();
                    } catch (IllegalArgumentException e) {
                        // Fallback if relativize fails (shouldn't happen with absolute paths)
                        return absolute.getFileName().toString();
                    }
                }
            }
            current = current.getParent();
        }

        // No "qe-harness" ancestor found; use bare filename (e.g., @TempDir test case)
        Path fileName = absolute.getFileName();
        return fileName != null ? fileName.toString() : absolute.toString();
    }

    /**
     * Wrapper DTO for serialization. Includes RunFragment data plus computed report_path.
     * Jackson will serialize this using field names and @JsonValue annotations from RunFragment.
     */
    static class EvidenceWrapper {
        public final String archetype;
        public final String module;
        public final String service_name;
        public final String tier;
        public final String oracle;
        public final String result;
        public final java.util.List<EvidenceWrapper.InvariantData> invariants;
        public final java.util.List<EvidenceWrapper.ThresholdData> thresholds;
        public final EvidenceWrapper.EvidenceData evidence;

        EvidenceWrapper(RunFragment frag, String reportPath) {
            this.archetype = frag.archetype();
            this.module = frag.module();
            this.service_name = frag.serviceName();
            this.tier = frag.tier();
            this.oracle = frag.oracle();
            this.result = frag.result().wire();
            this.invariants = frag.invariants().isEmpty() ? null : frag.invariants().stream()
                .map(InvariantData::new)
                .toList();
            this.thresholds = frag.thresholds().isEmpty() ? null : frag.thresholds().stream()
                .map(ThresholdData::new)
                .toList();
            this.evidence = new EvidenceData(frag, reportPath);
        }

        static class InvariantData {
            public final String id;
            public final String description;
            public final String result;

            InvariantData(RunFragment.Entry entry) {
                this.id = entry.id();
                this.description = entry.description();
                this.result = entry.result().wire();
            }
        }

        static class ThresholdData {
            public final String name;
            public final String threshold_ref;
            public final String result;
            public final String reason;

            ThresholdData(RunFragment.Threshold threshold) {
                this.name = threshold.name();
                this.threshold_ref = threshold.thresholdRef();
                this.result = threshold.result().wire();
                this.reason = threshold.reason();
            }
        }

        static class EvidenceData {
            public final String executed_on;
            public final String environment;
            public final String sut_defect;
            public final String report_path;

            EvidenceData(RunFragment frag, String reportPath) {
                this.executed_on = frag.executedOn().toString();
                this.environment = frag.environment();
                this.sut_defect = frag.sutDefect();
                this.report_path = reportPath;
            }
        }
    }
}
