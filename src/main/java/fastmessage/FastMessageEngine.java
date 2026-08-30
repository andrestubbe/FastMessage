package FastMessage;

import FastMessage.telegram.FastTelegram;
import FastMessage.whatsapp.FastWhatsApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Universal High-Performance Zero-Copy Messaging Engine.
 * Coordinates Telegram Bot API, WhatsApp Cloud API, custom adapters, and routing pipelines.
 */
public final class FastMessageEngine implements AutoCloseable {

    @FunctionalInterface
    public interface MessageInterceptor {
        UniversalMessage intercept(UniversalMessage message);
    }

    public record EngineMetrics(
        long totalIngested,
        long totalDispatched,
        long telegramMessages,
        long whatsappMessages,
        long errorsCount,
        long totalProcessingTimeNanos
    ) {
        public double averageProcessingMicros() {
            if (this.totalIngested == 0) return 0.0;
            return (this.totalProcessingTimeNanos / (double) this.totalIngested) / 1000.0;
        }
    }

    private FastTelegram telegramAdapter;
    private FastWhatsApp whatsAppAdapter;
    private final MessageRouter router;
    private final List<MessageInterceptor> inboundInterceptors = new ArrayList<>();
    private final List<MessageInterceptor> outboundInterceptors = new ArrayList<>();
    private final Map<MessagingChannel, Consumer<UniversalMessage>> customEgressAdapters = new ConcurrentHashMap<>();

    // Performance & Telemetry Counters
    private final AtomicLong totalIngested = new AtomicLong();
    private final AtomicLong totalDispatched = new AtomicLong();
    private final AtomicLong telegramMessages = new AtomicLong();
    private final AtomicLong whatsappMessages = new AtomicLong();
    private final AtomicLong errorsCount = new AtomicLong();
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong();

    public FastMessageEngine() {
        this.router = new MessageRouter();
    }

    public static FastMessageEngine create() {
        return new FastMessageEngine();
    }

    public FastMessageEngine withTelegram(final FastTelegram telegram) {
        this.telegramAdapter = telegram;
        return this;
    }

    public FastMessageEngine withWhatsApp(final FastWhatsApp whatsApp) {
        this.whatsAppAdapter = whatsApp;
        return this;
    }

    public FastMessageEngine withCustomChannel(final MessagingChannel channel, final Consumer<UniversalMessage> sender) {
        this.customEgressAdapters.put(channel, sender);
        return this;
    }

    public FastMessageEngine addInboundInterceptor(final MessageInterceptor interceptor) {
        this.inboundInterceptors.add(interceptor);
        return this;
    }

    public FastMessageEngine addOutboundInterceptor(final MessageInterceptor interceptor) {
        this.outboundInterceptors.add(interceptor);
        return this;
    }

    public MessageRouter router() {
        return this.router;
    }

    public FastTelegram telegram() {
        return this.telegramAdapter;
    }

    public FastWhatsApp whatsApp() {
        return this.whatsAppAdapter;
    }

    public UniversalMessage ingestTelegram(final ByteSlice webhookSlice) {
        final long start = System.nanoTime();
        try {
            if (this.telegramAdapter == null) {
                this.telegramAdapter = new FastTelegram("local-dev-mock-token");
            }
            UniversalMessage msg = this.telegramAdapter.toUniversal(webhookSlice);
            msg = processInbound(msg);
            this.telegramMessages.incrementAndGet();
            this.totalIngested.incrementAndGet();
            this.router.route(msg);
            return msg;
        } catch (final Throwable t) {
            this.errorsCount.incrementAndGet();
            throw t;
        } finally {
            this.totalProcessingTimeNanos.addAndGet(System.nanoTime() - start);
        }
    }

    public UniversalMessage ingestTelegram(final String webhookJson) {
        return ingestTelegram(ByteSlice.fromString(webhookJson));
    }

