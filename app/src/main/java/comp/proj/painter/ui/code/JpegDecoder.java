package comp.proj.painter.ui.code;
/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Base64;
import android.util.Log;

//import java.awt.image.BufferedImage;
//import java.awt.image.ColorModel;
//import java.awt.image.DataBuffer;
//import java.awt.image.DirectColorModel;
//import java.awt.image.Raster;
//import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.color.ColorConversions;
import org.apache.commons.imaging.common.BinaryFileParser;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;
import org.apache.commons.imaging.formats.jpeg.JpegUtils;
import org.apache.commons.imaging.formats.jpeg.segments.DhtSegment;
import org.apache.commons.imaging.formats.jpeg.segments.DqtSegment;
import org.apache.commons.imaging.formats.jpeg.segments.SofnSegment;
import org.apache.commons.imaging.formats.jpeg.segments.SosSegment;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static org.apache.commons.imaging.common.BinaryFunctions.read2Bytes;
import static org.apache.commons.imaging.common.BinaryFunctions.readBytes;

//import comp.proj.painter.ui.code.ByteSource;

public class JpegDecoder extends BinaryFileParser implements JpegUtils.Visitor {
    /*
     * JPEG is an advanced image format that takes significant computation to
     * decode. Keep decoding fast: - Don't allocate memory inside loops,
     * allocate it once and reuse. - Minimize calculations per pixel and per
     * block (using lookup tables for YCbCr->RGB conversion doubled
     * performance). - Math.round() is slow, use (int)(x+0.5f) instead for
     * positive numbers.
     */

    private final DqtSegment.QuantizationTable[] quantizationTables = new DqtSegment.QuantizationTable[4];
    private final DhtSegment.HuffmanTable[] huffmanDCTables = new DhtSegment.HuffmanTable[4];
    private final DhtSegment.HuffmanTable[] huffmanACTables = new DhtSegment.HuffmanTable[4];
    private SofnSegment sofnSegment;
    private SosSegment sosSegment;
    private final float[][] scaledQuantizationTables = new float[4][];
    private Bitmap image;
    private ImageReadException imageReadException;
    private IOException ioException;
    private final int[] zz = new int[64];
    private final int[] blockInt = new int[64];
    private final float[] block = new float[64];


    Extract ex = new Extract();


    @Override
    public boolean beginSOS() {
        return true;
    }

