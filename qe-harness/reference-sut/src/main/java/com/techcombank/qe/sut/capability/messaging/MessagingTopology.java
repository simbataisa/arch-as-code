package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The messaging topology for TST-026/027/028/029.
 *
 * <p>Declared as {@link Declarables} in code rather than a mounted
 * {@code definitions.json}: less compose surface, and the shape becomes
 * unit-testable -- which matters, because two of its properties ARE invariants.
 *
 * <p><b>No catch-all binding on {@code qe.route}.</b> Only
 * {@code pay.domestic.*} and {@code pay.intl.*} are bound, with an
 * alternate-exchange path sending everything else to {@code qe.q.unroutable}.
 * A {@code #} binding would make TST-026's I2 ("zero messages on the default
 * route") trivially true and therefore worthless. The quarantine queue is what
 * makes an unmatched key observable rather than merely absent.
 *
 * <p><b>Single active consumer on {@code qe.q.sequence}.</b> TST-027's ordering
 * scope is declared {@code per_key}: RabbitMQ has no partitions, so the
 * archetype's per-partition scope is out of scope here and the module reports
 * {@code coverage: partial} for that reason.
 *
 * <p><b>Every queue is durable</b> so TST-029's I2 (nothing acked-persisted is
 * lost across a broker restart) is possible at all.
 *
 * <p>Declaration happens on first use, not at context refresh: the
 * {@link RabbitAdmin} is {@code autoStartup=false} precisely so an absent
 * broker cannot fail the {@code core} profile's startup (Task 14).
 */
@Component
public class MessagingTopology {

    static final String IN_EXCHANGE = "qe.in";
    static final String ROUTE_EXCHANGE = "qe.route";
    static final String FANOUT_EXCHANGE = "qe.fanout";
    static final String DLX = "qe.dlx";
    static final String UNROUTABLE_EXCHANGE = "qe.unroutable";

    static final String Q_DOMESTIC = "qe.q.route.domestic";
    static final String Q_INTL = "qe.q.route.intl";
    static final String Q_UNROUTABLE = "qe.q.unroutable";
    static final String Q_SEQUENCE = "qe.q.sequence";
    static final String Q_BRANCH_A = "qe.q.branch.a";
    static final String Q_BRANCH_B = "qe.q.branch.b";
    static final String Q_BRANCH_C = "qe.q.branch.c";
    static final String Q_AGGREGATE = "qe.q.aggregate";
    static final String Q_WORK = "qe.q.work";
    static final String Q_DLQ = "qe.q.dlq";

    private final RabbitAdmin admin;
    private final int maxDeliveryAttempts;
    private final List<Long> retryIntervalsMs;
    private volatile boolean declared;

    public MessagingTopology(RabbitAdmin admin,
                             @Value("${app.messaging.max-delivery-attempts}") int maxDeliveryAttempts,
                             @Value("${app.messaging.retry-intervals-ms}") List<Long> retryIntervalsMs) {
        this.admin = admin;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
        this.retryIntervalsMs = List.copyOf(retryIntervalsMs);
    }

    public List<Long> retryIntervalsMs() {
        return retryIntervalsMs;
    }

