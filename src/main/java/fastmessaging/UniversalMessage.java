package fastmessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Universal canonical message record uniting Telegram, WhatsApp, and custom messaging protocols.
 */
public final class UniversalMessage {

    private final String id;
    private final MessagingChannel channel;
    private final String platformMessageId;
    private final String senderId;
    private final String senderName;
    private final String recipientId;
    private final String chatId;
    private final String threadId;
    private final MessageType type;
    private final MessagePayload payload;
    private final List<InlineButton> buttons;
    private final MessageStatus status;
    private final long timestampEpochMs;
    private final boolean isOutgoing;

    public record InlineButton(String text, String callbackData, String url) {}

    private UniversalMessage(final Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.channel = builder.channel != null ? builder.channel : MessagingChannel.UNIFIED;
        this.platformMessageId = builder.platformMessageId != null ? builder.platformMessageId : "";
        this.senderId = builder.senderId != null ? builder.senderId : "";
        this.senderName = builder.senderName != null ? builder.senderName : "";
        this.recipientId = builder.recipientId != null ? builder.recipientId : "";
        this.chatId = builder.chatId != null ? builder.chatId : "";
        this.threadId = builder.threadId != null ? builder.threadId : "";
        this.type = builder.type != null ? builder.type : MessageType.TEXT;
        this.payload = builder.payload != null ? builder.payload : MessagePayload.ofText("");
        this.buttons = builder.buttons != null ? Collections.unmodifiableList(new ArrayList<>(builder.buttons)) : Collections.emptyList();
        this.status = builder.status != null ? builder.status : MessageStatus.PENDING;
        this.timestampEpochMs = builder.timestampEpochMs > 0 ? builder.timestampEpochMs : System.currentTimeMillis();
        this.isOutgoing = builder.isOutgoing;
    }

    public static UniversalMessage text(final MessagingChannel channel, final String chatId, final String text) {
        return new Builder()
            .channel(channel)
            .chatId(chatId)
            .type(MessageType.TEXT)
            .payload(MessagePayload.ofText(text))
            .build();
    }

    public String id() {
        return this.id;
    }

    public MessagingChannel channel() {
        return this.channel;
    }

    public String platformMessageId() {
        return this.platformMessageId;
    }

    public String senderId() {
        return this.senderId;
    }

    public String senderName() {
        return this.senderName;
    }

    public String recipientId() {
        return this.recipientId;
    }

    public String chatId() {
        return this.chatId;
    }

    public String threadId() {
        return this.threadId;
    }

    public MessageType type() {
        return this.type;
    }

    public MessagePayload payload() {
        return this.payload;
    }

    public String text() {
        return this.payload.text();
    }

    public List<InlineButton> buttons() {
        return this.buttons;
    }

    public MessageStatus status() {
        return this.status;
    }

    public long timestampEpochMs() {
        return this.timestampEpochMs;
    }

    public boolean isOutgoing() {
        return this.isOutgoing;
    }

    public UniversalMessage withStatus(final MessageStatus newStatus) {
        return new Builder(this).status(newStatus).build();
    }

    public UniversalMessage asForwardedTo(final MessagingChannel targetChannel, final String targetChatId) {
        return new Builder(this)
            .id(UUID.randomUUID().toString())
            .channel(targetChannel)
            .chatId(targetChatId)
            .isOutgoing(true)
            .status(MessageStatus.PENDING)
            .timestampEpochMs(System.currentTimeMillis())
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private MessagingChannel channel;
        private String platformMessageId;
        private String senderId;
        private String senderName;
        private String recipientId;
        private String chatId;
        private String threadId;
        private MessageType type;
        private MessagePayload payload;
        private List<InlineButton> buttons;
        private MessageStatus status;
        private long timestampEpochMs;
        private boolean isOutgoing;

        public Builder() {}

        public Builder(final UniversalMessage source) {
            this.id = source.id;
            this.channel = source.channel;
            this.platformMessageId = source.platformMessageId;
            this.senderId = source.senderId;
            this.senderName = source.senderName;
            this.recipientId = source.recipientId;
            this.chatId = source.chatId;
            this.threadId = source.threadId;
            this.type = source.type;
            this.payload = source.payload;
            this.buttons = new ArrayList<>(source.buttons);
            this.status = source.status;
            this.timestampEpochMs = source.timestampEpochMs;
            this.isOutgoing = source.isOutgoing;
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder channel(final MessagingChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder platformMessageId(final String platformMessageId) {
            this.platformMessageId = platformMessageId;
            return this;
        }

        public Builder senderId(final String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder senderName(final String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder recipientId(final String recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder chatId(final String chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder threadId(final String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder type(final MessageType type) {
            this.type = type;
            return this;
        }

        public Builder payload(final MessagePayload payload) {
            this.payload = payload;
            return this;
        }

        public Builder text(final String text) {
            this.payload = MessagePayload.ofText(text);
            return this;
        }

        public Builder addButton(final String text, final String callbackData) {
            if (this.buttons == null) {
                this.buttons = new ArrayList<>();
            }
            this.buttons.add(new InlineButton(text, callbackData, null));
            return this;
        }

        public Builder addUrlButton(final String text, final String url) {
            if (this.buttons == null) {
                this.buttons = new ArrayList<>();
            }
            this.buttons.add(new InlineButton(text, null, url));
            return this;
        }

        public Builder buttons(final List<InlineButton> buttons) {
            this.buttons = buttons;
            return this;
        }

        public Builder status(final MessageStatus status) {
            this.status = status;
            return this;
        }

        public Builder timestampEpochMs(final long timestampEpochMs) {
            this.timestampEpochMs = timestampEpochMs;
            return this;
        }

        public Builder isOutgoing(final boolean isOutgoing) {
            this.isOutgoing = isOutgoing;
            return this;
        }

        public UniversalMessage build() {
            return new UniversalMessage(this);
        }
    }

    @Override
    public String toString() {
        return "[" + this.channel.name() + ":" + this.type.name() + "] " +
               (this.senderName.isEmpty() ? this.senderId : this.senderName) +
               " -> " + this.chatId + ": " + this.payload;
    }
}
