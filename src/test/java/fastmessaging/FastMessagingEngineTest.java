package fastmessaging;

import fastmessaging.telegram.FastTelegram;
import fastmessaging.whatsapp.FastWhatsApp;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FastMessagingEngineTest {

    @Test
    public void testEngineIngestionAndMetrics() {
        final FastMessagingEngine engine = FastMessagingEngine.create()
            .withTelegram(new FastTelegram("mock-telegram-token"))
            .withWhatsApp(new FastWhatsApp("mock-phone-id", "mock-whatsapp-token"));

        final AtomicInteger receivedCount = new AtomicInteger();
        engine.router().onGlobal(msg -> receivedCount.incrementAndGet());

        final String tgPayload = "{\"update_id\":501,\"message\":{\"message_id\":10,\"chat\":{\"id\":999},\"from\":{\"id\":777,\"first_name\":\"Dave\"},\"text\":\"Ping Telegram\"}}";
        final String waPayload = "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"id\":\"wamid.123\",\"from\":\"15550001111\",\"type\":\"text\",\"text\":{\"body\":\"Ping WhatsApp\"}}]}}]}]}";

        final UniversalMessage tgMsg = engine.ingestTelegram(tgPayload);
        final UniversalMessage waMsg = engine.ingestWhatsApp(waPayload);

        assertEquals(2, receivedCount.get());
        assertEquals("Ping Telegram", tgMsg.text());
        assertEquals("Ping WhatsApp", waMsg.text());

        final FastMessagingEngine.EngineMetrics metrics = engine.metrics();
        assertEquals(2, metrics.totalIngested());
        assertEquals(1, metrics.telegramMessages());
        assertEquals(1, metrics.whatsappMessages());
        assertEquals(0, metrics.errorsCount());
        assertTrue(metrics.totalProcessingTimeNanos() > 0);
    }

    @Test
    public void testInterceptors() {
        final FastMessagingEngine engine = FastMessagingEngine.create();

        // Inbound interceptor modifying text (e.g. profanity filter or tagging)
        engine.addInboundInterceptor(msg -> UniversalMessage.builder()
            .channel(msg.channel())
            .chatId(msg.chatId())
            .text("[FILTERED] " + msg.text())
            .build());

        final UniversalMessage ingested = engine.ingestUniversal(UniversalMessage.builder()
            .channel(MessagingChannel.UNIFIED)
            .chatId("room-1")
            .text("System Notice")
            .build());

        assertEquals("[FILTERED] System Notice", ingested.text());
    }

    @Test
    public void testBroadcast() {
        final FastMessagingEngine engine = FastMessagingEngine.create();
        final List<UniversalMessage> sentMessages = new ArrayList<>();

        engine.withCustomChannel(MessagingChannel.CUSTOM, sentMessages::add);

        final UniversalMessage alert = UniversalMessage.builder()
            .id("alert-1")
            .text("Critical Server Alert")
            .build();

        final List<CompletableFuture<UniversalMessage>> futures = engine.broadcast(
            alert,
            List.of(MessagingChannel.CUSTOM, MessagingChannel.UNIFIED),
            Map.of(MessagingChannel.CUSTOM, "ops-channel", MessagingChannel.UNIFIED, "admin-room")
        );

        assertEquals(2, futures.size());
        for (final CompletableFuture<UniversalMessage> f : futures) {
            final UniversalMessage res = f.join();
            assertEquals(MessageStatus.SENT, res.status());
        }
    }
}
