package io.micrometer.core.instrument.binder.kafka;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.NonNullFields;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.streams.KafkaStreams;

import static java.util.Collections.emptyList;

/**
 * Kafka metrics binder.
 * <p>
 * It is based on {@code metrics()} method returning {@link Metric} map exposed by clients and
 * streams interface.
 *
 * @author Jorge Quilcate
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html">Kakfa monitoring
 * documentation</a>
 * @since 1.3.0
 */
@Incubating(since = "1.3.0")
@NonNullApi
@NonNullFields
public class KafkaMetrics implements MeterBinder {
  private static final String METRIC_NAME_PREFIX = "kafka.";

  private final Supplier<Map<MetricName, ? extends Metric>> metricsSupplier;

  private final Iterable<Tag> extraTags;

  /**
   * Keep track of number of metrics, when this changes, metrics are re-bind.
   */
  private AtomicInteger currentSize = new AtomicInteger(0);

  public KafkaMetrics(Producer<?, ?> kafkaProducer, Iterable<Tag> tags) {
    this(kafkaProducer::metrics, tags);
  }

  public KafkaMetrics(Producer<?, ?> kafkaProducer) {
    this(kafkaProducer::metrics);
  }

  public KafkaMetrics(Consumer<?, ?> kafkaConsumer, Iterable<Tag> tags) {
    this(kafkaConsumer::metrics, tags);
  }

  public KafkaMetrics(Consumer<?, ?> kafkaConsumer) {
    this(kafkaConsumer::metrics);
  }

  public KafkaMetrics(KafkaStreams kafkaStreams, Iterable<Tag> tags) {
    this(kafkaStreams::metrics, tags);
  }

  public KafkaMetrics(KafkaStreams kafkaStreams) {
    this(kafkaStreams::metrics);
  }

  public KafkaMetrics(AdminClient adminClient, Iterable<Tag> tags) {
    this(adminClient::metrics, tags);
  }

  public KafkaMetrics(AdminClient adminClient) {
    this(adminClient::metrics);
  }

  KafkaMetrics(Supplier<Map<MetricName, ? extends Metric>> metricsSupplier) {
    this.metricsSupplier = metricsSupplier;
    this.extraTags = emptyList();
  }

  KafkaMetrics(Supplier<Map<MetricName, ? extends Metric>> metricsSupplier,
      Iterable<Tag> extraTags) {
    this.metricsSupplier = metricsSupplier;
    this.extraTags = extraTags;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    checkAndRegisterMetrics(registry);
  }

  private void checkAndRegisterMetrics(MeterRegistry registry) {
    Map<MetricName, ? extends Metric> metrics = metricsSupplier.get();
    if (currentSize.get() != metrics.size()) {
      currentSize.set(metrics.size());
      metrics.forEach((metricName, metric) -> {
        if (metric.metricName().name().endsWith("total")
            || metric.metricName().name().endsWith("count")) {
          registerCounter(registry, metric, extraTags);
        } else if (metric.metricName().name().endsWith("min")
            || metric.metricName().name().endsWith("max")
            || metric.metricName().name().endsWith("avg")) {
          registerGauge(registry, metric, extraTags);
        } else if (metric.metricName().name().endsWith("rate")) {
          registerTimeGauge(registry, metric, extraTags);
        } else { // this filter might need to be more extensive.
          registerCounter(registry, metric, extraTags);
        }
      });
    }
  }

  private void registerTimeGauge(MeterRegistry registry, Metric metric, Iterable<Tag> extraTags) {
    TimeGauge.builder(
        metricName(metric), metric, TimeUnit.SECONDS, m -> {
          checkAndRegisterMetrics(registry);
          if (m.metricValue() instanceof Double) {
            return (double) m.metricValue();
          } else {
            return Double.NaN;
          }
        })
        .tags(metric.metricName().tags()
            .entrySet()
            .stream()
            .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()))
        .tags(extraTags)
        .description(metric.metricName().description())
        .register(registry);
  }

  private void registerGauge(MeterRegistry registry, Metric metric,
      Iterable<Tag> extraTags) {
    Gauge.builder(
        metricName(metric), metric, m -> {
          checkAndRegisterMetrics(registry);
          if (m.metricValue() instanceof Double) {
            return (double) m.metricValue();
          } else {
            return Double.NaN;
          }
        })
        .tags(metric.metricName().tags()
            .entrySet()
            .stream()
            .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()))
        .tags(extraTags)
        .description(metric.metricName().description())
        .register(registry);
  }

  private void registerCounter(MeterRegistry registry, Metric metric,
      Iterable<Tag> extraTags) {
    FunctionCounter.builder(
        metricName(metric), metric, m -> {
          checkAndRegisterMetrics(registry);
          if (m.metricValue() instanceof Double) {
            return (double) m.metricValue();
          } else {
            return Double.NaN;
          }
        })
        .tags(metric.metricName().tags()
            .entrySet()
            .stream()
            .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()))
        .tags(extraTags)
        .description(metric.metricName().description())
        .register(registry);
  }

  private String metricName(Metric metric) {
    String value =
        METRIC_NAME_PREFIX + metric.metricName().group() + "." + metric.metricName().name();
    return value.replaceAll("-", ".");
  }
}
