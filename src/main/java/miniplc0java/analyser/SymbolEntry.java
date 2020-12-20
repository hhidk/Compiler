package miniplc0java.analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    int stackOffset;
    int def; //0变量，1函数
    String type; //0void，1int，2double
    int scope; //0全局，1参数，2局部
    int order;
    int value_count;

    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared, int stackOffset, int def, String type, int scope, int order) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.def = def;
        this.type = type;
        this.scope = scope;
        this.order = order;
    }

    public SymbolEntry(boolean isConstant, boolean isDeclared, int stackOffset, int def, String type, int scope, int order, int value_count) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.def = def;
        this.type = type;
        this.scope = scope;
        this.order = order;
        this.value_count = value_count;
    }

    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    public int getOrder() {
        return order;
    }

    public String getType() {
        return type;
    }

    public int getValue_count() {
        return value_count;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
