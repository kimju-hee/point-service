package miniprojectjo.infra;

import javax.transaction.Transactional;
// import com.fasterxml.jackson.databind.DeserializationFeature; // Not used, remove
// import com.fasterxml.jackson.databind.ObjectMapper; // Not used, remove
// import javax.naming.NameParser; // Duplicate, remove (already imported once)
import miniprojectjo.config.kafka.KafkaProcessor;
import miniprojectjo.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.slf4j.Logger; // Logger import
import org.slf4j.LoggerFactory; // LoggerFactory import

//<<< Clean Arch / Inbound Adaptor
@Service
@Transactional
public class PolicyHandler {

    private static final Logger logger = LoggerFactory.getLogger(PolicyHandler.class); // 로거 인스턴스 생성

    @Autowired
    PointRepository pointRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
        // 이 리스너는 모든 이벤트를 받으므로, 특별한 로깅 없이 유지
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='UserRegistered'"
    )
    public void wheneverUserRegistered_GainRegisterPoint(
        @Payload UserRegistered userRegistered
    ) {
        logger.info("\n\n##### PolicyHandler: UserRegistered 이벤트 수신 - GainRegisterPoint 시작: {}\n\n", userRegistered);

        // --- 이 자동 포인트 지급 로직은 테스트를 위해 주석 처리된 상태로 유지됩니다. ---
        /*
        Point point = new Point();
        point.setId(String.valueOf(userRegistered.getId()));
        point.setUserId(new UserId(String.valueOf(userRegistered.getId())));
        point.setPoint(1000); // 1000 포인트 지급
        point.setIsSubscribe(false);
        pointRepository.save(point);

        PointRegistered pointRegistered = new PointRegistered(point);
        pointRegistered.publishAfterCommit();
        logger.info("PolicyHandler: PointRegistered 이벤트 발행 완료 (자동 지급): ID={}", point.getId());
        */
        // --- 주석 처리된 로직 끝 ---
        logger.info("PolicyHandler: UserRegistered 이벤트 처리 완료 (자동 포인트 지급 로직 주석 처리됨).");
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='SubscriptionApplied'"
    )
    public void wheneverSubscriptionApplied_DecreasePoint(
        @Payload SubscriptionApplied subscriptionApplied
    ) {
        logger.info("\n\n##### PolicyHandler: SubscriptionApplied 이벤트 수신 - DecreasePoint 시작: {}\n\n", subscriptionApplied);
        try {
            Point.decreasePoint(subscriptionApplied);
            logger.info("PolicyHandler: SubscriptionApplied 이벤트 처리 완료.");
        } catch (Exception e) {
            logger.error("PolicyHandler: SubscriptionApplied 처리 중 오류 발생: {}", e.getMessage(), e);
            // 예외를 던지면 Kafka가 재처리 시도할 수 있음. 필요에 따라 throw e;
        }
    }

    /* ★ 추가: PointBought 수신 → purchasePoint 호출 */
    @StreamListener(
        value   = KafkaProcessor.INPUT,
        condition = "headers['type']=='PointBought'"
    )
    public void wheneverPointBought_PurchasePoint(
        @Payload PointBought pointBought
    ) {
        logger.info("\n\n##### PolicyHandler: PointBought 이벤트 수신 - PurchasePoint 시작: {}\n\n", pointBought);
        try {
            // 도메인 로직 호출
            Point.purchasePoint(pointBought);
            logger.info("PolicyHandler: PointBought 이벤트 처리 완료.");
        } catch (Exception e) {
            logger.error("PolicyHandler: PointBought 처리 중 오류 발생: {}", e.getMessage(), e);
            // 예외를 던지면 Kafka가 재처리 시도할 수 있음. 필요에 따라 throw e;
        }
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='PointRegistered'"
    )
    public void wheneverPointRegistered_ChargePoint(@Payload PointRegistered pointRegistered) {
        logger.info("\n\n##### PolicyHandler: PointRegistered 이벤트 수신 - ChargePoint 시작: {}\n\n", pointRegistered);

        pointRepository.findByUserId(new UserId(pointRegistered.getSubscriberInfo()))  // 필드명 맞게 수정 필요
            .ifPresentOrElse(point -> {
                // 포인트 충전
                point.chargePoint(pointRegistered.getPointAmount());
                pointRepository.save(point);
                logger.info("PolicyHandler: 포인트 충전 완료 (PointRegistered 이벤트): ID={}, 최종포인트={}", point.getId(), point.getPoint());
            }, () -> {
                // 없으면 신규 생성 (필요시)
                Point newPoint = new Point();
                // UUID 생성하여 ID 설정 (기존 Point 엔티티 ID 생성 로직과 일관성을 위해 추가)
                newPoint.setId(java.util.UUID.randomUUID().toString()); // ID 설정 로직 추가
                newPoint.setUserId(new UserId(pointRegistered.getSubscriberInfo()));
                newPoint.setPoint(pointRegistered.getPointAmount());
                newPoint.setIsSubscribe(pointRegistered.isHasSubscription());
                pointRepository.save(newPoint);
                logger.info("PolicyHandler: 새 포인트 생성 완료 (PointRegistered 이벤트): ID={}, 초기포인트={}", newPoint.getId(), newPoint.getPoint());
            });
        logger.info("PolicyHandler: PointRegistered 이벤트 처리 완료.");
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='OutOfPoint'"
    )
    public void wheneverOutOfPoint_NotifyUser(@Payload OutOfPoint outOfPoint) {
        logger.warn("\n\n##### PolicyHandler: OutOfPoint 이벤트 수신 - NotifyUser 시작: {}\n\n", outOfPoint); // WARN 레벨로 변경 (알림은 경고성)

        // TODO: 사용자에게 포인트 부족 알림을 보내거나, 로그 남기거나, 재충전 안내 처리 등
        String userId = outOfPoint.getUserId().getValue(); // UserId가 Embedded라면 getValue()로
        logger.warn("⚠️ 포인트 부족! userId = {}, 현재 포인트 = {}", userId, outOfPoint.getPoint()); // logger 사용

        // 향후: 알림 서비스 호출, 이메일 전송 등 확장 가능
    }
}
//>>> Clean Arch / Inbound Adaptor