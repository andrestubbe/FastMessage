package FastMessage.telegram;

import FastMessage.ByteSlice;
import FastMessage.MessagePayload;
import FastMessage.MessageStatus;
import FastMessage.MessageType;
import FastMessage.MessagingChannel;
import FastMessage.UniversalMessage;

import java.util.UUID;

/**
 * Zero-allocation, lightweight Telegram Webhook Update model & decoder.
 */
public final class TelegramUpdate {

    private final long updateId;
    private final long messageId;
    private final long chatId;
    private final long senderId;
    private final String senderUsername;
    private final String senderFirstName;
    private final String text;
    private final String callbackData;
    private final String callbackQueryId;
    private final MessageType messageType;
    private final String mediaFileId;
    private final double latitude;
    private final double longitude;
    private final long dateEpochSec;

    public TelegramUpdate(
            final long updateId,
            final long messageId,
            final long chatId,
            final long senderId,
            final String senderUsername,
            final String senderFirstName,
            final String text,
            final String callbackData,
            final String callbackQueryId,
            final MessageType messageType,
            final String mediaFileId,
            final double latitude,
            final double longitude,
            final long dateEpochSec) {
        this.updateId = updateId;
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderUsername = senderUsername != null ? senderUsername : "";
        this.senderFirstName = senderFirstName != null ? senderFirstName : "";
        this.text = text != null ? text : "";
        this.callbackData = callbackData != null ? callbackData : "";
        this.callbackQueryId = callbackQueryId != null ? callbackQueryId : "";
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.mediaFileId = mediaFileId != null ? mediaFileId : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateEpochSec = dateEpochSec;
    }

    public static TelegramUpdate parse(final ByteSlice jsonSlice) {
        return parse(jsonSlice.asUtf8String());
    }

    public static TelegramUpdate parse(final String json) {
        if (json == null || json.isEmpty()) {
            return new TelegramUpdate(0, 0, 0, 0, "", "", "", "", "", MessageType.TEXT, "", 0, 0, 0);
        }

        final long updateId = extractLong(json, "\"update_id\":");
        
        // Check for callback query
        if (json.contains("\"callback_query\":")) {
            final String cbId = extractString(json, "\"callback_query\":[^{]*\\{[^}]*\"id\":\"", "\"");
            final String data = extractString(json, "\"data\":\"", "\"");
            final long senderId = extractLong(json, "\"from\":[^{]*\\{[^}]*\"id\":");
            final String username = extractString(json, "\"username\":\"", "\"");
            final String firstName = extractString(json, "\"first_name\":\"", "\"");
            final long chatId = extractLong(json, "\"chat\":[^{]*\\{[^}]*\"id\":");
            final long messageId = extractLong(json, "\"message\":[^{]*\\{[^}]*\"message_id\":");

            return new TelegramUpdate(
                updateId, messageId, chatId, senderId, username, firstName,
                data, data, cbId, MessageType.INTERACTIVE, "", 0, 0, System.currentTimeMillis() / 1000
            );
        }

        final long messageId = extractLong(json, "\"message_id\":");
        final long chatId = extractLong(json, "\"chat\":[^{]*\\{[^}]*\"id\":");
        final long senderId = extractLong(json, "\"from\":[^{]*\\{[^}]*\"id\":");
        final String username = extractString(json, "\"username\":\"", "\"");
        final String firstName = extractString(json, "\"first_name\":\"", "\"");
        final long date = extractLong(json, "\"date\":");

        MessageType type = MessageType.TEXT;
        String text = extractString(json, "\"text\":\"", "\"");
        String mediaId = "";
        double lat = 0.0;
        double lon = 0.0;

        if (json.contains("\"photo\":")) {
            type = MessageType.IMAGE;
            mediaId = extractString(json, "\"file_id\":\"", "\"");
            final String cap = extractString(json, "\"caption\":\"", "\"");
            if (!cap.isEmpty()) text = cap;
        } else if (json.contains("\"document\":")) {
            type = MessageType.DOCUMENT;
            mediaId = extractString(json, "\"file_id\":\"", "\"");
            final String cap = extractString(json, "\"caption\":\"", "\"");
            if (!cap.isEmpty()) text = cap;
        } else if (json.contains("\"location\":")) {
            type = MessageType.LOCATION;
            lat = extractDouble(json, "\"latitude\":");
            lon = extractDouble(json, "\"longitude\":");
        } else if (json.contains("\"sticker\":")) {
            type = MessageType.STICKER;
            mediaId = extractString(json, "\"file_id\":\"", "\"");
        }

        return new TelegramUpdate(
            updateId, messageId, chatId, senderId, username, firstName,
            text, "", "", type, mediaId, lat, lon, date
        );
    }

