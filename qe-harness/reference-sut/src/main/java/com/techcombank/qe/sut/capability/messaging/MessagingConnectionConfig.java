package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Broker connection wiring for the messaging capability (Wave 17).
 *
 * <p><b>Why every bean here is lazy about its socket:</b> reference-sut is in
 * compose profile {@code ["core"]} and broker is in {@code ["messaging"]}, so
 * the overwhelmingly common case -- {@code make up PROFILES=core}, which every
 * pre-Wave-17 module uses -- has no broker at all. A connection attempt at
 * context startup would fail the container's {@code /_capabilities}
 * healthcheck and break seven working modules. {@code CoreProfileBootTest}
 * pins this.
 *
 * <p>{@code RabbitAdmin.setAutoStartup(false)} is the load-bearing line: the
 * default {@code RabbitAdmin} declares every {@code Declarables} bean during
 * context refresh, which opens a connection. With auto-startup off, the
 * topology is declared on first use instead -- see
 * {@link MessagingTopology#declareTopology()}.
 *
 * <p>Connection retry is capped rather than infinite so a module run against a
 * genuinely absent broker fails fast with a legible error instead of hanging:
 * Spring AMQP's default is to retry indefinitely.
 *
 * <p><b>Why {@code RabbitTemplate.setMandatory(true)} is paired with
 * {@code CachingConnectionFactory.setPublisherReturns(true)}:</b> per Spring
 * AMQP's own contract, a publisher return is only delivered to the
 * application when BOTH flags are set -- {@code mandatory} on the template
 * asks the broker to return (rather than silently drop) a message it cannot
 * route to any queue, and {@code publisherReturns} on the connection factory
 * is what makes the underlying channel actually listen for and dispatch that
 * returned-message callback. Setting only {@code mandatory} compiles and
 * looks correct, but an unroutable message is still silently dropped from the
 * application's point of view. This matters beyond this task: TST-026's
 * alternate-exchange quarantine invariant can only be observed by a later
 * routing module if a returned/unroutable message is actually visible here.
 */
@Configuration
public class MessagingConnectionConfig {

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password,
            @Value("${app.messaging.connection-timeout-ms}") int connectionTimeoutMs) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setConnectionTimeout(connectionTimeoutMs);
        factory.setPublisherReturns(true);
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(false);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }
}
