package fastmessaging.whatsapp;

import fastmessaging.ByteSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast WhatsApp Cloud API Interactive Message Builder (Reply Buttons, Lists, CTAs).
 */
public final class WhatsAppInteractive {

    public enum InteractiveType {
        BUTTON("button"),
        LIST("list"),
        CTA_URL("cta_url");

        private final String code;
        InteractiveType(final String code) { this.code = code; }
        public String code() { return this.code; }
    }

    public record Button(String id, String title) {}
    public record ListRow(String id, String title, String description) {}
    public record ListSection(String title, List<ListRow> rows) {}

    private final InteractiveType type;
    private String headerText = "";
    private String bodyText = "";
    private String footerText = "";
    private String actionButtonLabel = "Options";
    private final List<Button> buttons = new ArrayList<>();
    private final List<ListSection> sections = new ArrayList<>();
    private String ctaUrl = "";
    private String ctaDisplayText = "";

    private WhatsAppInteractive(final InteractiveType type) {
        this.type = type;
    }

    public static WhatsAppInteractive buttons(final String bodyText) {
        final WhatsAppInteractive interactive = new WhatsAppInteractive(InteractiveType.BUTTON);
        interactive.bodyText = bodyText;
        return interactive;
    }

    public static WhatsAppInteractive list(final String bodyText, final String buttonLabel) {
        final WhatsAppInteractive interactive = new WhatsAppInteractive(InteractiveType.LIST);
        interactive.bodyText = bodyText;
        interactive.actionButtonLabel = buttonLabel;
        return interactive;
    }

    public static WhatsAppInteractive ctaUrl(final String bodyText, final String displayText, final String url) {
        final WhatsAppInteractive interactive = new WhatsAppInteractive(InteractiveType.CTA_URL);
        interactive.bodyText = bodyText;
        interactive.ctaDisplayText = displayText;
        interactive.ctaUrl = url;
        return interactive;
    }

    public WhatsAppInteractive header(final String headerText) {
        this.headerText = headerText;
        return this;
    }

    public WhatsAppInteractive footer(final String footerText) {
        this.footerText = footerText;
        return this;
    }

    public WhatsAppInteractive addButton(final String id, final String title) {
        if (this.buttons.size() < 3) {
            this.buttons.add(new Button(id, title));
        }
        return this;
    }

    public WhatsAppInteractive addSection(final String title, final List<ListRow> rows) {
        this.sections.add(new ListSection(title, new ArrayList<>(rows)));
        return this;
    }

    public String toJson() {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        sb.append("\"type\":\"").append(this.type.code()).append("\",");

        if (!this.headerText.isEmpty()) {
            sb.append("\"header\":{\"type\":\"text\",\"text\":\"").append(escapeJson(this.headerText)).append("\"},");
        }

        sb.append("\"body\":{\"text\":\"").append(escapeJson(this.bodyText)).append("\"}");

        if (!this.footerText.isEmpty()) {
            sb.append(",\"footer\":{\"text\":\"").append(escapeJson(this.footerText)).append("\"}");
        }

        sb.append(",\"action\":{");

        if (this.type == InteractiveType.BUTTON) {
            sb.append("\"buttons\":[");
            for (int i = 0; i < this.buttons.size(); i++) {
                if (i > 0) sb.append(",");
                final Button btn = this.buttons.get(i);
                sb.append("{\"type\":\"reply\",\"reply\":{\"id\":\"")
                  .append(escapeJson(btn.id)).append("\",\"title\":\"")
                  .append(escapeJson(btn.title)).append("\"}}");
            }
            sb.append("]");
        } else if (this.type == InteractiveType.LIST) {
            sb.append("\"button\":\"").append(escapeJson(this.actionButtonLabel)).append("\",\"sections\":[");
            for (int s = 0; s < this.sections.size(); s++) {
                if (s > 0) sb.append(",");
                final ListSection sec = this.sections.get(s);
                sb.append("{\"title\":\"").append(escapeJson(sec.title)).append("\",\"rows\":[");
                for (int r = 0; r < sec.rows.size(); r++) {
                    if (r > 0) sb.append(",");
                    final ListRow row = sec.rows.get(r);
                    sb.append("{\"id\":\"").append(escapeJson(row.id))
                      .append("\",\"title\":\"").append(escapeJson(row.title)).append("\"");
                    if (row.description != null && !row.description.isEmpty()) {
                        sb.append(",\"description\":\"").append(escapeJson(row.description)).append("\"");
                    }
                    sb.append("}");
                }
                sb.append("]}");
            }
            sb.append("]");
        } else if (this.type == InteractiveType.CTA_URL) {
            sb.append("\"name\":\"cta_url\",\"parameters\":{\"display_text\":\"")
              .append(escapeJson(this.ctaDisplayText))
              .append("\",\"url\":\"").append(escapeJson(this.ctaUrl)).append("\"}");
        }

        sb.append("}}");
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
