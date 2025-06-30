package miniprojectjo.domain;

import javax.persistence.Embeddable; // <-- 이 import 추가

@Embeddable // <-- 이 어노테이션 추가
public class SubscriptionId {
    private String value;

    // 기본 생성자 (Hibernate/JPA가 객체를 인스턴스화할 때 필요합니다)
    public SubscriptionId() {
        // 기본 생성자 내용은 비워두어도 됩니다.
    }

    public SubscriptionId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { // <-- 이 setter 추가 (필요에 따라)
        this.value = value;
    }

    // equals, hashCode, toString 등 필요에 따라 추가 가능 ( Lombok @Data 사용하면 자동으로 생성 )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionId that = (SubscriptionId) o;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SubscriptionId{" +
               "value='" + value + '\'' +
               '}';
    }
}