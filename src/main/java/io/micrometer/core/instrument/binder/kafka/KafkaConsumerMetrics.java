package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Kafka consumer binder.
 *
 * @author Jorge Quilcate
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html">Kakfa monitoring
 * documentation</a>
 * @since 1.3.0
 */
@Incubating(since = "1.3.0")
@NonNullApi
@NonNullFields
public final class KafkaConsumerMetrics extends KafkaApiMetrics {
  public KafkaConsumerMetrics(Consumer<?, ?> kafkaConsumer) {
    super(kafkaConsumer::metrics);
  }

  public KafkaConsumerMetrics(Consumer<?, ?> kafkaConsumer, Iterable<Tag> tags) {
    super(kafkaConsumer::metrics, tags);
  }
}
