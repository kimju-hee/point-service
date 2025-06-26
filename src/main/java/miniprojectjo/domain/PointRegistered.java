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
    private String 구독자정보;
    private String 포인트;
    private String 구독권여부;

    public PointRegistered(Point aggregate) {
        super(aggregate);
    }

    public PointRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
