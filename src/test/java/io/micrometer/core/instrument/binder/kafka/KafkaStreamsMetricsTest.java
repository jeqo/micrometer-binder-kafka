package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Properties;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;

class KafkaStreamsMetricsTest {
  private final static String BOOTSTRAP_SERVERS = "localhost:9092";
  private Tags tags = Tags.of("app", "myapp", "version", "1");

  @Test void verify() {
    try (KafkaStreams kafkaStreams = createStreams()) {
      KafkaMetrics metrics = new KafkaMetrics(kafkaStreams, tags);
      MeterRegistry registry = new SimpleMeterRegistry();

      metrics.bindTo(registry);

      registry.get("kafka.admin.client.metrics.connection.close.total").tags(tags).functionCounter();
      registry.get("kafka.producer.metrics.batch.size.max").tags(tags).gauge();
      registry.get("kafka.consumer.metrics.request.total").tags(tags).functionCounter();
    }
  }

  private KafkaStreams createStreams() {
    StreamsBuilder builder = new StreamsBuilder();
    builder.stream("input").to("output");
    Properties streamsConfig = new Properties();
    streamsConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    streamsConfig.put(StreamsConfig.APPLICATION_ID_CONFIG, "app");
    return new KafkaStreams(builder.build(), streamsConfig);
  }
}
