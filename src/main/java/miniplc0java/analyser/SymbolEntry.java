package miniplc0java.analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    int stackOffset;
    int def; //0变量，1函数，2临时变量
    Type type;
    int scope; //0全局，1参数，2局部
    int order;

    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared, int stackOffset, int def, Type type, int scope, int order) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.def = def;
        this.type = type;
        this.scope = scope;
        this.order = order;
    }

    public SymbolEntry(Type type) {
        this.isConstant = false;
        this.isInitialized = true;
        this.def = 2;
        this.type = type;
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

    public Type getType() {
        return type;
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

    public void setType(Type type) {
        this.type = type;
    }
}
