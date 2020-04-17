package me.k11i.croppng.test.helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JavaIoImageCrop {
    public static byte[] crop(byte[] src, int x, int y, int srcWidth, int srcHeight, int scaleFactor) {
        int dstWidth = srcWidth * scaleFactor;
        int dstHeight = srcHeight * scaleFactor;

        BufferedImage srcImage;
        try {
            srcImage = ImageIO.read(new ByteArrayInputStream(src));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read PNG image", e);
        }

        ColorModel colorModel = srcImage.getColorModel();
        BufferedImage dstImage = new BufferedImage(
                dstWidth,
                dstHeight,
                BufferedImage.TYPE_BYTE_INDEXED,
                (IndexColorModel) colorModel);

        Graphics2D g = dstImage.createGraphics();
        g.drawImage(
                srcImage,
                0, 0, dstWidth, dstHeight,
                x, y, x + srcWidth, y + srcHeight,
                null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(dstImage, "png", out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write PNG image", e);
        }

        return out.toByteArray();
    }
}