    @Override
    public void visitSOS(final int marker, final byte[] markerBytes, final byte[] imageData) {
        final ByteArrayInputStream is = new ByteArrayInputStream(imageData);
        try {
            // read the scan header
            final int segmentLength = read2Bytes("segmentLength", is,"Not a Valid JPEG File", getByteOrder());
            final byte[] sosSegmentBytes = readBytes("SosSegment", is, segmentLength - 2, "Not a Valid JPEG File");
            sosSegment = new SosSegment(marker, sosSegmentBytes);
            // read the payload of the scan, this is the remainder of image data after the header
            // the payload contains the entropy-encoded segments (or ECS) divided by RST markers
            // or only one ECS if the entropy-encoded data is not divided by RST markers
            // length of payload = length of image data - length of data already read
            final int[] scanPayload = new int[imageData.length - segmentLength];
            int payloadReadCount = 0;
            while (payloadReadCount < scanPayload.length) {
                scanPayload[payloadReadCount] = is.read();
                payloadReadCount++;
            }

            int hMax = 0;
            int vMax = 0;
            for (int i = 0; i < sofnSegment.numberOfComponents; i++) {
                hMax = Math.max(hMax,
                        sofnSegment.getComponents(i).horizontalSamplingFactor);
                vMax = Math.max(vMax,
                        sofnSegment.getComponents(i).verticalSamplingFactor);
            }
            final int hSize = 8 * hMax;
            final int vSize = 8 * vMax;

            final int xMCUs = (sofnSegment.width + hSize - 1) / hSize;
            final int yMCUs = (sofnSegment.height + vSize - 1) / vSize;
            final Block[] mcu = allocateMCUMemory();
            final Block[] scaledMCU = new Block[mcu.length];
            for (int i = 0; i < scaledMCU.length; i++) {
                scaledMCU[i] = new Block(hSize, vSize);
            }
            final int[] preds = new int[sofnSegment.numberOfComponents];
//            ColorModel colorModel;
//            WritableRaster raster;
//            if (sofnSegment.numberOfComponents == 4) {
//                colorModel = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
//                int bandMasks[] = new int[] { 0x00ff0000, 0x0000ff00, 0x000000ff };
//                raster = Raster.createPackedRaster(DataBuffer.TYPE_INT, sofnSegment.width, sofnSegment.height, bandMasks, null);
//            } else if (sofnSegment.numberOfComponents == 3) {
//                colorModel = new DirectColorModel(24, 0x00ff0000, 0x0000ff00,
//                        0x000000ff);
//                raster = Raster.createPackedRaster(DataBuffer.TYPE_INT,
//                        sofnSegment.width, sofnSegment.height, new int[] {
//                                0x00ff0000, 0x0000ff00, 0x000000ff }, null);
//            } else if (sofnSegment.numberOfComponents == 1) {
//                colorModel = new DirectColorModel(24, 0x00ff0000, 0x0000ff00,
//                        0x000000ff);
//                raster = Raster.createPackedRaster(DataBuffer.TYPE_INT,
//                        sofnSegment.width, sofnSegment.height, new int[] {
//                                0x00ff0000, 0x0000ff00, 0x000000ff }, null);
//                // FIXME: why do images come out too bright with CS_GRAY?
//                 colorModel = new ComponentColorModel(
//                 ColorSpace.getInstance(ColorSpace.CS_GRAY), false, true,
//                 Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
//                 raster = colorModel.createCompatibleWritableRaster(
//                 sofnSegment.width, sofnSegment.height);
//            } else {
//                throw new ImageReadException(sofnSegment.numberOfComponents
//                        + " components are invalid or unsupported");
//            }
            //final DataBuffer dataBuffer = raster.getDataBuffer();

            final JpegInputStream[] bitInputStreams = splitByRstMarkers(scanPayload);
            int bitInputStreamCount = 0;
            JpegInputStream bitInputStream = bitInputStreams[0];

            for (int y1 = 0; y1 < vSize * yMCUs; y1 += vSize) {
                for (int x1 = 0; x1 < hSize * xMCUs; x1 += hSize) {
                    // Provide the next interval if an interval is read until it's end
                    // as long there are unread intervals available
                    if (!bitInputStream.hasNext()) {
                        bitInputStreamCount++;
                        if (bitInputStreamCount < bitInputStreams.length) {
                            bitInputStream = bitInputStreams[bitInputStreamCount];
                        }
                    }

                    readMCU(bitInputStream, preds, mcu);
                    rescaleMCU(mcu, hSize, vSize, scaledMCU);
                    int srcRowOffset = 0;
                    int dstRowOffset = y1 * sofnSegment.width + x1;
                    for (int y2 = 0; y2 < vSize && y1 + y2 < sofnSegment.height; y2++) {
                        for (int x2 = 0; x2 < hSize
                                && x1 + x2 < sofnSegment.width; x2++) {
                            if (scaledMCU.length == 4) {
                                final int C = scaledMCU[0].samples[srcRowOffset + x2];
                                final int M = scaledMCU[1].samples[srcRowOffset + x2];
                                final int Y = scaledMCU[2].samples[srcRowOffset + x2];
                                final int K = scaledMCU[3].samples[srcRowOffset + x2];
                                final int rgb = ColorConversions.convertCMYKtoRGB(C, M, Y, K);
                                //dataBuffer.setElem(dstRowOffset + x2, rgb);
                            } else if (scaledMCU.length == 3) {
                                final int Y = scaledMCU[0].samples[srcRowOffset + x2];
                                final int Cb = scaledMCU[1].samples[srcRowOffset + x2];
                                final int Cr = scaledMCU[2].samples[srcRowOffset + x2];
                                final int rgb = YCbCrConverter.convertYCbCrToRGB(Y,
                                        Cb, Cr);
                                //dataBuffer.setElem(dstRowOffset + x2, rgb);
                            } else if (mcu.length == 1) {
                                final int Y = scaledMCU[0].samples[srcRowOffset + x2];
                                //dataBuffer.setElem(dstRowOffset + x2, (Y << 16) | (Y << 8) | Y);
                            } else {
                                throw new ImageReadException(
                                        "Unsupported JPEG with " + mcu.length
                                                + " components");
                            }
                        }
                        srcRowOffset += hSize;
                        dstRowOffset += sofnSegment.width;
                    }
                }
            }
//            image = new BufferedImage(colorModel, raster,
//                    colorModel.isAlphaPremultiplied(), new Properties());


            // byte[] remainder = super.getStreamBytes(is);
            // for (int i = 0; i < remainder.length; i++)
            // {
            // System.out.println("" + i + " = " +
            // Integer.toHexString(remainder[i]));
            // }
        } catch (final ImageReadException imageReadEx) {
            imageReadException = imageReadEx;
        } catch (final IOException ioEx) {
            ioException = ioEx;
        } catch (final RuntimeException ex) {
            // Corrupt images can throw NPE and IOOBE
            imageReadException = new ImageReadException("Error parsing JPEG",ex);
        }
    }

    @Override
    public boolean visitSegment(final int marker, final byte[] markerBytes,
                                final int segmentLength, final byte[] segmentLengthBytes, final byte[] segmentData)
            throws ImageReadException, IOException {
        final int[] sofnSegments = {
                JpegConstants.SOF0_MARKER,
                JpegConstants.SOF1_MARKER,
                JpegConstants.SOF2_MARKER,
                JpegConstants.SOF3_MARKER,
                JpegConstants.SOF5_MARKER,
                JpegConstants.SOF6_MARKER,
                JpegConstants.SOF7_MARKER,
                JpegConstants.SOF9_MARKER,
                JpegConstants.SOF10_MARKER,
                JpegConstants.SOF11_MARKER,
                JpegConstants.SOF13_MARKER,
                JpegConstants.SOF14_MARKER,
                JpegConstants.SOF15_MARKER,
        };

        if (Arrays.binarySearch(sofnSegments, marker) >= 0) {
            if (marker != JpegConstants.SOF0_MARKER) {
                throw new ImageReadException("Only sequential, baseline JPEGs "
                        + "are supported at the moment");
            }
            sofnSegment = new SofnSegment(marker, segmentData);
        } else if (marker == JpegConstants.DQT_MARKER) {
            final DqtSegment dqtSegment = new DqtSegment(marker, segmentData);
            for (int i = 0; i < dqtSegment.quantizationTables.size(); i++) {
                final DqtSegment.QuantizationTable table = dqtSegment.quantizationTables.get(i);
                if (0 > table.destinationIdentifier
                        || table.destinationIdentifier >= quantizationTables.length) {
                    throw new ImageReadException(
                            "Invalid quantization table identifier "
                                    + table.destinationIdentifier);
                }
                quantizationTables[table.destinationIdentifier] = table;
                final int[] quantizationMatrixInt = new int[64];
                ZigZag.zigZagToBlock(table.getElements(), quantizationMatrixInt);
                final float[] quantizationMatrixFloat = new float[64];
                for (int j = 0; j < 64; j++) {
                    quantizationMatrixFloat[j] = quantizationMatrixInt[j];
                }
                Dct.scaleDequantizationMatrix(quantizationMatrixFloat);
                scaledQuantizationTables[table.destinationIdentifier] = quantizationMatrixFloat;
            }
        } else if (marker == JpegConstants.DHT_MARKER) {
            final DhtSegment dhtSegment = new DhtSegment(marker, segmentData);
            for (int i = 0; i < dhtSegment.huffmanTables.size(); i++) {
                final DhtSegment.HuffmanTable table = dhtSegment.huffmanTables.get(i);
                DhtSegment.HuffmanTable[] tables;
                if (table.tableClass == 0) {
                    tables = huffmanDCTables;
                } else if (table.tableClass == 1) {
                    tables = huffmanACTables;
                } else {
                    throw new ImageReadException("Invalid huffman table class "
                            + table.tableClass);
                }
                if (0 > table.destinationIdentifier
                        || table.destinationIdentifier >= tables.length) {
                    throw new ImageReadException(
                            "Invalid huffman table identifier "
                                    + table.destinationIdentifier);
                }
                tables[table.destinationIdentifier] = table;
            }
        }
        return true;
    }

