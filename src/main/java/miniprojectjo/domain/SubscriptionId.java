package miniprojectjo.domain;

public class SubscriptionId {
    private String value;

    public SubscriptionId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // equals, hashCode, toString 등 필요에 따라 추가 가능
}
