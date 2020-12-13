package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;
import miniplc0java.vm.FunctionDef;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;

    /**
     * 全局符号表
     */
    HashMap<String, SymbolEntry> globalTable;

    /**
     * 函数符号表及指令集
     */
    List<FunctionTable> functionTables;

    /**
     * 当前函数
     */
    FunctionTable functionTable;

    /**
     * 当前偷看的 token
     */
    Token peekedToken = null;

    /**
     * 下一个变量的栈偏移
     */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.globalTable = new LinkedHashMap<>();
        this.functionTables = new ArrayList<>();
        this.functionTable = init_start();
    }

    public Map<String, Object> analyse() throws CompileError {
        analyseProgram();
        Map<String, Object> map = new HashMap<>();
        map.put("globalTable", globalTable);
        map.put("functionTables", functionTables);
        return map;
    }

    public FunctionTable init_start() {
        FunctionTable functionTable = new FunctionTable(0);
        functionTables.add(functionTable);
        globalTable.put("_start", new SymbolEntry(false, true, getNextVariableOffset(), 1, 0, 0, 0));
        return functionTable;
    }

    public SymbolEntry getSymbolEntry(String name) {
        if (functionTable.argsTable.get(name) != null) {
            return functionTable.argsTable.get(name);
        } else if (functionTable.localTable.get(name) != null) {
            return functionTable.localTable.get(name);
        } else {
            return globalTable.get(name);
        }
    }

    public void startFunction(String name, Pos curPos) throws AnalyzeError {
        if (globalTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        int order = globalTable.size();
        globalTable.put(name, new SymbolEntry(false, true, getNextVariableOffset(), 1, 0, 0, order));
        this.functionTable = new FunctionTable(order);
    }

    public void endFunction() {
        functionTables.add(functionTable);
        this.functionTable = functionTables.get(0);
    }

    public void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos, int type, boolean isArg) throws AnalyzeError {
        if (getSymbolEntry(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        int order;
        if (this.functionTable.order == 0) {
            order = globalTable.size();
            globalTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 0, order));
        } else if (isArg) {
            order = this.functionTable.argsTable.size();
            this.functionTable.argsTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order));
        } else {
            order = this.functionTable.localTable.size();
            this.functionTable.localTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order));
        }
    }

    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    private void analyseProgram() throws CompileError {
        // program -> item *
        // item -> function | decl_stmt
        // decl_stmt -> let_decl_stmt | const_decl_stmt

        while(true) {
            var peeked = peek();
            if (peeked.getTokenType() == TokenType.FN_KW) {
                analyseFunction();
            }
            else if (peeked.getTokenType() == TokenType.LET_KW) {
                analyseLetDeclStmt();
            }
            else if(peeked.getTokenType() == TokenType.CONST_KW) {
                analyseConstDeclStmt();
            }
            else {
                expect(TokenType.EOF);
                break;
            }
        }
    }

    private void analyseFunction() throws CompileError {
        // function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt

        expect(TokenType.FN_KW);
        expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);
        if (peek().getTokenType() == TokenType.CONST_KW ||
            peek().getTokenType() == TokenType.IDENT) {
            analyseFunctionParamList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        String type = analyseType();//todo
        analyseBlockStmt();
    }

    private void analyseFunctionParamList() throws CompileError {
        // function_param_list -> function_param (',' function_param)*

        analyseFunctionParam();
        while (nextIf(TokenType.COMMA) != null) {
            analyseFunctionParam();
        }
    }

    private void analyseFunctionParam() throws CompileError {
        // function_param -> 'const'? IDENT ':' ty

        if (peek().getTokenType() == TokenType.CONST_KW) {
            //todo:const not implemented
            next();
        }
        expect(TokenType.IDENT);//todo: param name not implemented
        expect(TokenType.COLON);
        String type = analyseType();//todo
    }

    private void analyseBlockStmt() throws CompileError {
        // block_stmt -> '{' stmt* '}'
        // stmt -> expr_stmt | decl_stmt | if_stmt | while_stmt |
        //          return_stmt | block_stmt | empty_stmt

        expect(TokenType.L_BRACE);
        TokenType tokenType = peek().getTokenType();
        if (tokenType == TokenType.LET_KW) {
            analyseLetDeclStmt();
        } else if (tokenType == TokenType.CONST_KW) {
            analyseConstDeclStmt();
        } else if (tokenType == TokenType.IF_KW) {
            analyseIfStmt();
        } else if (tokenType == TokenType.WHILE_KW) {
            analyseWhileStmt();
        } else if (tokenType == TokenType.RETURN_KW) {
            analyseReturnStmt();
        } else if (tokenType == TokenType.L_BRACE) {
            analyseBlockStmt();
        } else if (tokenType == TokenType.SEMICOLON) {
            next();
        } else if (isExpr()){
            analyseExpr();
            expect(TokenType.SEMICOLON);
        } else {
            throw new Error("Not a statement");
        }
        expect(TokenType.R_BRACE);
    }

    private void analyseLetDeclStmt() throws CompileError {
        // let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'

        expect(TokenType.LET_KW);
        expect(TokenType.IDENT);//todo
        expect(TokenType.COLON);
        String type = analyseType();//todo
        if (peek().getTokenType() == TokenType.ASSIGN) {
            expect(TokenType.ASSIGN);
            if (isExpr())
                analyseExpr();
            else
                throw new Error("Expression illegal");
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConstDeclStmt() throws CompileError {
        // const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'

        expect(TokenType.CONST_KW);
        expect(TokenType.IDENT);//todo
        expect(TokenType.COLON);
        String type = analyseType();//todo
        if (peek().getTokenType() == TokenType.ASSIGN) {
            expect(TokenType.ASSIGN);
            if (isExpr())
                analyseExpr();
            else
                throw new Error("Expression illegal");
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseIfStmt() throws CompileError {
        // if_stmt -> 'if' expr block_stmt ('else' (block_stmt | if_stmt))?

        expect(TokenType.IF_KW);
        if (isExpr())
            analyseExpr();
        else
            throw new Error("Expression illegal");
        if (nextIf(TokenType.ELSE_KW) != null) {
            if (peek().getTokenType() == TokenType.L_BRACE) {
                analyseBlockStmt();
            } else if (peek().getTokenType() == TokenType.IF_KW) {
                analyseIfStmt();
            } else {
                throw new Error("If Statement not completed");
            }
        }
    }

    private void analyseWhileStmt() throws CompileError {
        // while_stmt -> 'while' expr block_stmt

        expect(TokenType.WHILE_KW);
        if (isExpr())
            analyseExpr();
        else
            throw new Error("Expression illegal");
        analyseBlockStmt();
    }

    private void analyseReturnStmt() throws CompileError {
        // return_stmt -> 'return' expr? ';'

        expect(TokenType.RETURN_KW);
        if (isExpr())
            analyseExpr();
        expect(TokenType.SEMICOLON);
    }

    private void analyseExpr() throws CompileError {
        // expr -> operator_expr | negate_expr | assign_expr | as_expr
        //       | call_expr | literal_expr | ident_expr | group_expr

        analyseExpr1();
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.ASSIGN) {
            analyseExpr1();
        }
        // todo: symbol
    }

    private void analyseExpr1() throws CompileError {
        analyseExpr2();
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.GT || tt == TokenType.LT || tt == TokenType.GE
            || tt == TokenType.LE || tt == TokenType.EQ || tt == TokenType.NEQ) {
            analyseExpr2();
        }
        // todo: symbol
    }

    private void analyseExpr2() throws CompileError {
        analyseExpr3();
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.PLUS || tt == TokenType.MINUS) {
            analyseExpr3();
        }
        // todo: symbol
    }

    private void analyseExpr3() throws CompileError {
        analyseExpr4();
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.MUL || tt == TokenType.DIV) {
            analyseExpr4();
        }
        // todo: symbol
    }

    private void analyseExpr4() throws CompileError {
        analyseExpr5();
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.AS_KW) {
            String type = analyseType();
        }
        // todo: symbol
    }

    private void analyseExpr5() throws CompileError {
        TokenType tt = peek().getTokenType();
        boolean isNeg = false;
        if (tt == TokenType.MINUS) {
            next();
            isNeg = true;
        }
        analyseExpr6();
        // todo: symbol
    }

    private void analyseExpr6() throws CompileError {
        TokenType tt = peek().getTokenType();
        if (tt == TokenType.L_PAREN) {
            next();
            analyseExpr();
            expect(TokenType.R_PAREN);
        } else if (tt == TokenType.UINT_LITERAL || tt == TokenType.CHAR_LITERAL || tt == TokenType.STRING_LITERAL) {
            next();
        } else if (tt == TokenType.IDENT) {
            next();
            if (peek().getTokenType() == TokenType.L_PAREN) {
                next();
                analyseCallParamList();
                expect(TokenType.R_PAREN);
            }
        } else {
            throw new Error("yinggaijinbulai");
        }
        // todo: symbol
    }

    private void analyseCallParamList() throws CompileError {
        throw new Error("Not implemented");
    }

    private String analyseType() throws CompileError {
        // ty -> IDENT
        // IDENT:void/int(/double)

        Token token = expect(TokenType.IDENT);
        String value = (String) token.getValue();
        if (value.equals("void") || value.equals("int") || value.equals("double")) {
            return value;
        } else {
            throw new Error("Type illegal");
        }
    }

    private boolean isExpr() throws CompileError {
        TokenType tt = peek().getTokenType();
        return tt == TokenType.MINUS  || tt == TokenType.IDENT || tt == TokenType.L_PAREN ||
                tt == TokenType.UINT_LITERAL || tt == TokenType.DOUBLE_LITERAL ||
                tt == TokenType.STRING_LITERAL || tt == TokenType.CHAR_LITERAL;
    }
}