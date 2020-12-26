package miniplc0java.tokenizer;

import miniplc0java.error.ErrorCode;
import miniplc0java.error.TokenizeError;
import miniplc0java.util.Pos;

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

        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntOrDoubleLiteral();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '\'') {
            return lexCharLiteral();
        } else if (peek == '"') {
            return lexStringLiteral();
        } else {
            return lexOperatorOrCommentOrUnknown();
        }
    }

    private Token lexUIntOrDoubleLiteral() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder stringBuilder = new StringBuilder();
        while (Character.isDigit(it.peekChar())) {
            char peek = it.nextChar();
            stringBuilder.append(peek);
        }
        if (it.peekChar() == '.') {
            it.nextChar();
            stringBuilder.append('.');
            if(!Character.isDigit(it.peekChar()))
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            while (Character.isDigit(it.peekChar())) {
                stringBuilder.append(it.nextChar());
            }
            if(it.peekChar() == 'e' || it.peekChar() == 'E') {
                stringBuilder.append(it.nextChar());
                if(it.peekChar() == '+' || it.peekChar() == '-') {
                    stringBuilder.append(it.nextChar());
                }
                if(!Character.isDigit(it.peekChar()))
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                while (Character.isDigit(it.peekChar())) {
                    stringBuilder.append(it.nextChar());
                }
            }
            double value = Double.parseDouble(stringBuilder.toString());
            Pos endPos = it.currentPos();
            return new Token(TokenType.DOUBLE_LITERAL, value, startPos, endPos);
        }
        Pos endPos = it.currentPos();
        long value = Long.parseLong(stringBuilder.toString());
        return new Token(TokenType.UINT_LITERAL, value, startPos, endPos);
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
        Pos startPos = it.currentPos();
        StringBuilder stringBuilder = new StringBuilder();
        if(it.peekChar() == '\'') {
            it.nextChar();
            if(isCharRegularChar(it.peekChar())) {
                stringBuilder.append(it.nextChar());
            } else if(it.peekChar() == '\\') {
                it.nextChar();
                if(isEscapeSequenceChar(it.peekChar())) {
                    stringBuilder.append(switchToEscapeSequenceChar(it.nextChar()));
                } else {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            } else {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            if (it.peekChar() == '\'') {
                it.nextChar();
            } else {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }
        Pos endPos = it.currentPos();
        char value = stringBuilder.charAt(0);
        return new Token(TokenType.CHAR_LITERAL, value, startPos, endPos);
    }

    private Token lexStringLiteral() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder stringBuilder = new StringBuilder();
        if(it.peekChar() == '"') {
            it.nextChar();
            while(isStringRegularChar(it.peekChar()) || it.peekChar() == '\\') {
                if(it.peekChar() == '\\') {
                    it.nextChar();
                    if(isEscapeSequenceChar(it.peekChar())) {
                        stringBuilder.append(switchToEscapeSequenceChar(it.nextChar()));
                    } else {
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                    }
                } else {
                    stringBuilder.append(it.nextChar());
                }
            }
            if (it.peekChar() == '"') {
                it.nextChar();
            } else {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }
        Pos endPos = it.currentPos();
        String value = stringBuilder.toString();
        return new Token(TokenType.STRING_LITERAL, value, startPos, endPos);
    }

    private Token lexOperatorOrCommentOrUnknown() throws TokenizeError {
        char peek = it.nextChar();
        Pos startPos = it.currentPos();
        switch (peek) {
            case '+':
                return new Token(TokenType.PLUS, peek, it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MINUS, peek, it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, peek, it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, peek, it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, peek, it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, peek, it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, peek, it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, peek, it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, peek, it.previousPos(), it.currentPos());
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
            case '/':
                if(it.peekChar() == '/'){
                    it.nextChar();
                    while(true) {
                        if(it.nextChar() == '\\') {
                            if(it.peekChar() == 'n') {
                                it.nextChar();
                                break;
                            }
                        }
                    }
                    return new Token(TokenType.COMMENT, null, startPos, it.currentPos());
                }
                return new Token(TokenType.DIV, peek, it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private boolean isCharRegularChar(char c) {
        return (c != '\'') && (c != '\\');
    }

    private boolean isStringRegularChar(char c) {
        return (c != '"') && (c != '\\');
    }

    private boolean isEscapeSequenceChar(char c) {
        return (c == '\\') || (c == '"') || (c == '\'') || (c == 'n') || (c == 'r') || (c == 't');
    }

    private char switchToEscapeSequenceChar(char c) {
        switch (c) {
            case '\\':
            case '"':
            case '\'':
                return c;
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                throw new Error("logic wrong");
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
