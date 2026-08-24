package com.techcombank.qe.harness.evidence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class EvidenceEmitter {
    private final Path outputDir;
    private final ObjectMapper mapper;

    public EvidenceEmitter(Path outputDir) {
        this.outputDir = outputDir;
        this.mapper = new ObjectMapper();
    }

    public Path emit(RunFragment fragment) throws Exception {
        // Create filename: <ISO-instant>-<archetype>.json
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        String filename = timestamp + "-" + fragment.archetype() + ".json";
        Path outputPath = outputDir.resolve(filename);

        // Compute repo-relative path from current working directory
        Path repoRoot = Paths.get(System.getProperty("user.dir"));
        String reportPath = repoRoot.relativize(outputPath).toString();

        // Build JSON with proper escaping using Jackson utilities and manual formatting
        String json = buildJSON(fragment, reportPath);

        // Write to file
        Files.createDirectories(outputDir);
        Files.writeString(outputPath, json);

        return outputPath;
    }

    private String buildJSON(RunFragment frag, String reportPath) throws JsonProcessingException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        append(json, "  ", "archetype", escapeJson(frag.archetype()), true);
        append(json, "  ", "module", escapeJson(frag.module()), true);
        append(json, "  ", "service_name", escapeJson(frag.serviceName()), true);
        append(json, "  ", "tier", escapeJson(frag.tier()), true);
        append(json, "  ", "oracle", escapeJson(frag.oracle()), true);
        append(json, "  ", "result", escapeJson(frag.result().wire()), true);

        if (!frag.invariants().isEmpty()) {
            json.append(",\n  \"invariants\": [\n");
            for (int i = 0; i < frag.invariants().size(); i++) {
                RunFragment.Entry entry = frag.invariants().get(i);
                json.append("    {\n");
                append(json, "      ", "id", escapeJson(entry.id()), true);
                append(json, "      ", "description", escapeJson(entry.description()), true);
                append(json, "      ", "result", escapeJson(entry.result().wire()), false);
                json.append("\n    }");
                if (i < frag.invariants().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        if (!frag.thresholds().isEmpty()) {
            json.append(",\n  \"thresholds\": [\n");
            for (int i = 0; i < frag.thresholds().size(); i++) {
                RunFragment.Threshold threshold = frag.thresholds().get(i);
                json.append("    {\n");
                append(json, "      ", "name", escapeJson(threshold.name()), true);
                append(json, "      ", "threshold_ref", escapeJson(threshold.thresholdRef()), true);
                append(json, "      ", "result", escapeJson(threshold.result().wire()), threshold.reason() == null);
                if (threshold.reason() != null) {
                    append(json, "      ", "reason", escapeJson(threshold.reason()), false);
                }
                json.append("\n    }");
                if (i < frag.thresholds().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        json.append(",\n  \"evidence\": {\n");
        append(json, "    ", "executed_on", escapeJson(frag.executedOn().toString()), true);
        append(json, "    ", "environment", escapeJson(frag.environment()), true);
        if (frag.sutDefect() != null) {
            append(json, "    ", "sut_defect", escapeJson(frag.sutDefect()), true);
        } else {
            json.append("    \"sut_defect\": null,\n");
        }
        append(json, "    ", "report_path", escapeJson(reportPath), false);
        json.append("\n  }\n");
        json.append("}\n");

        return json.toString();
    }

    private void append(StringBuilder sb, String indent, String key, String value, boolean comma) {
        sb.append(indent).append("\"").append(key).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(value).append("\"");
        }
        if (comma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private String escapeJson(String value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        // Use Jackson's escaping - writeValueAsString wraps in quotes, so strip them
        String quoted = mapper.writeValueAsString(value);
        return quoted.substring(1, quoted.length() - 1);
    }
}
