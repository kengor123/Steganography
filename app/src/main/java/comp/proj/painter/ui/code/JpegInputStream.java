package comp.proj.painter.ui.code;

import java.util.Arrays;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;

//class JpegInputStream {
//    // Figure F.18, F.2.2.5, page 111 of ITU-T T.81
//    private final int[] interval;
//    // next position in the array to read
//    private int nextPos;
//    private int cnt;
//    private int b;
//
//    JpegInputStream(final int[] interval) {
//        this.interval = Arrays.copyOf(interval, interval.length);
//        this.nextPos = 0;
//    }
//
//    /**
//     * Returns {@code true} as long there are unread fields available, else {@code false}
//     * @return
//     */
//    public boolean hasNext() {
//        return nextPos < this.interval.length;
//    }
//
//    public int nextBit() throws ImageReadException {
//        if (cnt == 0) {
//            b = this.read();
//            if (b < 0) {
//                throw new ImageReadException("Premature End of File");
//            }
//            cnt = 8;
//            if (b == 0xff) {
//                final int b2 = this.read();
//                if (b2 < 0) {
//                    throw new ImageReadException("Premature End of File");
//                }
//                if (b2 != 0) {
//                    if (b2 == (0xff & JpegConstants.DNL_MARKER)) {
//                        throw new ImageReadException("DNL not yet supported");
//                    }
//                    throw new ImageReadException("Invalid marker found "
//                            + "in entropy data: 0xFF " + Integer.toHexString(b2));
//                }
//            }
//        }
//        final int bit = (b >> 7) & 0x1;
//        cnt--;
//        b <<= 1;
//        return bit;
//    }
//
//    /**
//     * Returns the value from current field (as {@code InputStream.read()} would do)
//     * and set the position of the pointer to the next field to read.
//     * @return
//     * @throws IllegalStateException if the stream hasn't any other value.
//     */
//    int read() {
//        if (!this.hasNext()) {
//            throw new IllegalStateException("This stream hasn't any other value, all values were already read.");
//        }
//        final int value = this.interval[nextPos];
//        this.nextPos++;
//        return value;
//    }
//}