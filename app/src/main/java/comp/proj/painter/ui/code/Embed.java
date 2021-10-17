package comp.proj.painter.ui.code;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;

public class Embed {

    public static int count =0;
    public static int length;
    public String text;
    public File secretImg;
    public ArrayList<Integer> imgArray = new ArrayList<>();
    public ArrayList<Integer> sizeCount = new ArrayList<>();

    public Embed(String text){
        this.count=count;
        this.text = text;
        this.length= text2Bin(text).length();
        this.getCapacity();
    }

    public Embed(File secretImg){
        this.count=count;
        this.secretImg = secretImg;
        this.length= text2Bin(text).length();
        this.getCapacity();
    }

    public void read(int [] input){
        for(int i=0;i<input.length;i++){
            imgArray.add(input[i]);
        }
    }

    public int LSB(int value) {
        int LSB = (value) & 1;
        return LSB;
    }

    public void editOneBit_v2(int[] q, int size, String src) {
        String binary = text2Bin(src);
        for (int i = 0; i < binary.length(); i++) {
            if (q[i] != 0 && q[i] != 1) {
                if (binary.charAt(i) == 49) {  //1
                    q[i] = (q[i] + 1);
                } else if (binary.charAt(i) == 48) {  //0
                    q[i] = (q[i] + 1);
                }
            } else if (q[i] == 0) {
                q[i] = q[i];
            }
            count ++;
        }
    }

    public String text2Bin(String src){                    //covert message text to binary code
        byte[] bytes = src.getBytes();
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }
        //int [] endMark = new int [] {0,0,0,0,0,0,1,1};
        String [] endMark = new String [] {"0","0","0","0","0","0","1","1"};
        for(int i=0;i<endMark.length;i++) binary.append(endMark[i]);
        System.out.println(binary.toString());
        return binary.toString();
    }

    // using
    public void embedText2() {
        editOneBitArrayList(text);
    }

    //using
    private void editOneBitArrayList(String src) {
        String binary = text2Bin(src);                 // convert String to binary code
        for (int i = 0; i < binary.length(); i++) {
            int bitToEmbed = binary.charAt(i) - 48;    //  handle ASCII
            if (LSB(imgArray.get(i)) == bitToEmbed) {
                // do nothing
            }else {
                imgArray.set(i, imgArray.get(i) + 1);   // replace the bit
            }
        }
    }

    //developing
    private void editOneBitArrayList2(String src) {
        String binary = text2Bin(src);     // convert String to binary code
        int pos = 0;
        int [] embedData = binArray(binary);
        int [] endMark = new int [] {0,0,0,0,0,0,1,1};
        for (int i = 0; i < imgArray.size(); i++) {
            if(imgArray.get(i) ==0 || imgArray.get(i) ==1){continue;}
            else{
                if ((imgArray.get(i) & 1) == embedData[pos]) {
                    pos ++;
                }else {
                    imgArray.set(i, imgArray.get(i) + 1);   // replace the bit
                    pos ++;
                }
            }
            if(imgArray.get(i)!= 0){ }
            if(pos == embedData.length){
                break;
            }
//            for(int y=0;y<endMark.length;y++){
//                if ((imgArray.get(i) & 1) == endMark[y]) {
//                }else {
//                    imgArray.set(i, imgArray.get(i) + 1);   // replace the bit
//                }
//            }
        }
        int remain = getCapacityNoZero() - embedData.length;
        if(getCapacityNoZero() >= embedData.length)
        System.out.println("True: "+ getCapacityNoZero()+ ", space left ("+ remain +")" );
    }

    public int [] binArray(String src){
        int [] array = new int[src.length()];
        for(int i= 0;i<array.length;i++){
            array[i] =  src.charAt(i)-48;
        }
        return array;
    }

    public String getText(){
        return text;
    }

    public int getLength(){
        return length;
    }

    public int getCapacity(){
        return imgArray.size();
    }

    public int getCapacityNoZero(){
        int count =0;
        for (int i =0;i<imgArray.size();i++)
            if(imgArray.get(i) ==0 || imgArray.get(i) ==1){continue;}else{
                count++;
                //sizeCount.add(imgArray.get(i));
            }
        return count;
    }

    public int [][] listToArray(){
        int count = imgArray.size() / 64;
        int [][] result = new int[count][8*8];
        for (int i=0;i<count;i++) {
            for (int j=0;j<8*8;j++) {
                result[i][j] = imgArray.get(i*8*8+j);
            }
        }
        return result;
    }

    public void setSecretImg() throws Exception {
        //FileInputStream fin = new FileInputStream(secretImg);
        byte[] b = Files.readAllBytes(secretImg.toPath());
        System.out.println(b);

    }
}
