package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.core.TopicExchange;

import java.util.Map;

/**
 * The alternate-exchange argument is passed as a raw argument map rather than
 * through a first-class setter, so it is applied here rather than inline,
 * keeping {@link MessagingTopology#declarables()} readable.
 *
 * <p>The alternate exchange is what turns an unroutable message into an
 * observable one -- TST-026's I2 reads the quarantine queue's depth as its
 * verdict, which is only possible because the broker parks it instead of
 * discarding it.
 */
final class ExchangeBuilderCompat {

    private ExchangeBuilderCompat() {
    }

    static TopicExchange topicWithAlternate(String name, String alternateExchange) {
        return new TopicExchange(name, true, false,
            Map.of("alternate-exchange", alternateExchange));
    }
}
