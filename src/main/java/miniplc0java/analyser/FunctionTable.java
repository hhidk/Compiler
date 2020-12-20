package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.*;

public class FunctionTable {
    // 函数在全局变量表中的序号
    int order;
    // 返回值类型
    String type;
    // 函数体
    List<Instruction> body;
    // 参数表
    HashMap<String, SymbolEntry> argsTable;
    // 局部变量表
    HashMap<String, SymbolEntry> localTable;

    public FunctionTable(int order) {
        this.order = order;
        this.body = new ArrayList<>();
        this.argsTable = new LinkedHashMap<>();
        this.localTable = new LinkedHashMap<>();
    }

    public void setType(String type) {
        this.type = type;
    }
}
