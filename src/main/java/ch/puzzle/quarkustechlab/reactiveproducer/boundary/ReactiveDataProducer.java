package ch.puzzle.quarkustechlab.reactiveproducer.boundary;

import ch.puzzle.quarkustechlab.reactiveproducer.control.HeadersMapExtractAdapter;
import ch.puzzle.quarkustechlab.restproducer.entity.SensorMeasurement;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.reactive.messaging.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class ReactiveDataProducer {

    private final Logger logger = Logger.getLogger(ReactiveDataProducer.class.getName());

    @Inject
    @Channel("data")
    Emitter<SensorMeasurement> emitter;

    @Inject
    Tracer tracer;

    @Scheduled(every = "2s")
    public void sendMessage() {
        SensorMeasurement measurement = new SensorMeasurement();
        HeadersMapExtractAdapter headersMapExtractAdapter = new HeadersMapExtractAdapter();
        tracer.buildSpan("sendMessage").startActive(true);
        tracer.inject(tracer.activeSpan().context(), Format.Builtin.TEXT_MAP, headersMapExtractAdapter);
        OutgoingKafkaRecordMetadata metadata = OutgoingKafkaRecordMetadata.<SensorMeasurement>builder()
                .withKey(measurement)
                .withTopic("manual")
                .withHeaders(headersMapExtractAdapter.getRecordHeaders())
                .build();
        Message<SensorMeasurement> message = Message.of(measurement, Metadata.of(metadata));
        logger.info("Sending message with Jaeger Tracing Headers");
        emitter.send(message);
        tracer.scopeManager().active().close();
    }
}