    private void rescaleMCU(final Block[] dataUnits, final int hSize, final int vSize, final Block[] ret) {

        for (int i = 0; i < dataUnits.length; i++) {
            final Block dataUnit = dataUnits[i];

            if (dataUnit.width == hSize && dataUnit.height == vSize) {
                System.arraycopy(dataUnit.samples, 0, ret[i].samples, 0, hSize
                        * vSize);
            } else {
                final int hScale = hSize / dataUnit.width;
                final int vScale = vSize / dataUnit.height;
                if (hScale == 2 && vScale == 2) {
                    int srcRowOffset = 0;
                    int dstRowOffset = 0;
                    for (int y = 0; y < dataUnit.height; y++) {
                        for (int x = 0; x < hSize; x++) {
                            final int sample = dataUnit.samples[srcRowOffset + (x >> 1)];
                            ret[i].samples[dstRowOffset + x] = sample;
                            ret[i].samples[dstRowOffset + hSize + x] = sample;
                        }
                        srcRowOffset += dataUnit.width;
                        dstRowOffset += 2 * hSize;
                    }
                } else {
                    // FIXME: optimize
                    int dstRowOffset = 0;
                    for (int y = 0; y < vSize; y++) {
                        for (int x = 0; x < hSize; x++) {
                            ret[i].samples[dstRowOffset + x] = dataUnit.samples[(y / vScale)
                                    * dataUnit.width + (x / hScale)];
                        }
                        dstRowOffset += hSize;
                    }
                }
            }
        }
    }
 // --- 12/7 10:30
    @NotNull
    private Block[] allocateMCUMemory() throws ImageReadException {
        final Block[] mcu = new Block[sosSegment.numberOfComponents];
        for (int i = 0; i < sosSegment.numberOfComponents; i++) {
            final SosSegment.Component scanComponent = sosSegment.getComponents(i);
            SofnSegment.Component frameComponent = null;
            for (int j = 0; j < sofnSegment.numberOfComponents; j++) {
                if (sofnSegment.getComponents(j).componentIdentifier
                        == scanComponent.scanComponentSelector) {
                    frameComponent = sofnSegment.getComponents(j);
                    break;
                }
            }
            if (frameComponent == null) {
                throw new ImageReadException("Invalid component");
            }
            final Block fullBlock = new Block(
                    8 * frameComponent.horizontalSamplingFactor,
                    8 * frameComponent.verticalSamplingFactor);
            mcu[i] = fullBlock;
        }
        return mcu;
    }

    private void readMCU(final JpegInputStream is, final int[] preds, final Block[] mcu)
            throws IOException, ImageReadException {
        //Extract extract = new  Extract;
        int count =0;
        for (int i = 0; i < sosSegment.numberOfComponents; i++) {
            final SosSegment.Component scanComponent = sosSegment.getComponents(i);
            SofnSegment.Component frameComponent = null;
            for (int j = 0; j < sofnSegment.numberOfComponents; j++) {
                if (sofnSegment.getComponents(j).componentIdentifier
                        == scanComponent.scanComponentSelector) {
                    frameComponent = sofnSegment.getComponents(j);
                    break;
                }
            }
            if (frameComponent == null) {
                throw new ImageReadException("Invalid component");
            }
            final Block fullBlock = mcu[i];
            for (int y = 0; y < frameComponent.verticalSamplingFactor; y++) {
                for (int x = 0; x < frameComponent.horizontalSamplingFactor; x++) {
                    Arrays.fill(zz, 0);
                    // page 104 of T.81
                    final int t = decode(
                            is,
                            huffmanDCTables[scanComponent.dcCodingTableSelector]);
                    int diff = receive(t, is);
                    diff = extend(diff, t);
                    zz[0] = preds[i] + diff;
                    preds[i] = zz[0];

                    // "Decode_AC_coefficients", figure F.13, page 106 of T.81
                    int k = 1;
                    while (true) {
                        final int rs = decode(
                                is,
                                huffmanACTables[scanComponent.acCodingTableSelector]);
                        final int ssss = rs & 0xf;
                        final int rrrr = rs >> 4;
                        final int r = rrrr;

                        if (ssss == 0) {
                            if (r == 15) {
                                k += 16;
                            } else {
                                break;
                            }
                        } else {
                            k += r;

                            // "Decode_ZZ(k)", figure F.14, page 107 of T.81
                            zz[k] = receive(ssss, is);
                            zz[k] = extend(zz[k], ssss);
                            if (k == 63) {
                                break;
                            } else {
                                k++;
                            }
                        }
                    }

                    final int shift = (1 << (sofnSegment.precision - 1));
                    final int max = (1 << sofnSegment.precision) - 1;

                    final float[] scaledQuantizationTable = scaledQuantizationTables[frameComponent.quantTabDestSelector];
                    ZigZag.zigZagToBlock(zz, blockInt);
                    //System.out.println(Arrays.toString(blockInt));
                    ex.read(blockInt);
                    for (int j = 0; j < 64; j++) {
                        block[j] = blockInt[j] * scaledQuantizationTable[j];

                    }


                    Dct.inverseDCT8x8(block);
                    for(int a=0;a<block.length;a++){
                        block[a]=Math.round(block[a]);
                    }

                    int dstRowOffset = 8 * y * 8
                            * frameComponent.horizontalSamplingFactor + 8 * x;
                    int srcNext = 0;
                    for (int yy = 0; yy < 8; yy++) {
                        for (int xx = 0; xx < 8; xx++) {
                            float sample = block[srcNext++];
                            sample += shift;
                            int result;
                            if (sample < 0) {
                                result = 0;
                            } else if (sample > max) {
                                result = max;
                            } else {
                                result = fastRound(sample);
                            }
                            fullBlock.samples[dstRowOffset + xx] = result;
                        }
                        dstRowOffset += 8 * frameComponent.horizontalSamplingFactor;
                    }
                }
            }

        }
        //System.out.println( ex.quantizedValue);
    }

