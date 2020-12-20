package miniplc0java.vm;

import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class o0 {
    int magic;
    int version;
    char globals_count;
    List<GlobalDef> globals = new ArrayList<>();
    char functions_count;
    List<FunctionDef> functions = new ArrayList<>();

    public o0(HashMap<String, SymbolEntry> globalTable, HashMap<String, FunctionTable> functionTables) {
        this.magic = 0x72303b3e;
        this.version = 0x00000001;
        this.globals_count = (char) globalTable.size();
        for (Map.Entry<String, SymbolEntry> entry : globalTable.entrySet()) {
            GlobalDef globalDef = new GlobalDef(entry.getValue());
            this.globals.add(globalDef);
        }
        this.functions_count = (char) functionTables.size();
        for (Map.Entry<String, FunctionTable> entry : functionTables.entrySet()) {
            FunctionDef functionDef = new FunctionDef(entry.getValue());
            this.functions.add(functionDef);
        }
    }
}
