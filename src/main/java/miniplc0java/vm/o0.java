package miniplc0java.vm;

import java.util.ArrayList;
import java.util.List;

public class o0 {
    int magic = 0x72303b3e;
    int version = 0x00000001;
    char globals_count;
    List<GlobalDef> globals = new ArrayList<>();
    char functions_count;
    List<FunctionDef> functions = new ArrayList<>();
}
