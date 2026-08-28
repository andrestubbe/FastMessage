package fastmessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * High-speed rule-based router with deduplication, rate-limiting, and channel routing.
 */
public final class MessageRouter {

    public record RouteRule(String name, Predicate<UniversalMessage> predicate, Consumer<UniversalMessage> handler) {}

    private final List<RouteRule> rules = new ArrayList<>();
    private final List<Consumer<UniversalMessage>> globalListeners = new ArrayList<>();
    private final Map<String, Long> deduplicationCache;
    private final Map<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();
    private final long deduplicationTtlMs;
    private final int maxDeduplicationEntries;

    public MessageRouter() {
        this(10_000, 60_000L);
    }

    public MessageRouter(final int maxDeduplicationEntries, final long deduplicationTtlMs) {
        this.maxDeduplicationEntries = maxDeduplicationEntries;
        this.deduplicationTtlMs = deduplicationTtlMs;
        this.deduplicationCache = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<String, Long> eldest) {
                return size() > maxDeduplicationEntries;
            }
        });
    }

    public MessageRouter addRule(final String name, final Predicate<UniversalMessage> predicate, final Consumer<UniversalMessage> handler) {
        this.rules.add(new RouteRule(name, predicate, handler));
        return this;
    }

    public MessageRouter onChannel(final MessagingChannel channel, final Consumer<UniversalMessage> handler) {
        return addRule("channel_" + channel.id(), msg -> msg.channel() == channel, handler);
    }

    public MessageRouter onCommand(final String command, final Consumer<UniversalMessage> handler) {
        return addRule("cmd_" + command, msg -> {
            final String text = msg.text();
            return text != null && (text.equals(command) || text.startsWith(command + " "));
        }, handler);
    }

    public MessageRouter onGlobal(final Consumer<UniversalMessage> listener) {
        this.globalListeners.add(listener);
        return this;
    }

    public boolean isDuplicate(final String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        final long now = System.currentTimeMillis();
        final Long prev = this.deduplicationCache.put(messageId, now);
        if (prev != null && (now - prev) < this.deduplicationTtlMs) {
            return true;
        }
        return false;
    }

    public boolean allowRateLimit(final String key, final int capacity, final double refillPerSecond) {
        final TokenBucket bucket = this.rateLimiters.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond));
        return bucket.tryConsume();
    }

    public int route(final UniversalMessage message) {
        if (message == null) {
            return 0;
        }

        // Deduplication check
        if (isDuplicate(message.platformMessageId().isEmpty() ? message.id() : message.platformMessageId())) {
            return 0;
        }

        // Global listeners
        for (final Consumer<UniversalMessage> listener : this.globalListeners) {
            try {
                listener.accept(message);
            } catch (final Throwable ignored) {}
        }

        int matches = 0;
        for (final RouteRule rule : this.rules) {
            if (rule.predicate.test(message)) {
                matches++;
                try {
                    rule.handler.accept(message);
                } catch (final Throwable ignored) {}
            }
        }
        return matches;
    }

    public static final class TokenBucket {
        private final int capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(final int capacity, final double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            final long now = System.nanoTime();
            final double deltaSeconds = (now - this.lastRefillTime) / 1_000_000_000.0;
            this.lastRefillTime = now;
            this.tokens = Math.min(this.capacity, this.tokens + (deltaSeconds * this.refillPerSecond));

            if (this.tokens >= 1.0) {
                this.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
