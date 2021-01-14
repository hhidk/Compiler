package miniplc0java.vm;

import miniplc0java.analyser.SymbolEntry;
import miniplc0java.analyser.Type;

public class GlobalDef {
    byte is_count;
    int value_count;
    String value;

    Type type;

    public GlobalDef(String name, SymbolEntry symbolEntry) {
        this.is_count = (byte) symbolEntry.getOrder();
        this.type = symbolEntry.getType();
        if (symbolEntry.getType() == Type.int_ty || symbolEntry.getType() == Type.double_ty) {
            this.value_count = 8;
        } else {
            this.value_count = name.length();
            this.value = name;
        }
    }

    @Override
    public String toString() {
        return "GlobalDef{" +
                "is_count=" + is_count +
                ", value_count=" + value_count +
                ", value='" + value + '\'' +
                ", type=" + type +
                '}';
    }

    public String toVmCode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toHexByte(is_count));
        stringBuilder.append(toHexString(value_count));
        if (type == Type.int_ty || type == Type.double_ty) {
            stringBuilder.append(toHexString(0));
        } else {
            stringBuilder.append(toHexString(value));
        }
        return stringBuilder.toString();
    }

    public String toHexByte(int x) {
        int i = x % 256;
        StringBuilder stringBuilder = new StringBuilder();
        String s = Integer.toBinaryString(i);
        int length = s.length();
        for (int j = 0; j < 8 - length; j++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(s);

        return stringBuilder.toString();
    }

    public String toHexString(int x) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(toHexByte(x >> 24));
        stringBuilder.append(toHexByte(x >> 16));
        stringBuilder.append(toHexByte(x >> 8));
        stringBuilder.append(toHexByte(x));

        return stringBuilder.toString();
    }

    public String toHexString(long x) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(toHexByte((int) (x >> 56)));
        stringBuilder.append(toHexByte((int) ((x >> 48) % 256)));
        stringBuilder.append(toHexByte((int) ((x >> 40) % 256)));
        stringBuilder.append(toHexByte((int) ((x >> 32) % 256)));
        stringBuilder.append(toHexByte((int) ((x >> 24) % 256)));
        stringBuilder.append(toHexByte((int) ((x >> 16) % 256)));
        stringBuilder.append(toHexByte((int) ((x >> 8) % 256)));
        stringBuilder.append(toHexByte((int) (x % 256)));

        return stringBuilder.toString();
    }

    public String toHexString(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            int x = str.charAt(i);
            stringBuilder.append(toHexByte(x));
        }
        return stringBuilder.toString();
    }

}
