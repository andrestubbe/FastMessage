package fastmessaging.telegram;

import fastmessaging.ByteSlice;
import fastmessaging.MessagePayload;
import fastmessaging.MessageStatus;
import fastmessaging.MessageType;
import fastmessaging.MessagingChannel;
import fastmessaging.UniversalMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * FastTelegram - High-performance adapter for Telegram Bot API & Webhooks.
 */
public final class FastTelegram {

    private final String botToken;
    private final String apiBaseUrl;
    private final HttpClient httpClient;

    public FastTelegram(final String botToken) {
        this(botToken, "https://api.telegram.org");
    }

    public FastTelegram(final String botToken, final String apiBaseUrl) {
        this.botToken = Objects.requireNonNull(botToken, "botToken cannot be null");
        this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl, "apiBaseUrl cannot be null");
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String botToken() {
        return this.botToken;
    }

    public String apiBaseUrl() {
        return this.apiBaseUrl;
    }

    public TelegramUpdate decodeWebhook(final ByteSlice payloadSlice) {
        return TelegramUpdate.parse(payloadSlice);
    }

    public TelegramUpdate decodeWebhook(final String payloadJson) {
        return TelegramUpdate.parse(payloadJson);
    }

    public UniversalMessage toUniversal(final String webhookJson) {
        return decodeWebhook(webhookJson).toUniversalMessage();
    }

    public UniversalMessage toUniversal(final ByteSlice webhookSlice) {
        return decodeWebhook(webhookSlice).toUniversalMessage();
    }

    public String encodeSendMessage(final String chatId, final String text, final TelegramKeyboard keyboard, final String parseMode) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"chat_id\":\"").append(escapeJson(chatId)).append("\",");
        sb.append("\"text\":\"").append(escapeJson(text)).append("\"");
        if (parseMode != null && !parseMode.isEmpty()) {
            sb.append(",\"parse_mode\":\"").append(escapeJson(parseMode)).append("\"");
        }
        if (keyboard != null) {
            sb.append(",\"reply_markup\":").append(keyboard.toJson());
        }
        sb.append("}");
        return sb.toString();
    }

    public String encodeSendPhoto(final String chatId, final String photoUrl, final String caption) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"chat_id\":\"").append(escapeJson(chatId)).append("\",");
        sb.append("\"photo\":\"").append(escapeJson(photoUrl)).append("\"");
        if (caption != null && !caption.isEmpty()) {
            sb.append(",\"caption\":\"").append(escapeJson(caption)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public String encodeAnswerCallbackQuery(final String callbackQueryId, final String text, final boolean showAlert) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("{");
        sb.append("\"callback_query_id\":\"").append(escapeJson(callbackQueryId)).append("\"");
        if (text != null && !text.isEmpty()) {
            sb.append(",\"text\":\"").append(escapeJson(text)).append("\"");
        }
        if (showAlert) {
            sb.append(",\"show_alert\":true");
        }
        sb.append("}");
        return sb.toString();
    }

    public String fromUniversalMessage(final UniversalMessage message) {
        final String chatId = message.chatId();
        final MessagePayload payload = message.payload();

        if (message.type() == MessageType.IMAGE && payload.hasMedia()) {
            return encodeSendPhoto(chatId, payload.mediaUrl(), payload.caption().isEmpty() ? payload.text() : payload.caption());
        }

        TelegramKeyboard kb = null;
        if (!message.buttons().isEmpty()) {
            kb = TelegramKeyboard.inline();
            for (final UniversalMessage.InlineButton btn : message.buttons()) {
                if (btn.url() != null) {
                    kb.urlButton(btn.text(), btn.url());
                } else {
                    kb.button(btn.text(), btn.callbackData() != null ? btn.callbackData() : btn.text());
                }
            }
            kb.row();
        }

        return encodeSendMessage(chatId, payload.text(), kb, "HTML");
    }

    public CompletableFuture<UniversalMessage> sendAsync(final UniversalMessage message) {
        final String jsonBody = fromUniversalMessage(message);
        final String endpoint = message.type() == MessageType.IMAGE ? "sendPhoto" : "sendMessage";
        final String url = this.apiBaseUrl + "/bot" + this.botToken + "/" + endpoint;

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(15))
            .build();

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(res -> {
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    final TelegramUpdate update = TelegramUpdate.parse(res.body());
                    return UniversalMessage.builder()
                        .id(message.id())
                        .channel(MessagingChannel.TELEGRAM)
                        .platformMessageId(String.valueOf(update.messageId()))
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

    /**
     * Polls new updates from Telegram via long-polling getUpdates API.
     *
     * @param offset  Identifier of the first update to be returned (or 0 for latest)
     * @param timeout Timeout in seconds for long polling (e.g. 10s)
     * @return Raw JSON response string from Telegram
     */
    public CompletableFuture<String> getUpdatesAsync(final long offset, final int timeout) {
        final String url = this.apiBaseUrl + "/bot" + this.botToken + "/getUpdates?offset=" + offset + "&timeout=" + timeout;
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(timeout + 5))
            .build();

        return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(HttpResponse::body);
    }

    public static String escapeMarkdownV2(final String text) {
        if (text == null) return "";
        final StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if ("_*[]()~`>#+-=|{}.!".indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String escapeHtml(final String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