    public UniversalMessage ingestWhatsApp(final ByteSlice webhookSlice) {
        final long start = System.nanoTime();
        try {
            if (this.whatsAppAdapter == null) {
                this.whatsAppAdapter = new FastWhatsApp("local-phone-id", "mock-token");
            }
            UniversalMessage msg = this.whatsAppAdapter.toUniversal(webhookSlice);
            msg = processInbound(msg);
            this.whatsappMessages.incrementAndGet();
            this.totalIngested.incrementAndGet();
            this.router.route(msg);
            return msg;
        } catch (final Throwable t) {
            this.errorsCount.incrementAndGet();
            throw t;
        } finally {
            this.totalProcessingTimeNanos.addAndGet(System.nanoTime() - start);
        }
    }

    public UniversalMessage ingestWhatsApp(final String webhookJson) {
        return ingestWhatsApp(ByteSlice.fromString(webhookJson));
    }

    public UniversalMessage ingestUniversal(UniversalMessage message) {
        final long start = System.nanoTime();
        try {
            message = processInbound(message);
            if (message.channel() == MessagingChannel.TELEGRAM) {
                this.telegramMessages.incrementAndGet();
            } else if (message.channel() == MessagingChannel.WHATSAPP) {
                this.whatsappMessages.incrementAndGet();
            }
            this.totalIngested.incrementAndGet();
            this.router.route(message);
            return message;
        } catch (final Throwable t) {
            this.errorsCount.incrementAndGet();
            throw t;
        } finally {
            this.totalProcessingTimeNanos.addAndGet(System.nanoTime() - start);
        }
    }

    public CompletableFuture<UniversalMessage> sendAsync(UniversalMessage message) {
        final long start = System.nanoTime();
        try {
            message = processOutbound(message);
            final MessagingChannel channel = message.channel();
            this.totalDispatched.incrementAndGet();

            if (channel == MessagingChannel.TELEGRAM && this.telegramAdapter != null) {
                return this.telegramAdapter.sendAsync(message);
            } else if (channel == MessagingChannel.WHATSAPP && this.whatsAppAdapter != null) {
                return this.whatsAppAdapter.sendAsync(message);
            } else if (this.customEgressAdapters.containsKey(channel)) {
                final Consumer<UniversalMessage> customSender = this.customEgressAdapters.get(channel);
                final UniversalMessage outboundMsg = message;
                return CompletableFuture.supplyAsync(() -> {
                    customSender.accept(outboundMsg);
                    return outboundMsg.withStatus(MessageStatus.SENT);
                });
            } else {
                return CompletableFuture.completedFuture(message.withStatus(MessageStatus.SENT));
            }
        } catch (final Throwable t) {
            this.errorsCount.incrementAndGet();
            return CompletableFuture.completedFuture(message.withStatus(MessageStatus.FAILED));
        } finally {
            this.totalProcessingTimeNanos.addAndGet(System.nanoTime() - start);
        }
    }

    public List<CompletableFuture<UniversalMessage>> broadcast(final UniversalMessage message, final List<MessagingChannel> channels, final Map<MessagingChannel, String> targetChats) {
        final List<CompletableFuture<UniversalMessage>> futures = new ArrayList<>(channels.size());
        for (final MessagingChannel channel : channels) {
            final String targetChat = targetChats.getOrDefault(channel, message.chatId());
            final UniversalMessage outbound = message.asForwardedTo(channel, targetChat);
            futures.add(sendAsync(outbound));
        }
        return futures;
    }

    private UniversalMessage processInbound(UniversalMessage msg) {
        for (final MessageInterceptor interceptor : this.inboundInterceptors) {
            msg = interceptor.intercept(msg);
        }
        return msg;
    }

    private UniversalMessage processOutbound(UniversalMessage msg) {
        for (final MessageInterceptor interceptor : this.outboundInterceptors) {
            msg = interceptor.intercept(msg);
        }
        return msg;
    }

    public EngineMetrics metrics() {
        return new EngineMetrics(
            this.totalIngested.get(),
            this.totalDispatched.get(),
            this.telegramMessages.get(),
            this.whatsappMessages.get(),
            this.errorsCount.get(),
            this.totalProcessingTimeNanos.get()
        );
    }

    public void resetMetrics() {
        this.totalIngested.set(0);
        this.totalDispatched.set(0);
        this.telegramMessages.set(0);
        this.whatsappMessages.set(0);
        this.errorsCount.set(0);
        this.totalProcessingTimeNanos.set(0);
    }

    @Override
    public void close() {
        // Resource cleanups if applicable
    }
}
