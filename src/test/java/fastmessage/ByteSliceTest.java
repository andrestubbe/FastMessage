package fastmessage;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ByteSliceTest {

    @Test
    public void testByteSliceCreationAndSlicing() {
        final byte[] bytes = "Hello, FastMessage Zero-Copy World!".getBytes(StandardCharsets.UTF_8);
        final ByteSlice slice = ByteSlice.wrap(bytes);

        assertEquals(bytes.length, slice.length());
        assertFalse(slice.isEmpty());
        assertEquals("Hello, FastMessage Zero-Copy World!", slice.asUtf8String());

        final ByteSlice sub = slice.subSlice(7, 20);
        assertEquals("FastMessage", sub.asUtf8String());
        assertEquals(13, sub.length());
        assertTrue(sub.equalsUtf8("FastMessage"));
        assertFalse(sub.equalsUtf8("Other"));
    }

    @Test
    public void testByteBufferWrapping() {
        final ByteBuffer buffer = ByteBuffer.wrap("Direct ByteBuffer Test".getBytes(StandardCharsets.UTF_8));
        final ByteSlice slice = ByteSlice.fromByteBuffer(buffer);

        assertEquals("Direct ByteBuffer Test", slice.asUtf8String());
        assertEquals(22, slice.length());
    }

    @Test
    public void testNumericParsing() {
        final ByteSlice num1 = ByteSlice.fromString("123456789");
        assertEquals(123456789L, num1.parseLong());
        assertEquals(123456789, num1.parseInt());

        final ByteSlice negative = ByteSlice.fromString("-98765");
        assertEquals(-98765L, negative.parseLong());
        assertEquals(-98765, negative.parseInt());
    }

    @Test
    public void testIndexOfAndStartsWith() {
        final ByteSlice slice = ByteSlice.fromString("telegram:999888");
        assertTrue(slice.startsWith("telegram"));
        assertFalse(slice.startsWith("whatsapp"));
        assertEquals(8, slice.indexOf((byte) '9'));
    }

    @Test
    public void testEmptyAndEquals() {
        final ByteSlice empty1 = ByteSlice.EMPTY;
        final ByteSlice empty2 = ByteSlice.fromString("");
        assertEquals(empty1, empty2);
        assertEquals(0, empty1.length());
        assertTrue(empty1.isEmpty());
    }
}
