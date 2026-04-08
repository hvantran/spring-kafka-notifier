package com.hoatv.kafka.notifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SpringKafkaNotifierApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"spring-kafka-notifier-test"})
@DisplayName("Spring Kafka Notifier Application Tests")
class SpringKafkaNotifierApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
    }
}
