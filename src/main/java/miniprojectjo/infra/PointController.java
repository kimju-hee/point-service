package miniprojectjo.infra;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import miniprojectjo.domain.*; // Point, UserId, SubscriptionId, PointRepository 등
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // HttpStatus 추가
import org.springframework.http.ResponseEntity; // ResponseEntity 추가
import org.springframework.web.bind.annotation.*;
// import com.fasterxml.jackson.databind.ObjectMapper; // 현재 사용되지 않으므로 주석 처리 또는 제거 가능

@RestController
@RequestMapping(value = "/points") // 주석 해제하여 /points 기본 경로 적용
@Transactional
public class PointController {

    @Autowired
    PointRepository pointRepository;

    // 1. 포인트 조회 (GET)
    // 예: GET http://localhost:8084/points/userId/{userId}
    @GetMapping("/userId/{userId}")
    public ResponseEntity<Point> getPointByUserId(@PathVariable String userId) {
        // Point.java에 findByUserId가 있으므로 이를 활용
        Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(userId));
        return optionalPoint
            .map(point -> new ResponseEntity<>(point, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 기존에 있던 ID로 조회하는 엔드포인트는 UserId로 조회하는 것과 겹치거나
    // ID가 Point 엔티티의 고유 ID라면 User ID와 혼동될 수 있어 UserId로 조회하는 것을 메인으로 변경했습니다.
    // 만약 Point 엔티티의 @Id 필드 (String id)가 User ID와 다르다면, 해당 ID로 조회하는 엔드포인트도 필요할 수 있습니다.
    // 여기서는 @Id id 필드를 Point 자체의 UUID 등으로 사용하고, userId는 Embedded 형태로 사용한다고 가정했습니다.
    // @GetMapping("/{id}")
    // public ResponseEntity<Point> getPointById(@PathVariable String id) {
    //     Optional<Point> optionalPoint = pointRepository.findById(id);
    //     return optionalPoint.map(point -> new ResponseEntity<>(point, HttpStatus.OK))
    //                         .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    // }


    // 2. 테스트 이벤트 발행 (POST) - 기존 코드 유지
    // 예: POST http://localhost:8084/points/publish-test-event
    @PostMapping("/publish-test-event")
    public String publishTestEvent(@RequestBody TestEventRequest request) {
        try {
            UserRegistered event = new UserRegistered();
            event.setId(request.getId()); // UserRegistered 이벤트의 ID는 사용자 ID가 될 수 있습니다.
            event.setEventType("UserRegistered");

            // UserRegistered 이벤트를 카프카로 발행
            event.publishAfterCommit();

            return "Event published for user: " + request.getId();
        } catch (Exception e) {
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
        try {
            // userId로 기존 포인트 조회 또는 새로운 포인트 생성
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            Point point;
            if (optionalPoint.isPresent()) {
                point = optionalPoint.get();
                point.chargePoint(request.getAmount()); // 기존 포인트에 합산
            } else {
                point = new Point();
                point.setId(java.util.UUID.randomUUID().toString()); // 새로운 Point 엔티티 ID 생성
                point.setUserId(new UserId(request.getUserId()));
                point.setPoint(request.getAmount()); // 초기 가입 포인트 설정
                point.setIsSubscribe(false); // 초기 구독 상태
            }
            pointRepository.save(point);

            // PointRegistered 이벤트 발행 (옵션)
            PointRegistered pointRegistered = new PointRegistered(point);
            pointRegistered.publishAfterCommit();

            return new ResponseEntity<>("Signup points gained for user: " + request.getUserId(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
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
        try {
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            if (optionalPoint.isPresent()) {
                Point point = optionalPoint.get();
                point.chargePoint(request.getAmount()); // Point 엔티티의 chargePoint 메서드 사용
                pointRepository.save(point);

                // PointBought 이벤트 발행 (구매 이벤트가 있다면)
                // PointBought 이벤트는 원래 차감 시 발행되므로, 충전/구매는 다른 이벤트를 발행할 수도 있습니다.
                // 여기서는 예시로 PointBought의 필드를 활용하여 발행해봅니다. 실제 이벤트는 프로젝트에 맞게 정의해주세요.
                PointBought pointBought = new PointBought();
                pointBought.setUserId(new UserId(request.getUserId()));
                pointBought.setPoint(request.getAmount()); // 구매(충전)된 포인트
                pointBought.publishAfterCommit();

                return new ResponseEntity<>("Points charged for user: " + request.getUserId(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User not found or no existing point record for user: " + request.getUserId(), HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
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
        try {
            Optional<Point> optionalPoint = pointRepository.findByUserId(new UserId(request.getUserId()));
            if (optionalPoint.isPresent()) {
                Point point = optionalPoint.get();
                if (point.getPoint() == null || point.getPoint() < request.getAmount()) {
                    // OutOfPoint 이벤트 발행 (부족 시) - Point.java의 로직과 유사
                    OutOfPoint outOfPoint = new OutOfPoint(point);
                    outOfPoint.publishAfterCommit();
                    return new ResponseEntity<>("Not enough points for user: " + request.getUserId(), HttpStatus.BAD_REQUEST);
                }
                point.setPoint(point.getPoint() - request.getAmount());
                pointRepository.save(point);

                // PointDecreased 이벤트 발행
                PointDecreased pointDecreased = new PointDecreased(point);
                pointDecreased.publishAfterCommit();

                return new ResponseEntity<>("Points deducted for user: " + request.getUserId(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User not found or no existing point record for user: " + request.getUserId(), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
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