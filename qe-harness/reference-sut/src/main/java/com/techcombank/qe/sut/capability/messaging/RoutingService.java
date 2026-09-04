package com.techcombank.qe.sut.capability.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * TST-026 message transformation and routing capability.
 *
 * <p><b>Every field maps or is a documented discard (I1)</b> -- the transform
 * copies the declared set and appends the resolved route; anything else is a
 * schema violation the published contract rejects rather than a silent pass.
 *
 * <p><b>Amounts travel as scaled strings, not JSON numbers (I5).</b> A JSON
 * number lets a parser normalise 1500.00 to 1500, which would destroy exactly
 * the scale the invariant exists to protect. Comparison is by
 * {@code BigDecimal.compareTo}, never {@code equals}.
 *
 * <p><b>Defect injection:</b> {@code route-default-fallthrough} rewrites an
 * unmatched routing key to a real one, so an unroutable message reaches a live
 * queue and the quarantine stays empty -- I2 alone fails while transformation
 * fidelity (I1/I5/I6) is untouched.
 */
@Service
public class RoutingService {

    /** Thrown when a message carries an enum member outside the declared domain. */
    public static class UnmappedEnum extends RuntimeException {
        public UnmappedEnum(String field, String value) {
            super("unmapped " + field + ": " + value);
        }
    }

    private static final Set<String> KINDS = Set.of("CREDIT", "DEBIT", "REVERSAL");

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final MessageLog log;
    private final ObjectMapper mapper = new ObjectMapper();

    public RoutingService(RabbitTemplate rabbit, MessagingTopology topology, MessageLog log) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.log = log;
    }

    public void publish(String routingKey, String payload) {
        topology.declareTopology();
        String transformed = transform(payload);

        String effectiveKey = routingKey;
        if (DefectFlags.isActive("route-default-fallthrough")
                && !routingKey.startsWith("pay.domestic.")
                && !routingKey.startsWith("pay.intl.")) {
            // The defect: an unmatched key is rewritten to a real one rather
            // than being left to the alternate exchange, so quarantine stays
            // empty and I2 fails. Transformation is untouched.
            effectiveKey = "pay.domestic.credit";
        }

        log.recordPublished(effectiveKey, null);
        rabbit.convertAndSend(MessagingTopology.ROUTE_EXCHANGE, effectiveKey, transformed);
    }

    /** Transforms and appends the resolved route. Rejects an unmapped enum
     *  rather than defaulting it (I3). */
    public String transform(String payload) {
        try {
            ObjectNode in = (ObjectNode) mapper.readTree(payload);
            String kind = in.path("kind").asText();
            if (!KINDS.contains(kind)) {
                throw new UnmappedEnum("kind", kind);
            }
            ObjectNode out = mapper.createObjectNode();
            out.put("messageId", in.path("messageId").asText());
            out.put("kind", kind);
            out.put("currency", in.path("currency").asText());
            // Kept as a string so the declared scale survives (I5).
            out.put("amount", in.path("amount").asText());
            // Written through as-is so diacritics stay byte-identical (I6).
            out.put("party", in.path("party").asText());
            out.put("route", "domestic");
            return mapper.writeValueAsString(out);
        } catch (UnmappedEnum e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot transform payload", e);
        }
    }

    public BigDecimal amountOf(String message) {
        try {
            JsonNode node = mapper.readTree(message);
            return new BigDecimal(node.path("amount").asText());
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot read amount", e);
        }
    }

    public String currencyOf(String message) {
        try {
            return mapper.readTree(message).path("currency").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot read currency", e);
        }
    }
}
