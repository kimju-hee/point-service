package miniprojectjo.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import miniprojectjo.domain.*;
import miniprojectjo.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class PointRegistered extends AbstractEvent {

    private Long id;
    // private String 구독자정보;
    // private String 포인트;
    // private String 구독권여부;
    private String subscriberInfo;
    private int pointAmount;
    private boolean hasSubscription;

    public PointRegistered(Point aggregate) {
    super(aggregate);
    this.id = aggregate.getId();
    this.subscriberInfo = aggregate.getUserId().getValue();  // userId에서 문자열 값 가져오기
    this.pointAmount = aggregate.getPoint();
    this.hasSubscription = aggregate.getIsSubscribe();
    }


    public PointRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
