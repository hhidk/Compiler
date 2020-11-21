package main.java.c0.tokenizer;

import main.java.c0.error.TokenizeError;

public enum TokenType {
    NONE(""),
    EOF(""),

    //keyword
    FN_KW("fn"),
    LET_KW("let"),
    CONST_KW("const"),
    AS_KW("as"),
    WHILE_KW("while"),
    IF_KW("if"),
    ELSE_KW("else"),
    RETURN_KW("return"),
    BREAK_KW("break"),
    CONTINUE_KW("continue"),

    //operator
    PLUS("+"),
    MINUS("-"),
    MUL("*"),
    DIV("/"),
    ASSIGN("="),
    EQ("=="),
    NEQ("!="),
    LT("<"),
    GT(">"),
    LE("<="),
    GE(">="),
    L_PAREN("("),
    R_PAREN(")"),
    L_BRACE("{"),
    R_BRACE("}"),
    ARROW("->"),
    COMMA(","),
    COLON(":"),
    SEMICOLON(";"),

    //identification
    IDENT(""),

    //literal
    UINT_LITERAL(""),
    STRING_LITERAL(""),
    DOUBLE_LITERAL(""),
    CHAR_LITERAL(""),

    //comment
    COMMENT(""),
    ;

    String string;
    TokenType(String string){
        this.string = string;
    }

    public static TokenType getTokenType(char c) {
        StringBuilder stringBuilder = new StringBuilder(c);
        return getTokenType(stringBuilder.toString());
    }

    public static TokenType getTokenType(String s) {
        for(TokenType tokenType : TokenType.values()) {
            if(tokenType.string.equals(s)) {
                return tokenType;
            }
        }
        return null;
    }
}
