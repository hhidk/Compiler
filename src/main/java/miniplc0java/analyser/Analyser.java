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
    HashMap<String, FunctionTable> functionTables;

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
        this.functionTables = new LinkedHashMap<>();
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
        functionTables.put("_start",functionTable);
        globalTable.put("_start", new SymbolEntry(false, true, getNextVariableOffset(), 1, "void", 0, 0));
        return functionTable;
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

    public SymbolEntry getSymbolEntry(String name) {
        if (functionTable.argsTable.get(name) != null) {
            return functionTable.argsTable.get(name);
        } else if (functionTable.localTable.get(name) != null) {
            return functionTable.localTable.get(name);
        } else {
            return globalTable.get(name);
        }
    }

    public SymbolEntry addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos, String type, boolean isArg) throws AnalyzeError {
        if (getSymbolEntry(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        int order;
        SymbolEntry symbol;
        if (this.functionTable.order == 0) {
            order = globalTable.size();
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 0, order);
            globalTable.put(name, symbol);
        } else if (isArg) {
            order = this.functionTable.argsTable.size();
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order);
            this.functionTable.argsTable.put(name, symbol);
        } else {
            order = this.functionTable.localTable.size();
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order);
            this.functionTable.localTable.put(name, symbol);
        }
        return symbol;
    }

    public SymbolEntry addString(String value) {
        int order = globalTable.size();
        SymbolEntry symbol = new SymbolEntry(true, true, getNextVariableOffset(), 1, "string", 0, order);
        globalTable.put(value, symbol);
        return symbol;
    }

    public Instruction addInstruction(Operation opt, Integer x) {
        Instruction instruction = new Instruction(opt, x);
        this.functionTable.body.add(instruction);
        return instruction;
    }

    public void addInstruction(Operation opt, Long x) {
        this.functionTable.body.add(new Instruction(opt, x));
    }

    public void addInstruction(Operation opt, Double x) {
        this.functionTable.body.add(new Instruction(opt, x));
    }

    public void addInstruction(Operation opt) {
        this.functionTable.body.add(new Instruction(opt));
    }

    public void popInstruction() {
        this.functionTable.body.remove(this.functionTable.body.size() - 1);
    }

    public int getInstructionOffset() {
        return this.functionTable.body.size();
    }

    public void startFunction(String name, Pos curPos) throws AnalyzeError {
        if (globalTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        SymbolEntry symbolEntry = addString(name);
        this.functionTable = new FunctionTable(symbolEntry.order);
    }

    public void endFunction(String name, String type) {
        this.functionTable.setType(type);
        functionTables.put(name, functionTable);
        this.functionTable = functionTables.get("_start");
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
        var nameToken = expect(TokenType.IDENT);

        String name = (String) nameToken.getValue();
        startFunction(name, nameToken.getStartPos());

        expect(TokenType.L_PAREN);
        if (peek().getTokenType() == TokenType.CONST_KW ||
            peek().getTokenType() == TokenType.IDENT) {
            analyseFunctionParamList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        String type = analyseType();
        analyseBlockStmt();

        endFunction(name, type);
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

        boolean isConstant = false, isInitialized = true, isArg = true;
        if (nextIf(TokenType.CONST_KW) != null) {
            isConstant = true;
        }
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        String type = analyseType();

        String name = (String) nameToken.getValue();
        addSymbol(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);
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
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        String type = analyseType();
        if (type.equals("void"))
            throw new Error("Illegal type");

        boolean isConstant = false, isInitialized = false, isArg = false;
        String name = (String) nameToken.getValue();
        SymbolEntry symbolEntry = addSymbol(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);

        if (symbolEntry.scope == 0) {
            addInstruction(Operation.globa, symbolEntry.order);
        } else if (symbolEntry.scope == 2) {
            addInstruction(Operation.loca, symbolEntry.order);
        }

        if (peek().getTokenType() == TokenType.ASSIGN) {
            expect(TokenType.ASSIGN);
            String exprType = analyseExpr();
            if (!type.equals(exprType))
                throw new Error("Illegal declaration");
            symbolEntry.setInitialized(true);
            addInstruction(Operation.store64);
        }
        expect(TokenType.SEMICOLON);


    }

    private void analyseConstDeclStmt() throws CompileError {
        // const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'

        expect(TokenType.CONST_KW);
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        String type = analyseType();
        if (type.equals("void"))
            throw new Error("Illegal type");

        boolean isConstant = true, isInitialized = true, isArg = false;
        String name = (String) nameToken.getValue();
        SymbolEntry symbolEntry = addSymbol(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);

        if (symbolEntry.scope == 0) {
            addInstruction(Operation.globa, symbolEntry.order);
        } else if (symbolEntry.scope == 2) {
            addInstruction(Operation.loca, symbolEntry.order);
        }

        expect(TokenType.ASSIGN);
        String exprType = analyseExpr();
        if (!type.equals(exprType))
            throw new Error("Illegal declaration");
        expect(TokenType.SEMICOLON);
        addInstruction(Operation.store64);
    }

    private void analyseIfStmt() throws CompileError {
        // if_stmt -> 'if' expr block_stmt ('else' (block_stmt | if_stmt))?

        expect(TokenType.IF_KW);
        analyseExpr();
        Instruction br1 = addInstruction(Operation.brfalse, 0);
        int offset1 = getInstructionOffset();

        analyseBlockStmt();
        Instruction br2 = addInstruction(Operation.brfalse, 0);
        int offset2 = getInstructionOffset();
        br1.setX(offset2 - offset1);

        if (nextIf(TokenType.ELSE_KW) != null) {
            if (peek().getTokenType() == TokenType.L_BRACE) {
                analyseBlockStmt();
            } else if (peek().getTokenType() == TokenType.IF_KW) {
                analyseIfStmt();
            } else {
                throw new Error("If Statement not completed");
            }
        }
        int offset3 = getInstructionOffset();
        br2.setX(offset3 - offset2);

    }

    private void analyseWhileStmt() throws CompileError {
        // while_stmt -> 'while' expr block_stmt

        expect(TokenType.WHILE_KW);
        int offset1 = getInstructionOffset();

        analyseExpr();
        Instruction br1 = addInstruction(Operation.brfalse, 0);
        int offset2 = getInstructionOffset();

        analyseBlockStmt();
        Instruction br2 = addInstruction(Operation.br, 0);
        int offset3 = getInstructionOffset();
        br2.setX(offset1 - offset3);
        br1.setX(offset3 - offset2);
    }

    private void analyseReturnStmt() throws CompileError {
        // return_stmt -> 'return' expr? ';'

        expect(TokenType.RETURN_KW);
        String exprType = analyseExpr();
        if (!this.functionTable.type.equals(exprType))
            throw new Error("Illegal return type");
        expect(TokenType.SEMICOLON);

        addInstruction(Operation.ret);
    }

    private String analyseExpr() throws CompileError {
        // expr -> operator_expr | negate_expr | assign_expr | as_expr
        //       | call_expr | literal_expr | ident_expr | group_expr

        // operator_expr -> expr binary_operator expr
        // negate_expr -> '-' expr
        // assign_expr -> l_expr '=' expr
        // as_expr -> expr 'as' ty
        // call_expr -> IDENT '(' call_param_list? ')'
        // literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
        // ident_expr -> IDENT
        // group_expr -> '(' expr ')'

        // todo: 尚未考虑连续等于的情况
        String ltype = analyseExpr1();
        if (nextIf(TokenType.ASSIGN) != null) {
            popInstruction();
            String rtype = analyseExpr();

            if (!ltype.equals("int") && !ltype.equals("double"))
                throw new Error("Illegal assign expr");
            if (!ltype.equals(rtype))
                throw new Error("Cannot assign value of different type");
            addInstruction(Operation.store64);
        }
        return ltype;
    }

    private String analyseExpr1() throws CompileError {
        String ltype = analyseExpr2();
        TokenType tt = peek().getTokenType();
        while (tt == TokenType.GT || tt == TokenType.LT || tt == TokenType.GE
            || tt == TokenType.LE || tt == TokenType.EQ || tt == TokenType.NEQ) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            String rtype = analyseExpr2();

            if (!ltype.equals(rtype))
                throw new Error("Cannot compare different type");
            if (ltype.equals("int")) {
                addInstruction(Operation.cmpi);
            } else if (ltype.equals("double")) {
                addInstruction(Operation.cmpf);
            } else {
                throw new Error("Illegal type for comparison");
            }
            switch (opt) {
                case GT:
                    addInstruction(Operation.setgt);
                    break;
                case LT:
                    addInstruction(Operation.setlt);
                    break;
                case GE:
                    addInstruction(Operation.setlt);
                    addInstruction(Operation.not);
                    break;
                case LE:
                    addInstruction(Operation.setgt);
                    addInstruction(Operation.not);
                    break;
                case EQ:
                    addInstruction(Operation.not);
                    break;
                case NEQ:
                default:
                    break;
            }
        }
        return ltype;
    }

    private String analyseExpr2() throws CompileError {
        String ltype = analyseExpr3();
        TokenType tt = peek().getTokenType();
        while (tt == TokenType.PLUS || tt == TokenType.MINUS) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            String rtype = analyseExpr3();

            if (!ltype.equals(rtype))
                throw new Error("Illegal operation");
            if (opt == TokenType.PLUS && ltype.equals("int")) {
                addInstruction(Operation.addi);
            } else if (opt == TokenType.PLUS && ltype.equals("double")) {
                addInstruction(Operation.addf);
            } else if (opt == TokenType.MINUS && ltype.equals("int")) {
                addInstruction(Operation.subi);
            } else if (opt == TokenType.MINUS && ltype.equals("double")) {
                addInstruction(Operation.subf);
            } else {
                throw new Error("Illegal operation");
            }
        }
        return ltype;
    }

    private String analyseExpr3() throws CompileError {
        String ltype = analyseExpr4();
        TokenType tt = peek().getTokenType();
        while (tt == TokenType.MUL || tt == TokenType.DIV) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            String rtype = analyseExpr4();

            if (!ltype.equals(rtype))
                throw new Error("Illegal operation");
            if (opt == TokenType.MUL && ltype.equals("int")) {
                addInstruction(Operation.muli);
            } else if (opt == TokenType.MUL && ltype.equals("double")) {
                addInstruction(Operation.mulf);
            } else if (opt == TokenType.DIV && ltype.equals("int")) {
                addInstruction(Operation.divi);
            } else if (opt == TokenType.DIV && ltype.equals("double")) {
                addInstruction(Operation.divf);
            } else {
                throw new Error("Illegal operation");
            }
        }
        return ltype;
    }

    private String analyseExpr4() throws CompileError {
        String type = analyseExpr5();
        while (nextIf(TokenType.AS_KW) != null) {
            String newType = analyseType();
            if (newType.equals("void")) {
                throw new Error("Illegal type transition");
            } else if (type.equals("int") && newType.equals("double")) {
                addInstruction(Operation.itof);
            } else if (type.equals("double") && newType.equals("int")) {
                addInstruction(Operation.ftoi);
            }
            type = newType;
        }
        return type;
    }

    private String analyseExpr5() throws CompileError {
        boolean isNeg = false;
        while (nextIf(TokenType.MINUS) != null) {
            isNeg = true;
        }
        String type = analyseExpr6();
        if (isNeg) {
            if (type == "int") {
                addInstruction(Operation.negi);
            } else if (type == "double") {
                addInstruction(Operation.negf);
            } else {
                throw new Error("Illegal negate");
            }
        }
        return type;
    }

    private String analyseExpr6() throws CompileError {
        TokenType tt = peek().getTokenType();
        String type = null;
        // group expr
        if (nextIf(TokenType.L_PAREN) != null) {
            type = analyseExpr();
            expect(TokenType.R_PAREN);
        }
        // literal expr
        else if (tt == TokenType.UINT_LITERAL || tt == TokenType.CHAR_LITERAL || tt == TokenType.STRING_LITERAL) {
            type = analyseLiteral();
        }
        // call & ident expr
        else if (tt == TokenType.IDENT) {
            var nameToken = next();
            String name = (String) nameToken.getValue();
            // call expr
            // todo: 不知道需不需要判断参数类型吻合
            if (nextIf(TokenType.L_PAREN) != null) {
                if (functionTables.get(name) == null && !isStdlib(name))
                    throw new Error("Illegal function call");

                if (!isStdlib(name)) {
                    addInstruction(Operation.stackalloc, 1);
                }
                if (nextIf(TokenType.R_PAREN) == null) {
                    analyseCallParamList();
                    expect(TokenType.R_PAREN);
                }

                if (!isStdlib(name)) {
                    addInstruction(Operation.callname, functionTables.get(name).order);
                    type = functionTables.get(name).type;
                } else {
                    type = callStdlib(name);
                }
            }
            // ident expr
            else {
                SymbolEntry symbolEntry = getSymbolEntry(name);
                if (symbolEntry == null) {
                    throw new Error("Undefined param");
                } else if (symbolEntry.scope == 0) {
                    addInstruction(Operation.globa, symbolEntry.order);
                } else if (symbolEntry.scope == 1) {
                    addInstruction(Operation.arga, symbolEntry.order);
                } else if (symbolEntry.scope == 2) {
                    addInstruction(Operation.loca, symbolEntry.order);
                }
                addInstruction(Operation.load64);
                type = symbolEntry.type;
            }
        }
        // error
        else {
            throw new Error("Illegal Expression");
        }

        return type;
    }

    private String analyseLiteral() throws CompileError {
        var nameToken = next();
        TokenType tt = nameToken.getTokenType();
        switch (tt) {
            case UINT_LITERAL:
            case CHAR_LITERAL:
                addInstruction(Operation.push, (Long) nameToken.getValue());
                return "int";
            case STRING_LITERAL:
                SymbolEntry symbol = addString((String) nameToken.getValue());
                int order = symbol.order;
                addInstruction(Operation.push, order);
                return "string";
            case DOUBLE_LITERAL:
                addInstruction(Operation.push, (Double) nameToken.getValue());
                return "double";
            default:
                return null;
        }
    }

    private void analyseCallParamList() throws CompileError {
        // call_param_list -> expr (',' expr)*

        analyseExpr();
        while (nextIf(TokenType.COMMA) != null) {
            analyseExpr();
        }
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

    private boolean isStdlib(String name) {
        return name.equals("getint") || name.equals("getdouble") || name.equals("getchar") ||
                name.equals("putint") || name.equals("putdouble") || name.equals("putchar") ||
                name.equals("putstr") || name.equals("putln");
    }

    private String callStdlib(String name) {
        if (name.equals("getint")) {
            addInstruction(Operation.scani);
            return "int";
        } else if (name.equals("getdouble")) {
            addInstruction(Operation.scanf);
            return "double";
        } else if (name.equals("getchar")) {
            addInstruction(Operation.scanc);
            return "int";
        } else if (name.equals("putint")) {
            addInstruction(Operation.printi);
            return "void";
        } else if (name.equals("putdouble")) {
            addInstruction(Operation.printf);
            return "void";
        } else if (name.equals("putchar")) {
            addInstruction(Operation.printc);
            return "void";
        } else if (name.equals("putln")) {
            addInstruction(Operation.println);
            return "void";
        } else if (name.equals("putstr")) {
            addInstruction(Operation.prints);
            return "void";
        } else {
            return "void";
        }
    }
}