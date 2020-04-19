package me.k11i.croppng;

import me.k11i.croppng.test.helper.CropParam;
import me.k11i.croppng.test.helper.JavaAwtImageCrop;
import me.k11i.croppng.test.helper.TestImage;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.SplittableRandom;

@State(Scope.Thread)
public class CropPngBenchmark {
    @State(Scope.Thread)
    public static class BenchmarkContext {
        private boolean firstPass = true;
        private byte[][] testImageBytes;
        private SplittableRandom random;
        private Iterator<CropParam> cropParamItr;

        private byte[] src;
        private int x;
        private int y;
        private int width;
        private int height;
        private int scaleFactor;

        @Setup(Level.Invocation)
        public void setUp() {
            if (firstPass) {
                TestImage testImage = TestImage.TILE;
                testImageBytes = testImage.loadImages();
                random = new SplittableRandom(1);
                cropParamItr = testImage.randomCropParameters(random).iterator();
                firstPass = false;
            }

            src = testImageBytes[random.nextInt(5)];
            CropParam cropParam = cropParamItr.next();
            x = cropParam.x;
            y = cropParam.y;
            width = cropParam.width;
            height = cropParam.height;
            scaleFactor = cropParam.scaleFactor;
        }
    }

    @Benchmark
    public byte[] cropPng6(BenchmarkContext ctx) {
        var result = new CropPng().crop(ctx.src, ctx.x, ctx.y, ctx.width, ctx.height, ctx.scaleFactor);
        return Arrays.copyOfRange(result.array(), result.arrayOffset(), result.limit());
    }

    @Benchmark
    public byte[] cropPng6ThreadLocalSoftRef(BenchmarkContext ctx) {
        var result = CropPng.defaultLevel().crop(ctx.src, ctx.x, ctx.y, ctx.width, ctx.height, ctx.scaleFactor);
        return Arrays.copyOfRange(result.array(), result.arrayOffset(), result.limit());
    }

    @Benchmark
    public byte[] cropPng1(BenchmarkContext ctx) {
        var result = new CropPng(1).crop(ctx.src, ctx.x, ctx.y, ctx.width, ctx.height, ctx.scaleFactor);
        return Arrays.copyOfRange(result.array(), result.arrayOffset(), result.limit());
    }

    @Benchmark
    public  byte[] cropPng1ThreadLocalSoftRef(BenchmarkContext ctx) {
        var result = CropPng.compressionLevel(1).crop(ctx.src, ctx.x, ctx.y, ctx.width, ctx.height, ctx.scaleFactor);
        return Arrays.copyOfRange(result.array(), result.arrayOffset(), result.limit());
    }

    @Benchmark
    public byte[] javaAwtImage(BenchmarkContext ctx) {
        return JavaAwtImageCrop.crop(ctx.src, ctx.x, ctx.y, ctx.width, ctx.height, ctx.scaleFactor);
    }
}
