package me.k11i.croppng;

import ar.com.hjg.pngj.PngReaderByte;
import me.k11i.croppng.test.helper.CropParam;
import me.k11i.croppng.test.helper.JavaIoImageCrop;
import me.k11i.croppng.test.helper.TestImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CropPngTest {
    private static final byte[][] TEST_IMAGE_BYTES = TestImage.SOCIAL.loadImages();

    static Stream<CropParam> cropParameters() {
        return TestImage.SOCIAL.randomCropParameters(new SplittableRandom(1)).limit(20);
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
}
