package com.adria.bank.transaction.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Value("${bank.kafka.topic.transactions-created:bank.transactions.created}")
    private String topic;

    @Value("${bank.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void publish(TransactionCreatedEvent event) {
        if (!kafkaEnabled) {
            log.warn("Kafka disabled — skipping publish of event {}", event.eventId());
            return;
        }
        try {
            kafkaTemplate.send(topic, event.accountId().toString(), event);
            log.info("Published TransactionCreated eventId={} accountId={} txId={}",
                    event.eventId(), event.accountId(), event.transactionId());
        } catch (Exception ex) {
            log.error("Failed to publish Kafka event: {}", ex.getMessage());
        }
    }
}
