package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Kafka admin binder.
 *
 * @author Jorge Quilcate
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html">Kakfa monitoring
 * documentation</a>
 * @since 1.3.0
 */
@Incubating(since = "1.3.0")
@NonNullApi
@NonNullFields
public final class KafkaAdminMetrics extends KafkaMetrics {
  public KafkaAdminMetrics(AdminClient adminClient) {
    super(adminClient::metrics);
  }

  public KafkaAdminMetrics(AdminClient adminClient, Iterable<Tag> tags) {
    super(adminClient::metrics, tags);
  }
}
