package FastMessage;

import FastMessage.telegram.FastTelegram;
import FastMessage.telegram.TelegramKeyboard;
import FastMessage.telegram.TelegramUpdate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FastTelegramTest {

    @Test
    public void testTelegramUpdateParsing() {
        final String json = "{\"update_id\":1001,\"message\":{\"message_id\":42,\"chat\":{\"id\":12345,\"type\":\"private\",\"first_name\":\"Alice\"},\"from\":{\"id\":999,\"username\":\"alicewonder\"},\"text\":\"Hello Telegram\",\"date\":1700000000}}";
        final TelegramUpdate update = TelegramUpdate.parse(json);

        assertEquals(1001, update.updateId());
        assertEquals(42, update.messageId());
        assertEquals(12345, update.chatId());
        assertEquals(999, update.senderId());
        assertEquals("alicewonder", update.senderUsername());
        assertEquals("Alice", update.senderFirstName());
        assertEquals("Hello Telegram", update.text());
        assertEquals(MessageType.TEXT, update.messageType());

        final UniversalMessage universal = update.toUniversalMessage();
        assertEquals(MessagingChannel.TELEGRAM, universal.channel());
        assertEquals("42", universal.platformMessageId());
        assertEquals("12345", universal.chatId());
        assertEquals("@alicewonder", universal.senderName());
        assertEquals("Hello Telegram", universal.text());
    }

    @Test
    public void testCallbackQueryParsing() {
        final String json = "{\"update_id\":1002,\"callback_query\":{\"id\":\"cb_99\",\"from\":{\"id\":888,\"username\":\"bob\"},\"chat\":{\"id\":555},\"message\":{\"message_id\":100},\"data\":\"btn_confirm\"}}";
        final TelegramUpdate update = TelegramUpdate.parse(json);

        assertEquals(1002, update.updateId());
        assertEquals("btn_confirm", update.callbackData());
        assertEquals("cb_99", update.callbackQueryId());
        assertEquals(MessageType.INTERACTIVE, update.messageType());
    }

    @Test
    public void testKeyboardSerialization() {
        final TelegramKeyboard kb = TelegramKeyboard.inline()
            .button("Option 1", "opt_1")
            .button("Option 2", "opt_2")
            .row()
            .urlButton("Visit Website", "https://github.com/andrestubbe/FastMessage")
            .row();

        final String json = kb.toJson();
        assertTrue(json.contains("\"Option 1\""));
        assertTrue(json.contains("\"opt_1\""));
        assertTrue(json.contains("\"Visit Website\""));
        assertTrue(json.contains("https://github.com/andrestubbe/FastMessage"));
    }

    @Test
    public void testEscaping() {
        final String raw = "Hello *world* [link](url) _test_ & <tag>";
        final String mdV2 = FastTelegram.escapeMarkdownV2(raw);
        assertTrue(mdV2.contains("\\*"));
        assertTrue(mdV2.contains("\\["));

        final String html = FastTelegram.escapeHtml(raw);
        assertTrue(html.contains("&lt;tag&gt;"));
        assertTrue(html.contains("&amp;"));
    }

    @Test
    public void testUniversalToTelegramEncoding() {
        final FastTelegram telegram = new FastTelegram("test-token");
        final UniversalMessage msg = UniversalMessage.builder()
            .channel(MessagingChannel.TELEGRAM)
            .chatId("123456")
            .text("Welcome to FastMessage")
            .addButton("Click Here", "act_click")
            .build();

        final String encoded = telegram.fromUniversalMessage(msg);
        assertTrue(encoded.contains("\"chat_id\":\"123456\""));
        assertTrue(encoded.contains("\"Welcome to FastMessage\""));
        assertTrue(encoded.contains("\"reply_markup\""));
        assertTrue(encoded.contains("\"act_click\""));
    }
}
