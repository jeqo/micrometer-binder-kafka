package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class KafkaClientMetricsIT {
    @Container
    private KafkaContainer kafkaContainer = new KafkaContainer("5.3.0");

    @Test
    void should_manage_producer_and_consumer_metrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertEquals(0, registry.getMeters().size());

        Properties producerConfigs = new Properties();
        producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        Producer<String, String> producer = new KafkaProducer<>(
                producerConfigs, new StringSerializer(), new StringSerializer());

        new KafkaMetrics(producer).bindTo(registry);

        int producerMetrics = registry.getMeters().size();
        assertTrue(producerMetrics > 0);

        Properties consumerConfigs = new Properties();
        consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        Consumer<String, String> consumer = new KafkaConsumer<>(
                consumerConfigs, new StringDeserializer(), new StringDeserializer());

        new KafkaMetrics(consumer).bindTo(registry);

        int producerAndConsumerMetrics = registry.getMeters().size();
        assertTrue(producerAndConsumerMetrics > producerMetrics);

        String topic = "test";
        producer.send(new ProducerRecord<>(topic, "key", "value"));
        producer.flush();

        registry.getMeters().forEach(meter -> {
            System.out.println(meter.getId() + " => " + meter.measure());
        });

        int producerAndConsumerMetricsAfterSend = registry.getMeters().size();
        assertTrue(producerAndConsumerMetricsAfterSend > producerAndConsumerMetrics);

        consumer.subscribe(Collections.singletonList(topic));

        consumer.poll(Duration.ofMillis(100));

        registry.getMeters().forEach(meter -> {
            System.out.println(meter.getId() + " => " + meter.measure());
        });

        int producerAndConsumerMetricsAfterPoll = registry.getMeters().size();
        assertTrue(producerAndConsumerMetricsAfterPoll > producerAndConsumerMetricsAfterSend);

        registry.getMeters().forEach(meter -> {
            System.out.println(meter.getId() + " => " + meter.measure());
        });
    }
}
