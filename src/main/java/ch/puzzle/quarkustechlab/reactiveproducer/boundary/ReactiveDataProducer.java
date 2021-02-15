package ch.puzzle.quarkustechlab.reactiveproducer.boundary;

import ch.puzzle.quarkustechlab.reactiveproducer.control.HeadersMapInjectAdapter;
import ch.puzzle.quarkustechlab.restproducer.entity.SensorMeasurement;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.logging.Logger;

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

    @ConfigProperty(name = "producer.jaeger.enabled")
    Optional<Boolean> jaegerEnabled;

    @Scheduled(every = "30s")
    @Traced
    public void sendMessage() {
      SensorMeasurement measurement = new SensorMeasurement();
      HeadersMapInjectAdapter headersMapInjectAdapter = new HeadersMapInjectAdapter();
      try (Scope scope = tracer.buildSpan("data-produced").startActive(true)) {
        tracer.inject(scope.span().context(), Format.Builtin.TEXT_MAP, headersMapInjectAdapter);
        OutgoingKafkaRecordMetadata metadata = OutgoingKafkaRecordMetadata.<SensorMeasurement>builder()
                .withKey(measurement)
                .withTopic("manual")
                .withHeaders(headersMapInjectAdapter.getRecordHeaders())
                .build();
        emitter.send(Message.of(measurement, Metadata.of(metadata)));
        logger.info("Sending message with Jaeger Tracing Headers");
      }
    }
}
