package miniplc0java.analyser;

import miniplc0java.vm.FunctionDef;

import java.util.*;

public class SymbolTable {

    /**
     * 全局符号表
     */
    HashMap<String, SymbolEntry> globalTable = new LinkedHashMap<>();

    /**
     * 当前函数
     */
    FunctionDef functionDef = null;

    public void setFunctionDef(FunctionDef functionDef) {
        this.functionDef = functionDef;
    }

    public Object get(String name) {
        if (functionDef.getArgsTable().get(name) != null) {
            return functionDef.getArgsTable().get(name);
        } else if (functionDef.getLocalTable().get(name) != null) {
            return functionDef.getLocalTable().get(name);
        } else {
            return globalTable.get(name);
        }
    }
}
