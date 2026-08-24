package com.techcombank.qe.harness.evidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class EvidenceEmitter {
    private final Path outputDir;

    public EvidenceEmitter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public Path emit(RunFragment fragment) throws Exception {
        // Create filename: <ISO-instant>-<archetype>.json
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        String filename = timestamp + "-" + fragment.archetype() + ".json";
        Path outputPath = outputDir.resolve(filename);

        // Build JSON string with proper formatting
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"archetype\": \"").append(fragment.archetype()).append("\",\n");
        json.append("  \"module\": \"").append(fragment.module()).append("\",\n");
        json.append("  \"service_name\": \"").append(fragment.serviceName()).append("\",\n");
        json.append("  \"tier\": \"").append(fragment.tier()).append("\",\n");
        json.append("  \"oracle\": \"").append(fragment.oracle()).append("\",\n");
        json.append("  \"result\": \"").append(fragment.result().wire()).append("\"");

        if (!fragment.invariants().isEmpty()) {
            json.append(",\n  \"invariants\": [\n");
            for (int i = 0; i < fragment.invariants().size(); i++) {
                RunFragment.Entry entry = fragment.invariants().get(i);
                json.append("    {\n");
                json.append("      \"id\": \"").append(entry.id()).append("\",\n");
                json.append("      \"description\": \"").append(entry.description()).append("\",\n");
                json.append("      \"result\": \"").append(entry.result().wire()).append("\"\n");
                json.append("    }");
                if (i < fragment.invariants().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        if (!fragment.thresholds().isEmpty()) {
            json.append(",\n  \"thresholds\": [\n");
            for (int i = 0; i < fragment.thresholds().size(); i++) {
                RunFragment.Threshold threshold = fragment.thresholds().get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(threshold.name()).append("\",\n");
                json.append("      \"threshold_ref\": \"").append(threshold.thresholdRef()).append("\",\n");
                json.append("      \"result\": \"").append(threshold.result().wire()).append("\"");
                if (threshold.reason() != null) {
                    json.append(",\n      \"reason\": \"").append(threshold.reason()).append("\"");
                }
                json.append("\n    }");
                if (i < fragment.thresholds().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }

        json.append(",\n  \"evidence\": {\n");
        json.append("    \"executed_on\": \"").append(fragment.executedOn().toString()).append("\",\n");
        json.append("    \"environment\": \"").append(fragment.environment()).append("\",\n");
        if (fragment.sutDefect() != null) {
            json.append("    \"sut_defect\": \"").append(fragment.sutDefect()).append("\",\n");
        } else {
            json.append("    \"sut_defect\": null,\n");
        }
        json.append("    \"report_path\": \"").append(outputPath.getFileName().toString()).append("\"\n");
        json.append("  }\n");
        json.append("}\n");

        // Write to file
        Files.createDirectories(outputDir);
        Files.writeString(outputPath, json.toString());

        return outputPath;
    }
}