    /**
     * Returns an array of JpegInputStream where each field contains the JpegInputStream
     * for one interval.
     * @param scanPayload array to read intervals from
     * @return JpegInputStreams for all intervals, at least one stream is always provided
     */
    static JpegInputStream[] splitByRstMarkers(final int[] scanPayload) {
        final List<Integer> intervalStarts = getIntervalStartPositions(scanPayload);
        // get number of intervals in payload to init an array of appropriate length
        final int intervalCount = intervalStarts.size();
        JpegInputStream[] streams = new JpegInputStream[intervalCount];
        for (int i = 0; i < intervalCount; i++) {
            int from = intervalStarts.get(i);
            int to;
            if (i < intervalCount - 1) {
                // because each restart marker needs two bytes the end of
                // this interval is two bytes before the next interval starts
                to = intervalStarts.get(i + 1) - 2;
            } else { // the last interval ends with the array
                to = scanPayload.length;
            }
            int[] interval = Arrays.copyOfRange(scanPayload, from, to);
            streams[i] = new JpegInputStream(interval);
        }
        return streams;
    }

    /**
     * Returns the positions of where each interval in the provided array starts. The number
     * of start positions is also the count of intervals while the number of restart markers
     * found is equal to the number of start positions minus one (because restart markers
     * are between intervals).
     *
     * @param scanPayload array to examine
     * @return the start positions
     */
    @NotNull
    static List<Integer> getIntervalStartPositions(@NotNull final int[] scanPayload) {
        final List<Integer> intervalStarts = new ArrayList<Integer>();
        intervalStarts.add(0);
        boolean foundFF = false;
        boolean foundD0toD7 = false;
        int pos = 0;
        while (pos < scanPayload.length) {
            if (foundFF) {
                // found 0xFF D0 .. 0xFF D7 => RST marker
                if (scanPayload[pos] >= (0xff & JpegConstants.RST0_MARKER) &&
                        scanPayload[pos] <= (0xff & JpegConstants.RST7_MARKER)) {
                    foundD0toD7 = true;
                } else { // found 0xFF followed by something else => no RST marker
                    foundFF = false;
                }
            }

            if (scanPayload[pos] == 0xFF) {
                foundFF = true;
            }

            // true if one of the RST markers was found
            if (foundFF && foundD0toD7) {
                // we need to add the position after the current position because
                // we had already read 0xFF and are now at 0xDn
                intervalStarts.add(pos + 1);
                foundFF = foundD0toD7 = false;
            }
            pos++;
        }
        return intervalStarts;
    }

    private static int fastRound(final float x) {
        return (int) (x + 0.5f);
    }

    private int extend(int v, final int t) {
        // "EXTEND", section F.2.2.1, figure F.12, page 105 of T.81
        int vt = (1 << (t - 1));
        if (v < vt) {
            vt = (-1 << t) + 1;
            v += vt;
        }
        return v;
    }

    private int receive(final int ssss, final JpegInputStream is) throws IOException,
            ImageReadException {
        // "RECEIVE", section F.2.2.4, figure F.17, page 110 of T.81
        int i = 0;
        int v = 0;
        while (i != ssss) {
            i++;
            v = (v << 1) + is.nextBit();
        }
        return v;
    }

    private int decode(@NotNull final JpegInputStream is, @NotNull final DhtSegment.HuffmanTable huffmanTable)
            throws IOException, ImageReadException {
        // "DECODE", section F.2.2.3, figure F.16, page 109 of T.81
        int i = 1;
        int code = is.nextBit();
        while (code > huffmanTable.getMaxCode(i)) {
            i++;
            code = (code << 1) | is.nextBit();
        }
        int j = huffmanTable.getValPtr(i);
        j += code - huffmanTable.getMinCode(i);
        return huffmanTable.getHuffVal(j);
    }

    public Bitmap decode(final ByteSource byteSource) throws Exception {
        final JpegUtils jpegUtils = new JpegUtils();
        jpegUtils.traverseJFIF(byteSource, this);
        Log.d("JpegDecode2", "quantizedValue size = "+ex.quantizedValue.size());
        ex.getMsg();
        //System.out.println("Message Binary Code:" + "ex.msg.toString());
        ex.returnMsg();
        if (imageReadException != null) {
            throw imageReadException;
        }
        if (ioException != null) {
            throw ioException;
        }
        return image;
    }

    public String getSecretText(String password) throws Exception {
        return ex.getSecretMsg(password);
    }

    //public String getDecryptedText() throws Exception {return ex.decrypt();}

}

