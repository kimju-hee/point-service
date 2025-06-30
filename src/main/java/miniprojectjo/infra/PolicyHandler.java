package miniprojectjo.infra;

import javax.transaction.Transactional;
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

     /* ★ 추가: PointBought 수신 → purchasePoint 호출 */
    @StreamListener(
        value   = KafkaProcessor.INPUT,
        condition = "headers['type']=='PointBought'"
    )
    public void wheneverPointBought_PurchasePoint(
        @Payload PointBought pointBought
    ) {
        PointBought event = pointBought;
        System.out.println(
            "\n\n##### listener PurchasePoint : " + event + "\n\n"
        );

        // 도메인 로직 호출
        Point.purchasePoint(event);
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='PointRegistered'"
    )
    public void wheneverPointRegistered_ChargePoint(@Payload PointRegistered pointRegistered) {
        System.out.println("\n\n##### listener ChargePoint : " + pointRegistered + "\n\n");

        pointRepository.findByUserId(new UserId(pointRegistered.getSubscriberInfo()))  // 필드명 맞게 수정 필요
            .ifPresentOrElse(point -> {
                // 포인트 충전
                point.chargePoint(pointRegistered.getPointAmount());
                pointRepository.save(point);
            }, () -> {
                // 없으면 신규 생성 (필요시)
                Point newPoint = new Point();
                newPoint.setUserId(new UserId(pointRegistered.getSubscriberInfo()));
                newPoint.setPoint(pointRegistered.getPointAmount());
                newPoint.setIsSubscribe(pointRegistered.isHasSubscription());
                pointRepository.save(newPoint);

            });
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='OutOfPoint'"
    )
    public void wheneverOutOfPoint_NotifyUser(@Payload OutOfPoint outOfPoint) {
        System.out.println("\n\n##### listener NotifyUser : " + outOfPoint + "\n\n");

         // TODO: 사용자에게 포인트 부족 알림을 보내거나, 로그 남기거나, 재충전 안내 처리 등
        String userId = outOfPoint.getUserId().getValue(); // UserId가 Embedded라면 getValue()로
        System.out.println("⚠️ 포인트 부족! userId = " + userId + ", 현재 포인트 = " + outOfPoint.getPoint());

    // 향후: 알림 서비스 호출, 이메일 전송 등 확장 가능
    }


}
//>>> Clean Arch / Inbound Adaptor
