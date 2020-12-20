package miniplc0java.vm;

import miniplc0java.analyser.SymbolEntry;

public class GlobalDef {
    char is_count;
    int value_count;
    // Object value;

    public GlobalDef(SymbolEntry symbolEntry) {
        this.is_count = (char) symbolEntry.getOrder();
        if (symbolEntry.getType().equals("int") || symbolEntry.getType().equals(("double")))
            this.value_count = 8;
        else
            this.value_count = symbolEntry.getValue_count();
    }

}
