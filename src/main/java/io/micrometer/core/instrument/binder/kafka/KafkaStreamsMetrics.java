package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.apache.kafka.streams.KafkaStreams;

/**
 * Kafka Streams binder.
 *
 * @author Jorge Quilcate
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html">Kakfa monitoring
 * documentation</a>
 * @since 1.3.0
 */
@Incubating(since = "1.3.0")
@NonNullApi
@NonNullFields
public final class KafkaStreamsMetrics extends KafkaApiMetrics {

  public KafkaStreamsMetrics(KafkaStreams kafkaStreams) {
    super(kafkaStreams::metrics);
  }

  public KafkaStreamsMetrics(KafkaStreams kafkaStreams, Iterable<Tag> tags) {
    super(kafkaStreams::metrics, tags);
  }
}
