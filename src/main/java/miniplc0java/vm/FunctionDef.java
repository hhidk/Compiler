package miniplc0java.vm;

import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.Type;
import miniplc0java.instruction.Instruction;

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

    public String toVmCode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(toHexString(name));
        stringBuilder.append(toHexString(return_slots));
        stringBuilder.append(toHexString(param_slots));
        stringBuilder.append(toHexString(loc_slots));
        stringBuilder.append(toHexString(body_count));
        for (Instruction instruction : body) {
            int optnum = instruction.getOpt().getOptnum();
            stringBuilder.append(toHexByte(optnum));
            Object x = instruction.getX();
            if (x == null) {
                continue;
            }
            if (x instanceof Long) {
                stringBuilder.append(toHexString((long) x));
            } else if (x instanceof Double) {
                stringBuilder.append(toHexString((double) x));
            } else if (x instanceof Integer) {
                stringBuilder.append(toHexString((int) x));
            }
        }

        return stringBuilder.toString();
    }

    public String toHexByte(int x) {
        int i = x % 256;
        StringBuilder stringBuilder = new StringBuilder();
        String s = Integer.toBinaryString(i);
        int length = s.length();
        for (int j = 0; j < 8 - length; j++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(s);

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

    public String toHexString(double x) {
        long l = Double.doubleToRawLongBits(x);
        return toHexString(l);
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
