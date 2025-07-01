package miniprojectjo.domain;

// 불필요한 import 제거 (ObjectMapper, LocalDate, Collections, Date, List, Map 등)
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor; // 유지: Lombok이 no-arg constructor 생성
import lombok.AllArgsConstructor; // 유지: 모든 필드 constructor 생성 (필요 시)

import miniprojectjo.PointApplication;
import miniprojectjo.domain.OutOfPoint;
import miniprojectjo.domain.PointBought;
import miniprojectjo.domain.PointDecreased;
import miniprojectjo.domain.PointRegistered;

@Entity
@Table(name = "Point_table")
@Data
@NoArgsConstructor // Lombok이 기본 생성자를 생성해 줄 것입니다.
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 만들어 줍니다. (필요 시 유지)
//<<< DDD / Aggregate Root
public class Point {

    @Id
    private String id;

    // 현재 보유 포인트 (초기 0)
    private int point = 0; // 'int' type은 기본값이 0이며, 명시적으로 0으로 초기화.

    private Boolean isSubscribe;

    @Embedded
    private UserId userId;

    @Embedded
    private SubscriptionId subscriptionId;

    // --- !!! 이 부분을 삭제합니다 !!! ---
    // @NoArgsConstructor가 대신 생성하므로 명시적인 정의는 필요 없습니다.
    /*
    public Point() {
        this.point = 0;
    }
    */
    // --- !!! 삭제 끝 !!! ---


    // 포인트 충전 (증가)
    public void chargePoint(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전할 포인트는 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    public static PointRepository repository() {
        return PointApplication.applicationContext.getBean(PointRepository.class);
    }

    // // 회원 가입 시 포인트 지급 (미구현 템플릿) - 기존 주석 처리 상태 유지

    // 구독료 결제 시 포인트 차감
    public static void decreasePoint(SubscriptionApplied subscriptionApplied) {

        UserId userIdValue      = subscriptionApplied.getUserId();
        int    subscriptionCost = subscriptionApplied.getCost();

        repository().findByUserId(userIdValue)
            .ifPresentOrElse(point -> {
                if (point.getPoint() < subscriptionCost) {
                    OutOfPoint outOfPoint = new OutOfPoint(point);
                    outOfPoint.publishAfterCommit();
                    return;
                }

                point.setPoint(point.getPoint() - subscriptionCost);
                repository().save(point);

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

    // 상품·서비스 구매 시 포인트 처리 (PointBought 이벤트에 대한 처리)
    // 이 메서드는 PointBought 이벤트가 '포인트 구매/충전'을 의미한다고 가정하고, 포인트를 '증가'시킵니다.
    public static void purchasePoint(PointBought pointBought) {

        UserId userId = pointBought.getUserId();
        repository().findByUserId(userId).ifPresent(point -> {

            // PointBought 이벤트의 'point' 필드가 '구매 금액'을 의미한다고 가정합니다.
            point.setPoint(point.getPoint() + pointBought.getPoint()); // <-- 포인트 증가 로직
            repository().save(point);

            // 무한 루프 원인이었던 이벤트 재발행 로직은 이미 제거됨.
        });
    }
}
//>>> DDD / Aggregate Root