    public Declarables declarables() {
        List<org.springframework.amqp.core.Declarable> objects = new ArrayList<>();

        DirectExchange in = new DirectExchange(IN_EXCHANGE, true, false);
        // The alternate exchange belongs on qe.route, NOT on qe.in: TST-026's I2
        // is about qe.route's bindings, and an unmatched pay.* key would
        // otherwise be dropped by the broker (or returned to the publisher via
        // setMandatory) rather than parked somewhere the harness can read a
        // depth from. Quarantine is what makes "zero messages on the default
        // route" an observable claim instead of an absence.
        TopicExchange route = ExchangeBuilderCompat.topicWithAlternate(ROUTE_EXCHANGE, UNROUTABLE_EXCHANGE);
        FanoutExchange fanout = new FanoutExchange(FANOUT_EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        FanoutExchange unroutable = new FanoutExchange(UNROUTABLE_EXCHANGE, true, false);
        objects.add(in);
        objects.add(route);
        objects.add(fanout);
        objects.add(dlx);
        objects.add(unroutable);

        Queue domestic = QueueBuilder.durable(Q_DOMESTIC).build();
        Queue intl = QueueBuilder.durable(Q_INTL).build();
        Queue quarantine = QueueBuilder.durable(Q_UNROUTABLE).build();
        Queue sequence = QueueBuilder.durable(Q_SEQUENCE)
            .withArgument("x-single-active-consumer", true)
            .build();
        Queue branchA = QueueBuilder.durable(Q_BRANCH_A).build();
        Queue branchB = QueueBuilder.durable(Q_BRANCH_B).build();
        Queue branchC = QueueBuilder.durable(Q_BRANCH_C).build();
        Queue aggregate = QueueBuilder.durable(Q_AGGREGATE).build();
        // x-delivery-limit is a quorum-queue-only argument (RabbitMQ rejects it
        // on a classic queue with PRECONDITION_FAILED), so qe.q.work is
        // declared as a quorum queue to accept the bounded-retry limit.
        Queue work = QueueBuilder.durable(Q_WORK)
            .withArgument("x-queue-type", "quorum")
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-delivery-limit", maxDeliveryAttempts)
            .build();
        Queue dlq = QueueBuilder.durable(Q_DLQ).build();
        objects.addAll(List.of(domestic, intl, quarantine, sequence,
            branchA, branchB, branchC, aggregate, work, dlq));

        // Only real conditions are bound. No '#' -- see the class javadoc.
        objects.add(BindingBuilder.bind(domestic).to(route).with("pay.domestic.*"));
        objects.add(BindingBuilder.bind(intl).to(route).with("pay.intl.*"));
        objects.add(BindingBuilder.bind(quarantine).to(unroutable));
        objects.add(BindingBuilder.bind(sequence).to(in).with("sequence"));
        objects.add(BindingBuilder.bind(work).to(in).with("work"));
        objects.add(BindingBuilder.bind(dlq).to(dlx).with(Q_WORK));
        objects.add(BindingBuilder.bind(branchA).to(fanout));
        objects.add(BindingBuilder.bind(branchB).to(fanout));
        objects.add(BindingBuilder.bind(branchC).to(fanout));

        // Retry ladder: one queue per interval, each dead-lettering back to
        // qe.in's work binding once its TTL expires. Distinct TTLs are what
        // make TST-029 I4's distinct_intervals > 1 satisfiable.
        for (int i = 0; i < retryIntervalsMs.size(); i++) {
            String name = "qe.q.retry." + (i + 1);
            Queue retry = QueueBuilder.durable(name)
                .withArgument("x-message-ttl", retryIntervalsMs.get(i))
                .withArgument("x-dead-letter-exchange", IN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "work")
                .build();
            objects.add(retry);
            objects.add(BindingBuilder.bind(retry).to(dlx).with(name));
        }

        return new Declarables(objects);
    }

    /** Declares the topology on first use. Idempotent: RabbitMQ's declare
     *  operations are themselves idempotent, and the flag keeps repeat calls
     *  from re-walking the object list on every publish. */
    public void declareTopology() {
        if (declared) {
            return;
        }
        synchronized (this) {
            if (declared) {
                return;
            }
            declarables().getDeclarables().forEach(this::declare);
            declared = true;
        }
    }

    /** {@link RabbitAdmin} in the pinned Spring AMQP version (3.2.12) has no
     *  generic "declare this Declarable" method -- only typed
     *  {@code declareExchange}/{@code declareQueue}/{@code declareBinding}.
     *  This dispatches by runtime type so {@link #declareTopology()} can still
     *  walk the flat {@link #declarables()} list uniformly. */
    private void declare(Declarable declarable) {
        if (declarable instanceof Exchange exchange) {
            admin.declareExchange(exchange);
        } else if (declarable instanceof Queue queue) {
            admin.declareQueue(queue);
        } else if (declarable instanceof Binding binding) {
            admin.declareBinding(binding);
        } else {
            throw new IllegalStateException("unsupported declarable type: " + declarable.getClass());
        }
    }
}
