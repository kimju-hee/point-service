package miniprojectjo.domain;

import java.util.Date;
import java.util.List;
import miniprojectjo.domain.*;
import java.util.Optional;   // ★ 추가
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

//<<< PoEAA / Repository
@RepositoryRestResource(collectionResourceRel = "points", path = "points")
public interface PointRepository
    extends PagingAndSortingRepository<Point, String> {
    @Query(
        value = "select point " +
        "from Point point " +
        "where(:id is null or point.id = :id) and (:point is null or point.point = :point) and (point.isSubscribe = :isSubscribe) and (:userId is null or point.userId = :userId) and (:subscriptionId is null or point.subscriptionId = :subscriptionId)"
    )
    Point getPoint(
        String id,
        Integer point,
        Boolean isSubscribe,
        UserId userId,
        SubscriptionId subscriptionId
    );

    Optional<Point> findByUserId(UserId userId);
}
