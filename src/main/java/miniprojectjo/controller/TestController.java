package miniprojectjo.controller;

import org.springframework.web.bind.annotation.*;
import miniprojectjo.domain.Point;
import miniprojectjo.domain.UserRegistered;

@RestController
public class TestController {

    

    @PostMapping("/userRegistered")
    public void userRegisteredTest(@RequestParam Long id) {

        UserRegistered event = new UserRegistered();
        event.setId(id);          // Long → Long, 타입 OK

        Point.gainRegisterPoint(event);
    }

}
