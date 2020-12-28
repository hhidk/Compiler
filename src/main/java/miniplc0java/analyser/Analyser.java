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
        int order = functionTables.get("main").order;
        addInstruction(Operation.call, order);
        Map<String, Object> map = new HashMap<>();
        map.put("globalTable", globalTable);
        map.put("functionTables", functionTables);
        return map;
    }

    public FunctionTable init_start() {
        FunctionTable functionTable = new FunctionTable(0);
        functionTables.put("_start",functionTable);
        globalTable.put("_start", new SymbolEntry(false, true, getNextVariableOffset(), 1, Type.void_ty, 0, 0));
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

    public SymbolEntry addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos, Type type, boolean isArg) throws AnalyzeError {
        int order;
        SymbolEntry symbol;
        // 插入全局变量
        if (this.functionTable.order == 0) {
            if (globalTable.get(name) != null)
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
            order = globalTable.size();
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 0, order);
            globalTable.put(name, symbol);
        }
        // 插入形参
        else if (isArg) {
            if (functionTable.argsTable.get(name) != null)
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
            order = this.functionTable.argsTable.size() + 1; // return value is arg 0
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order);
            this.functionTable.argsTable.put(name, symbol);
        }
        // 插入局部变量
        else {
            if (functionTable.localTable.get(name) != null)
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
            order = this.functionTable.localTable.size();
            symbol = new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(), 1, type, 1, order);
            this.functionTable.localTable.put(name, symbol);
        }
        return symbol;
    }

    public SymbolEntry addString(String value) {
        int order = globalTable.size();
        SymbolEntry symbol = new SymbolEntry(true, true, getNextVariableOffset(), 1, Type.string_ty, 0, order);
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
        functionTables.put(name, functionTable);
    }

    public void endFunction() {
        this.functionTable = functionTables.get("_start");
    }

    private void analyseProgram() throws CompileError {
        // program -> item *
        // item -> function | decl_stmt
        // decl_stmt -> let_decl_stmt | const_decl_stmt

        System.out.println("analyseProgram()");
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

        System.out.println("analyseFunction()");
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
        Type type = analyseType();
        functionTable.setType(type);
        analyseBlockStmt();

        endFunction();
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
        Type type = analyseType();

        String name = (String) nameToken.getValue();
        addSymbol(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);
    }

    private void analyseBlockStmt() throws CompileError {
        // block_stmt -> '{' stmt* '}'
        // stmt -> expr_stmt | decl_stmt | if_stmt | while_stmt |
        //          return_stmt | block_stmt | empty_stmt

        expect(TokenType.L_BRACE);
        TokenType tokenType = peek().getTokenType();
        while (tokenType == TokenType.LET_KW || tokenType == TokenType.CONST_KW || tokenType == TokenType.IF_KW || tokenType == TokenType.WHILE_KW
                || tokenType == TokenType.RETURN_KW || tokenType == TokenType.L_BRACE || tokenType == TokenType.SEMICOLON || isExpr()) {
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
            tokenType = peek().getTokenType();
        }
        expect(TokenType.R_BRACE);
    }

    private void analyseLetDeclStmt() throws CompileError {
        // let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'

        System.out.println("analyseLetDeclStmt()");
        expect(TokenType.LET_KW);
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Type type = analyseType();
        if (type == Type.void_ty)
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
            SymbolEntry exprSymbolEntry = analyseExpr();
            Type exprType = exprSymbolEntry.getType();
            if (type != exprType)
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
        Type type = analyseType();
        if (type == Type.void_ty)
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
        SymbolEntry exprSymbolEntry = analyseExpr();
        Type exprType = exprSymbolEntry.getType();
        if (type != exprType)
            throw new Error("Illegal declaration");
        addInstruction(Operation.store64);

        expect(TokenType.SEMICOLON);
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
        if (nextIf(TokenType.SEMICOLON) == null) {
            addInstruction(Operation.arga, 0);

            SymbolEntry symbolEntry = analyseExpr();
            Type type = symbolEntry.getType();
            expect(TokenType.SEMICOLON);

            if (!symbolEntry.isInitialized) {
                throw new Error("Return value not initialized");
            } else if (functionTable.type != type) {
                throw new Error("Return type not matched");
            }

            addInstruction(Operation.store64);
        }
        addInstruction(Operation.ret);
    }

    private SymbolEntry analyseExpr() throws CompileError {
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

        System.out.println("expr");
        SymbolEntry lsymbolEntry = analyseExpr1();
        Type ltype = lsymbolEntry.getType();
        if (nextIf(TokenType.ASSIGN) != null) {
            popInstruction();
            SymbolEntry rsymbolEntry = analyseExpr();
            Type rtype = rsymbolEntry.getType();

            // todo: 判断赋值合法性
            if (lsymbolEntry.def == 2) {
                throw new Error("Invalid assignment");
            } else if (lsymbolEntry.isConstant) {
                throw new Error("Assign to constant");
            } else if (!rsymbolEntry.isInitialized) {
                throw new Error("Expression not initialized");
            } else if (ltype != rtype) {
                throw new Error("Assign to different type");
            }

            addInstruction(Operation.store64);
            lsymbolEntry.setInitialized(true);
            return new SymbolEntry(Type.void_ty);
        }
        return lsymbolEntry;
    }

    private SymbolEntry analyseExpr1() throws CompileError {
        System.out.println("expr1");
        SymbolEntry symbolEntry = analyseExpr2();
        Type ltype = symbolEntry.getType();
        while (peek().getTokenType() == TokenType.GT || peek().getTokenType() == TokenType.LT || peek().getTokenType() == TokenType.GE
            || peek().getTokenType() == TokenType.LE || peek().getTokenType() == TokenType.EQ || peek().getTokenType() == TokenType.NEQ) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            SymbolEntry rsymbolEntry = analyseExpr2();
            Type rtype = rsymbolEntry.getType();

            if (!symbolEntry.isInitialized || !rsymbolEntry.isInitialized)
                throw new Error("Expression not initialized");
            else if (ltype != rtype)
                throw new Error("Cannot compare different type");

            if (ltype == Type.int_ty) {
                addInstruction(Operation.cmpi);
            } else if (ltype == Type.double_ty) {
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
                    break;
                default:
                    throw new Error("Illegal operator");
            }
            // 比较表达式的值不能继续使用
            symbolEntry = new SymbolEntry(Type.void_ty);
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr2() throws CompileError {
        System.out.println("expr2");
        SymbolEntry symbolEntry = analyseExpr3();
        Type ltype = symbolEntry.getType();
        while (peek().getTokenType() == TokenType.PLUS || peek().getTokenType() == TokenType.MINUS) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            SymbolEntry rsymbolEntry = analyseExpr3();
            Type rtype = rsymbolEntry.getType();

            if (!symbolEntry.isInitialized || !rsymbolEntry.isInitialized)
                throw new Error("Expression not initialized");
            else if (ltype != rtype)
                throw new Error("Cannot compare different type");

            if (opt == TokenType.PLUS && ltype == Type.int_ty) {
                addInstruction(Operation.addi);
            } else if (opt == TokenType.PLUS && ltype == Type.double_ty) {
                addInstruction(Operation.addf);
            } else if (opt == TokenType.MINUS && ltype == Type.int_ty) {
                addInstruction(Operation.subi);
            } else if (opt == TokenType.MINUS && ltype == Type.double_ty) {
                addInstruction(Operation.subf);
            } else {
                throw new Error("Illegal operation");
            }
            // 表达式变为临时变量
            symbolEntry = new SymbolEntry(ltype);
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr3() throws CompileError {
        System.out.println("expr3");
        SymbolEntry symbolEntry = analyseExpr4();
        Type ltype = symbolEntry.getType();
        while (peek().getTokenType() == TokenType.MUL || peek().getTokenType() == TokenType.DIV) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            SymbolEntry rsymbolEntry = analyseExpr4();
            Type rtype = rsymbolEntry.getType();

            if (!symbolEntry.isInitialized || !rsymbolEntry.isInitialized)
                throw new Error("Expression not initialized");
            else if (ltype != rtype)
                throw new Error("Cannot compare different type");

            if (opt == TokenType.MUL && ltype == Type.int_ty) {
                addInstruction(Operation.muli);
            } else if (opt == TokenType.MUL && ltype == Type.double_ty) {
                addInstruction(Operation.mulf);
            } else if (opt == TokenType.DIV && ltype == Type.int_ty) {
                addInstruction(Operation.divi);
            } else if (opt == TokenType.DIV && ltype == Type.double_ty) {
                addInstruction(Operation.divf);
            } else {
                throw new Error("Illegal operation");
            }
            // 表达式变为临时变量
            symbolEntry = new SymbolEntry(ltype);
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr4() throws CompileError {
        System.out.println("expr4");
        SymbolEntry symbolEntry = analyseExpr5();
        Type type = symbolEntry.getType();
        while (nextIf(TokenType.AS_KW) != null) {
            Type newType = analyseType();

            if (!symbolEntry.isInitialized)
                throw new Error("Expression not initialized");

            if (newType == Type.void_ty) {
                throw new Error("Illegal type transition");
            } else if (type == Type.int_ty && newType == Type.double_ty) {
                addInstruction(Operation.itof);
            } else if (type == Type.double_ty && newType == Type.int_ty) {
                addInstruction(Operation.ftoi);
            }
            symbolEntry.setType(type);
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr5() throws CompileError {
        System.out.println("expr5");
        boolean isNeg = false;
        while (nextIf(TokenType.MINUS) != null) {
            isNeg = true;
        }
        SymbolEntry symbolEntry = analyseExpr6();
        Type type = symbolEntry.getType();
        if (isNeg) {
            if (!symbolEntry.isInitialized)
                throw new Error("Expression not initialized");
            if (type == Type.int_ty) {
                addInstruction(Operation.negi);
            } else if (type == Type.double_ty) {
                addInstruction(Operation.negf);
            } else {
                throw new Error("Illegal expr");
            }
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr6() throws CompileError {
        System.out.println("expr6");
        TokenType tt = peek().getTokenType();
        SymbolEntry symbolEntry = null;
        // group expr
        if (nextIf(TokenType.L_PAREN) != null) {
            symbolEntry = analyseExpr();
            expect(TokenType.R_PAREN);
        }
        // literal expr
        else if (tt == TokenType.UINT_LITERAL || tt == TokenType.CHAR_LITERAL || tt == TokenType.STRING_LITERAL) {
            symbolEntry = analyseLiteral();
        }
        // call & ident expr
        else if (tt == TokenType.IDENT) {
            var nameToken = next();
            String name = (String) nameToken.getValue();
            // call expr
            if (nextIf(TokenType.L_PAREN) != null) {
                if (functionTables.get(name) == null && !isStdlib(name))
                    throw new Error("Illegal function call");
                // call function
                else if (!isStdlib(name)) {
                    addInstruction(Operation.stackalloc, 1);
                    if (nextIf(TokenType.R_PAREN) == null) {
                        analyseCallParamList();
                        expect(TokenType.R_PAREN);
                    }
                    addInstruction(Operation.call, functionTables.get(name).order);
                    symbolEntry = new SymbolEntry(functionTables.get(name).type);
                }
                // call stdlib
                else {
                    if (nextIf(TokenType.R_PAREN) == null) {
                        analyseCallParamList();
                        expect(TokenType.R_PAREN);
                    }
                    Type type = callStdlib(name);
                    symbolEntry = new SymbolEntry(type);
                }
            }
            // ident expr
            else {
                symbolEntry = getSymbolEntry(name);
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
            }
        }
        // error
        else {
            throw new Error("Illegal Expression");
        }

        return symbolEntry;
    }

    private SymbolEntry analyseLiteral() throws CompileError {
        var nameToken = next();
        TokenType tt = nameToken.getTokenType();
        switch (tt) {
            case UINT_LITERAL:
            case CHAR_LITERAL:
                addInstruction(Operation.push, (Long) nameToken.getValue());
                return new SymbolEntry(Type.int_ty);
            case STRING_LITERAL:
                SymbolEntry symbol = addString((String) nameToken.getValue());
                int order = symbol.order;
                addInstruction(Operation.push, order);
                return new SymbolEntry(Type.string_ty);
            case DOUBLE_LITERAL:
                addInstruction(Operation.push, (Double) nameToken.getValue());
                return new SymbolEntry(Type.double_ty);
            default:
                throw new Error("Illegal literal");
        }
    }

    private void analyseCallParamList() throws CompileError {
        // call_param_list -> expr (',' expr)*
        // todo: 判断调用参数和函数定义参数类型相符
        analyseCallParam();
        while (nextIf(TokenType.COMMA) != null) {
            analyseCallParam();
        }
    }

    private void analyseCallParam() throws CompileError {
        SymbolEntry symbolEntry = analyseExpr();
        if (!symbolEntry.isInitialized) {
            throw new Error("Expr not initialized");
        }
    }

    private Type analyseType() throws CompileError {
        // ty -> IDENT
        // IDENT:void/int(/double)

        Token token = expect(TokenType.IDENT);
        String value = (String) token.getValue();
        if (value.equals("void"))
            return Type.void_ty;
        else if (value.equals("int"))
            return Type.int_ty;
        else if (value.equals("double"))
            return Type.double_ty;
        else
            throw new Error("Type illegal");
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

    private Type callStdlib(String name) {
        if (name.equals("getint")) {
            addInstruction(Operation.scani);
            return Type.int_ty;
        } else if (name.equals("getdouble")) {
            addInstruction(Operation.scanf);
            return Type.double_ty;
        } else if (name.equals("getchar")) {
            addInstruction(Operation.scanc);
            return Type.int_ty;
        } else if (name.equals("putint")) {
            addInstruction(Operation.printi);
            return Type.void_ty;
        } else if (name.equals("putdouble")) {
            addInstruction(Operation.printf);
            return Type.void_ty;
        } else if (name.equals("putchar")) {
            addInstruction(Operation.printc);
            return Type.void_ty;
        } else if (name.equals("putln")) {
            addInstruction(Operation.println);
            return Type.void_ty;
        } else if (name.equals("putstr")) {
            addInstruction(Operation.prints);
            return Type.void_ty;
        } else {
            return Type.void_ty;
        }
    }
}