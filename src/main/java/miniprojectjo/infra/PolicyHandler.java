package miniprojectjo.infra;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.naming.NameParser;
import javax.naming.NameParser;
import javax.transaction.Transactional;
import miniprojectjo.config.kafka.KafkaProcessor;
import miniprojectjo.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

//<<< Clean Arch / Inbound Adaptor
@Service
@Transactional
public class PolicyHandler {

    @Autowired
    PointRepository pointRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {}

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='UserRegistered'"
    )
    public void wheneverUserRegistered_GainRegisterPoint(
        @Payload UserRegistered userRegistered
    ) {
        UserRegistered event = userRegistered;
        System.out.println(
            "\n\n##### listener GainRegisterPoint : " + userRegistered + "\n\n"
        );

        // Sample Logic //
        Point.gainRegisterPoint(event);
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='SubscriptionApplied'"
    )
    public void wheneverSubscriptionApplied_DecreasePoint(
        @Payload SubscriptionApplied subscriptionApplied
    ) {
        SubscriptionApplied event = subscriptionApplied;
        System.out.println(
            "\n\n##### listener DecreasePoint : " + subscriptionApplied + "\n\n"
        );

        // Sample Logic //
        Point.decreasePoint(event);
    }
}
//>>> Clean Arch / Inbound Adaptor
