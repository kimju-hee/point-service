package miniprojectjo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import lombok.Data;
import miniprojectjo.PointApplication;
import miniprojectjo.domain.OutOfPoint;
import miniprojectjo.domain.PointBought;
import miniprojectjo.domain.PointDecreased;
import miniprojectjo.domain.PointRegistered;

@Entity
@Table(name = "Point_table")
@Data
//<<< DDD / Aggregate Root
public class Point {

    @Id
    private String id;

    // 현재 보유 포인트 (초기 0)
    private int point = 0;

    private Boolean isSubscribe;

    @Embedded
    private UserId userId;

    @Embedded
    private SubscriptionId subscriptionId;
    // 기본 생성자를 명시적으로 추가
    public Point() {
        this.point = 0; // 새로 생성될 때 항상 0으로 초기화
    }

    // 포인트 충전
    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 커야 합니다.");
        }
        
        this.point += amount;
    }

    public static PointRepository repository() {
        return PointApplication.applicationContext.getBean(PointRepository.class);
    }

    // // 회원 가입 시 포인트 지급 (미구현 템플릿)
    // public static void gainRegisterPoint(UserRegistered userRegistered) {
    //     // TODO: 구현
    //     System.out.println("##### KAFKA MESSAGE RECEIVED, UserRegistered : " + userRegistered.getId() + " #####");
    //     // 1) 새 포인트 객체 생성
    //     Point point = new Point();

    //     // 추가
    //     point.setId(String.valueOf(userRegistered.getId()));
    
    //     // 2) 사용자 ID 세팅 (UserRegistered 이벤트의 필드 이름에 맞춰 수정)
    //     point.setUserId(new UserId(String.valueOf(userRegistered.getId())));

    //     // 3) 초기 포인트 지급 (예: 1000 포인트)
    //     point.setPoint(1000);

    //     // 4) 구독 상태 초기화 (예: false)
    //     point.setIsSubscribe(false);

    //     // 5) 저장
    //     repository().save(point);

    //     // 6) PointRegistered 이벤트 발행
    //     PointRegistered pointRegistered = new PointRegistered(point);
    //     pointRegistered.publishAfterCommit();
    // }

    // 구독료 결제 시 포인트 차감
    public static void decreasePoint(SubscriptionApplied subscriptionApplied) {

        // 1) 이벤트에서 정보 꺼내기
        UserId userIdValue     = subscriptionApplied.getUserId(); 
        int    subscriptionCost = subscriptionApplied.getCost();

        // 2) 포인트 레코드 조회
        repository().findByUserId(userIdValue)
            .ifPresentOrElse(point -> {

                // 3‑1) 포인트 부족
                if (point.getPoint() < subscriptionCost) {
                    OutOfPoint outOfPoint = new OutOfPoint(point);
                    outOfPoint.publishAfterCommit();
                    return;
                }

                // 3‑2) 차감 후 저장
                point.setPoint(point.getPoint() - subscriptionCost);
                repository().save(point);

                // 4) 차감 이벤트 발행
                PointDecreased pointDecreased = new PointDecreased(point);
                pointDecreased.publishAfterCommit();

            }, () -> {
                // 포인트 레코드가 없을 때
                OutOfPoint outOfPoint = new OutOfPoint(
                    new Point() {{
                        setUserId(userIdValue);
                        setPoint(0);
                    }}
                );
                outOfPoint.publishAfterCommit();
            });
    }

    // 상품·서비스 구매 시 포인트 차감
    public static void purchasePoint(PointBought pointBought) {

        // [수정] String → UserId 객체로 감싸기
        UserId userId = pointBought.getUserId(); // ✅ 
        repository().findByUserId(userId).ifPresent(point -> {

            if (point.getPoint() < pointBought.getPoint()) {
                OutOfPoint outOfPoint = new OutOfPoint(point);
                outOfPoint.publishAfterCommit();
                return;
            }

            point.setPoint(point.getPoint() - pointBought.getPoint());
            repository().save(point);

            pointBought.setId(point.getId());
            pointBought.setPoint(point.getPoint());
            pointBought.publishAfterCommit();
        });
    }
}
//>>> DDD / Aggregate Root
