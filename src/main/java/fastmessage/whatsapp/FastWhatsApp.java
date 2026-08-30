package FastMessage.whatsapp;

import FastMessage.ByteSlice;
import FastMessage.MessagePayload;
import FastMessage.MessageStatus;
import FastMessage.MessageType;
import FastMessage.MessagingChannel;
import FastMessage.UniversalMessage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * FastWhatsApp - High-performance adapter for WhatsApp Cloud API & Webhooks.
 */
public final class FastWhatsApp {

    private final String phoneNumberId;
    private final String accessToken;
    private final String apiVersion;
    private final String apiBaseUrl;
    private final HttpClient httpClient;

    public FastWhatsApp(final String phoneNumberId, final String accessToken) {
        this(phoneNumberId, accessToken, "v20.0", "https://graph.facebook.com");
    }

    public FastWhatsApp(final String phoneNumberId, final String accessToken, final String apiVersion, final String apiBaseUrl) {
        this.phoneNumberId = Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken cannot be null");
        this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion cannot be null");
        this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl, "apiBaseUrl cannot be null");
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String phoneNumberId() {
        return this.phoneNumberId;
    }

    public String accessToken() {
        return this.accessToken;
    }

    public String apiVersion() {
        return this.apiVersion;
    }

    public WhatsAppMessage decodeWebhook(final ByteSlice payloadSlice) {
        return WhatsAppMessage.parse(payloadSlice);
    }

    public WhatsAppMessage decodeWebhook(final String payloadJson) {
        return WhatsAppMessage.parse(payloadJson);
    }

    public UniversalMessage toUniversal(final String webhookJson) {
        return decodeWebhook(webhookJson).toUniversalMessage();
    }

    public UniversalMessage toUniversal(final ByteSlice webhookSlice) {
        return decodeWebhook(webhookSlice).toUniversalMessage();
    }

    public static String verifyWebhookChallenge(final String mode, final String token, final String challenge, final String expectedVerifyToken) {
        if ("subscribe".equals(mode) && expectedVerifyToken != null && expectedVerifyToken.equals(token)) {
            return challenge != null ? challenge : "";
        }
        return null;
    }

    public static boolean verifySignature(final byte[] payload, final String signatureHeader, final String appSecret) {
        if (payload == null || signatureHeader == null || appSecret == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            final String expectedHex = signatureHeader.substring(7);
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            final byte[] hmac = mac.doFinal(payload);
            final StringBuilder sb = new StringBuilder(hmac.length * 2);
            for (final byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(sb.toString().getBytes(StandardCharsets.UTF_8), expectedHex.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            return false;
        }
    }

    public String encodeSendText(final String toPhone, final String text, final boolean previewUrl) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"messaging_product\":\"whatsapp\",");
        sb.append("\"recipient_type\":\"individual\",");
        sb.append("\"to\":\"").append(escapeJson(toPhone)).append("\",");
        sb.append("\"type\":\"text\",");
        sb.append("\"text\":{\"preview_url\":").append(previewUrl).append(",\"body\":\"").append(escapeJson(text)).append("\"}");
        sb.append("}");
        return sb.toString();
    }

    public String encodeSendImage(final String toPhone, final String imageUrl, final String caption) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"messaging_product\":\"whatsapp\",");
        sb.append("\"recipient_type\":\"individual\",");
        sb.append("\"to\":\"").append(escapeJson(toPhone)).append("\",");
        sb.append("\"type\":\"image\",");
        sb.append("\"image\":{\"link\":\"").append(escapeJson(imageUrl)).append("\"");
        if (caption != null && !caption.isEmpty()) {
            sb.append(",\"caption\":\"").append(escapeJson(caption)).append("\"");
        }
        sb.append("}}");
        return sb.toString();
    }

    public String encodeSendInteractive(final String toPhone, final WhatsAppInteractive interactive) {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        sb.append("\"messaging_product\":\"whatsapp\",");
        sb.append("\"recipient_type\":\"individual\",");
        sb.append("\"to\":\"").append(escapeJson(toPhone)).append("\",");
        sb.append("\"type\":\"interactive\",");
        sb.append("\"interactive\":").append(interactive.toJson());
        sb.append("}");
        return sb.toString();
    }

    public String encodeSendTemplate(final String toPhone, final String templateName, final String languageCode) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"messaging_product\":\"whatsapp\",");
        sb.append("\"recipient_type\":\"individual\",");
        sb.append("\"to\":\"").append(escapeJson(toPhone)).append("\",");
        sb.append("\"type\":\"template\",");
        sb.append("\"template\":{\"name\":\"").append(escapeJson(templateName)).append("\",\"language\":{\"code\":\"").append(escapeJson(languageCode)).append("\"}}");
        sb.append("}");
        return sb.toString();
    }

    public String fromUniversalMessage(final UniversalMessage message) {
        final String to = message.chatId().isEmpty() ? message.recipientId() : message.chatId();
        final MessagePayload payload = message.payload();

        if (message.type() == MessageType.IMAGE && payload.hasMedia()) {
            return encodeSendImage(to, payload.mediaUrl(), payload.caption().isEmpty() ? payload.text() : payload.caption());
        }

        if (!message.buttons().isEmpty()) {
            final WhatsAppInteractive interactive = WhatsAppInteractive.buttons(payload.text().isEmpty() ? "Choose an option:" : payload.text());
            for (int i = 0; i < Math.min(3, message.buttons().size()); i++) {
                final UniversalMessage.InlineButton btn = message.buttons().get(i);
                interactive.addButton(btn.callbackData() != null ? btn.callbackData() : ("btn_" + i), btn.text());
            }
            return encodeSendInteractive(to, interactive);
        }

        return encodeSendText(to, payload.text(), false);
    }

    public CompletableFuture<UniversalMessage> sendAsync(final UniversalMessage message) {
        final String jsonBody = fromUniversalMessage(message);
        final String url = this.apiBaseUrl + "/" + this.apiVersion + "/" + this.phoneNumberId + "/messages";

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + this.accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(15))
            .build();

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(res -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    final WhatsAppMessage parsed = WhatsAppMessage.parse(res.body());
                    return UniversalMessage.builder()
                        .id(message.id())
                        .channel(MessagingChannel.WHATSAPP)
                        .platformMessageId(parsed.wamid())
                        .chatId(message.chatId())
                        .type(message.type())
                        .payload(message.payload())
                        .status(MessageStatus.SENT)
                        .isOutgoing(true)
                        .build();
                } else {
                    return message.withStatus(MessageStatus.FAILED);
                }
            })
            .exceptionally(ex -> message.withStatus(MessageStatus.FAILED));
    }

    private static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
