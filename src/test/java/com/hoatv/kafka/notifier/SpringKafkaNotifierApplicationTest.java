package com.hoatv.kafka.notifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SpringKafkaNotifierApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"spring-kafka-notifier-test"})
@DisplayName("Spring Kafka Notifier Application Tests")
class SpringKafkaNotifierApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }
}
