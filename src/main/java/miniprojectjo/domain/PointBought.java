package miniprojectjo.domain;

// import java.time.LocalDate; // 사용되지 않으므로 제거
// import java.util.*; // 사용되지 않으므로 제거
import lombok.*; // @Data, @ToString, @NoArgsConstructor, @AllArgsConstructor 사용을 위해 유지

import miniprojectjo.infra.AbstractEvent; // 유지

//<<< DDD / Domain Event
@Data
@ToString
@NoArgsConstructor // Lombok이 기본 생성자를 만들어 줍니다. (필수)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 만들어 줍니다. (테스트 등에 유용)
public class PointBought extends AbstractEvent {

    private String id; // 이 필드는 트랜잭션 ID 또는 이벤트 자체의 고유 ID로 사용될 수 있습니다.
    private int point; // <-- 이 필드가 구매/충전된 '금액'을 나타내도록 사용됩니다. (이전의 'amount' 역할)
    private UserId userId; // 포인트를 구매한 사용자의 ID

    // private Integer amount; // <-- 이 필드는 'point' 필드와 의미가 중복되므로 제거합니다.

    // 기존의 Point aggregate를 인자로 받는 생성자는 삭제합니다.
    // PointBought 이벤트는 구매/충전이라는 '사실'에 대한 정보를 담아야 하며,
    // 이 정보는 보통 트랜잭션 요청으로부터 직접 오거나, 처리 후의 변경된 집계 상태에서 파생됩니다.
    // 기존 aggregate의 'point' 필드는 전체 잔액을 의미하므로, PointBought 이벤트의 'point' (구매 금액)와는 의미가 다릅니다.
    /*
    public PointBought(Point aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.point = aggregate.getPoint(); // 이 경우 aggregate.getPoint()는 총 잔액이므로 적절치 않음.
        this.userId = aggregate.getUserId();
    }
    */
    // 기본 생성자는 @NoArgsConstructor로 자동 생성됩니다.
    // public PointBought() {
    //     super();
    // }
}
//>>> DDD / Domain Event