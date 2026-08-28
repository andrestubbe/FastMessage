package fastmessaging;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Zero-copy payload holder encapsulating text, byte slices, media references,
 * interactive payloads, and structured attributes.
 */
public final class MessagePayload {

    private final ByteSlice rawSlice;
    private final String text;
    private final String caption;
    private final String mediaUrl;
    private final String mimeType;
    private final long mediaSizeBytes;
    private final double latitude;
    private final double longitude;
    private final Map<String, Object> attributes;

    private MessagePayload(final Builder builder) {
        this.rawSlice = builder.rawSlice != null ? builder.rawSlice : (builder.text != null ? ByteSlice.fromString(builder.text) : ByteSlice.EMPTY);
        this.text = builder.text != null ? builder.text : (this.rawSlice.isEmpty() ? "" : this.rawSlice.asUtf8String());
        this.caption = builder.caption != null ? builder.caption : "";
        this.mediaUrl = builder.mediaUrl != null ? builder.mediaUrl : "";
        this.mimeType = builder.mimeType != null ? builder.mimeType : "";
        this.mediaSizeBytes = builder.mediaSizeBytes;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.attributes = builder.attributes != null ? Collections.unmodifiableMap(new HashMap<>(builder.attributes)) : Collections.emptyMap();
    }

    public static MessagePayload ofText(final String text) {
        return new Builder().text(text).build();
    }

    public static MessagePayload ofSlice(final ByteSlice slice) {
        return new Builder().rawSlice(slice).build();
    }

    public static MessagePayload ofMedia(final String mediaUrl, final String mimeType, final String caption) {
        return new Builder()
            .mediaUrl(mediaUrl)
            .mimeType(mimeType)
            .caption(caption)
            .build();
    }

    public static MessagePayload ofLocation(final double latitude, final double longitude) {
        return new Builder()
            .latitude(latitude)
            .longitude(longitude)
            .build();
    }

    public ByteSlice rawSlice() {
        return this.rawSlice;
    }

    public String text() {
        return this.text;
    }

    public String caption() {
        return this.caption;
    }

    public String mediaUrl() {
        return this.mediaUrl;
    }

    public String mimeType() {
        return this.mimeType;
    }

    public long mediaSizeBytes() {
        return this.mediaSizeBytes;
    }

    public double latitude() {
        return this.latitude;
    }

    public double longitude() {
        return this.longitude;
    }

    public Map<String, Object> attributes() {
        return this.attributes;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String key, final T defaultValue) {
        final Object val = this.attributes.get(key);
        if (val != null) {
            return (T) val;
        }
        return defaultValue;
    }

    public boolean hasMedia() {
        return !this.mediaUrl.isEmpty() || !this.mimeType.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ByteSlice rawSlice;
        private String text;
        private String caption;
        private String mediaUrl;
        private String mimeType;
        private long mediaSizeBytes;
        private double latitude;
        private double longitude;
        private Map<String, Object> attributes;

        public Builder rawSlice(final ByteSlice rawSlice) {
            this.rawSlice = rawSlice;
            return this;
        }

        public Builder text(final String text) {
            this.text = text;
            return this;
        }

        public Builder caption(final String caption) {
            this.caption = caption;
            return this;
        }

        public Builder mediaUrl(final String mediaUrl) {
            this.mediaUrl = mediaUrl;
            return this;
        }

        public Builder mimeType(final String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder mediaSizeBytes(final long mediaSizeBytes) {
            this.mediaSizeBytes = mediaSizeBytes;
            return this;
        }

        public Builder latitude(final double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(final double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder attribute(final String key, final Object value) {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }

        public MessagePayload build() {
            return new MessagePayload(this);
        }
    }

    @Override
    public String toString() {
        if (!this.text.isEmpty()) {
            return this.text;
        }
        if (!this.mediaUrl.isEmpty()) {
            return "[Media: " + this.mimeType + " -> " + this.mediaUrl + (this.caption.isEmpty() ? "" : " | " + this.caption) + "]";
        }
        if (this.latitude != 0 || this.longitude != 0) {
            return "[Location: " + this.latitude + ", " + this.longitude + "]";
        }
        return this.rawSlice.toString();
    }
}
