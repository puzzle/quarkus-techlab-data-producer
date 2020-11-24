package ch.puzzle.quarkustechlab.reactiveproducer.boundary;

import ch.puzzle.quarkustechlab.restproducer.entity.SensorMeasurement;
import io.reactivex.Flowable;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ReactiveDataProducer {

    @Outgoing("data")
    public Flowable<SensorMeasurement> generateStream() {
        return Flowable.interval(30, TimeUnit.SECONDS)
                .map(tick -> new SensorMeasurement());
    }
}
