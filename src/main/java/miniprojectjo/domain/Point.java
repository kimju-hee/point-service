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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer point;

    private Boolean isSubscribe;

    @Embedded
    private UserId userId;

    @Embedded
    private SubscriptionId subscriptionId;

    public static PointRepository repository() {
        PointRepository pointRepository = PointApplication.applicationContext.getBean(
            PointRepository.class
        );
        return pointRepository;
    }

    //<<< Clean Arch / Port Method
    public static void gainRegisterPoint(UserRegistered userRegistered) {
        //implement business logic here:

        /** Example 1:  new item 
        Point point = new Point();
        repository().save(point);

        PointRegistered pointRegistered = new PointRegistered(point);
        pointRegistered.publishAfterCommit();
        */

        /** Example 2:  finding and process
        

        repository().findById(userRegistered.get???()).ifPresent(point->{
            
            point // do something
            repository().save(point);

            PointRegistered pointRegistered = new PointRegistered(point);
            pointRegistered.publishAfterCommit();

         });
        */

    }

    //>>> Clean Arch / Port Method
    //<<< Clean Arch / Port Method
    public static void decreasePoint(SubscriptionApplied subscriptionApplied) {
        //implement business logic here:

        /** Example 1:  new item 
        Point point = new Point();
        repository().save(point);

        PointDecreased pointDecreased = new PointDecreased(point);
        pointDecreased.publishAfterCommit();
        OutOfPoint outOfPoint = new OutOfPoint(point);
        outOfPoint.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        // if subscriptionApplied.bookIduserId exists, use it
        
        // ObjectMapper mapper = new ObjectMapper();
        // Map<Long, Object> subscriptionMap = mapper.convertValue(subscriptionApplied.getBookId(), Map.class);
        // Map<Long, Object> subscriptionMap = mapper.convertValue(subscriptionApplied.getUserId(), Map.class);

        repository().findById(subscriptionApplied.get???()).ifPresent(point->{
            
            point // do something
            repository().save(point);

            PointDecreased pointDecreased = new PointDecreased(point);
            pointDecreased.publishAfterCommit();
            OutOfPoint outOfPoint = new OutOfPoint(point);
            outOfPoint.publishAfterCommit();

         });
        */

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
