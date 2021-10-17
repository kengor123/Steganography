//package comp.proj.painter.ui.code;
//
//import java.awt.*;
//import java.io.*;
//import java.nio.file.Files;
//import java.util.Arrays;
//import java.util.*;
//
//import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
//
////import jpeg.view.Window;
//
//public class test2 {
//
//
//    public static void main(String[] arg) throws Exception {
//
//        test2 t2 =new test2();
//        while(true) {
//            t2.showCommand();
//            System.out.print("input your command: ");
//            Scanner scanner = new Scanner(System.in);
//            String command = scanner.next();
//            switch (command) {
//                case "0":
//                    System.exit(0);
//                case "1":
//                    t2.selectCoverImg();
//                    break;
//                case "2":
//                    t2.callDecode();
//                    break;
//                default:
//                    break;
//
//            }
//            System.out.println("-----------------------------");
//            System.out.println("");
//        }
////        File imageFile = new File("/Users/kenfu/fypMaterial/bmp-test.bmp");
////        byte[] b = Files.readAllBytes(imageFile.toPath());
////        System.out.println(Arrays.toString(b));
////        System.out.println(b.length);
////        FileOutputStream out = new FileOutputStream("/Users/kenfu/fypMaterial/out_test.jpg");
////        out.write(b);
////        out.flush();
//    }
//
//    public void callEncode() throws Exception {
//        System.out.print("Please enter the secret message: ");
//        Scanner scanner = new Scanner(System.in);
//        String text = scanner.nextLine();
//        //System.out.print("Please select the cover image: ");
//        //String fileN = scanner.next();
//        BufferedImage image = ImageIO.read(new File("/Users/kenfu/fypMaterial/autumn.jpg"));
//        //  /Users/kenfu/fypMaterial/bmp-test.bmp;
//
//        String desFile = "/Users/kenfu/fypMaterial/result.jpg";
//        FileOutputStream imgOut = new FileOutputStream(desFile);
//        BufferedOutputStream out = new BufferedOutputStream(imgOut);
//        JpegEncoder jpegEncoder = new JpegEncoder(image, 90, out, text);
//        jpegEncoder.Compress();
//        System.out.println("DONE! your image is save as "+ desFile);
//    }
//
//    public void callEncode(String path) throws Exception {
//        System.out.print("Please enter the secret message: ");
//        Scanner scanner = new Scanner(System.in);
//        String text = scanner.nextLine();
//        //System.out.print("Please select the cover image: ");
//        //String fileN = scanner.next();
//        BufferedImage image = ImageIO.read(new File(path));
//        //  /Users/kenfu/fypMaterial/bmp-test.bmp;
//
//        String desFile = "/Users/kenfu/fypMaterial/result.jpg";
//        FileOutputStream imgOut = new FileOutputStream(desFile);
//        BufferedOutputStream out = new BufferedOutputStream(imgOut);
//        JpegEncoder jpegEncoder = new JpegEncoder(image, 90, out, text);
//        jpegEncoder.Compress();
//        System.out.println("DONE! your image is save as "+ desFile);
//    }
//
//    public void selectCoverImg() throws Exception {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Please select the cover image:");
//        String path = scanner.next();
//        callEncode(path);
//    }
//    public void callDecode() throws Exception {
//        File imageFile = new File("/Users/kenfu/fypMaterial/result.jpg");
////        BufferedImage imageDecode = ImageIO.read(new File("/Users/kenfu/fypMaterial/bmp-result-mac.jpg"));
//
//        //---- jpegDecode2
//        JpegDecode2 jpegDecode2=new JpegDecode2();
//        jpegDecode2.decode(new ByteSourceFile(imageFile));
//    }
//
//    public void showCommand(){
//        System.out.println("Welcome to use Steganography Demo");
//        System.out.println("Exit: 0");
//        System.out.println("Embed: 1");
//        System.out.println("Extract: 2");
//
//
//    }
//}
