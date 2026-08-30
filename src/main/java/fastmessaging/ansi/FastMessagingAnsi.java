package fastmessaging.ansi;

/**
 * 120-column Terminal ANSI Formatter strictly styled for FastJava Hero Demos.
 * Features dark gray tree branches, bold white values, colored channel badges,
 * and middle-path truncation to guarantee precise 120-column layout.
 */
public final class FastMessagingAnsi {

    public static final int TERMINAL_WIDTH = 120;

    // ANSI Colors & Styles
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";

    // Standard & Bright Colors
    public static final String GRAY = "\u001B[90m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BOLD_WHITE = "\u001B[1;97m";
    public static final String BOLD_CYAN = "\u001B[1;96m";
    public static final String BOLD_GREEN = "\u001B[1;92m";
    public static final String BOLD_YELLOW = "\u001B[1;93m";
    public static final String BOLD_MAGENTA = "\u001B[1;95m";
    public static final String BOLD_BLUE = "\u001B[1;94m";

    // Tree Branches in Dark Gray
    public static final String TREE_BRANCH = GRAY + "├── " + RESET;
    public static final String TREE_LAST   = GRAY + "└── " + RESET;
    public static final String TREE_PIPE   = GRAY + "│   " + RESET;
    public static final String TREE_SPACE  = "    ";

    private FastMessagingAnsi() {}

    public static void printHeader(final String title, final String subtitle) {
        final String border = GRAY + "═".repeat(TERMINAL_WIDTH) + RESET;
        System.out.println(border);
        final String centeredTitle = center(title, TERMINAL_WIDTH);
        System.out.println(BOLD_WHITE + centeredTitle + RESET);
        if (subtitle != null && !subtitle.isEmpty()) {
            final String centeredSub = center(subtitle, TERMINAL_WIDTH);
            System.out.println(GRAY + centeredSub + RESET);
        }
        System.out.println(border);
    }

    public static void printSection(final String title) {
        final String text = " [ " + title + " ] ";
        final int sideLen = Math.max(0, (TERMINAL_WIDTH - text.length()) / 2);
        final String left = GRAY + "─".repeat(sideLen) + RESET;
        final String right = GRAY + "─".repeat(TERMINAL_WIDTH - sideLen - text.length()) + RESET;
        System.out.println("\n" + left + BOLD_WHITE + text + RESET + right);
    }

    public static void printTreeItem(final String label, final String value, final boolean isLast) {
        final String prefix = isLast ? TREE_LAST : TREE_BRANCH;
        System.out.println(prefix + GRAY + label + ": " + RESET + BOLD_WHITE + value + RESET);
    }

    public static void printTreeSubItem(final String label, final String value, final boolean parentIsLast, final boolean isLast) {
        final String parentPrefix = parentIsLast ? TREE_SPACE : TREE_PIPE;
        final String subPrefix = isLast ? TREE_LAST : TREE_BRANCH;
        System.out.println(parentPrefix + subPrefix + GRAY + label + ": " + RESET + BOLD_WHITE + value + RESET);
    }

    public static void printTreeMessage(final String channel, final String sender, final String target, final String payload, final boolean isLast) {
        final String prefix = isLast ? TREE_LAST : TREE_BRANCH;
        final String channelBadge = formatBadge(channel);
        final String textSnippet = truncateMiddle(payload, 45);
        System.out.println(prefix + channelBadge + " " + BOLD_WHITE + sender + RESET + GRAY + " ➔ " + RESET + CYAN + target + RESET + GRAY + " │ " + RESET + BOLD_WHITE + textSnippet + RESET);
    }

    public static void printBenchmarkRow(final String benchmark, final String mode, final long opsPerSec, final double avgLatencyNs) {
        final String benchTrunc = padRight(truncateMiddle(benchmark, 40), 40);
        final String modePad = padRight(mode, 12);
        final String opsFormatted = String.format("%,d ops/s", opsPerSec);
        final String opsPad = padLeft(opsFormatted, 18);
        final String latFormatted = String.format("%.2f ns/op", avgLatencyNs);
        final String latPad = padLeft(latFormatted, 16);

        System.out.println(TREE_BRANCH + GRAY + benchTrunc + RESET + " │ " + GRAY + modePad + RESET + " │ " + BOLD_WHITE + opsPad + RESET + " │ " + GRAY + latPad + RESET);
    }

    public static String formatBadge(final String channel) {
        if ("TELEGRAM".equalsIgnoreCase(channel)) {
            return BOLD_BLUE + "[ TELEGRAM ]" + RESET;
        } else if ("WHATSAPP".equalsIgnoreCase(channel)) {
            return BOLD_GREEN + "[ WHATSAPP ]" + RESET;
        } else if ("UNIFIED".equalsIgnoreCase(channel)) {
            return BOLD_MAGENTA + "[ UNIFIED  ]" + RESET;
        }
        return BOLD_CYAN + "[ " + padRight(channel, 8) + " ]" + RESET;
    }

    public static String truncateMiddle(final String text, final int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        if (maxLength <= 7) return text.substring(0, Math.max(0, maxLength - 3)) + "...";
        final int partLen = (maxLength - 3) / 2;
        final int remainder = (maxLength - 3) % 2;
        return text.substring(0, partLen + remainder) + "..." + text.substring(text.length() - partLen);
    }

    public static String center(final String text, final int width) {
        if (text == null) return "";
        final int pad = width - text.length();
        if (pad <= 0) return text;
        final int left = pad / 2;
        final int right = pad - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    public static String padRight(final String text, final int width) {
        if (text == null) return " ".repeat(width);
        if (text.length() >= width) return text;
        return text + " ".repeat(width - text.length());
    }

    public static String padLeft(final String text, final int width) {
        if (text == null) return " ".repeat(width);
        if (text.length() >= width) return text;
        return " ".repeat(width - text.length()) + text;
    }
}
