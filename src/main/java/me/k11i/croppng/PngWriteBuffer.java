package me.k11i.croppng;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class PngWriteBuffer {
    private static class ArrayOutputStream extends OutputStream {
        private byte[] buffer;
        private int pos;

        ArrayOutputStream(int estimatedSize) {
            buffer = new byte[estimatedSize];
        }

        @Override
        public void write(int b) {
            expandBufferIfNeeded(1);
            buffer[pos++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            expandBufferIfNeeded(len);
            System.arraycopy(b, off, buffer, pos, len);
            pos += len;
        }

        void setBigEndianIntAt(int val, int setPos) {
            buffer[setPos] = (byte) (val >>> 24);
            buffer[setPos + 1] = (byte) (val >>> 16);
            buffer[setPos + 2] = (byte) (val >>> 8);
            buffer[setPos + 3] = (byte) (val & 0xff);
        }

        void writeBigEndianInt(int val) {
            expandBufferIfNeeded(4);
            setBigEndianIntAt(val, pos);
            pos += 4;
        }

        private void expandBufferIfNeeded(int numBytesToWrite) {
            int required = pos + numBytesToWrite;
            if (required < buffer.length) {
                return;
            }

            int newBufferSize = buffer.length <= Integer.MAX_VALUE / 2
                    ? (buffer.length << 2)
                    : Integer.MAX_VALUE;
            if (newBufferSize < required) {
                newBufferSize = required;
            }

            buffer = Arrays.copyOf(buffer, newBufferSize);
        }
    }

    private static final byte[] EMPTY_LENGTH_IDAT = {0, 0, 0, 0, 0x49, 0x44, 0x41, 0x54};
    private final ArrayOutputStream out;
    private final Deflater deflater;
    private final CRC32 crc;

    PngWriteBuffer(int estimatedSize, Deflater deflater) {
        this.out = new ArrayOutputStream(estimatedSize);
        this.deflater = deflater;
        this.crc = new CRC32();
    }

    PngWriteBuffer writeBytes(ByteBuffer src, int pos, int len) {
        out.write(src.array(), pos, len);
        return this;
    }

    PngWriteBuffer writeIntAt(int val, int pos) {
        out.setBigEndianIntAt(val, pos);
        return this;
    }

    PngWriteBuffer updateCRC(int pos, int len, boolean write) {
        crc.reset();
        crc.update(out.buffer, pos, len);
        if (write) {
            out.writeBigEndianInt((int) crc.getValue());
        } else {
            out.setBigEndianIntAt((int) crc.getValue(), pos + len);
        }
        return this;
    }

    PngWriteBuffer writeImage(byte[] imageBytes) {
        int idatLengthPos = out.pos;

        out.write(EMPTY_LENGTH_IDAT, 0, EMPTY_LENGTH_IDAT.length);
        deflater.reset();

        try (DeflaterOutputStream deflateOut = new DeflaterOutputStream(out, deflater, 8192)) {
            deflateOut.write(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int numDeflatedBytes = out.pos - (idatLengthPos + 8);
        out.setBigEndianIntAt(numDeflatedBytes, idatLengthPos);

        updateCRC(idatLengthPos + 4, numDeflatedBytes + 4, true);

        return this;
    }

    ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(out.buffer, 0, out.pos);
    }
}
