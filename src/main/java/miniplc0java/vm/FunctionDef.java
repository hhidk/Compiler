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
}
