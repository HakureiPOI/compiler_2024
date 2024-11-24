package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

public class Symbol {
    private final Token token;
    private final NonTerminal nonTerminal;
    private SourceCodeType type = null;
    private IRValue value = null;

    private Symbol(Token token, NonTerminal nonTerminal) {
        if (token != null && nonTerminal != null) {
            throw new IllegalArgumentException("Symbol cannot be both a token and a non-terminal.");
        }
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token) {
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal) {
        this(null, nonTerminal);
    }

    public Token getToken() {
        return token;
    }

    public NonTerminal getNonTerminal() {
        return nonTerminal;
    }

    public boolean isToken() {
        return token != null;
    }

    public boolean isNonTerminal() {
        return nonTerminal != null;
    }

    public SourceCodeType getType() {
        return type;
    }

    public void setType(SourceCodeType type) {
        this.type = type;
    }

    public IRValue getValue() {
        return value;
    }

    public void setValue(IRValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (isToken()) {
            return "Token: " + token.toString();
        } else if (isNonTerminal()) {
            return "NonTerminal: " + nonTerminal.toString();
        }
        return "Empty Symbol";
    }
}
