package me.k11i.croppng;

import ar.com.hjg.pngj.PngReaderByte;
import me.k11i.croppng.test.helper.CropParam;
import me.k11i.croppng.test.helper.JavaIoImageCrop;
import me.k11i.croppng.test.helper.TestImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CropPngTest {
    private static final TestImage TEST_IMAGE = TestImage.SOCIAL;
    private static final byte[][] TEST_IMAGE_BYTES = TEST_IMAGE.loadImages();

    static Stream<CropParam> cropParameters() {
        return TEST_IMAGE.randomCropParameters(new SplittableRandom(1)).limit(20);
    }

    @ParameterizedTest
    @MethodSource("cropParameters")
    void testRandomCropping(CropParam param) {
        CropPng sut = new CropPng();

        for (int i = 0; i < TEST_IMAGE_BYTES.length; i++) {
            var expected = JavaIoImageCrop.crop(TEST_IMAGE_BYTES[i], param.x, param.y, param.width, param.height, param.scaleFactor);
            var result = sut.crop(TEST_IMAGE_BYTES[i], param.x, param.y, param.width, param.height, param.scaleFactor);
            var resultBytes = Arrays.copyOfRange(result.array(), result.arrayOffset(), result.limit());

            assertThat(decodePng(resultBytes))
                    .describedAs("Using test data PNG_TEST_IMAGES[%d]", i)
                    .containsExactly(decodePng(expected));
        }
    }

    @Test
    void testCompressionLevel() {
        var l1 = new CropPng(1).crop(TEST_IMAGE_BYTES[0], 0, 0, TEST_IMAGE.width / 8, TEST_IMAGE.height / 8, 2);
        var l6 = new CropPng(6).crop(TEST_IMAGE_BYTES[0], 0, 0, TEST_IMAGE.width / 8, TEST_IMAGE.height / 8, 2);
        var l9 = new CropPng(9).crop(TEST_IMAGE_BYTES[0], 0, 0, TEST_IMAGE.width / 8, TEST_IMAGE.height / 8, 2);

        assertThat(l1.limit()).isGreaterThanOrEqualTo(l6.limit());
        assertThat(l6.limit()).isGreaterThanOrEqualTo(l9.limit());
    }

    @Test
    void testConcurrency() {
        var results = Stream.generate(() -> CropPng.compressionLevel(1).crop(TEST_IMAGE_BYTES[0], 0, 0, TEST_IMAGE.width / 8, TEST_IMAGE.height / 8, 2))
                .parallel()
                .limit(100)
                .map(wrap(bb -> {
                    var md = MessageDigest.getInstance("MD5");
                    md.update(bb);
                    return Arrays.toString(md.digest());
                }))
                .distinct()
                .collect(Collectors.toList());

        assertThat(results).hasSize(1);
    }

    private static byte[] decodePng(byte[] src) {
        var reader = new PngReaderByte(new ByteArrayInputStream(src));
        var result = new byte[reader.imgInfo.cols * reader.imgInfo.rows];

        for (var i = 0; i < reader.imgInfo.rows; i++) {
            var rowByte = reader.readRowByte();
            var scanline = rowByte.getScanline();
            System.arraycopy(scanline, 0, result, reader.imgInfo.cols * i, reader.imgInfo.cols);
        }

        return result;
    }

    @FunctionalInterface
    interface FunctionWithException<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    private <T, R, E extends Exception> Function<T, R> wrap(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return fe.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
