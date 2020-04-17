package me.k11i.croppng;

import me.k11i.croppng.test.helper.TestImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PngFilterTest {
    private static final byte[][] TEST_IMAGE_BYTES = TestImage.GRADIENT.loadImages();
    private static final byte[] expectedReverseFilteredBytes = {65, 43, 2, 99, 15, 11, 92, 62, 72, 27, 106, 31, 39, 73, 74, 69, 85, 87, 3, 52, 37, 21, 19, 60, 88, 119, 75, 14, 83, 36, 29, 121, -123, -118, -100, -108, -105, -70, -84, -101, -106, -98, -104, -85, -107, -89, -69, -111, -79, -109, -72, -82, -119, -128, 124, 113, 25, 122, -122, -116, -95, -91, -110, -94, -96, -81, -113, -124, 127, 20, 107, 82, 56, 68, 42, 110, 54, 78, 53, 120, -126, -117, -75, -83, -86, -90, -87, -78, -112, -88, -80, -73, -93, -74, -97, -77, -99, -76, -115, -127, 125, 64, 98, 46, 103, 45, 4, 109, 61, 123, -121, -114, -102, -103, -71, -92, -120, -125, 126, 7, 77, 101, 1, 76, 94, 38, 24, 16};

    @ParameterizedTest
    @EnumSource(value = PngFilter.class, names = {"NONE", "SUB"}, mode = EnumSource.Mode.EXCLUDE)
    void doesNotDependOnPreviousScanline_shouldReturnTrue(PngFilter sut) {
        assertThat(PngFilter.dependsOnPreviousScanline(sut.ordinal())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PngFilter.class, names = {"NONE", "SUB"}, mode = EnumSource.Mode.INCLUDE)
    void doesNotDependOnPreviousScanline_shouldReturnFalse(PngFilter sut) {
        assertThat(PngFilter.dependsOnPreviousScanline(sut.ordinal())).isFalse();
    }


    @ParameterizedTest
    @EnumSource(value = PngFilter.class)
    void testReverse(PngFilter sut) {
        int width = expectedReverseFilteredBytes.length;
        int numScanlineBytes = width + 1;

        var reader = new PngReader(TEST_IMAGE_BYTES[sut.ordinal()]);
        var data = reader.inflateImage(numScanlineBytes * 2);

        sut.reverseFirst0(data, width);
        sut.reverse0(data, numScanlineBytes, width, numScanlineBytes);

        var line1 = Arrays.copyOfRange(data, 1, 1 + width);
        assertThat(line1).containsExactly(expectedReverseFilteredBytes);

        var line2 = Arrays.copyOfRange(data, numScanlineBytes + 1, numScanlineBytes + 1 + width);
        assertThat(line2).containsExactly(expectedReverseFilteredBytes);
    }
}
