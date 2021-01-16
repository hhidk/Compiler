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
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
    public static void main(String[] args) throws CompileError, IOException {
        var argparse = buildArgparse();
        Namespace result;
        try {
            result = argparse.parseArgs(args);
        } catch (ArgumentParserException e1) {
            argparse.handleError(e1);
            return;
        }

        var inputFileName = result.getString("input");
        var outputFileName = result.getString("asm");
        System.out.println(inputFileName + " " + outputFileName);
//        var inputFileName = args[0];
//        var outputFileName = args[1];

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
            // output = new PrintStream(new FileOutputStream(outputFileName));
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

    private static ArgumentParser buildArgparse() {
        var builder = ArgumentParsers.newFor("miniplc0-java");
        var parser = builder.build();
        parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
        parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
        parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("asm")
                .action(Arguments.store());
        parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
        return parser;
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }

}
