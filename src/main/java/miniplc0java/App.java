package miniplc0java;

import java.io.*;
import java.util.*;

import miniplc0java.analyser.Analyser;
import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolTable;
import miniplc0java.error.CompileError;
import miniplc0java.tokenizer.StringIter;

import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.vm.o0;

public class App {
    public static void main(String[] args) throws CompileError, IOException {

        var inputFileName = args[1];
        var outputFileName = args[3];

        InputStream input;
        try {
            input = new FileInputStream(inputFileName);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find input file.");
            e.printStackTrace();
            System.exit(2);
            return;
        }

        FileOutputStream output;
        try {
            output = new FileOutputStream(outputFileName);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open output file.");
            e.printStackTrace();
            System.exit(2);
            return;
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var globalTable = new SymbolTable();
        HashMap<String, FunctionTable> functionTables = new LinkedHashMap<>();

        var analyzer = new Analyser(tokenizer, globalTable, functionTables);
        analyzer.analyse();
        o0 o00 = new o0(globalTable, functionTables);

        System.out.println(o00.toString());
        System.out.println(o00.toVmCode());
        for (Byte b : o00.toVmCode()) {
            output.write(b);
        }
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }

}
