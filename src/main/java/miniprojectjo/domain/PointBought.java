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

    public PointBought(Point aggregate) {
        super(aggregate);
    }

    public PointBought() {
        super();
    }
}
//>>> DDD / Domain Event
