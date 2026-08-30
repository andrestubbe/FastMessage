package FastMessage;

/**
 * Universal message categories supported across Telegram, WhatsApp, and Custom bridges.
 */
public enum MessageType {
    TEXT("text"),
    IMAGE("image"),
    AUDIO("audio"),
    VOICE("voice"),
    VIDEO("video"),
    DOCUMENT("document"),
    LOCATION("location"),
    CONTACT("contact"),
    STICKER("sticker"),
    INTERACTIVE("interactive"),
    TEMPLATE("template"),
    REACTION("reaction"),
    SYSTEM("system");

    private final String code;

    MessageType(final String code) {
        this.code = code;
    }

    public String code() {
        return this.code;
    }

    public static MessageType fromCode(final String code) {
        if (code == null) {
            return TEXT;
        }
        for (final MessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return TEXT;
    }
}
