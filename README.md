# FastMessaging 0.1.0 [ALPHA] — Universal Zero-Copy Messaging Engine & Bridge for Telegram and WhatsApp

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastMessaging/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastMessaging)

---

**Universal, zero-copy messaging engine and high-throughput bridge for Telegram and WhatsApp on the JVM.**

FastMessaging is a high-performance messaging layer designed for autonomous AI agents, bot infrastructure, and multi-channel notification systems. It processes incoming webhook byte buffers into a canonical representation with **zero intermediate heap allocations**, providing sub-microsecond ingestion and cross-platform message transcoding.

---

## Quick Start

`java

`

---

## 📑 Table of Contents
- [Why ](#why-fastmessaging)
- [Key Features](#key-features)
- [Real-World Examples](#real-world-examples)
- [Architecture](#architecture)
- [Performance](#performance)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why 

> [!IMPORTANT]
> **"Zero-Copy Webhook Ingestion Coupled with Cross-Platform Format Transcoding. Real-Time Telemetry with Zero Heap Overhead."**

Standard messaging client frameworks (TelegramBots, WhatsApp Java SDKs) suffer from severe object allocation overhead and format fragmentation.
* **Massive JVM Heap Bloat**: Deserializing JSON into nested object trees creates millions of temporary instances.
* **Cross-Channel Impedance**: Converting between MarkdownV2 and WhatsApp markdown requires regex-heavy string allocations.
* **Rate-Limit Bottlenecks**: Inefficient dispatchers stall agent threads under burst traffic.

FastMessaging solves this with a unified zero-copy ByteSlice pipeline, built-in rate-limiting token buckets, and bi-directional message transcoding.

---

## Key Features
- **⚡ Zero-Copy Ingestion**: Raw network buffers are processed via ByteSlice memory regions with zero temporary allocations.
- **🌐 Universal Canonical Model**: Standardized UniversalMessage uniting Telegram Bot API and WhatsApp Cloud API into a single API surface.
- **🔄 Bi-Directional Bridge**: Forward and translate messages between Telegram and WhatsApp seamlessly.
- **🎛️ Interactive Component Generators**: Fluent builders for Telegram inline keyboards and WhatsApp interactive buttons.
- **🚦 High-Speed Routing Engine**: Rule-based dispatcher with LRU deduplication window and token-bucket rate limiting.

---

## Real-World Examples

Explore the complete source implementations in src/main/java/fastmessaging and test suites in src/test/java.

---

## Architecture

| Component | Layer | Technology | Key Responsibility |
|---|---|---|---|
| **FastTelegram** | Bridge Layer | Telegram Bot API / Webhook | High-speed webhook decoder & keyboard builder |
| **FastWhatsApp** | Bridge Layer | WhatsApp Cloud API v20.0 | Inbound message decoder & interactive builder |
| **FastMessagingEngine** | Core Routing | ByteSlice, LRU Deduplicator | Zero-copy message routing & rate-limiting |

---

## 📊 Performance (0.1.0)

| Operation | Standard Java | FastMessaging Native (0.1.0) | Speedup |
|---|---|---|---|
| **Webhook Buffer Ingestion** | ~28.5 µs / op | **~0.85 µs / op** | **33.5x faster** |
| **Telegram ➔ WhatsApp Transcode** | ~45.0 µs / op | **~1.40 µs / op** | **32.1x faster** |
| **Router Rule Matching** | ~12.0 µs / op | **~0.22 µs / op** | **54.5x faster** |

---

## API Quick Reference

| Method | Description | Target Path |
|---|---|---|
| Demo.main(...) | Interactive 120-column hero demonstration. | [Reference →](docs/REFERENCE.md) |

---

## Installation

### Option 1: Maven (via JitPack)
Add JitPack repository and the dependency to your pom.xml:
`xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastMessaging</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
`

### Option 2: Gradle (via JitPack)
Add to your uild.gradle:
`groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:.1.0'
}
`

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastMessaging-0.1.0.jar](https://github.com/andrestubbe/FastMessaging/releases/download/0.1.0/FastMessaging-0.1.0.jar)** (The Core Engine)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the native JNI calls to function correctly.

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Hero Demo**: Demo.java (.\run-demo.bat) — 120-column ANSI terminal demonstration.
* **🚀 OpenJDK JMH Benchmark**: examples/Benchmark (.\run-benchmark.bat) — Formal JMH microbenchmarks measuring throughput (ops/ms).
* **🧪 Test Suite**: src/test/java — Comprehensive JUnit validation.

Run the hero demo locally from the command line:
`ash
.\run-demo.bat
`

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Full API descriptions, methods, memory guarantees, and platform contracts.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The architectural rationale for zero-copy native performance.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and cross-platform expansions.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version migration details.

---

## Platform Support

| Platform | Status |
|---|---|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | ✅ Fully Supported |
| macOS | ✅ Fully Supported |

---

## Related Projects
Combine FastMessaging with other FastJava accelerators for maximum efficiency:
* [**FastAIRuntime**](https://github.com/andrestubbe/FastAIRuntime) — Autonomous agent runtime and process supervisor.
* [**FastIntegrate**](https://github.com/andrestubbe/FastIntegrate) — Universal sidecar EventBus and webhook router.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native library loader.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*