class Extract {

    public ArrayList<Integer> quantizedValue =new ArrayList();
    public StringBuilder msg =new StringBuilder();
    public Extract(){}

    public void read(@NotNull int[] input){
        for(int i=0;i<input.length;i++){
            quantizedValue.add(input[i]);
        }
    }
    public int getSize(){
        return quantizedValue.size();
    }
    public int LSB(@NotNull int[] q) {
        int LSB = (q[0]) & 1;
        return LSB;
    }

    private int LSB(int q) {
        int LSB = (q) & 1;
        return LSB;
    }

    public void getMsg(){
        int count =0;
       while(count< quantizedValue.size()) {

        if(LSB(quantizedValue.get(count)) == 0)
        {
            msg.append(0);
        }
        else if(LSB(quantizedValue.get(count)) == 1)
        {
            msg.append(1);
        }
        count++;
        }
    }

    public void getMsg2(){
        //String[] arr ={"0", "0", "1", "1", "0", "0", "0", "0"};
        int size = msg.length();
        char [] last= new char []{};

        for (int i=0;i<quantizedValue.size();i++) {
            if(quantizedValue.get(i)==0 || quantizedValue.get(i)==1)continue;
//            if(i == quantizedValue.size()-1){
//                for (int j=0;j<8;j++) msg.append(arr[i]);
//            }
            if ((quantizedValue.get(i) & 1) == 0) {
                msg.append(0);
            } else if ((quantizedValue.get(i) & 1) == 1) {
                msg.append(1);
            }
//            for(int x =8;x<0;x--){
//                int j =0;
//                last[j++]= msg.charAt(size-x);
//            }
//            System.out.println("last"+Arrays.toString(last));
        }

    }

    public void returnMsg(){

        //String [] test = msg.toString().split("(?<=\\G.{8})");
        String [] test = splitStringEvery(msg.toString(),8);
        System.out.println("test [] = "+ Arrays.toString(test));
        System.out.println("test.length = "+ test.length);
        System.out.println("msg.length()="+msg.length());
        System.out.println(""+(quantizedValue.size()/8));
        StringBuilder result =new StringBuilder();
//        byte[] bb = new byte[1024];
//        int k = 0;
//        for (int i=0;i<msg.length();i+=8) {
//            byte c = 0;
//            for (int j=0;j<8;j++) {
//                if (msg.charAt(i+j) == '1')
//                    c |= 0x1 << j;
//            }
//            if (c==0)
//                break;
////            bb[k++] = c;
//            result.append((char) (c & 0xFF));
//        }
////        result.append(new String(bb));
        for(int i =0;i< msg.length()/8;i++) {
            //byte b = Byte.parseByte(test[i], 2) ;
            //if (b==0) break;
            int charCode = Integer.parseInt(test[i], 2);
            if (charCode == 3)
                break;
            String str = new Character((char) charCode).toString();
            //result.append((char));
            result.append(str);
        }
        System.out.println("result:"+result.toString());

    }

    public String[] splitStringEvery(String s, int interval) {
        int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
        String[] result = new String[arrayLength];

        int j = 0;
        int lastIndex = result.length - 1;
        for (int i = 0; i < lastIndex; i++) {
            result[i] = s.substring(j, j + interval);
            j += interval;
        } //Add the last bit
        result[lastIndex] = s.substring(j);

        return result;
    }

    public String getSecretMsg(String password) throws Exception {
        String [] test = splitStringEvery(msg.toString(),8);

        StringBuilder result =new StringBuilder();
        for(int i =0;i< msg.length()/8;i++) {
//            byte b = Byte.parseByte(test[i], 2) ;
//            if (b==0) break;
           // byte charCode = Byte.parseByte(test[i], 2);
            int charCode = Integer.parseInt(test[i], 2);
            if (charCode == 3)
                break;
            String str = new Character((char) charCode).toString();
            //result.append((char));
            result.append(str);
        }
        String msgDecrypted = decrypt(result.toString(),password);
        return msgDecrypted;
        //return result.toString();
    }

    public String decrypt(String s, String password) throws Exception {
        //val salt = ByteArray(256)
        byte [] salt = new byte[256];
        Arrays.fill(salt, (byte) 0);
        //char [] password = {'1','2','3'};
        PBEKeySpec pbKeySpec = new PBEKeySpec(password.toCharArray(), salt,
                1324, 256); // 1
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // 2
        byte [] keyBytes = secretKeyFactory.generateSecret(pbKeySpec).getEncoded(); // 3
        SecretKeySpec keySpec =  new SecretKeySpec(keyBytes, "AES"); // 4
//        val ivRandom = SecureRandom() //not caching previous seeded instance of SecureRandom
// 1
        //val iv = ByteArray(16)
        byte [] iv = new byte[16];
        Arrays.fill(iv, (byte) 1);
//        ivRandom.nextBytes(iv)
        IvParameterSpec ivSpec = new IvParameterSpec(iv); // 2

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding"); // 1

//regenerate key from password

        byte[] data = Base64.decode(s.getBytes(), Base64.DEFAULT);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        //val encrypted = ByteArray(12)
        byte [] decrypted = cipher.doFinal(data);


        String text = new String(decrypted, StandardCharsets.UTF_8);
        Log.e("Decrypt", text);
        return text;
    }
}

