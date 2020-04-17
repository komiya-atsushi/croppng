package me.k11i.croppng.test.helper;

public class CropParam {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final int scaleFactor;

    public CropParam(int x, int y, int width, int height, int scaleFactor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scaleFactor = scaleFactor;
    }

    @Override
    public String toString() {
        return "CropParam{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", scaleFactor=" + scaleFactor +
                '}';
    }
}
