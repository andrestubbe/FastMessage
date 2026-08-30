package FastMessage;

/**
 * Supported messaging transport platforms and channels.
 */
public enum MessagingChannel {
    TELEGRAM("telegram", "Telegram Bot & MTProto"),
    WHATSAPP("whatsapp", "WhatsApp Cloud API & Baileys"),
    UNIFIED("unified", "Universal Multi-Platform Bridge"),
    CUSTOM("custom", "Custom Protocol Adapter");

    private final String id;
    private final String displayName;

    MessagingChannel(final String id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public static MessagingChannel fromId(final String id) {
        if (id == null) {
            return UNIFIED;
        }
        for (final MessagingChannel channel : values()) {
            if (channel.id.equalsIgnoreCase(id)) {
                return channel;
            }
        }
        return CUSTOM;
    }
}
