package me.k11i.croppng;

public interface PngConsts {
    long PNG_SIGNATURE = 0x8950_4e47_0d0a_1a0aL;

    int CHUNK_TYPE_IHDR = 0x4948_4452;
    int CHUNK_TYPE_IDAT = 0x4944_4154;

    int IHDR_CHUNK_POS = 8;
    int IHDR_CHUNK_LEN = 13;
    int IHDR_WIDTH_POS = 16;
    int IHDR_HEIGHT_POS = 20;

    int AFTER_IHDR_CHUNK_POS = IHDR_CHUNK_POS + 8 + IHDR_CHUNK_LEN + 4;
}
