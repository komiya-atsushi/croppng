package me.k11i.croppng.test.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum TestImage {
    GRADIENT(128, 68),

    SOCIAL(500, 501),

    TILE(256, 256) {
        @Override
        public Stream<CropParam> randomCropParameters(SplittableRandom r) {
            return Stream.generate(() -> null)
                    .map(ignore -> {
                        int z = r.nextInt(8) + 1;
                        int w = 256 >>> z;
                        int scaleFactor = 1 << z;
                        return new CropParam(
                                r.nextInt(scaleFactor) * w,
                                r.nextInt(scaleFactor) * w,
                                w,
                                w,
                                scaleFactor);
                    });
        }
    };

    public final int width;
    public final int height;

    TestImage(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public byte[][] loadImages() {
        var filenamePrefix = name().toLowerCase(Locale.ENGLISH);
        return IntStream.rangeClosed(0, 4)
                .mapToObj(i -> String.format("%s-f%d.png", filenamePrefix, i))
                .map(TestImage::load)
                .toArray(byte[][]::new);
    }

    public Stream<CropParam> randomCropParameters(SplittableRandom r) {
        return Stream.generate(() -> null)
                .map(ignore -> new CropParam(
                        r.nextInt(width / 2),
                        r.nextInt(height / 2),
                        r.nextInt(width / 4, width / 2),
                        r.nextInt(height / 4, height / 2),
                        r.nextInt(1, 5)));
    }

    private static byte[] load(String name) {
        var resourceName = "images/" + name;
        try (var in = ClassLoader.getSystemResourceAsStream(resourceName)) {
            if (in == null) {
                throw new RuntimeException("Test image does not found: " + resourceName);
            }

            var out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
