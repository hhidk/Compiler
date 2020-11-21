package main.java.c0.tokenizer;

import main.java.c0.error.ErrorCode;
import main.java.c0.error.TokenizeError;
import main.java.c0.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntLiteral();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '\'') {
            return lexCharLiteral();
        } else if (peek == '"') {
            return lexStringLiteral();
        } else if (peek == '/') {
            return lexComment();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUIntLiteral() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder stringBuilder = new StringBuilder();
        while (Character.isDigit(it.peekChar())) {
            char peek = it.nextChar();
            stringBuilder.append(peek);
        }
        if (it.peekChar() == '.') {
            throw new Error("Not Implemented");
        }
        else {
            Pos endPos = it.currentPos();
            int value = Integer.parseInt(stringBuilder.toString());
            return new Token(TokenType.UINT_LITERAL, value, startPos, endPos);
        }
    }

    private Token lexDoubleLiteral(StringBuilder stringBuilder) throws TokenizeError {
        it.nextChar();
        stringBuilder.append('.');
        while (Character.isDigit(it.peekChar())) {
            char peek = it.nextChar();
            stringBuilder.append(peek);
        }
        if(it.peekChar() == 'e' || it.peekChar() == 'E') {
            char peek = it.nextChar();
            stringBuilder.append(peek);
        }
        throw new Error("Not Implemented");
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder stringBuilder = new StringBuilder();
        while (Character.isDigit(it.peekChar()) || Character.isLetter(it.peekChar()) || it.peekChar() == '_') {
            char peek = it.nextChar();
            stringBuilder.append(peek);
        }
        Pos endPos = it.currentPos();
        String value = stringBuilder.toString();

        TokenType tokenType = TokenType.getTokenType(value);
        if(tokenType == null)
            tokenType = TokenType.IDENT;
        return new Token(tokenType, value, startPos, endPos);
    }

    private Token lexCharLiteral() throws TokenizeError {
        throw new Error("Not Implemented");
    }

    private Token lexStringLiteral() throws TokenizeError {
        throw new Error("Not Implemented");
    }

    private Token lexComment() throws TokenizeError {
        throw new Error("Not Implemented");
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        char peek = it.nextChar();
        Pos startPos = it.currentPos();
        switch (it.nextChar()) {
            case '+':
            case '*':
            case '/':
            case ';':
            case '(':
            case ')':
            case '{':
            case '}':
            case ',':
            case ':':
                TokenType tokenType = TokenType.getTokenType(peek);
                return new Token(tokenType, peek, it.previousPos(), it.currentPos());
            case '-':
                if(it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, TokenType.ARROW.string, startPos, it.currentPos());
                }
                return new Token(TokenType.MINUS, peek, it.previousPos(), it.currentPos());
            case '=':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, TokenType.EQ.string, startPos, it.currentPos());
                }
                return new Token(TokenType.ASSIGN, peek, it.previousPos(), it.currentPos());
            case '!':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, TokenType.NEQ.string, startPos, it.currentPos());
                }
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            case '<':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, TokenType.LE.string, startPos, it.currentPos());
                }
                return new Token(TokenType.LT, peek, it.previousPos(), it.currentPos());
            case '>':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, TokenType.GE.string, startPos, it.currentPos());
                }
                return new Token(TokenType.GT, peek, it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
