package miniprojectjo.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import miniprojectjo.domain.*;
import miniprojectjo.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class PointBought extends AbstractEvent {

    private Long id;
    private Integer point;
    private UserId userId;
    private Integer amount;        // 구매 포인트 수량 추가

    public PointBought(Point aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.point = aggregate.getPoint();
        this.userId = aggregate.getUserId();
    }

    public PointBought() {
        super();
    }
}
//>>> DDD / Domain Event
