package miniplc0java.vm;

import miniplc0java.analyser.SymbolEntry;

public class GlobalDef {
    byte is_count;
    int value_count;
    String value;

    String type;

    public GlobalDef(String name, SymbolEntry symbolEntry) {
        this.is_count = (byte) symbolEntry.getOrder();
        this.type = symbolEntry.getType();
        if (symbolEntry.getType().equals("int") || symbolEntry.getType().equals(("double"))) {
            this.value_count = 8;
        } else {
            this.value_count = name.length();
            this.value = name;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toHexByte(is_count));
        stringBuilder.append(toHexString(value_count));
        if (type.equals("int") || type.equals("double")) {
            stringBuilder.append(toHexString(0));
        } else {
            stringBuilder.append(toHexString(value));
        }
        return stringBuilder.toString();
    }

    public String toHexByte(int x) {
        int i = x % 256;
        StringBuilder stringBuilder = new StringBuilder();
        if (i < 16) {
            stringBuilder.append(0);
            stringBuilder.append(Integer.toHexString(i));
        } else if (i < 128) {
            stringBuilder.append(Integer.toHexString(i));
        } else {
            stringBuilder.append(Integer.toHexString(i));
        }
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