final class Dct {
    /*
     * The book "JPEG still image data compression standard", by Pennebaker and
     * Mitchell, Chapter 4, discusses a number of approaches to the fast DCT.
     * Here's the cost, exluding modified (de)quantization, for transforming an
     * 8x8 block:
     *
     * Algorithm                     Adds Multiplies RightShifts Total
     * Naive                          896       1024           0  1920
     * "Symmetries"                   448        224           0   672
     * Vetterli and Ligtenberg        464        208           0   672
     * Arai, Agui and Nakajima (AA&N) 464         80           0   544
     * Feig 8x8                       462         54           6   522
     * Fused mul/add (a pipe dream)                                416
     *
     * IJG's libjpeg, FFmpeg, and a number of others use AA&N.
     *
     * It would appear that Feig does 4-5% less operations, and multiplications
     * are reduced from 80 in AA&N to only 54. But in practice:
     *
     * Benchmarks, Intel Core i3 @ 2.93 GHz in long mode, 4 GB RAM Time taken to
     * do 100 million IDCTs (less is better):
     * Rene' Stöckel's Feig, int: 45.07 seconds
     * My Feig, floating point: 36.252 seconds
     * AA&N, unrolled loops, double[][] -> double[][]: 25.167 seconds
     *
     * Clearly Feig is hopeless. I suspect the performance killer is simply the
     * weight of the algorithm: massive number of local variables, large code
     * size, and lots of random array accesses.
     *
     * Also, AA&N can be optimized a lot:
     * AA&N, rolled loops, double[][] -> double[][]: 21.162 seconds
     * AA&N, rolled loops, float[][] -> float[][]: no improvement,
     * but at some stage Hotspot might start doing SIMD, so let's
     * use float AA&N, rolled loops, float[] -> float[][]: 19.979 seconds
     * apparently 2D arrays are slow!
     * AA&N, rolled loops, inlined 1D AA&N
     * transform, float[] transformed in-place: 18.5 seconds
     * AA&N, previous version rewritten in C and compiled with "gcc -O3"
     * takes: 8.5 seconds
     * (probably due to heavy use of SIMD)
     *
     * Other brave attempts: AA&N, best float version converted to 16:16 fixed
     * point: 23.923 seconds
     *
     * Anyway the best float version stays. 18.5 seconds = 5.4 million
     * transforms per second per core :-)
     */

    private static final float[] DCT_SCALING_FACTORS = {
            (float) (0.5 / Math.sqrt(2.0)),
            (float) (0.25 / Math.cos(Math.PI / 16.0)),
            (float) (0.25 / Math.cos(2.0 * Math.PI / 16.0)),
            (float) (0.25 / Math.cos(3.0 * Math.PI / 16.0)),
            (float) (0.25 / Math.cos(4.0 * Math.PI / 16.0)),
            (float) (0.25 / Math.cos(5.0 * Math.PI / 16.0)),
            (float) (0.25 / Math.cos(6.0 * Math.PI / 16.0)),
            (float) (0.25 / Math.cos(7.0 * Math.PI / 16.0)), };

