package fastmessaging;

/**
 * Lifecycle and delivery statuses for UniversalMessage instances.
 */
public enum MessageStatus {
    PENDING("pending"),
    SENT("sent"),
    DELIVERED("delivered"),
    READ("read"),
    FAILED("failed");

    private final String status;

    MessageStatus(final String status) {
        this.status = status;
    }

    public String status() {
        return this.status;
    }

    public static MessageStatus fromStatus(final String status) {
        if (status == null) {
            return PENDING;
        }
        for (final MessageStatus s : values()) {
            if (s.status.equalsIgnoreCase(status)) {
                return s;
            }
        }
        return PENDING;
    }
}
