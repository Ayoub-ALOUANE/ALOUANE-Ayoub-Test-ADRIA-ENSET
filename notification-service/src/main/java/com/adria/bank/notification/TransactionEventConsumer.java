package com.adria.bank.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes bank.transactions.created events and journals them.
 * Does NOT modify banking data.
 */
@Component
@Slf4j
public class TransactionEventConsumer {

    @KafkaListener(
            topics = "${bank.kafka.topic.transactions-created:bank.transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onTransactionCreated(@Payload TransactionCreatedEvent event) {
        log.info(
                "JOURNAL | eventId={} type={} txId={} accountId={} amount={} {} occurredAt={}",
                event.eventId(),
                event.eventType(),
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.occurredAt()
        );
    }
}
