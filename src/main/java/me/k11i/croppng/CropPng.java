package me.k11i.croppng;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static me.k11i.croppng.PngConsts.*;

/**
 * Provides a function to crop and resize (enlarge) at once.
 * <p>
 * <b>Note that an object of this class is not thread safe.</b>
 * </p>
 */
public final class CropPng {
    private static class Rectangle {
        final int x;
        final int y;
        final int width;
        final int height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int left() {
            return x;
        }

        int top() {
            return y;
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    private final Deflater deflater;
    private final Inflater inflater;

    /**
     * Constructs an object with default Deflate settings.
     */
    public CropPng() {
        this(new Deflater());
    }

    /**
     * Constructs an object with specified Deflate compression level.
     *
     * @param level compression level of Deflate algorithm.
     */
    public CropPng(int level) {
        this(new Deflater(level));
    }

    /**
     * Constructs an object that uses specified {@code Deflater} object to encode PNG image.
     *
     * @param deflater {@link Deflater} object that is already configured.
     */
    public CropPng(Deflater deflater) {
        this.deflater = deflater;
        this.inflater = new Inflater();
    }

    /**
     * Extracts the absolute rectangular region of pixels from {@code src} PNG image and encodes extracted pixels into PNG image.
     *
     * <p>
     * This method returns {@link ByteBuffer} object that contains bytes of PNG-encoded image.
     * You can use this object as follows:
     * </p>
     *
     * <pre>
     * ByteBuffer buf = new CropPng().crop(src, x, y, width, height, scaleFactor);
     *
     * // Get raw bytes
     * byte[] bytes = Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.limit())
     *
     * // Write to file
     * try (var out = new FileOutputStream("path/to/output.png")) {
     *     out.write(buf.array(), buf.arrayOffset(), buf.limit());
     * }
     * </pre>
     *
     * @param src         byte data of the source PNG image.
     * @param x           x coordinate of the top-left corner of the rectangle to be extracted.
     * @param y           y coordinate of the top-left corner of the rectangle to be extracted.
     * @param width       width of the rectangle to be extracted.
     * @param height      height of the rectangle to be extracted.
     * @param scaleFactor magnification factor (must be > 0).
     * @return {@link ByteBuffer} object of PNG-encoded image.
     */
    public ByteBuffer crop(byte[] src, int x, int y, int width, int height, int scaleFactor) {
        return crop0(
                src,
                new Rectangle(x, y, width, height),
                scaleFactor);
    }

    private ByteBuffer crop0(byte[] src, Rectangle rect, int scaleFactor) {
        Objects.requireNonNull(src, "src must be non-null");

        if (scaleFactor < 1) {
            throw new IllegalArgumentException("scaleFactor must be greater than or equal to 1 but " + scaleFactor);
        }

        PngReader reader = new PngReader(src, inflater);

        if (rect.right() > reader.width()) {
            throw new IllegalArgumentException(
                    String.format("'x + width' must be less than or equal to %d but %d", reader.width(), rect.right()));
        }
        if (rect.bottom() > reader.height()) {
            throw new IllegalArgumentException(
                    String.format("'y + height' must be less than or equal to %d but %d", reader.height(), rect.bottom()));
        }

        byte[] srcImageBytes = inflateImage(reader, rect);
        reverseFilter(srcImageBytes, rect, reader.width());

        byte[] croppedImageBytes = cropImage(srcImageBytes, rect, reader.width(), scaleFactor);

        return encode(reader, croppedImageBytes, rect, scaleFactor);
    }

    private byte[] inflateImage(PngReader reader, Rectangle rect) {
        int numBytesToInflate = (reader.width() + 1) * (rect.bottom() - 1) + rect.right() + 1;
        return reader.inflateImage(numBytesToInflate);
    }

    private void reverseFilter(byte[] srcImageBytes, Rectangle rect, int srcWidth) {
        int numScanlineBytes = srcWidth + 1;
        int pos = rect.top() * numScanlineBytes;

        while (pos > 0 && PngFilter.dependsOnPreviousScanline(srcImageBytes[pos])) {
            pos -= numScanlineBytes;
        }

        if (pos == 0) {
            PngFilter.reverseFirst(srcImageBytes, rect.right());
            pos += numScanlineBytes;
        }

        for (int limit = numScanlineBytes * rect.bottom(); pos < limit; pos += numScanlineBytes) {
            PngFilter.reverse(srcImageBytes, pos, rect.right(), numScanlineBytes);
        }
    }

    private static byte[] cropImage(byte[] srcImageBytes, Rectangle rect, int srcWidth, int scaleFactor) {
        int numDstRows = rect.height * scaleFactor;
        int numDstScanlineBytes = rect.width * scaleFactor + 1;
        byte[] result = new byte[numDstScanlineBytes * numDstRows];

        int numSrcScanlineBytes = srcWidth + 1;

        for (int i = 0; i < numDstRows; i++) {
            result[numDstScanlineBytes * i] = (byte) PngFilter.UP.ordinal();
        }

        for (int y = rect.top(); y < rect.bottom(); y++) {
            int srcPos = numSrcScanlineBytes * y + 1;
            int dstPos = numDstScanlineBytes * (y - rect.top()) * scaleFactor;

            result[dstPos++] = (byte) PngFilter.SUB.ordinal();

            byte cur;
            byte prev = 0;

            for (int x = rect.left(); x < rect.right(); x++) {
                cur = srcImageBytes[srcPos + x];
                result[dstPos] = (byte) (cur - prev);
                prev = cur;
                dstPos += scaleFactor;
            }
        }

        return result;
    }

    private ByteBuffer encode(PngReader reader, byte[] croppedImageBytes, Rectangle rect, int scaleFactor) {
        return new PngWriteBuffer(reader.src.limit(), deflater)
                .writeBytes(reader.src, 0, reader.firstIDATChunkPos())
                .writeIntAt(rect.width * scaleFactor, IHDR_WIDTH_POS)
                .writeIntAt(rect.height * scaleFactor, IHDR_HEIGHT_POS)
                .updateCRC(IHDR_CHUNK_POS + 4, IHDR_CHUNK_LEN + 4, false)
                .writeImage(croppedImageBytes)
                .writeBytes(reader.src, reader.afterIDATChunkPos(), reader.src.limit() - reader.afterIDATChunkPos())
                .toByteBuffer();
    }
}
