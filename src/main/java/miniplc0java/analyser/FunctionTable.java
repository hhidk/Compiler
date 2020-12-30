package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.*;

public class FunctionTable {
    // 函数在全局变量表中的序号
    int order;
    // 返回值类型
    Type type;
    // 函数体
    List<Instruction> body;
    // 局部变量个数
    int locals;
    // 参数个数
    int args;

    public FunctionTable(int order) {
        this.order = order;
        this.type = Type.void_ty;
        this.body = new ArrayList<>();
        this.locals = 0;
        this.args = 0;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public Type getType() {
        return type;
    }

    public int getLocals() {
        return locals;
    }

    public int getArgs() {
        return args;
    }

    public List<Instruction> getBody() {
        return body;
    }

    public boolean isGlobal() {
        if (order == 0)
            return true;
        else
            return false;
    }
}
