package miniplc0java.vm;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BytesHandler {

    public static ArrayList<Byte> handleInt(int num) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(0, num);
        ArrayList<Byte> res = new ArrayList<>();
        for (byte b : buffer.array()) {
            res.add(b);
        }
        return res;
    }

    public static ArrayList<Byte> handleDouble(double num) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putDouble(0, num);
        ArrayList<Byte> res = new ArrayList<>();
        for (byte b : buffer.array()) {
            res.add(b);
        }
        return res;
    }

    public static ArrayList<Byte> handleLong(long num) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, num);
        ArrayList<Byte> res = new ArrayList<>();
        for (byte b : buffer.array()) {
            res.add(b);
        }
        return res;
    }

    public static ArrayList<Byte> handleByte(int num) {
        Byte b = (byte) num;
        ArrayList<Byte> res = new ArrayList<>();
        res.add(b);
        return res;
    }

    public static ArrayList<Byte> handleByte(byte num) {
        ArrayList<Byte> res = new ArrayList<>();
        res.add(num);
        return res;
    }

    public static ArrayList<Byte> handleString(String s) {
        ArrayList<Byte> res = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.allocate(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            byte b = (byte) c;
            res.add(b);
        }

        return res;
    }

}