    private static final float[] IDCT_SCALING_FACTORS = {
            (float) (2.0 * 4.0 / Math.sqrt(2.0) * 0.0625),
            (float) (4.0 * Math.cos(Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(2.0 * Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(3.0 * Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(4.0 * Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(5.0 * Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(6.0 * Math.PI / 16.0) * 0.125),
            (float) (4.0 * Math.cos(7.0 * Math.PI / 16.0) * 0.125), };

    private static final float A1 = (float) (Math.cos(2.0 * Math.PI / 8.0));
    private static final float A2 = (float) (Math.cos(Math.PI / 8.0) - Math.cos(3.0 * Math.PI / 8.0));
    private static final float A3 = A1;
    private static final float A4 = (float) (Math.cos(Math.PI / 8.0) + Math.cos(3.0 * Math.PI / 8.0));
    private static final float A5 = (float) (Math.cos(3.0 * Math.PI / 8.0));

    private static final float C2 = (float) (2.0 * Math.cos(Math.PI / 8));
    private static final float C4 = (float) (2.0 * Math.cos(2 * Math.PI / 8));
    private static final float C6 = (float) (2.0 * Math.cos(3 * Math.PI / 8));
    private static final float Q = C2 - C6;
    private static final float R = C2 + C6;

    private Dct() {
    }

    public static void scaleQuantizationVector(final float[] vector) {
        for (int x = 0; x < 8; x++) {
            vector[x] *= DCT_SCALING_FACTORS[x];
        }
    }

    public static void scaleDequantizationVector(final float[] vector) {
        for (int x = 0; x < 8; x++) {
            vector[x] *= IDCT_SCALING_FACTORS[x];
        }
    }

    public static void scaleQuantizationMatrix(final float[] matrix) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                matrix[8 * y + x] *= DCT_SCALING_FACTORS[y]
                        * DCT_SCALING_FACTORS[x];
            }
        }
    }

    public static void scaleDequantizationMatrix(final float[] matrix) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                matrix[8 * y + x] *= IDCT_SCALING_FACTORS[y]
                        * IDCT_SCALING_FACTORS[x];
            }
        }
    }

    /**
     * Fast forward Dct using AA&N. Taken from the book
     * "JPEG still image data compression standard", by Pennebaker and Mitchell,
     * chapter 4, figure "4-8".
     */
    public static void forwardDCT8(@NotNull final float[] vector) {
        final float a00 = vector[0] + vector[7];
        final float a10 = vector[1] + vector[6];
        final float a20 = vector[2] + vector[5];
        final float a30 = vector[3] + vector[4];
        final float a40 = vector[3] - vector[4];
        final float a50 = vector[2] - vector[5];
        final float a60 = vector[1] - vector[6];
        final float a70 = vector[0] - vector[7];

        final float a01 = a00 + a30;
        final float a11 = a10 + a20;
        final float a21 = a10 - a20;
        final float a31 = a00 - a30;
        // Avoid some negations:
        // float a41 = -a40 - a50;
        final float neg_a41 = a40 + a50;
        final float a51 = a50 + a60;
        final float a61 = a60 + a70;

        final float a22 = a21 + a31;

        final float a23 = a22 * A1;
        final float mul5 = (a61 - neg_a41) * A5;
        final float a43 = neg_a41 * A2 - mul5;
        final float a53 = a51 * A3;
        final float a63 = a61 * A4 - mul5;

        final float a54 = a70 + a53;
        final float a74 = a70 - a53;

        vector[0] = a01 + a11;
        vector[4] = a01 - a11;
        vector[2] = a31 + a23;
        vector[6] = a31 - a23;
        vector[5] = a74 + a43;
        vector[1] = a54 + a63;
        vector[7] = a54 - a63;
        vector[3] = a74 - a43;
    }

    public static void forwardDCT8x8(final float[] matrix) {
        float a00, a10, a20, a30, a40, a50, a60, a70;
        float a01, a11, a21, a31, neg_a41, a51, a61;
        float a22, a23, mul5, a43, a53, a63;
        float a54, a74;

        for (int i = 0; i < 8; i++) {
            a00 = matrix[8 * i] + matrix[8 * i + 7];
            a10 = matrix[8 * i + 1] + matrix[8 * i + 6];
            a20 = matrix[8 * i + 2] + matrix[8 * i + 5];
            a30 = matrix[8 * i + 3] + matrix[8 * i + 4];
            a40 = matrix[8 * i + 3] - matrix[8 * i + 4];
            a50 = matrix[8 * i + 2] - matrix[8 * i + 5];
            a60 = matrix[8 * i + 1] - matrix[8 * i + 6];
            a70 = matrix[8 * i] - matrix[8 * i + 7];
            a01 = a00 + a30;
            a11 = a10 + a20;
            a21 = a10 - a20;
            a31 = a00 - a30;
            neg_a41 = a40 + a50;
            a51 = a50 + a60;
            a61 = a60 + a70;
            a22 = a21 + a31;
            a23 = a22 * A1;
            mul5 = (a61 - neg_a41) * A5;
            a43 = neg_a41 * A2 - mul5;
            a53 = a51 * A3;
            a63 = a61 * A4 - mul5;
            a54 = a70 + a53;
            a74 = a70 - a53;
            matrix[8 * i] = a01 + a11;
            matrix[8 * i + 4] = a01 - a11;
            matrix[8 * i + 2] = a31 + a23;
            matrix[8 * i + 6] = a31 - a23;
            matrix[8 * i + 5] = a74 + a43;
            matrix[8 * i + 1] = a54 + a63;
            matrix[8 * i + 7] = a54 - a63;
            matrix[8 * i + 3] = a74 - a43;
        }

        for (int i = 0; i < 8; i++) {
            a00 = matrix[i] + matrix[56 + i];
            a10 = matrix[8 + i] + matrix[48 + i];
            a20 = matrix[16 + i] + matrix[40 + i];
            a30 = matrix[24 + i] + matrix[32 + i];
            a40 = matrix[24 + i] - matrix[32 + i];
            a50 = matrix[16 + i] - matrix[40 + i];
            a60 = matrix[8 + i] - matrix[48 + i];
            a70 = matrix[i] - matrix[56 + i];
            a01 = a00 + a30;
            a11 = a10 + a20;
            a21 = a10 - a20;
            a31 = a00 - a30;
            neg_a41 = a40 + a50;
            a51 = a50 + a60;
            a61 = a60 + a70;
            a22 = a21 + a31;
            a23 = a22 * A1;
            mul5 = (a61 - neg_a41) * A5;
            a43 = neg_a41 * A2 - mul5;
            a53 = a51 * A3;
            a63 = a61 * A4 - mul5;
            a54 = a70 + a53;
            a74 = a70 - a53;
            matrix[i] = a01 + a11;
            matrix[32 + i] = a01 - a11;
            matrix[16 + i] = a31 + a23;
            matrix[48 + i] = a31 - a23;
            matrix[40 + i] = a74 + a43;
            matrix[8 + i] = a54 + a63;
            matrix[56 + i] = a54 - a63;
            matrix[24 + i] = a74 - a43;
        }
    }

    /**
     * Fast inverse Dct using AA&N. This is taken from the beautiful
     * http://vsr.finermatik.tu-chemnitz.de/~jan/MPEG/HTML/IDCT.html which gives
     * easy equations and properly explains constants and scaling factors. Terms
     * have been inlined and the negation optimized out of existence.
     */
    public static void inverseDCT8(@NotNull final float[] vector) {
        // B1
        final float a2 = vector[2] - vector[6];
        final float a3 = vector[2] + vector[6];
        final float a4 = vector[5] - vector[3];
        final float tmp1 = vector[1] + vector[7];
        final float tmp2 = vector[3] + vector[5];
        final float a5 = tmp1 - tmp2;
        final float a6 = vector[1] - vector[7];
        final float a7 = tmp1 + tmp2;

        // M
        final float tmp4 = C6 * (a4 + a6);
        // Eliminate the negative:
        // float b4 = -Q*a4 - tmp4;
        final float neg_b4 = Q * a4 + tmp4;
        final float b6 = R * a6 - tmp4;
        final float b2 = a2 * C4;
        final float b5 = a5 * C4;

        // A1
        final float tmp3 = b6 - a7;
        final float n0 = tmp3 - b5;
        final float n1 = vector[0] - vector[4];
        final float n2 = b2 - a3;
        final float n3 = vector[0] + vector[4];
        final float neg_n5 = neg_b4;

        // A2
        final float m3 = n1 + n2;
        final float m4 = n3 + a3;
        final float m5 = n1 - n2;
        final float m6 = n3 - a3;
        // float m7 = n5 - n0;
        final float neg_m7 = neg_n5 + n0;

        // A3
        vector[0] = m4 + a7;
        vector[1] = m3 + tmp3;
        vector[2] = m5 - n0;
        vector[3] = m6 + neg_m7;
        vector[4] = m6 - neg_m7;
        vector[5] = m5 + n0;
        vector[6] = m3 - tmp3;
        vector[7] = m4 - a7;
    }

    public static void inverseDCT8x8(final float[] matrix) {
        float a2, a3, a4, tmp1, tmp2, a5, a6, a7;
        float tmp4, neg_b4, b6, b2, b5;
        float tmp3, n0, n1, n2, n3, neg_n5;
        float m3, m4, m5, m6, neg_m7;

        for (int i = 0; i < 8; i++) {
            a2 = matrix[8 * i + 2] - matrix[8 * i + 6];
            a3 = matrix[8 * i + 2] + matrix[8 * i + 6];
            a4 = matrix[8 * i + 5] - matrix[8 * i + 3];
            tmp1 = matrix[8 * i + 1] + matrix[8 * i + 7];
            tmp2 = matrix[8 * i + 3] + matrix[8 * i + 5];
            a5 = tmp1 - tmp2;
            a6 = matrix[8 * i + 1] - matrix[8 * i + 7];
            a7 = tmp1 + tmp2;
            tmp4 = C6 * (a4 + a6);
            neg_b4 = Q * a4 + tmp4;
            b6 = R * a6 - tmp4;
            b2 = a2 * C4;
            b5 = a5 * C4;
            tmp3 = b6 - a7;
            n0 = tmp3 - b5;
            n1 = matrix[8 * i] - matrix[8 * i + 4];
            n2 = b2 - a3;
            n3 = matrix[8 * i] + matrix[8 * i + 4];
            neg_n5 = neg_b4;
            m3 = n1 + n2;
            m4 = n3 + a3;
            m5 = n1 - n2;
            m6 = n3 - a3;
            neg_m7 = neg_n5 + n0;
            matrix[8 * i] = m4 + a7;
            matrix[8 * i + 1] = m3 + tmp3;
            matrix[8 * i + 2] = m5 - n0;
            matrix[8 * i + 3] = m6 + neg_m7;
            matrix[8 * i + 4] = m6 - neg_m7;
            matrix[8 * i + 5] = m5 + n0;
            matrix[8 * i + 6] = m3 - tmp3;
            matrix[8 * i + 7] = m4 - a7;
        }

        for (int i = 0; i < 8; i++) {
            a2 = matrix[16 + i] - matrix[48 + i];
            a3 = matrix[16 + i] + matrix[48 + i];
            a4 = matrix[40 + i] - matrix[24 + i];
            tmp1 = matrix[8 + i] + matrix[56 + i];
            tmp2 = matrix[24 + i] + matrix[40 + i];
            a5 = tmp1 - tmp2;
            a6 = matrix[8 + i] - matrix[56 + i];
            a7 = tmp1 + tmp2;
            tmp4 = C6 * (a4 + a6);
            neg_b4 = Q * a4 + tmp4;
            b6 = R * a6 - tmp4;
            b2 = a2 * C4;
            b5 = a5 * C4;
            tmp3 = b6 - a7;
            n0 = tmp3 - b5;
            n1 = matrix[i] - matrix[32 + i];
            n2 = b2 - a3;
            n3 = matrix[i] + matrix[32 + i];
            neg_n5 = neg_b4;
            m3 = n1 + n2;
            m4 = n3 + a3;
            m5 = n1 - n2;
            m6 = n3 - a3;
            neg_m7 = neg_n5 + n0;
            matrix[i] = m4 + a7;
            matrix[8 + i] = m3 + tmp3;
            matrix[16 + i] = m5 - n0;
            matrix[24 + i] = m6 + neg_m7;
            matrix[32 + i] = m6 - neg_m7;
            matrix[40 + i] = m5 + n0;
            matrix[48 + i] = m3 - tmp3;
            matrix[56 + i] = m4 - a7;
        }
    }
}

