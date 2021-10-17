package comp.proj.painter.ui.code;

final class ZigZag {
    private static final int[] ZIG_ZAG = {
            0,  1,  5,  6, 14, 15, 27, 28,
            2,  4,  7, 13, 16, 26, 29, 42,
            3,  8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
    };

    private ZigZag() {
    }

    public static void zigZagToBlock(final int[] zz, final int[] block) {
        for (int i = 0; i < ZIG_ZAG.length; i++) {
            block[i] = zz[ZIG_ZAG[i]];
        }
    }

    public static void blockToZigZag(final int[] block, final int[] zz) {
        for (int i = 0; i < ZIG_ZAG.length; i++) {
            zz[ZIG_ZAG[i]] = block[i];
        }
    }
}