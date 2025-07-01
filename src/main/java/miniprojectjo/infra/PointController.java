package miniprojectjo.infra;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import miniprojectjo.domain.*; // Point, UserId, SubscriptionId, PointRepository 등
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger; // Logger import
import org.slf4j.LoggerFactory; // LoggerFactory import

@RestController
@RequestMapping(value = "/points") // 주석 해제하여 /points 기본 경로 적용
@Transactional
public class PointController {

    private static final Logger logger = LoggerFactory.getLogger(PointController.class); // 로거 인스턴스 생성

    @Autowired
    PointRepository pointRepository;

    // 1. 포인트 조회 (GET)
    // 예: GET http://localhost:8084/points/userId/{userId}
    @GetMapping("/userId/{userId}")
    public ResponseEntity<Point> getPointByUserId(@PathVariable String userId) {
        logger.info("조회 요청 수신: GET /points/userId/{}", userId); // 로그 추가
        Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(userId));
        if (optionalPoint.isPresent()) {
            Point foundPoint = optionalPoint.get();
            logger.info("포인트 데이터 찾음: userId={}, id={}, currentPoint={}", userId, foundPoint.getId(), foundPoint.getPoint()); // 로그 추가
            return new ResponseEntity<>(foundPoint, HttpStatus.OK);
        } else {
            logger.warn("포인트 데이터 없음: userId={}", userId); // 로그 추가
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    // 2. 테스트 이벤트 발행 (POST) - 기존 코드 유지
    // 예: POST http://localhost:8084/points/publish-test-event
    @PostMapping("/publish-test-event")
    public String publishTestEvent(@RequestBody TestEventRequest request) {
        logger.info("테스트 이벤트 발행 요청 수신: /points/publish-test-event, id={}", request.getId()); // 로그 추가
        try {
            UserRegistered event = new UserRegistered();
            event.setId(request.getId()); // UserRegistered 이벤트의 ID는 사용자 ID가 될 수 있습니다.
            event.setEventType("UserRegistered");

            // UserRegistered 이벤트를 카프카로 발행
            event.publishAfterCommit();
            logger.info("UserRegistered 이벤트 발행 완료: id={}", request.getId()); // 로그 추가

            return "Event published for user: " + request.getId();
        } catch (Exception e) {
            logger.error("테스트 이벤트 발행 중 오류 발생: {}", e.getMessage(), e); // 오류 로그 추가
            return "Failed to publish event: " + e.getMessage();
        }
    }

    // TestEventRequest 클래스 정의
    static class TestEventRequest {
        private String id; // 이 id는 UserRegistered 이벤트의 id에 매핑됩니다.
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }


    // --- 새로 추가되는 핵심 기능 API 엔드포인트 ---

    // 3. 가입포인트 획득 (POST)
    // UserRegistered 이벤트가 발생했을 때 Point.gainRegisterPoint()가 호출되어야 하지만,
    // 외부에서 명시적으로 가입 포인트를 부여하는 API가 필요하다면 사용합니다.
    // 예: POST http://localhost:8084/points/gain-signup
    @PostMapping("/gain-signup")
    public ResponseEntity<String> gainSignupPoint(@RequestBody SignupPointRequest request) {
        logger.info("가입포인트 획득 요청 수신: userId={}, amount={}", request.getUserId(), request.getAmount()); // 로그 추가
        try {
            // userId로 기존 포인트 조회 또는 새로운 포인트 생성
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            Point point;
            if (optionalPoint.isPresent()) {
                point = optionalPoint.get();
                logger.info("기존 포인트 엔티티 찾음 (가입포인트): ID={}, 현재포인트={}", point.getId(), point.getPoint()); // 로그 추가
                point.chargePoint(request.getAmount()); // 기존 포인트에 합산
                logger.info("가입포인트 합산 후 (기존): ID={}, 업데이트포인트={}", point.getId(), point.getPoint()); // 로그 추가
            } else {
                point = new Point();
                point.setId(java.util.UUID.randomUUID().toString()); // 새로운 Point 엔티티 ID 생성
                point.setUserId(new UserId(request.getUserId()));
                point.setPoint(request.getAmount()); // 초기 가입 포인트 설정
                point.setIsSubscribe(false); // 초기 구독 상태
                logger.info("새로운 포인트 엔티티 생성 (가입포인트): ID={}, 초기포인트={}", point.getId(), point.getPoint()); // 로그 추가
            }
            pointRepository.save(point);
            logger.info("가입포인트 엔티티 저장 완료: ID={}, 최종포인트={}", point.getId(), point.getPoint()); // 로그 추가

            // PointRegistered 이벤트 발행 (옵션)
            PointRegistered pointRegistered = new PointRegistered(point);
            pointRegistered.publishAfterCommit();
            logger.info("PointRegistered 이벤트 발행 완료: ID={}", point.getId()); // 로그 추가

            return new ResponseEntity<>("Signup points gained for user: " + request.getUserId(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.error("가입포인트 요청 처리 중 유효성 오류: {}", e.getMessage(), e); // 오류 로그 추가
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("가입포인트 요청 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e); // 오류 로그 추가
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
        logger.info("포인트 충전/구매 요청 수신: userId={}, amount={}", request.getUserId(), request.getAmount()); // 로그 추가
        try {
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            if (optionalPoint.isPresent()) {
                Point point = optionalPoint.get();
                logger.info("기존 포인트 엔티티 찾음 (충전/구매): ID={}, 현재포인트={}", point.getId(), point.getPoint()); // 로그 추가
                point.chargePoint(request.getAmount()); // Point 엔티티의 chargePoint 메서드 사용
                logger.info("포인트 충전 후 (기존): ID={}, 업데이트포인트={}", point.getId(), point.getPoint()); // 로그 추가
                pointRepository.save(point);
                logger.info("포인트 엔티티 저장 완료 (충전/구매): ID={}, 최종포인트={}", point.getId(), point.getPoint()); // 로그 추가

                // PointBought 이벤트 발행 (구매 이벤트가 있다면)
                PointBought pointBought = new PointBought();
                pointBought.setUserId(new UserId(request.getUserId()));
                pointBought.setPoint(request.getAmount()); // 구매(충전)된 포인트
                pointBought.publishAfterCommit();
                logger.info("PointBought 이벤트 발행 완료: userId={}", request.getUserId()); // 로그 추가

                return new ResponseEntity<>("Points charged for user: " + request.getUserId(), HttpStatus.OK);
            } else {
                logger.warn("충전/구매 요청 실패: 사용자 {}를 찾을 수 없거나 포인트 레코드가 없음", request.getUserId()); // 로그 추가
                return new ResponseEntity<>("User not found or no existing point record for user: " + request.getUserId(), HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            logger.error("포인트 충전/구매 요청 처리 중 유효성 오류: {}", e.getMessage(), e); // 오류 로그 추가
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("포인트 충전/구매 요청 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e); // 오류 로그 추가
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

                // --- !!! 이 부분 수정 !!! ---
                // point.getPoint()가 int 타입이므로 null 체크가 필요 없음
                // point.getPoint() == null ||  <-- 이 부분을 삭제합니다.
                if (point.getPoint() < request.getAmount()) {
                // --- !!! 수정 끝 !!! ---

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