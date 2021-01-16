package miniplc0java.vm;

import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.Type;
import miniplc0java.instruction.Instruction;

import java.util.ArrayList;
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
    // 函数体长度
    int body_count;
    // 函数体
    List<Instruction> body;

    public FunctionDef(FunctionTable functionTable) {
        this.name = functionTable.getOrder();
        if (functionTable.getType() == Type.void_ty)
            this.return_slots = 0;
        else
            this.return_slots = 1;
        this.param_slots = functionTable.getArgs();
        this.loc_slots = functionTable.getLocals();
        this.body = functionTable.getBody();
        this.body_count = this.body.size();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Instruction instruction : body) {
            stringBuilder.append(instruction);
            stringBuilder.append(' ');
        }

        return "FunctionDef{" +
                "name=" + name +
                ", return_slots=" + return_slots +
                ", param_slots=" + param_slots +
                ", loc_slots=" + loc_slots +
                ", body_count=" + body_count +
                ", body=[" + stringBuilder.toString() + ']' +
                '}';
    }

    public ArrayList<Byte> toVmCode() {
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.addAll(BytesHandler.handleInt(name));
        bytes.addAll(BytesHandler.handleInt(return_slots));
        bytes.addAll(BytesHandler.handleInt(param_slots));
        bytes.addAll(BytesHandler.handleInt(loc_slots));
        bytes.addAll(BytesHandler.handleInt(body_count));
        for (Instruction instruction : body) {
            int optnum = instruction.getOpt().getOptnum();
            bytes.addAll(BytesHandler.handleByte(optnum));
            Object x = instruction.getX();
            if (x == null) {
                continue;
            }
            if (x instanceof Long) {
                bytes.addAll(BytesHandler.handleLong((long) x));
            } else if (x instanceof Double) {
                bytes.addAll(BytesHandler.handleDouble((double) x));
            } else if (x instanceof Integer) {
                bytes.addAll(BytesHandler.handleInt((int) x));
            }
        }

        return bytes;
    }

}
