package miniplc0java.analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    int def; //0变量，1函数，2临时变量
    Type type;
    int scope; //0全局，1参数，2局部
    int order;

    public SymbolEntry(boolean isConstant, boolean isDeclared, int def, Type type, int scope, int order) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
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
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }


    public void setType(Type type) {
        this.type = type;
    }
}
