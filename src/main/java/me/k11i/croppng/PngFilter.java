package me.k11i.croppng;

/**
 * PNG filter algorithms.
 *
 * @see <a href="https://www.w3.org/TR/PNG-Filters.html">https://www.w3.org/TR/PNG-Filters.html</a>
 */
enum PngFilter {
    /**
     * @see <a href="https://www.w3.org/TR/PNG-Filters.html#Filter-type-0-None">Filter type 0: None</a>
     */
    NONE {
        @Override
        void reverseFirst0(byte[] imageBytes, int len) {
            // do nothing
        }

        @Override
        void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
            // do nothing
        }
    },

    /**
     * @see <a href="https://www.w3.org/TR/PNG-Filters.html#Filter-type-1-Sub">Filter type 1: Sub</a>
     */
    SUB {
        @Override
        void reverseFirst0(byte[] imageBytes, int len) {
            reverse(imageBytes, 0, len, 0);
        }

        @Override
        void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
            for (int i = 1; i < len; i++) {
                imageBytes[1 + pos + i] += imageBytes[pos + i];
            }
        }
    },

    /**
     * @see <a href="https://www.w3.org/TR/PNG-Filters.html#Filter-type-2-Up">Filter type 2: Up</a>
     */
    UP {
        @Override
        void reverseFirst0(byte[] imageBytes, int len) {
            // do nothing
        }

        @Override
        void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
            for (int i = 0; i < len; i++) {
                imageBytes[1 + pos + i] += imageBytes[1 + pos + i - numScanlineBytes];
            }
        }
    },

    /**
     * @see <a href="https://www.w3.org/TR/PNG-Filters.html#Filter-type-3-Average">Filter type 3: Average</a>
     */
    AVERAGE {
        @Override
        void reverseFirst0(byte[] imageBytes, int len) {
            for (int i = 1; i < len; i++) {
                imageBytes[1 + i] += (imageBytes[i] & 0xff) >>> 1;
            }
        }

        @Override
        void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
            imageBytes[1 + pos] += (imageBytes[1 + pos - numScanlineBytes] & 0xff) >>> 1;

            for (int i = 1; i < len; i++) {
                int p = 1 + pos + i;
                imageBytes[p] += ((imageBytes[p - 1] & 0xff) + (imageBytes[p - numScanlineBytes] & 0xff)) >>> 1;
            }
        }
    },

    /**
     * @see <a href="https://www.w3.org/TR/PNG-Filters.html#Filter-type-4-Paeth">Filter type 4: Paeth</a>
     */
    PAETH {
        @Override
        void reverseFirst0(byte[] imageBytes, int len) {
            for (int i = 1; i < len; i++) {
                imageBytes[1 + i] += imageBytes[i];
            }
        }

        @Override
        void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
            imageBytes[1 + pos] += imageBytes[1 + pos - numScanlineBytes];

            for (int i = 1; i < len; i++) {
                int p = 1 + pos + i;
                imageBytes[p] += predict(
                        imageBytes[p - 1] & 0xff,
                        imageBytes[p - numScanlineBytes] & 0xff,
                        imageBytes[p - numScanlineBytes - 1] & 0xff);
            }
        }

        int predict(int a, int b, int c) {
            int p = a + b - c;
            int pa = Math.abs(p - a);
            int pb = Math.abs(p - b);
            int pc = Math.abs(p - c);
            if (pa <= pb && pa <= pc) {
                return a;
            }
            if (pb <= pc) {
                return b;
            }
            return c;
        }
    };

    private static final PngFilter[] FILTERS;
    private static final int BIT_FLAGS_DOES_NOT_DEPEND_ON_PREV_SCANLINE;

    static {
        FILTERS = PngFilter.values();
        BIT_FLAGS_DOES_NOT_DEPEND_ON_PREV_SCANLINE = (1 << NONE.ordinal()) | (1 << SUB.ordinal());
    }

    abstract void reverseFirst0(byte[] imageBytes, int len);

    abstract void reverse0(byte[] imageBytes, int pos, int len, int numScanlineBytes);

    /**
     * Returns {@code true} if given filter type depends on the previous scanline; {@code false} otherwise.
     *
     * @param filterType filter type value (0-4).
     */
    static boolean dependsOnPreviousScanline(int filterType) {
        return ((BIT_FLAGS_DOES_NOT_DEPEND_ON_PREV_SCANLINE >> filterType) & 1) != 1;
    }

    /**
     * Reverses filtered bytes of the first scanline.
     *
     * @param imageBytes filtered image byes.
     * @param len        number of bytes to reverse filter.
     */
    static void reverseFirst(byte[] imageBytes, int len) {
        FILTERS[imageBytes[0]].reverseFirst0(imageBytes, len);
    }

    /**
     * Reverses filtered bytes of the specified scanline.
     *
     * @param imageBytes       filtered image bytes.
     * @param pos              position that points filter value of the scanline.
     * @param len              number of bytes to reverse filter.
     * @param numScanlineBytes number of scanline bytes (includes filter type value).
     */
    static void reverse(byte[] imageBytes, int pos, int len, int numScanlineBytes) {
        FILTERS[imageBytes[pos]].reverse0(imageBytes, pos, len, numScanlineBytes);
    }
}
