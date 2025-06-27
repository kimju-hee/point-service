package miniprojectjo.domain;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserId implements Serializable {

    private String value;

    // JPA는 기본 생성자 필요
    public UserId() {}

    public UserId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // equals, hashCode 재정의 (임베디드 타입 비교에 중요)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId)) return false;
        UserId userId = (UserId) o;
        return Objects.equals(getValue(), userId.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
