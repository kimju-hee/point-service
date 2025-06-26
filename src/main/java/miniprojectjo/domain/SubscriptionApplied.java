package miniprojectjo.domain;

import java.util.*;
import lombok.*;
import miniprojectjo.domain.*;
import miniprojectjo.infra.AbstractEvent;

@Data
@ToString
public class SubscriptionApplied extends AbstractEvent {

    private Long id;
    private Object bookId;
    private Object userId;
    private Boolean isSubscription;
    private Date startSubscription;
    private Date endSubscription;
    private String pdfPath;
}
