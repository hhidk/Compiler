package miniplc0java.vm;

import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolEntry;
import miniplc0java.analyser.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class o0 {
    int magic;
    int version;
    int globals_count;
    List<GlobalDef> globals = new ArrayList<>();
    int functions_count;
    List<FunctionDef> functions = new ArrayList<>();

    public o0(SymbolTable globalTable, HashMap<String, FunctionTable> functionTables) {
        this.magic = 0x72303b3e;
        this.version = 0x00000001;
        this.globals_count = globalTable.size();
        for (Map.Entry<String, SymbolEntry> entry : globalTable.getSymbolMap().entrySet()) {
            GlobalDef globalDef = new GlobalDef(entry.getKey(), entry.getValue());
            this.globals.add(globalDef);
        }
        this.functions_count = functionTables.size();
        for (Map.Entry<String, FunctionTable> entry : functionTables.entrySet()) {
            FunctionDef functionDef = new FunctionDef(entry.getValue());
            this.functions.add(functionDef);
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (GlobalDef globalDef : globals) {
            stringBuilder.append(globalDef.toString());
            stringBuilder.append('\n');
        }
        for (FunctionDef functionDef : functions) {
            stringBuilder.append(functionDef.toString());
            stringBuilder.append('\n');
        }

        return "o0{" +
                "magic=" + magic +
                ", version=" + version +
                ", globals_count=" + globals_count +
                ", functions_count=" + functions_count + '\n' +
                stringBuilder.toString() +
                '}';
    }

    public String toVmCode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toHexString(magic));
        stringBuilder.append(toHexString(version));
        stringBuilder.append(toHexString(globals_count));
        for (GlobalDef globalDef : globals) {
            stringBuilder.append(globalDef.toVmCode());
        }
        stringBuilder.append(toHexString(functions_count));
        for (FunctionDef functionDef : functions) {
            stringBuilder.append(functionDef.toVmCode());
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
