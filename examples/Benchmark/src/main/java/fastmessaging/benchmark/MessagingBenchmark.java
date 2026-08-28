package fastmessaging.benchmark;

import fastmessaging.ByteSlice;
import fastmessaging.FastMessagingEngine;
import fastmessaging.MessagePayload;
import fastmessaging.MessageType;
import fastmessaging.MessagingChannel;
import fastmessaging.UniversalMessage;
import fastmessaging.telegram.FastTelegram;
import fastmessaging.telegram.TelegramKeyboard;
import fastmessaging.telegram.TelegramUpdate;
import fastmessaging.whatsapp.FastWhatsApp;
import fastmessaging.whatsapp.WhatsAppInteractive;
import fastmessaging.whatsapp.WhatsAppMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MessagingBenchmark {

    private byte[] rawTgBytes;
    private byte[] rawWaBytes;
    private ByteSlice tgSlice;
    private ByteSlice waSlice;
    private FastTelegram telegram;
    private FastWhatsApp whatsApp;
    private FastMessagingEngine engine;
    private UniversalMessage sampleUniversalMessage;

    @Setup
    public void setup() {
        final String rawTgWebhook = "{\"update_id\":8921004,\"message\":{\"message_id\":5412,\"chat\":{\"id\":-1001928374,\"title\":\"FastJava Core Team\",\"type\":\"supergroup\"},\"from\":{\"id\":998877,\"first_name\":\"Alice\",\"username\":\"alicewonder\"},\"text\":\"/deploy --target=production --profile=zero-copy\",\"date\":1724856254}}";
        final String rawWaWebhook = "{\"entry\":[{\"changes\":[{\"value\":{\"messaging_product\":\"whatsapp\",\"metadata\":{\"phone_number_id\":\"109988776655443\"},\"contacts\":[{\"profile\":{\"name\":\"Bob Systems\"},\"wa_id\":\"15550192834\"}],\"messages\":[{\"id\":\"wamid.HBgLMTU1NTAxOTI4MzQVAgASGBQzQUJDMDEyMzQ1Njc4OTA=\",\"from\":\"15550192834\",\"timestamp\":1724856255,\"type\":\"text\",\"text\":{\"body\":\"Approve production deployment for v0.1.0?\"}}]}}]}]}";

        this.rawTgBytes = rawTgWebhook.getBytes(StandardCharsets.UTF_8);
        this.rawWaBytes = rawWaWebhook.getBytes(StandardCharsets.UTF_8);

        this.tgSlice = ByteSlice.wrap(this.rawTgBytes);
        this.waSlice = ByteSlice.wrap(this.rawWaBytes);

        this.telegram = new FastTelegram("bot771234567:AAFn90MockToken");
        this.whatsApp = new FastWhatsApp("109988776655443", "EAAXmockToken");

        this.engine = FastMessagingEngine.create()
            .withTelegram(this.telegram)
            .withWhatsApp(this.whatsApp);

        this.sampleUniversalMessage = UniversalMessage.builder()
            .channel(MessagingChannel.TELEGRAM)
            .chatId("-1001928374")
            .text("FastMessaging Unified Pipeline Active")
            .addButton("Approve", "act_approve")
            .addButton("Reject", "act_reject")
            .build();
    }

    @Benchmark
    public void benchmarkByteSliceCreationAndSubSlice(final Blackhole bh) {
        final ByteSlice slice = ByteSlice.wrap(this.rawTgBytes);
        final ByteSlice sub = slice.subSlice(10, 45);
        bh.consume(sub);
    }

    @Benchmark
    public void benchmarkTelegramWebhookDecoding(final Blackhole bh) {
        final TelegramUpdate update = TelegramUpdate.parse(this.tgSlice);
        final UniversalMessage msg = update.toUniversalMessage();
        bh.consume(msg);
    }

    @Benchmark
    public void benchmarkWhatsAppWebhookDecoding(final Blackhole bh) {
        final WhatsAppMessage update = WhatsAppMessage.parse(this.waSlice);
        final UniversalMessage msg = update.toUniversalMessage();
        bh.consume(msg);
    }

    @Benchmark
    public void benchmarkTelegramKeyboardSerialization(final Blackhole bh) {
        final TelegramKeyboard kb = TelegramKeyboard.inline()
            .button("Confirm", "btn_yes")
            .button("Cancel", "btn_no")
            .row()
            .urlButton("Documentation", "https://github.com/andrestubbe/FastMessaging");
        bh.consume(kb.toJson());
    }

    @Benchmark
    public void benchmarkWhatsAppInteractiveSerialization(final Blackhole bh) {
        final WhatsAppInteractive interactive = WhatsAppInteractive.buttons("Please select action:")
            .addButton("opt_1", "Option 1")
            .addButton("opt_2", "Option 2");
        bh.consume(interactive.toJson());
    }

    @Benchmark
    public void benchmarkEndToEndCrossPlatformBridge(final Blackhole bh) {
        final UniversalMessage tgMsg = this.telegram.toUniversal(this.tgSlice);
        final String waPayload = this.whatsApp.fromUniversalMessage(tgMsg.asForwardedTo(MessagingChannel.WHATSAPP, "15550192834"));
        bh.consume(waPayload);
    }

    @Benchmark
    public void benchmarkRouterDispatching(final Blackhole bh) {
        final int matched = this.engine.router().route(this.sampleUniversalMessage);
        bh.consume(matched);
    }
}
