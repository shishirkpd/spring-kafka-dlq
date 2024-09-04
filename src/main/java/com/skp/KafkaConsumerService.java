package com.skp;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Service
public class KafkaConsumerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${dlq.reprocess-topic}")
    private String reprocessTopic;

    public KafkaConsumerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${dlq.topic}", groupId = "my-group")
    public void listen(ConsumerRecord<String, String> record) {
        System.out.println("Received message: " + record.value());

        // Simulate a processing failure
        if (record.value().contains("fail")) {
            throw new RuntimeException("Failed to process message");
        }

        // Process the message
        System.out.println("Successfully processed message: " + record.value());
    }

    @KafkaListener(topics = "${dlq.topic}.DLT", groupId = "my-group-dlq")

    public void listenToDLQ(ConsumerRecord<String, String> record) {
        System.out.println("Received message in DLQ: " + record.value());

        // Attempt to reprocess the message
        try {
            if (record.value().contains("reprocess")) {
                throw new RuntimeException("Failed to reprocess message");
            }
            System.out.println("Successfully reprocessed message: " + record.value());
        } catch (Exception e) {
            // If reprocessing fails, send it to another topic for manual intervention
            kafkaTemplate.send(reprocessTopic, record.value());
            System.err.println("Failed to reprocess message, sent to manual intervention topic: " + record.value());
        }
    }
}
