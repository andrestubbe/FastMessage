package fastmessaging;

import fastmessaging.whatsapp.FastWhatsApp;
import fastmessaging.whatsapp.WhatsAppInteractive;
import fastmessaging.whatsapp.WhatsAppMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FastWhatsAppTest {

    @Test
    public void testWhatsAppWebhookMessageParsing() {
        final String json = "{\"entry\":[{\"changes\":[{\"value\":{\"messaging_product\":\"whatsapp\",\"metadata\":{\"phone_number_id\":\"987654321\"},\"contacts\":[{\"profile\":{\"name\":\"Charlie\"},\"wa_id\":\"15551234567\"}],\"messages\":[{\"id\":\"wamid.HBgL12345\",\"from\":\"15551234567\",\"timestamp\":1700001000,\"type\":\"text\",\"text\":{\"body\":\"Hello WhatsApp\"}}]}}]}]}";

        final WhatsAppMessage msg = WhatsAppMessage.parse(json);
        assertEquals("wamid.HBgL12345", msg.wamid());
        assertEquals("15551234567", msg.fromPhone());
        assertEquals("Charlie", msg.senderProfileName());
        assertEquals("987654321", msg.recipientPhoneId());
        assertEquals(MessageType.TEXT, msg.messageType());
        assertEquals("Hello WhatsApp", msg.textBody());

        final UniversalMessage universal = msg.toUniversalMessage();
        assertEquals(MessagingChannel.WHATSAPP, universal.channel());
        assertEquals("15551234567", universal.chatId());
        assertEquals("Charlie", universal.senderName());
        assertEquals("Hello WhatsApp", universal.text());
    }

    @Test
    public void testWhatsAppWebhookInteractiveParsing() {
        final String json = "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"id\":\"wamid.HBgL67890\",\"from\":\"15559876543\",\"timestamp\":1700002000,\"type\":\"interactive\",\"interactive\":{\"type\":\"button_reply\",\"button_reply\":{\"id\":\"btn_accept\",\"title\":\"Accept Terms\"}}}]}}]}]}";

        final WhatsAppMessage msg = WhatsAppMessage.parse(json);
        assertEquals("wamid.HBgL67890", msg.wamid());
        assertEquals(MessageType.INTERACTIVE, msg.messageType());
        assertEquals("btn_accept", msg.interactiveReplyId());
        assertEquals("Accept Terms", msg.interactiveReplyTitle());
        assertEquals("Accept Terms", msg.textBody());
    }

    @Test
    public void testWebhookVerificationAndSignature() {
        final String challenge = FastWhatsApp.verifyWebhookChallenge("subscribe", "my_secret_token", "challenge_12345", "my_secret_token");
        assertEquals("challenge_12345", challenge);

        assertNull(FastWhatsApp.verifyWebhookChallenge("subscribe", "wrong_token", "challenge_12345", "my_secret_token"));

        final byte[] payload = "{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8);
        final String appSecret = "super_app_secret";
        // Calculate known valid signature or test invalid rejection
        assertFalse(FastWhatsApp.verifySignature(payload, "sha256=invalid_hash", appSecret));
    }

    @Test
    public void testInteractiveButtonEncoding() {
        final WhatsAppInteractive interactive = WhatsAppInteractive.buttons("Please select an action:")
            .header("Order Confirmation")
            .footer("FastMessaging WhatsApp Gateway")
            .addButton("pay_now", "Pay Now")
            .addButton("cancel", "Cancel");

        final String json = interactive.toJson();
        assertTrue(json.contains("\"type\":\"button\""));
        assertTrue(json.contains("\"Order Confirmation\""));
        assertTrue(json.contains("\"Pay Now\""));
        assertTrue(json.contains("\"pay_now\""));
    }

    @Test
    public void testUniversalToWhatsAppEncoding() {
        final FastWhatsApp whatsApp = new FastWhatsApp("1234567890", "test-token");
        final UniversalMessage msg = UniversalMessage.builder()
            .channel(MessagingChannel.WHATSAPP)
            .chatId("15551234567")
            .text("Welcome to FastMessaging on WhatsApp!")
            .addButton("Learn More", "btn_learn")
            .build();

        final String encoded = whatsApp.fromUniversalMessage(msg);
        assertTrue(encoded.contains("\"to\":\"15551234567\""));
        assertTrue(encoded.contains("\"type\":\"interactive\""));
        assertTrue(encoded.contains("Learn More"));
    }
}