final class Block {
    final int[] samples;
    final int width;
    final int height;

    Block(final int width, final int height) {
        samples = new int[width * height];
        this.width = width;
        this.height = height;
    }
}

class JpegInputStream {
    // Figure F.18, F.2.2.5, page 111 of ITU-T T.81
    private final int[] interval;
    // next position in the array to read
    private int nextPos;
    private int cnt;
    private int b;

    JpegInputStream(final int[] interval) {
        this.interval = Arrays.copyOf(interval, interval.length);
        this.nextPos = 0;
    }

    /**
     * Returns {@code true} as long there are unread fields available, else {@code false}
     * @return
     */
    public boolean hasNext() {
        return nextPos < this.interval.length;
    }

    public int nextBit() throws ImageReadException {
        if (cnt == 0) {
            b = this.read();
            if (b < 0) {
                throw new ImageReadException("Premature End of File");
            }
            cnt = 8;
            if (b == 0xff) {
                final int b2 = this.read();
                if (b2 < 0) {
                    throw new ImageReadException("Premature End of File");
                }
                if (b2 != 0) {
                    if (b2 == (0xff & JpegConstants.DNL_MARKER)) {
                        throw new ImageReadException("DNL not yet supported");
                    }
                    throw new ImageReadException("Invalid marker found "
                            + "in entropy data: 0xFF " + Integer.toHexString(b2));
                }
            }
        }
        final int bit = (b >> 7) & 0x1;
        cnt--;
        b <<= 1;
        return bit;
    }

    /**
     * Returns the value from current field (as {@code InputStream.read()} would do)
     * and set the position of the pointer to the next field to read.
     * @return
     * @throws IllegalStateException if the stream hasn't any other value.
     */
    int read() {
        if (!this.hasNext()) {
            throw new IllegalStateException("This stream hasn't any other value, all values were already read.");
        }
        final int value = this.interval[nextPos];
        this.nextPos++;
        return value;
    }
}