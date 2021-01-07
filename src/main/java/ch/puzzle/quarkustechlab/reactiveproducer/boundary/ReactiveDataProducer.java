package ch.puzzle.quarkustechlab.reactiveproducer.boundary;

import ch.puzzle.quarkustechlab.reactiveproducer.control.HeadersMapExtractAdapter;
import ch.puzzle.quarkustechlab.restproducer.entity.SensorMeasurement;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

@ApplicationScoped
public class ReactiveDataProducer {

  private final Logger logger = Logger.getLogger(
    ReactiveDataProducer.class.getName()
  );

  @Inject
  @Channel("data")
  Emitter<SensorMeasurement> emitter;

  @Inject
  Tracer tracer;

  @ConfigProperty(name = "quarkus.jaeger.enabled")
  Boolean jaegerEnabled;

  @Scheduled(every = "30s")
  @Traced
  public void sendMessage() {
    SensorMeasurement measurement = new SensorMeasurement();
    HeadersMapExtractAdapter headersMapExtractAdapter = new HeadersMapExtractAdapter();
    OutgoingKafkaRecordMetadata metadata = OutgoingKafkaRecordMetadata
      .<SensorMeasurement>builder()
      .withKey(measurement)
      .withTopic("manual")
      .withHeaders(headersMapExtractAdapter.getRecordHeaders())
      .build();
    Message<SensorMeasurement> message = Message.of(
      measurement,
      Metadata.of(metadata)
    );

    if (jaegerEnabled) {
      try (Scope scope = tracer.buildSpan("sendMessage").startActive(true)) {
        tracer.inject(
          scope.span().context(),
          Format.Builtin.TEXT_MAP,
          headersMapExtractAdapter
        );
        logger.info("Sending message with Jaeger Tracing Headers");
        emitter.send(message);
      }
    } else {
      logger.info("Sending message");
      emitter.send(message);
    }
  }
}
