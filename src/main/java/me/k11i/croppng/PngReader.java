package me.k11i.croppng;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static me.k11i.croppng.PngConsts.*;

class PngReader {
    final ByteBuffer src;
    private final Inflater inflater;
    private final int width;
    private final int height;
    private final int firstIDATChunkPos;
    private final int afterIDATChunkPos;

    PngReader(byte[] src) {
        this(src, new Inflater());
    }

    PngReader(byte[] src, Inflater inflater) {
        this.src = ByteBuffer.wrap(src).order(ByteOrder.BIG_ENDIAN);
        this.inflater = inflater;

        verify();

        this.width = this.src.getInt(16);
        this.height = this.src.getInt(20);
        this.firstIDATChunkPos = findChunk(AFTER_IHDR_CHUNK_POS, CHUNK_TYPE_IDAT, true);
        this.afterIDATChunkPos = findChunk(firstIDATChunkPos, CHUNK_TYPE_IDAT, false);
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int firstIDATChunkPos() {
        return firstIDATChunkPos;
    }

    int afterIDATChunkPos() {
        return afterIDATChunkPos;
    }

    byte[] inflateImage(int numBytesToInflate) {
        byte[] result = new byte[numBytesToInflate];

        int chunkPos = firstIDATChunkPos;
        inflater.reset();

        int numInflatedBytes = 0;

        do {
            if (chunkPos >= afterIDATChunkPos) {
                throw new IllegalArgumentException("Run out of deflated image bytes");
            }

            int len = chunkLength(chunkPos);
            inflater.setInput(src.array(), chunkPos + 8, len);

            try {
                numInflatedBytes += inflater.inflate(result, numInflatedBytes, result.length - numInflatedBytes);
            } catch (DataFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Invalid deflated bytes: chunk position = %d, chunk length = %d, # of bytes inflated = %d", chunkPos, len, numInflatedBytes),
                        e);
            }

            chunkPos += 8 + len + 4;

        } while (numInflatedBytes < numBytesToInflate);

        return result;
    }

    private void verify() {
        if (src.getLong(0) != PNG_SIGNATURE) {
            throw new IllegalArgumentException("Bad signature");
        }
        if (src.getInt(8) != IHDR_CHUNK_LEN) {
            throw new IllegalArgumentException("Bad IHDR length");
        }
        if (src.getInt(12) != CHUNK_TYPE_IHDR) {
            throw new IllegalArgumentException("The IHDR chunk must appear FIRST, but does not appear.");
        }

        if (src.get(26) != 0) {
            throw new IllegalArgumentException("Unsupported compression method: " + src.get(26));
        }
        if (src.get(27) != 0) {
            throw new IllegalArgumentException("Unsupported filter method: " + src.get(27));
        }
        if (src.get(28) != 0) {
            throw new IllegalArgumentException("Any interlaced methods are not supported: " + src.get(28));
        }

        if (src.get(24) != 8) {
            // todo support other bit depth
            throw new IllegalArgumentException("Unsupported bit depth: " + src.get(24));
        }
        if (src.get(25) != 3) {
            // todo support other color type
            throw new IllegalArgumentException("Unsupported color type: " + src.get(25));
        }
    }

    private int findChunk(int startPos, int chunkType, boolean findMatches) {
        int pos = startPos;

        while (pos + 8 < src.limit()) {
            if ((src.getInt(pos + 4) == chunkType) == findMatches) {
                return pos;
            }
            pos += 8 + src.getInt(pos) + 4;
        }

        throw new IllegalArgumentException("Chunk not found");
    }

    private int chunkLength(int pos) {
        return src.getInt(pos);
    }
}
