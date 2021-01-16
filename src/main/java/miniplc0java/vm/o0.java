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

    public ArrayList<Byte> toVmCode() {
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.addAll(BytesHandler.handleInt(magic));
        bytes.addAll(BytesHandler.handleInt(version));
        bytes.addAll(BytesHandler.handleInt(globals_count));
        for (GlobalDef globalDef : globals) {
            bytes.addAll(globalDef.toVmCode());
        }
        bytes.addAll(BytesHandler.handleInt(functions_count));
        for (FunctionDef functionDef : functions) {
            bytes.addAll(functionDef.toVmCode());
        }
        return bytes;
    }



}
