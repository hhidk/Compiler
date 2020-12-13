package miniplc0java.vm;

import miniplc0java.analyser.SymbolEntry;
import miniplc0java.instruction.Instruction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class FunctionDef {
    // 函数名称在全局变量中的位置
    int name;
    // 返回值占据的slot数
    int return_slots;
    // 参数占据的slot数
    int param_slots;
    // 局部变量占据的 slot 数
    int loc_slots;
    // 函数体
    List<Instruction> body;
    // 参数表
    HashMap<String, SymbolEntry> argsTable = new LinkedHashMap<String, SymbolEntry>();
    // 局部变量表
    HashMap<String, SymbolEntry> localTable = new LinkedHashMap<String, SymbolEntry>();

    public HashMap getArgsTable() {
        return this.argsTable;
    }

    public HashMap getLocalTable() {
        return this.localTable;
    }
}