    public UniversalMessage toUniversalMessage() {
        final MessagePayload.Builder payloadBuilder = MessagePayload.builder()
            .text(this.text)
            .latitude(this.latitude)
            .longitude(this.longitude);

        if (!this.mediaFileId.isEmpty()) {
            payloadBuilder.mediaUrl(this.mediaFileId);
        }
        if (!this.callbackData.isEmpty()) {
            payloadBuilder.attribute("callback_data", this.callbackData);
            payloadBuilder.attribute("callback_query_id", this.callbackQueryId);
        }

        final String displayName = !this.senderUsername.isEmpty() ? "@" + this.senderUsername : this.senderFirstName;

        return UniversalMessage.builder()
            .id(UUID.randomUUID().toString())
            .channel(MessagingChannel.TELEGRAM)
            .platformMessageId(String.valueOf(this.messageId))
            .senderId(String.valueOf(this.senderId))
            .senderName(displayName)
            .chatId(String.valueOf(this.chatId))
            .type(this.messageType)
            .payload(payloadBuilder.build())
            .status(MessageStatus.DELIVERED)
            .timestampEpochMs(this.dateEpochSec > 0 ? this.dateEpochSec * 1000 : System.currentTimeMillis())
            .isOutgoing(false)
            .build();
    }

    public long updateId() { return this.updateId; }
    public long messageId() { return this.messageId; }
    public long chatId() { return this.chatId; }
    public long senderId() { return this.senderId; }
    public String senderUsername() { return this.senderUsername; }
    public String senderFirstName() { return this.senderFirstName; }
    public String text() { return this.text; }
    public String callbackData() { return this.callbackData; }
    public String callbackQueryId() { return this.callbackQueryId; }
    public MessageType messageType() { return this.messageType; }
    public String mediaFileId() { return this.mediaFileId; }
    public double latitude() { return this.latitude; }
    public double longitude() { return this.longitude; }
    public long dateEpochSec() { return this.dateEpochSec; }

    private static long extractLong(final String source, final String key) {
        final int idx = source.indexOf(key);
        if (idx < 0) return 0L;
        int start = idx + key.length();
        while (start < source.length() && (source.charAt(start) == ' ' || source.charAt(start) == ':')) {
            start++;
        }
        int end = start;
        while (end < source.length() && (Character.isDigit(source.charAt(end)) || source.charAt(end) == '-')) {
            end++;
        }
        if (start >= end) return 0L;
        try {
            return Long.parseLong(source.substring(start, end));
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    private static double extractDouble(final String source, final String key) {
        final int idx = source.indexOf(key);
        if (idx < 0) return 0.0;
        int start = idx + key.length();
        while (start < source.length() && (source.charAt(start) == ' ' || source.charAt(start) == ':')) {
            start++;
        }
        int end = start;
        while (end < source.length() && (Character.isDigit(source.charAt(end)) || source.charAt(end) == '.' || source.charAt(end) == '-')) {
            end++;
        }
        if (start >= end) return 0.0;
        try {
            return Double.parseDouble(source.substring(start, end));
        } catch (final NumberFormatException e) {
            return 0.0;
        }
    }

    private static String extractString(final String source, final String key, final String endDelimiter) {
        final int idx = source.indexOf(key);
        if (idx < 0) return "";
        final int start = idx + key.length();
        final int end = source.indexOf(endDelimiter, start);
        if (end < 0) return "";
        return source.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }
}
