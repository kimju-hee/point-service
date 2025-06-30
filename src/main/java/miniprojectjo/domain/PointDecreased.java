package miniprojectjo.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import miniprojectjo.domain.*;
import miniprojectjo.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class PointDecreased extends AbstractEvent {

    private String id;
    private Integer point;
    private UserId userId;

    public PointDecreased(Point aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.point = aggregate.getPoint();
        this.userId = aggregate.getUserId();
    }

    public PointDecreased() {
        super();
    }
}
//>>> DDD / Domain Event
