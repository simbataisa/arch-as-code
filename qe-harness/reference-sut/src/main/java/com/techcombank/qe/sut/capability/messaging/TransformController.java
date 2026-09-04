package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** TST-026's HTTP surface. */
@RestController
public class TransformController {

    private final RoutingService routing;
    private final RabbitAdmin admin;

    public TransformController(RoutingService routing, RabbitAdmin admin) {
        this.routing = routing;
        this.admin = admin;
    }

    /** POST /messaging/publish?routingKey=pay.domestic.credit -> 202, or 422 on
     *  an unmapped enum (I3: rejected, never defaulted). */
    @PostMapping("/messaging/publish")
    public ResponseEntity<?> publish(@RequestParam String routingKey, @RequestBody String payload) {
        try {
            routing.publish(routingKey, payload);
            return ResponseEntity.accepted().build();
        } catch (RoutingService.UnmappedEnum e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    /** POST /messaging/transform -> the transformed message, for the module's
     *  contract-schema oracle to validate without consuming from a queue. */
    @PostMapping(value = "/messaging/transform", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transform(@RequestBody String payload) {
        try {
            return ResponseEntity.ok(routing.transform(payload));
        } catch (RoutingService.UnmappedEnum e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    /** GET /messaging/routed -> per-queue depths, so I2's verdict is one call. */
    @GetMapping("/messaging/routed")
    public RoutedResponse routed() {
        return new RoutedResponse(
            depthOf(MessagingTopology.Q_DOMESTIC),
            depthOf(MessagingTopology.Q_INTL),
            depthOf(MessagingTopology.Q_UNROUTABLE));
    }

    /** GET /messaging/contract -> the published JSON Schema the module validates against. */
    @GetMapping(value = "/messaging/contract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> contract() throws IOException {
        String schema = new String(
            new ClassPathResource("contracts/payment-message.schema.json").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.OK).body(schema);
    }

    private long depthOf(String queue) {
        Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public record RoutedResponse(long domestic, long intl, long quarantine) {}
}
