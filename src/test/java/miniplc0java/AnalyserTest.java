package miniplc0java;

import miniplc0java.analyser.Analyser;
import miniplc0java.analyser.FunctionTable;
import miniplc0java.analyser.SymbolEntry;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.vm.o0;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AnalyserTest {


    @Test
    public void test() {
        InputStream input;
        String inputFileName = "E:\\Learning\\编译原理\\作业\\Compiler\\src\\main\\java\\miniplc0java\\in.txt";
        String outputFileName = "E:\\Learning\\编译原理\\作业\\Compiler\\src\\main\\java\\miniplc0java\\out.txt";
        try {
            input = new FileInputStream(inputFileName);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find input file.");
            e.printStackTrace();
            System.exit(2);
            return;
        }

        PrintStream output;
        try {
            output = new PrintStream(new FileOutputStream(outputFileName));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open output file.");
            e.printStackTrace();
            System.exit(2);
            return;
        }

        try {
            Scanner scanner;
            scanner = new Scanner(input);
            var iter = new StringIter(scanner);
            var tokenizer = tokenize(iter);

//            Token token;
//            while ((token = tokenizer.nextToken()).getTokenType() != TokenType.EOF) {
//                output.println(token.getTokenType());
//            }

            var analyzer = new Analyser(tokenizer);
            Map<String, Object> map = analyzer.analyse();
            HashMap<String, SymbolEntry> globalTable = (HashMap<String, SymbolEntry>) map.get("globalTable");
            HashMap<String, FunctionTable> functionTables = (HashMap<String, FunctionTable>) map.get("functionTables");
            o0 o00 = new o0(globalTable, functionTables);

            output.print(o00.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }

}
