package miniprojectjo.domain;

import java.util.*;
import lombok.*;
import miniprojectjo.domain.*;
import miniprojectjo.infra.AbstractEvent;

@Data
@ToString
public class UserRegistered extends AbstractEvent {

    private String id;
    private String email;
    private String userName;
}
