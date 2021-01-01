package miniplc0java.analyser;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class SymbolTable {
    public SymbolTable upperTable;
    HashMap<String, SymbolEntry> symbolMap;

    public SymbolTable() {
        this.upperTable = null;
        this.symbolMap = new LinkedHashMap<>();
    }

    public SymbolTable(SymbolTable upperTable) {
        this.upperTable = upperTable;
        this.symbolMap = new LinkedHashMap<>();
    }

    public HashMap<String, SymbolEntry> getSymbolMap() {
        return symbolMap;
    }

    public void put(String name, SymbolEntry symbolEntry) {
        symbolMap.put(name, symbolEntry);
    }

    public void putAllArgs(HashMap<String, SymbolEntry> map) {
        symbolMap.putAll(map);
    }

    public SymbolEntry get(String name) {
        SymbolEntry symbolEntry;
        SymbolTable symbolTable = this;
        while ((symbolEntry = symbolTable.symbolMap.get(name)) == null) {
            symbolTable = symbolTable.upperTable;
            if (symbolTable == null)
                break;
        }
        return symbolEntry;
    }

    public SymbolEntry getCurrent(String name) {
        return symbolMap.get(name);
    }

    public int size() {
        return symbolMap.size();
    }
}
