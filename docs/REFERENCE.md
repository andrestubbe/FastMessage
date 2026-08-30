# FastMessage Technical Reference Manual

FastMessage provides a universal high-performance messaging engine for Telegram Bot API, WhatsApp Cloud API, and custom event streams.

---

## 1. Core Classes & Data Models

### `FastMessage.ByteSlice`
Zero-copy UTF-8 byte slice representation for memory buffer indexing without string allocations.
- `ByteSlice.wrap(byte[] data)`: Wraps full byte array.
- `ByteSlice.wrap(byte[] data, int offset, int length)`: Wraps sub-slice.
- `ByteSlice.fromString(String str)`: Wraps UTF-8 bytes of string.
- `ByteSlice.fromByteBuffer(ByteBuffer bb)`: Wraps direct/heap byte buffers.
- `subSlice(int start, int end)`: Creates child sub-slice without buffer copies.
- `equalsUtf8(String str)`: Zero-alloc character comparison against string.
- `parseLong()` / `parseInt()`: High-speed ASCII numeric decoding.

### `FastMessage.UniversalMessage`
Canonical, platform-agnostic message model.
- `id()`: Unique identifier (UUID or platform-assigned).
- `channel()`: `MessagingChannel.TELEGRAM`, `WHATSAPP`, `UNIFIED`, `CUSTOM`.
- `platformMessageId()`: Native platform message ID (`message_id` or `wamid`).
- `senderId()`, `senderName()`: Originator identifier and display name.
- `recipientId()`, `chatId()`: Destination phone/chat ID.
- `type()`: `MessageType` (`TEXT`, `IMAGE`, `DOCUMENT`, `LOCATION`, `INTERACTIVE`, etc.).
- `payload()`: Zero-copy `MessagePayload` holding content, media URLs, metadata.
- `buttons()`: List of `InlineButton` elements for interactive replies.
- `status()`: `MessageStatus` (`PENDING`, `SENT`, `DELIVERED`, `READ`, `FAILED`).
- `asForwardedTo(MessagingChannel channel, String targetChatId)`: Transforms message for target channel.

### `FastMessage.FastMessageEngine`
Central orchestration runtime.
- `withTelegram(FastTelegram telegram)`: Binds Telegram adapter.
- `withWhatsApp(FastWhatsApp whatsApp)`: Binds WhatsApp Cloud API adapter.
- `ingestTelegram(ByteSlice / String payload)`: Decodes and routes Telegram webhook.
- `ingestWhatsApp(ByteSlice / String payload)`: Decodes and routes WhatsApp webhook.
- `ingestUniversal(UniversalMessage message)`: Routes canonical message.
- `sendAsync(UniversalMessage message)`: Dispatches message asynchronously via appropriate channel.
- `broadcast(UniversalMessage message, List<MessagingChannel> channels, Map<MessagingChannel, String> targetChats)`: Multicasts across platforms.
- `metrics()`: Returns live throughput, message counters, and average latency.

### `FastMessage.MessageRouter`
High-speed rule dispatcher with rate-limiting and deduplication.
- `addRule(String name, Predicate<UniversalMessage> pred, Consumer<UniversalMessage> handler)`: Adds routing rule.
- `onChannel(MessagingChannel channel, Consumer<UniversalMessage> handler)`: Channel-specific handler.
- `onCommand(String command, Consumer<UniversalMessage> handler)`: Prefix command matching (e.g. `/start`, `/help`).
- `onGlobal(Consumer<UniversalMessage> listener)`: Global tap for telemetry/logging.
- `allowRateLimit(String key, int capacity, double refillPerSecond)`: Token bucket rate limiter.
- `isDuplicate(String messageId)`: LRU deduplication window.

---

## 2. Telegram Bridge (`FastMessage.telegram`)

### `FastTelegram`
- `decodeWebhook(ByteSlice slice)`: Parses webhook update into `TelegramUpdate`.
- `toUniversal(ByteSlice slice)`: Directly creates `UniversalMessage`.
- `encodeSendMessage(chatId, text, keyboard, parseMode)`: Encodes Telegram JSON.
- `encodeSendPhoto(chatId, photoUrl, caption)`: Encodes Telegram Photo JSON.
- `fromUniversalMessage(UniversalMessage msg)`: Transcodes `UniversalMessage` to Telegram JSON.
- `sendAsync(UniversalMessage msg)`: Performs HTTP/2 POST to Telegram Bot API.

### `TelegramKeyboard`
- `TelegramKeyboard.inline()`: Creates inline keyboard builder.
- `button(String text, String callbackData)`: Adds callback button.
- `urlButton(String text, String url)`: Adds URL button.
- `row()`: Finalizes current row.
- `toJson()`: Zero-alloc JSON rendering.

---

## 3. WhatsApp Bridge (`FastMessage.whatsapp`)

### `FastWhatsApp`
- `decodeWebhook(ByteSlice slice)`: Parses WhatsApp webhook into `WhatsAppMessage`.
- `toUniversal(ByteSlice slice)`: Directly creates `UniversalMessage`.
- `verifyWebhookChallenge(mode, token, challenge, expectedToken)`: Hub challenge verification.
- `verifySignature(payloadBytes, headerSignature, appSecret)`: HMAC-SHA256 signature validator.
- `encodeSendText(toPhone, text, previewUrl)`: Encodes text payload.
- `encodeSendInteractive(toPhone, interactive)`: Encodes interactive reply button payload.
- `fromUniversalMessage(UniversalMessage msg)`: Transcodes `UniversalMessage` to WhatsApp JSON.
- `sendAsync(UniversalMessage msg)`: Performs HTTP/2 POST to Meta Graph API.

### `WhatsAppInteractive`
- `WhatsAppInteractive.buttons(String bodyText)`: Quick reply buttons.
- `WhatsAppInteractive.list(String bodyText, String buttonLabel)`: Multi-section list selector.
- `WhatsAppInteractive.ctaUrl(String bodyText, String displayText, String url)`: Call to action button.
- `header(String text)` / `footer(String text)`: Adds header/footer cards.
- `toJson()`: High-efficiency JSON representation.
