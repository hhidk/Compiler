package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

public class WhileBlock {
    /**
     * 存放当前块中的指令
     */
    Instruction instruction;

    /**
     * 存放指令的偏移
     */
    int offset;

    int level;

    int type; // 0continue 1break

    public WhileBlock(Instruction instruction, int offset, int level, int type) {
        this.instruction = instruction;
        this.offset = offset;
        this.level = level;
        this.type = type;
    }
}
