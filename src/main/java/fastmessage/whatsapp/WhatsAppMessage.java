package FastMessage.whatsapp;

import FastMessage.ByteSlice;
import FastMessage.MessagePayload;
import FastMessage.MessageStatus;
import FastMessage.MessageType;
import FastMessage.MessagingChannel;
import FastMessage.UniversalMessage;

import java.util.UUID;

/**
 * High-performance WhatsApp Webhook message model & zero-copy decoder.
 */
public final class WhatsAppMessage {

    private final String wamid;
    private final String fromPhone;
    private final String senderProfileName;
    private final String recipientPhoneId;
    private final MessageType messageType;
    private final String textBody;
    private final String mediaId;
    private final String mediaMimeType;
    private final String interactiveReplyId;
    private final String interactiveReplyTitle;
    private final MessageStatus status;
    private final long timestampEpochSec;

    public WhatsAppMessage(
            final String wamid,
            final String fromPhone,
            final String senderProfileName,
            final String recipientPhoneId,
            final MessageType messageType,
            final String textBody,
            final String mediaId,
            final String mediaMimeType,
            final String interactiveReplyId,
            final String interactiveReplyTitle,
            final MessageStatus status,
            final long timestampEpochSec) {
        this.wamid = wamid != null ? wamid : "";
        this.fromPhone = fromPhone != null ? fromPhone : "";
        this.senderProfileName = senderProfileName != null ? senderProfileName : "";
        this.recipientPhoneId = recipientPhoneId != null ? recipientPhoneId : "";
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.textBody = textBody != null ? textBody : "";
        this.mediaId = mediaId != null ? mediaId : "";
        this.mediaMimeType = mediaMimeType != null ? mediaMimeType : "";
        this.interactiveReplyId = interactiveReplyId != null ? interactiveReplyId : "";
        this.interactiveReplyTitle = interactiveReplyTitle != null ? interactiveReplyTitle : "";
        this.status = status != null ? status : MessageStatus.DELIVERED;
        this.timestampEpochSec = timestampEpochSec;
    }

    public static WhatsAppMessage parse(final ByteSlice jsonSlice) {
        return parse(jsonSlice.asUtf8String());
    }

    public static WhatsAppMessage parse(final String json) {
        if (json == null || json.isEmpty()) {
            return new WhatsAppMessage("", "", "", "", MessageType.TEXT, "", "", "", "", "", MessageStatus.DELIVERED, 0);
        }

        final String phoneId = extractString(json, "\"phone_number_id\":\"", "\"");
        
        // Status event check
        if (json.contains("\"statuses\":")) {
            final String statusWamid = extractString(json, "\"id\":\"", "\"");
            final String recipientId = extractString(json, "\"recipient_id\":\"", "\"");
            final String rawStatus = extractString(json, "\"status\":\"", "\"");
            final MessageStatus status = MessageStatus.fromStatus(rawStatus);
            final long ts = extractLong(json, "\"timestamp\":");

            return new WhatsAppMessage(
                statusWamid, recipientId, "", phoneId, MessageType.SYSTEM,
                "Status: " + rawStatus, "", "", "", "", status, ts
            );
        }

        final String wamid = extractString(json, "\"id\":\"", "\"");
        final String from = extractString(json, "\"from\":\"", "\"");
        final String profileName = extractString(json, "\"name\":\"", "\"");
        final long ts = extractLong(json, "\"timestamp\":");
        final String typeStr = extractString(json, "\"type\":\"", "\"");

        MessageType type = MessageType.fromCode(typeStr);
        String text = "";
        String mediaId = "";
        String mimeType = "";
        String interactiveId = "";
        String interactiveTitle = "";

        if ("text".equalsIgnoreCase(typeStr)) {
            text = extractString(json, "\"body\":\"", "\"");
        } else if ("image".equalsIgnoreCase(typeStr) || "video".equalsIgnoreCase(typeStr) || "audio".equalsIgnoreCase(typeStr) || "document".equalsIgnoreCase(typeStr)) {
            mediaId = extractString(json, "\"id\":\"", "\"");
            mimeType = extractString(json, "\"mime_type\":\"", "\"");
            text = extractString(json, "\"caption\":\"", "\"");
        } else if ("interactive".equalsIgnoreCase(typeStr) || "button".equalsIgnoreCase(typeStr)) {
            type = MessageType.INTERACTIVE;
            if (json.contains("\"button_reply\":")) {
                interactiveId = extractString(json, "\"id\":\"", "\"");
                interactiveTitle = extractString(json, "\"title\":\"", "\"");
                text = interactiveTitle;
            } else if (json.contains("\"list_reply\":")) {
                interactiveId = extractString(json, "\"id\":\"", "\"");
                interactiveTitle = extractString(json, "\"title\":\"", "\"");
                text = interactiveTitle;
            }
        }

        return new WhatsAppMessage(
            wamid, from, profileName, phoneId, type, text, mediaId, mimeType, interactiveId, interactiveTitle, MessageStatus.DELIVERED, ts
        );
    }

    public UniversalMessage toUniversalMessage() {
        final MessagePayload.Builder payloadBuilder = MessagePayload.builder()
            .text(this.textBody)
            .mediaUrl(this.mediaId)
            .mimeType(this.mediaMimeType);

        if (!this.interactiveReplyId.isEmpty()) {
            payloadBuilder.attribute("interactive_id", this.interactiveReplyId);
            payloadBuilder.attribute("interactive_title", this.interactiveReplyTitle);
        }

        return UniversalMessage.builder()
            .id(UUID.randomUUID().toString())
            .channel(MessagingChannel.WHATSAPP)
            .platformMessageId(this.wamid)
            .senderId(this.fromPhone)
            .senderName(this.senderProfileName.isEmpty() ? "+" + this.fromPhone : this.senderProfileName)
            .recipientId(this.recipientPhoneId)
            .chatId(this.fromPhone)
            .type(this.messageType)
            .payload(payloadBuilder.build())
            .status(this.status)
            .timestampEpochMs(this.timestampEpochSec > 0 ? this.timestampEpochSec * 1000 : System.currentTimeMillis())
            .isOutgoing(false)
            .build();
    }

    public String wamid() { return this.wamid; }
    public String fromPhone() { return this.fromPhone; }
    public String senderProfileName() { return this.senderProfileName; }
    public String recipientPhoneId() { return this.recipientPhoneId; }
    public MessageType messageType() { return this.messageType; }
    public String textBody() { return this.textBody; }
    public String mediaId() { return this.mediaId; }
    public String mediaMimeType() { return this.mediaMimeType; }
    public String interactiveReplyId() { return this.interactiveReplyId; }
    public String interactiveReplyTitle() { return this.interactiveReplyTitle; }
    public MessageStatus status() { return this.status; }
    public long timestampEpochSec() { return this.timestampEpochSec; }

    private static long extractLong(final String source, final String key) {
        final int idx = source.indexOf(key);
        if (idx < 0) return 0L;
        int start = idx + key.length();
        while (start < source.length() && (source.charAt(start) == ' ' || source.charAt(start) == ':' || source.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < source.length() && Character.isDigit(source.charAt(end))) {
            end++;
        }
        if (start >= end) return 0L;
        try {
            return Long.parseLong(source.substring(start, end));
        } catch (final NumberFormatException e) {
            return 0L;
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
