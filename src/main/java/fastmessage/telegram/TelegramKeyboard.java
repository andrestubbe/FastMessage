package fastmessage.telegram;

import fastmessage.ByteSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast zero-allocation Telegram Keyboard generator for inline and reply keyboards.
 */
public final class TelegramKeyboard {

    public record Button(String text, String callbackData, String url) {}

    private final List<List<Button>> rows = new ArrayList<>();
    private List<Button> currentRow = new ArrayList<>();

    public static TelegramKeyboard inline() {
        return new TelegramKeyboard();
    }

    public TelegramKeyboard button(final String text, final String callbackData) {
        this.currentRow.add(new Button(text, callbackData, null));
        return this;
    }

    public TelegramKeyboard urlButton(final String text, final String url) {
        this.currentRow.add(new Button(text, null, url));
        return this;
    }

    public TelegramKeyboard row() {
        if (!this.currentRow.isEmpty()) {
            this.rows.add(new ArrayList<>(this.currentRow));
            this.currentRow.clear();
        }
        return this;
    }

    public String toJson() {
        if (!this.currentRow.isEmpty()) {
            this.rows.add(new ArrayList<>(this.currentRow));
            this.currentRow.clear();
        }
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{\"inline_keyboard\":[");
        for (int r = 0; r < this.rows.size(); r++) {
            if (r > 0) sb.append(",");
            sb.append("[");
            final List<Button> row = this.rows.get(r);
            for (int b = 0; b < row.size(); b++) {
                if (b > 0) sb.append(",");
                final Button btn = row.get(b);
                sb.append("{\"text\":\"").append(escapeJson(btn.text)).append("\"");
                if (btn.callbackData != null) {
                    sb.append(",\"callback_data\":\"").append(escapeJson(btn.callbackData)).append("\"");
                }
                if (btn.url != null) {
                    sb.append(",\"url\":\"").append(escapeJson(btn.url)).append("\"");
                }
                sb.append("}");
            }
            sb.append("]");
        }
        sb.append("]}");
        return sb.toString();
    }

    public ByteSlice toByteSlice() {
        return ByteSlice.fromString(toJson());
    }

    private static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
