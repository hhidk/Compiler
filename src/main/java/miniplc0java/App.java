package miniplc0java;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

import miniplc0java.analyser.Analyser;
import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolEntry;
import miniplc0java.error.CompileError;
import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;

import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.vm.FunctionDef;
import miniplc0java.vm.GlobalDef;
import miniplc0java.vm.o0;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
    public static void main(String[] args) throws CompileError {
        var argparse = buildArgparse();
        Namespace result;
        try {
            result = argparse.parseArgs(args);
        } catch (ArgumentParserException e1) {
            argparse.handleError(e1);
            return;
        }

        var inputFileName = result.getString("input");
        var outputFileName = result.getString("output");

        InputStream input;
        if (inputFileName.equals("-")) {
            input = System.in;
        } else {
            try {
                input = new FileInputStream(inputFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find input file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        PrintStream output;
        if (outputFileName.equals("-")) {
            output = System.out;
        } else {
            try {
                output = new PrintStream(new FileOutputStream(outputFileName));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);

        var analyzer = new Analyser(tokenizer);
        Map<String, Object> map = analyzer.analyse();
        HashMap<String, SymbolEntry> globalTable = (HashMap<String, SymbolEntry>) map.get("globalTable");
        HashMap<String, FunctionTable> functionTables = (HashMap<String, FunctionTable>) map.get("functionTables");
        o0 o00 = new o0(globalTable, functionTables);

        output.print(o00.toString());
        System.out.println(o00.toString());

//        if (result.getBoolean("tokenize")) {
//            // tokenize
//            var tokens = new ArrayList<Token>();
//            try {
//                while (true) {
//                    var token = tokenizer.nextToken();
//                    if (token.getTokenType().equals(TokenType.EOF)) {
//                        break;
//                    }
//                    tokens.add(token);
//                }
//            } catch (Exception e) {
//                // 遇到错误不输出，直接退出
//                System.err.println(e);
//                System.exit(0);
//                return;
//            }
//            for (Token token : tokens) {
//                output.println(token.toString());
//            }
//        } else if (result.getBoolean("analyse")) {
//            // analyze
//            var analyzer = new Analyser(tokenizer);
//            Map<String, Object> map = analyzer.analyse();
//            HashMap<String, SymbolEntry> globalTable = (HashMap<String, SymbolEntry>) map.get("globalTable");
//            HashMap<String, FunctionTable> functionTables = (HashMap<String, FunctionTable>) map.get("functionTables");
//            o0 o00 = new o0(globalTable, functionTables);
//            // printO0(o00, output);
//        } else {
//            System.err.println("Please specify either '--analyse' or '--tokenize'.");
//            System.exit(3);
//        }
    }

    private static ArgumentParser buildArgparse() {
        var builder = ArgumentParsers.newFor("miniplc0-java");
        var parser = builder.build();
        parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
        parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
        parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
                .action(Arguments.store());
        parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
        return parser;
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
