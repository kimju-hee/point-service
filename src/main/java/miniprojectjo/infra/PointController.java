package miniprojectjo.infra;

import java.util.Optional;
// import javax.servlet.http.HttpServletRequest; // 사용되지 않으므로 제거
// import javax.servlet.http.HttpServletResponse; // 사용되지 않으므로 제거
import javax.transaction.Transactional;
import miniprojectjo.domain.*; // Point, UserId, SubscriptionId, PointRepository 등 필요한 도메인 클래스 import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(value = "/points")
@Transactional
public class PointController {

    private static final Logger logger = LoggerFactory.getLogger(PointController.class);

    @Autowired
    PointRepository pointRepository;

    // 1. 포인트 조회 (GET)
    // 예: GET http://localhost:8084/points/userId/{userId}
    @GetMapping("/userId/{userId}")
    public ResponseEntity<Point> getPointByUserId(@PathVariable String userId) {
        logger.info("조회 요청 수신: GET /points/userId/{}", userId);
        Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(userId));
        if (optionalPoint.isPresent()) {
            Point foundPoint = optionalPoint.get();
            logger.info("포인트 데이터 찾음: userId={}, id={}, currentPoint={}", userId, foundPoint.getId(), foundPoint.getPoint());
            return new ResponseEntity<>(foundPoint, HttpStatus.OK);
        } else {
            logger.warn("포인트 데이터 없음: userId={}", userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    // 2. 테스트 이벤트 발행 (POST)
    // 예: POST http://localhost:8084/points/publish-test-event
    @PostMapping("/publish-test-event")
    public String publishTestEvent(@RequestBody TestEventRequest request) {
        logger.info("테스트 이벤트 발행 요청 수신: /points/publish-test-event, id={}", request.getId());
        try {
            UserRegistered event = new UserRegistered();
            event.setId(request.getId());
            event.setEventType("UserRegistered");

            // UserRegistered 이벤트를 카프카로 발행
            event.publishAfterCommit();
            logger.info("UserRegistered 이벤트 발행 완료: id={}", request.getId());

            return "Event published for user: " + request.getId();
        } catch (Exception e) {
            logger.error("테스트 이벤트 발행 중 오류 발생: {}", e.getMessage(), e);
            return "Failed to publish event: " + e.getMessage();
        }
    }

    // TestEventRequest 클래스 정의
    static class TestEventRequest {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }


    // --- 핵심 기능 API 엔드포인트 ---

    // 3. 가입포인트 획득 (POST)
    // 예: POST http://localhost:8084/points/gain-signup
    @PostMapping("/gain-signup")
    public ResponseEntity<String> gainSignupPoint(@RequestBody SignupPointRequest request) {
        logger.info("가입포인트 획득 요청 수신: userId={}, amount={}", request.getUserId(), request.getAmount());
        try {
            // 실제 포인트 저장 로직 제거
            // Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            // Point point;
            // if (optionalPoint.isPresent()) { ... } else { ... }
            // pointRepository.save(point); // <-- 이 라인 제거!

            // PointRegistered 이벤트 발행만 담당
            PointRegistered pointRegistered = new PointRegistered(); // 이벤트를 새로 생성
            pointRegistered.setId(java.util.UUID.randomUUID().toString()); // 이벤트 ID (트랜잭션 ID)
            pointRegistered.setSubscriberInfo(request.getUserId()); // 사용자 정보
            pointRegistered.setPointAmount(request.getAmount()); // 지급할 포인트 금액
            pointRegistered.setHasSubscription(false); // 초기 구독 상태
            pointRegistered.setEventType("PointRegistered"); // 이벤트 타입 명시

            pointRegistered.publishAfterCommit();
            logger.info("PointRegistered 이벤트 발행 완료: userId={}", request.getUserId());

            return new ResponseEntity<>("Signup point request received for user: " + request.getUserId() + ". Processing via event.", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("가입포인트 요청 처리 중 유효성 오류: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("가입포인트 요청 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
            return new ResponseEntity<>("Failed to gain signup points: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // SignupPointRequest 클래스 정의
    static class SignupPointRequest {
        private String userId;
        private Integer amount; // 가입 시 지급할 포인트 금액
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
    }


    // 4. 포인트 구매/충전 (POST)
    // Point.java의 chargePoint 메서드와 연계됩니다.
    // 예: POST http://localhost:8084/points/charge

    @PostMapping("/charge")
    public ResponseEntity<String> chargePoint(@RequestBody PointChargeRequest request) {
        logger.info("포인트 충전/구매 요청 수신: userId={}, amount={}", request.getUserId(), request.getAmount());
        try {
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            if (optionalPoint.isPresent()) {
                Point point = optionalPoint.get();
                logger.info("기존 포인트 엔티티 찾음 (충전/구매): ID={}, 현재포인트={}", point.getId(), point.getPoint());
                //point.chargePoint(request.getAmount()); // Point 엔티티의 chargePoint 메서드 사용
                //logger.info("포인트 충전 후 (기존): ID={}, 업데이트포인트={}", point.getId(), point.getPoint());
                //pointRepository.save(point);
                //logger.info("포인트 엔티티 저장 완료 (충전/구매): ID={}, 최종포인트={}", point.getId(), point.getPoint());

                // PointBought 이벤트 발행
                PointBought pointBought = new PointBought();
                pointBought.setUserId(new UserId(request.getUserId()));
                // PointBought 이벤트의 'point' 필드를 구매/충전 '금액'으로 설정 (PointBought.java 수정 반영)
                pointBought.setPoint(request.getAmount()); // <--- 이 부분이 수정되었습니다.

                logger.debug("PointBought 이벤트 발행 직전: userId={}, eventPoint={}", pointBought.getUserId().getValue(), pointBought.getPoint());

                pointBought.publishAfterCommit();
                logger.info("PointBought 이벤트 발행 완료: userId={}", request.getUserId());

                return new ResponseEntity<>("Points charge request received for user: " + request.getUserId() + ". Processing via event.", HttpStatus.OK);
            } else {
                logger.warn("충전/구매 요청 실패: 사용자 {}를 찾을 수 없거나 포인트 레코드가 없음", request.getUserId());
                return new ResponseEntity<>("User not found or no existing point record for user: " + request.getUserId(), HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            logger.error("포인트 충전/구매 요청 처리 중 유효성 오류: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("포인트 충전/구매 요청 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
            return new ResponseEntity<>("Failed to charge points: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    // PointChargeRequest 클래스 정의
    static class PointChargeRequest {
        private String userId;
        private Integer amount; // 충전/구매할 포인트 금액
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
    }


    // 5. 포인트 차감 (POST)
    // 예: POST http://localhost:8084/points/deduct
    @PostMapping("/deduct")
    public ResponseEntity<String> deductPoint(@RequestBody PointDeductRequest request) {
        logger.info("포인트 차감 요청 수신: userId={}, amount={}", request.getUserId(), request.getAmount());
        try {
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            if (optionalPoint.isPresent()) {
                Point point = optionalPoint.get();
                logger.info("기존 포인트 엔티티 찾음 (차감): ID={}, 현재포인트={}", point.getId(), point.getPoint());

                if (point.getPoint() < request.getAmount()) {
                    logger.warn("포인트 부족 오류: userId={}, 현재포인트={}, 차감요청={}", request.getUserId(), point.getPoint(), request.getAmount());
                    OutOfPoint outOfPoint = new OutOfPoint(point);
                    outOfPoint.publishAfterCommit();
                    return new ResponseEntity<>("Not enough points for user: " + request.getUserId(), HttpStatus.BAD_REQUEST);
                }
                point.setPoint(point.getPoint() - request.getAmount());
                logger.info("포인트 차감 후: ID={}, 업데이트포인트={}", point.getId(), point.getPoint());
                pointRepository.save(point);
                logger.info("포인트 엔티티 저장 완료 (차감): ID={}, 최종포인트={}", point.getId(), point.getPoint());

                PointDecreased pointDecreased = new PointDecreased(point);
                pointDecreased.publishAfterCommit();
                logger.info("PointDecreased 이벤트 발행 완료: ID={}", point.getId());

                return new ResponseEntity<>("Points deducted for user: " + request.getUserId(), HttpStatus.OK);
            } else {
                logger.warn("포인트 차감 요청 실패: 사용자 {}를 찾을 수 없거나 포인트 레코드가 없음", request.getUserId());
                return new ResponseEntity<>("User not found or no existing point record for user: " + request.getUserId(), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("포인트 차감 요청 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
            return new ResponseEntity<>("Failed to deduct points: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // PointDeductRequest 클래스 정의
    static class PointDeductRequest {
        private String userId;
        private Integer amount; // 차감할 포인트 금액
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
    }
}