package fastmessage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MessageRouterTest {

    @Test
    public void testRuleAndChannelRouting() {
        final MessageRouter router = new MessageRouter();
        final AtomicInteger telegramCount = new AtomicInteger();
        final AtomicInteger cmdStartCount = new AtomicInteger();

        router.onChannel(MessagingChannel.TELEGRAM, msg -> telegramCount.incrementAndGet());
        router.onCommand("/start", msg -> cmdStartCount.incrementAndGet());

        final UniversalMessage msg1 = UniversalMessage.builder()
            .id("msg-1")
            .channel(MessagingChannel.TELEGRAM)
            .text("/start welcome")
            .build();

        final UniversalMessage msg2 = UniversalMessage.builder()
            .id("msg-2")
            .channel(MessagingChannel.WHATSAPP)
            .text("Hello")
            .build();

        router.route(msg1);
        router.route(msg2);

        assertEquals(1, telegramCount.get());
        assertEquals(1, cmdStartCount.get());
    }

    @Test
    public void testDeduplication() {
        final MessageRouter router = new MessageRouter(100, 5000L);
        final AtomicInteger counter = new AtomicInteger();

        router.onGlobal(msg -> counter.incrementAndGet());

        final UniversalMessage msg = UniversalMessage.builder()
            .id("dup-id-1")
            .text("Test Duplicate")
            .build();

        router.route(msg);
        router.route(msg); // should be dropped by deduplicator

        assertEquals(1, counter.get());
    }

    @Test
    public void testRateLimiterTokenBucket() {
        final MessageRouter router = new MessageRouter();
        final String userKey = "user_1001";

        // Capacity 2, 1 token/sec refill
        assertTrue(router.allowRateLimit(userKey, 2, 1.0));
        assertTrue(router.allowRateLimit(userKey, 2, 1.0));
        assertFalse(router.allowRateLimit(userKey, 2, 1.0)); // exhausted
    }
}
