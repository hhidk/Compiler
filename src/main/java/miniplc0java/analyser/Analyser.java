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

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;

    /**
     * 全局符号表
     */
    SymbolTable globalTable;

    /**
     * 整体符号表
     */
    SymbolTable symbolTable;

    /**
     * 参数暂存
     */
    HashMap argsMap;

    /**
     * 函数符号表及指令集
     */
    HashMap<String, FunctionTable> functionTables;

    /**
     * 当前函数
     */
    FunctionTable functionTable;

    /**
     * _start函数
     */
    FunctionTable initTable;

    /**
     * 当前偷看的 token
     */
    Token peekedToken = null;

    /**
     * 当前所在while block
     */
    int blockLevel;

    LinkedList<Integer> levelStack;

    LinkedList<WhileBlock> blockStack;

    int maxLevel;

    public Analyser(Tokenizer tokenizer, SymbolTable globalTable, HashMap<String, FunctionTable> functionTables) {
        this.tokenizer = tokenizer;
        this.globalTable = globalTable;
        this.symbolTable = globalTable;
        this.functionTables = functionTables;
        init_start();
        this.argsMap = new LinkedHashMap();
        this.blockLevel = 0;
        this.levelStack = new LinkedList<>();
        this.blockStack = new LinkedList<>();
        this.maxLevel = 0;
    }

    public void analyse() throws CompileError {
        analyseProgram();
        // 设置_start函数调用main
        int order = functionTables.get("main").order;
        addInstruction(Operation.callname, order);
        // 设置_start函数局部变量数为0
        functionTable.locals = 0;
    }

    public void init_start() {
        String name = "_start";
        int order = 0;
        FunctionTable functionTable = new FunctionTable(order);
        this.functionTable = functionTable;
        this.initTable = functionTable;
        functionTables.put(name, functionTable);
        addString(name);
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
     * 查符号表
     * 调用SymbolTable类的get方法
     */
    public SymbolEntry getSymbolEntry(String name) {
        return symbolTable.get(name);
    }

    /**
     * 添加局部变量或全局变量
     */
    public SymbolEntry addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos, Type type, boolean isArg) throws AnalyzeError {
        int order, scope, def = 1;
        SymbolEntry symbol;

        if (symbolTable.getCurrent(name) != null)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);

        if (functionTable.isGlobal()) {
            scope = 0;
        } else {
            scope = 2;
        }

        // 符号根据functionTable中的locals决定
        // 全局变量个数存在_start函数的locals中
        order = functionTable.locals ++;

        symbol = new SymbolEntry(isConstant, isInitialized, def, type, scope, order);
        symbolTable.put(name, symbol);
        return symbol;
    }

    /**
     * 添加函数形参
     * 函数形参暂存到临时表argsMap中
     * 当分析完函数声明、执行完startFunction方法后，将argsMap添加到symbolTable中
     */
    public SymbolEntry addArg(String name, boolean isInitialized, boolean isConstant, Pos curPos, Type type, boolean isArg) throws AnalyzeError {
        int order = functionTable.args ++, scope = 1, def = 1;
        SymbolEntry symbol;

        if (argsMap.get(name) != null)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);

        symbol = new SymbolEntry(isConstant, isInitialized, def, type, scope, order);
        argsMap.put(name, symbol);
        return symbol;
    }

    /**
     * 添加字符串到全局变量表
     */
    public SymbolEntry addString(String value) {
        int order = initTable.locals ++;
        SymbolEntry symbol = new SymbolEntry(true, true, 1, Type.string_ty, 0, order);
        globalTable.put(value, symbol);
        return symbol;
    }

    /**
     * 添加指令
     */
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

    /**
     * 获取当前指令在指令集body中的序号
     */
    public int getInstructionOffset() {
        return this.functionTable.body.size();
    }

    /**
     * 进入编译函数状态
     * 添加函数名至全局变量表
     * 生成新函数结构，并存入函数表
     */
    public void startFunction(String name, Pos curPos) throws AnalyzeError {
        if (globalTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        SymbolEntry symbolEntry = addString(name);
        this.functionTable = new FunctionTable(symbolEntry.order);
        functionTables.put(name, functionTable);
    }

    /**
     * 退出函数编译状态
     * return check
     * 将当前functionTable置换为_start
     */
    public void endFunction() {
        if (this.functionTable.body.size() == 0 || this.functionTable.body.get(this.functionTable.body.size() - 1).getOpt() != Operation.ret)
            addInstruction(Operation.ret);
        this.functionTable = initTable;
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

        addArg(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);
    }

    private void analyseBlockStmt() throws CompileError {
        // block_stmt -> '{' stmt* '}'
        // stmt -> expr_stmt | decl_stmt | if_stmt | while_stmt |
        //          return_stmt | block_stmt | empty_stmt

        SymbolTable symbolTable = new SymbolTable(this.symbolTable);
        this.symbolTable = symbolTable;
        if (argsMap.size() != 0) {
            this.symbolTable.putAllArgs(argsMap);
            argsMap.clear();
        }

        expect(TokenType.L_BRACE);
        TokenType tokenType = peek().getTokenType();
        while (tokenType == TokenType.LET_KW || tokenType == TokenType.CONST_KW || tokenType == TokenType.IF_KW || tokenType == TokenType.WHILE_KW
                || tokenType == TokenType.RETURN_KW || tokenType == TokenType.L_BRACE || tokenType == TokenType.SEMICOLON
                || tokenType == TokenType.CONTINUE_KW || tokenType == TokenType.BREAK_KW || isExpr()) {
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
            } else if (tokenType == TokenType.CONTINUE_KW) {
                analyseContinueStmt();
            } else if (tokenType == TokenType.BREAK_KW) {
                analyseBreakStmt();
            } else if (isExpr()){
                analyseExpr();
                expect(TokenType.SEMICOLON);
            } else {
                throw new Error("Not a statement");
            }
            tokenType = peek().getTokenType();
        }
        expect(TokenType.R_BRACE);

        this.symbolTable = symbolTable.upperTable;
    }

    private void analyseLetDeclStmt() throws CompileError {
        // let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'

        expect(TokenType.LET_KW);
        var nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Type type = analyseType();
        if (type == Type.void_ty)
            throw new Error("Illegal type");

        boolean isConstant = false, isInitialized = false, isArg = false;
        String name = (String) nameToken.getValue();
        SymbolEntry symbolEntry = addSymbol(name, isInitialized, isConstant, nameToken.getStartPos(), type, isArg);

        if (peek().getTokenType() == TokenType.ASSIGN) {
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
        Instruction br2 = addInstruction(Operation.br, 0);
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

        this.levelStack.add(this.blockLevel);
        this.blockLevel = ++maxLevel;

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

        while (!blockStack.isEmpty()) {
            if (blockStack.getLast().level == this.blockLevel) {
                WhileBlock block = blockStack.getLast();
                blockStack.removeLast();
                if (block.type == 0) {
                    block.instruction.setX(offset1 - block.offset);
                } else {
                    block.instruction.setX(offset3 - block.offset);
                }
            } else {
                break;
            }
        }

        this.blockLevel = levelStack.getLast();
        levelStack.removeLast();
    }

    private void analyseReturnStmt() throws CompileError {
        // return_stmt -> 'return' expr? ';'

        SymbolEntry symbolEntry = null;
        expect(TokenType.RETURN_KW);
        if (nextIf(TokenType.SEMICOLON) == null) {
            addInstruction(Operation.arga, 0);

            symbolEntry = analyseExpr();
            Type type = symbolEntry.getType();
            expect(TokenType.SEMICOLON);

            if (functionTable.type == Type.void_ty) {
                throw new Error("Should return void");
            } else if (!symbolEntry.isInitialized) {
                throw new Error("Return value not initialized");
            } else if (functionTable.type != type) {
                throw new Error("Return type not matched");
            }

            addInstruction(Operation.store64);
        }

        if (this.functionTable.type != Type.void_ty && symbolEntry == null){
            throw new Error("Should not return void");
        }
        addInstruction(Operation.ret);
    }

    private void analyseContinueStmt() throws CompileError {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);

        if (this.blockLevel != 0) {
            Instruction instruction = addInstruction(Operation.br, 0);
            int offset = getInstructionOffset();
            WhileBlock block = new WhileBlock(instruction, offset, this.blockLevel, 0);
            this.blockStack.add(block);
        } else {
            throw new Error("Invalid continue");
        }
    }

    private void analyseBreakStmt() throws CompileError {
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);

        if (this.blockLevel != 0) {
            Instruction instruction = addInstruction(Operation.br, 0);
            int offset = getInstructionOffset();
            WhileBlock block = new WhileBlock(instruction, offset, this.blockLevel, 1);
            this.blockStack.add(block);
        } else {
            throw new Error("Invalid break");
        }
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

        SymbolEntry lsymbolEntry = analyseExpr1();
        Type ltype = lsymbolEntry.getType();
        if (nextIf(TokenType.ASSIGN) != null) {
            popInstruction();
            SymbolEntry rsymbolEntry = analyseExpr();
            Type rtype = rsymbolEntry.getType();

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
        SymbolEntry symbolEntry = analyseExpr2();
        Type ltype = symbolEntry.getType();
        while (peek().getTokenType() == TokenType.GT || peek().getTokenType() == TokenType.LT || peek().getTokenType() == TokenType.GE
            || peek().getTokenType() == TokenType.LE || peek().getTokenType() == TokenType.EQ || peek().getTokenType() == TokenType.NEQ) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            SymbolEntry rsymbolEntry = analyseExpr2();
            Type rtype = rsymbolEntry.getType();

//            if (!symbolEntry.isInitialized || !rsymbolEntry.isInitialized)
//                throw new Error("Expression not initialized");
            if (ltype != rtype)
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
        SymbolEntry symbolEntry = analyseExpr3();
        Type ltype = symbolEntry.getType();
        while (peek().getTokenType() == TokenType.PLUS || peek().getTokenType() == TokenType.MINUS) {
            var operatorToken = next();
            TokenType opt = operatorToken.getTokenType();
            SymbolEntry rsymbolEntry = analyseExpr3();
            Type rtype = rsymbolEntry.getType();

            if (!symbolEntry.isInitialized && symbolEntry.scope != 0 || !rsymbolEntry.isInitialized && rsymbolEntry.scope != 0)
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
        SymbolEntry symbolEntry = analyseExpr5();
        while (nextIf(TokenType.AS_KW) != null) {
            Type newType = analyseType();

            if (!symbolEntry.isInitialized)
                throw new Error("Expression not initialized");

            if (newType == Type.void_ty) {
                throw new Error("Illegal type transition");
            } else if (symbolEntry.type == Type.int_ty && newType == Type.double_ty) {
                addInstruction(Operation.itof);
            } else if (symbolEntry.type == Type.double_ty && newType == Type.int_ty) {
                addInstruction(Operation.ftoi);
            }
            symbolEntry = new SymbolEntry(newType);
        }
        return symbolEntry;
    }

    private SymbolEntry analyseExpr5() throws CompileError {
        boolean isNeg = false;
        while (nextIf(TokenType.MINUS) != null) {
            isNeg = !isNeg;
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
        TokenType tt = peek().getTokenType();
        SymbolEntry symbolEntry = null;
        // group expr
        if (nextIf(TokenType.L_PAREN) != null) {
            symbolEntry = analyseExpr();
            expect(TokenType.R_PAREN);
        }
        // literal expr
        else if (tt == TokenType.UINT_LITERAL || tt == TokenType.CHAR_LITERAL || tt == TokenType.STRING_LITERAL || tt == TokenType.DOUBLE_LITERAL) {
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
                    if (functionTables.get(name).type != Type.void_ty) {
                        addInstruction(Operation.stackalloc, 1);
                    }
                    if (nextIf(TokenType.R_PAREN) == null) {
                        analyseCallParamList();
                        expect(TokenType.R_PAREN);
                    }
                    addInstruction(Operation.callname, functionTables.get(name).order);
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
                    int offset = 0;
                    if (functionTable.type != Type.void_ty)
                        offset = 1;
                    addInstruction(Operation.arga, symbolEntry.order + offset);
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
                addInstruction(Operation.push, (long) nameToken.getValue());
                return new SymbolEntry(Type.int_ty);
            case STRING_LITERAL:
                SymbolEntry symbol = addString((String) nameToken.getValue());
                int order = symbol.order;
                addInstruction(Operation.push, (long) order);
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