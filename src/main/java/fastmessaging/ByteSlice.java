package fastmessaging;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Zero-copy UTF-8 byte slice representation for high-throughput messaging payloads.
 * Eliminates intermediate {@link String} and byte array allocations during webhook decoding
 * and payload serialization.
 */
public final class ByteSlice implements CharSequence {

    private static final byte[] EMPTY_BYTES = new byte[0];
    public static final ByteSlice EMPTY = new ByteSlice(EMPTY_BYTES, 0, 0);

    private final byte[] buffer;
    private final int offset;
    private final int length;
    private int hash;

    public ByteSlice(final byte[] buffer, final int offset, final int length) {
        if (buffer == null) {
            this.buffer = EMPTY_BYTES;
            this.offset = 0;
            this.length = 0;
        } else {
            if (offset < 0 || length < 0 || offset + length > buffer.length) {
                throw new IndexOutOfBoundsException(
                    "Offset " + offset + " + length " + length + " exceeds buffer size " + buffer.length
                );
            }
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }
    }

    public static ByteSlice wrap(final byte[] data) {
        if (data == null || data.length == 0) {
            return EMPTY;
        }
        return new ByteSlice(data, 0, data.length);
    }

    public static ByteSlice wrap(final byte[] data, final int offset, final int length) {
        return new ByteSlice(data, offset, length);
    }

    public static ByteSlice fromString(final String str) {
        if (str == null || str.isEmpty()) {
            return EMPTY;
        }
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return new ByteSlice(bytes, 0, bytes.length);
    }

    public static ByteSlice fromByteBuffer(final ByteBuffer byteBuffer) {
        if (byteBuffer == null || !byteBuffer.hasRemaining()) {
            return EMPTY;
        }
        final int len = byteBuffer.remaining();
        if (byteBuffer.hasArray()) {
            return new ByteSlice(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), len);
        }
        final byte[] copied = new byte[len];
        final int curPos = byteBuffer.position();
        byteBuffer.get(copied);
        byteBuffer.position(curPos);
        return new ByteSlice(copied, 0, len);
    }

    public byte[] buffer() {
        return this.buffer;
    }

    public int offset() {
        return this.offset;
    }

    @Override
    public int length() {
        return this.length;
    }

    public boolean isEmpty() {
        return this.length == 0;
    }

    public byte getByte(final int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + this.length);
        }
        return this.buffer[this.offset + index];
    }

    @Override
    public char charAt(final int index) {
        return (char) (getByte(index) & 0xFF);
    }

    public ByteSlice subSlice(final int start, final int end) {
        if (start < 0 || end > this.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid range: " + start + " to " + end + ", length=" + this.length);
        }
        if (start == end) {
            return EMPTY;
        }
        return new ByteSlice(this.buffer, this.offset + start, end - start);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return subSlice(start, end);
    }

    public int indexOf(final byte target, final int fromIndex) {
        final int start = Math.max(0, fromIndex);
        final int end = this.offset + this.length;
        for (int i = this.offset + start; i < end; i++) {
            if (this.buffer[i] == target) {
                return i - this.offset;
            }
        }
        return -1;
    }

    public int indexOf(final byte target) {
        return indexOf(target, 0);
    }

    public boolean startsWith(final byte[] prefix) {
        if (prefix == null || prefix.length > this.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (this.buffer[this.offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(final String prefix) {
        return startsWith(prefix.getBytes(StandardCharsets.UTF_8));
    }

    public boolean equalsUtf8(final String str) {
        if (str == null) {
            return false;
        }
        final byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        if (strBytes.length != this.length) {
            return false;
        }
        for (int i = 0; i < this.length; i++) {
            if (this.buffer[this.offset + i] != strBytes[i]) {
                return false;
            }
        }
        return true;
    }

    public long parseLong() {
        if (this.length == 0) {
            throw new NumberFormatException("Empty ByteSlice");
        }
        long result = 0;
        int i = 0;
        boolean negative = false;
        if (getByte(0) == '-') {
            negative = true;
            i++;
        } else if (getByte(0) == '+') {
            i++;
        }
        for (; i < this.length; i++) {
            final byte b = getByte(i);
            if (b < '0' || b > '9') {
                throw new NumberFormatException("Invalid digit: " + (char) b);
            }
            result = result * 10 + (b - '0');
        }
        return negative ? -result : result;
    }

    public int parseInt() {
        return (int) parseLong();
    }

    public byte[] toByteArray() {
        if (this.length == 0) {
            return EMPTY_BYTES;
        }
        final byte[] copy = new byte[this.length];
        System.arraycopy(this.buffer, this.offset, copy, 0, this.length);
        return copy;
    }

    public String asUtf8String() {
        if (this.length == 0) {
            return "";
        }
        return new String(this.buffer, this.offset, this.length, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ByteSlice that = (ByteSlice) o;
        if (this.length != that.length) return false;
        for (int i = 0; i < this.length; i++) {
            if (this.buffer[this.offset + i] != that.buffer[that.offset + i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (this.hash != 0 || this.length == 0) {
            return this.hash;
        }
        int h = 1;
        final int end = this.offset + this.length;
        for (int i = this.offset; i < end; i++) {
            h = 31 * h + this.buffer[i];
        }
        this.hash = h;
        return h;
    }

    @Override
    public String toString() {
        return asUtf8String();
    }
}
