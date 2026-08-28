# Changelog

All notable changes to `FastMessaging` will be documented in this file.

## [0.1.0] - 2026-08-28

### Added
- **Core Zero-Copy Engine**: Initial release of `FastMessagingEngine` with direct `ByteSlice` memory slicing and zero JVM heap allocations on hot-path ingestion.
- **Canonical Data Models**: `UniversalMessage`, `MessagePayload`, `MessageType`, `MessageStatus`, and `MessagingChannel`.
- **FastTelegram Bridge**:
  - Full webhook update parser (`TelegramUpdate`) supporting messages, callback queries, photos, documents, and locations.
  - Interactive Inline & Reply keyboard builder (`TelegramKeyboard`).
  - Outgoing message serializer and HTTP/2 dispatch client (`FastTelegram`).
  - HTML & MarkdownV2 text sanitizers.
- **FastWhatsApp Bridge**:
  - Webhook parser (`WhatsAppMessage`) for WhatsApp Cloud API message and status change notifications.
  - Interactive components builder (`WhatsAppInteractive`) for reply buttons, section lists, and CTA links.
  - Webhook Hub challenge validation and HMAC SHA-256 signature verification.
  - Outgoing message serializer and Meta Graph API v20.0 client (`FastWhatsApp`).
- **High-Speed Message Router**:
  - Predicate-based rule dispatcher (`MessageRouter`).
  - Command prefix matching (`onCommand`).
  - LRU deduplication window with configurable TTL.
  - Token-bucket rate limiter per chat / user key.
- **FastANSI 120-Column Hero Demo**:
  - 120-column terminal rendering with dark gray tree branches (`│`, `├──`, `└──`) and bold white values.
  - End-to-end bi-directional forwarding demonstration between Telegram and WhatsApp.
- **JMH Benchmark Suite**:
  - OpenJDK JMH microbenchmarks in `examples/Benchmark` measuring zero-copy decoding, keyboard/interactive JSON serialization, and cross-platform bridge throughput.
- **Complete Documentation & Tooling**:
  - `README.md`, `docs/PHILOSOPHY.md`, `docs/REFERENCE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `release/youtube_description.txt`.
  - `run-demo.bat`, `run-benchmark.bat`, `compile.bat`.
