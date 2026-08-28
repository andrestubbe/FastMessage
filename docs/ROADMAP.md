# FastMessaging Roadmap

Future milestones and planned extensions for the FastMessaging engine.

## v0.2.0 — Enterprise Messaging & Media Pipelines
- [ ] **Direct Native Attachment Streaming**: Zero-copy file and media streaming directly from disk descriptors into HTTP/2 multipart payloads.
- [ ] **Discord & Slack Adapters**: Native bridge implementations for Discord Webhooks / Bot Gateway and Slack Events API.
- [ ] **Persistent State Store Integration**: Optional memory-mapped (`FastMemory` / `FastFileSystem`) durable message journaling for zero-loss recovery.

## v0.3.0 — MTProto & Direct Baileys Protocol
- [ ] **Pure Java MTProto 2.0 Transport**: Direct TCP socket bridge to Telegram Data Centers bypassing bot API limits.
- [ ] **WhatsApp Web Multi-Device (Baileys compatible)**: Direct WebSocket end-to-end encrypted protocol adapter for direct number pairing.

## v0.4.0 — Autonomous Agent Orchestration
- [ ] **Agent Tool Direct Hook**: Native integration with `FastAIAgent` and `FastAIMCP` to expose multi-channel messaging as self-registering tool nodes.
- [ ] **Thread Continuity & Session Memory**: Tight coupling with `FastAIMemory` for automatic context hydration across Telegram and WhatsApp chat threads.
