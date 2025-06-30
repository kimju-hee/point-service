package miniprojectjo.infra;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import miniprojectjo.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper; // 추가

@RestController
// @RequestMapping(value="/points")
@Transactional
public class PointController {

    @Autowired
    PointRepository pointRepository;

    // vvvvvv 이 부분이 추가되어야 합니다. vvvvvv
    @GetMapping("/points/{id}")
    public Point getPointById(@PathVariable String id) {
        // id를 이용해 포인트 정보를 찾아서 반환합니다.
        Optional<Point> optionalPoint = pointRepository.findById(id);
        return optionalPoint.orElse(null);
    }
    // ^^^^^^ 이 부분이 추가되어야 합니다. ^^^^^^

    @PostMapping("/points/publish-test-event")
    public String publishTestEvent(@RequestBody TestEventRequest request) {
        try {
            UserRegistered event = new UserRegistered();
            event.setId(request.getId());
            event.setEventType("UserRegistered");

            // UserRegistered 이벤트를 카프카로 발행
            event.publishAfterCommit();

            return "Event published for user: " + request.getId();

        } catch (Exception e) {
            return "Failed to publish event: " + e.getMessage();
        }
    }

    // 위 @PostMapping에서 사용할 데이터 구조를 정의하는 내부 클래스
    static class TestEventRequest {
        private String id;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }
}