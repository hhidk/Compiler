package miniplc0java.vm;

import miniplc0java.analyser.SymbolEntry;
import miniplc0java.analyser.Type;

import java.util.ArrayList;

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

    public ArrayList<Byte> toVmCode() {
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.addAll(BytesHandler.handleByte(is_count));
        bytes.addAll(BytesHandler.handleInt(value_count));
        if (type == Type.int_ty || type == Type.double_ty) {
            bytes.addAll(BytesHandler.handleLong(0));
        } else {
            bytes.addAll(BytesHandler.handleString(value));
        }
        return bytes;
    }

}
