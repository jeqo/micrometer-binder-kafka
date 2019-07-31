/**
 * Copyright 2018 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

import static java.util.Collections.emptyList;

/**
 * Kafka consumer metrics collected from metrics exposed by Kafka consumers via the MBeanServer.
 * Metrics are exposed at each consumer thread.
 * <p>
 * Metric names here are based on the naming scheme as it was last changed in Kafka version 0.11.0.
 * Metrics for earlier versions of Kafka will not report correctly.
 *
 * @author Jorge Quilcate
 * @see <a href="https://docs.confluent.io/current/kafka/monitoring.html">Kakfa monitoring
 * documentation</a>
 * @since 1.3.0
 */
@Incubating(since = "1.3.0")
@NonNullApi
@NonNullFields
abstract class KafkaMetrics implements MeterBinder {
  private static final String METRIC_NAME_PREFIX = "kafka.";

  private final Supplier<Map<MetricName, ? extends Metric>> metricsSupplier;

  private final Iterable<Tag> extraTags;

  private AtomicInteger currentSize = new AtomicInteger(0);

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
    registerMetrics(registry);
  }

  private void registerMetrics(MeterRegistry registry) {
    Map<MetricName, ? extends Metric> metrics = metricsSupplier.get();
    if (currentSize.get() != metrics.size()) {
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
        } else {
          registerCounter(registry, metric, extraTags);
        }
      });
      currentSize.set(metrics.size());
    }
  }

  private void registerTimeGauge(MeterRegistry registry, Metric metric, Iterable<Tag> extraTags) {
    TimeGauge.builder(
        metricName(metric), metric, TimeUnit.SECONDS, m -> {
          registerMetrics(registry);
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
          registerMetrics(registry);
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
          registerMetrics(registry);
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
    return METRIC_NAME_PREFIX + metric.metricName().group() + "." + metric.metricName().name();
  }
}
