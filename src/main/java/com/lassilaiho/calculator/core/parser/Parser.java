package com.lassilaiho.calculator.core.parser;

import java.util.ArrayList;
import java.util.List;
import com.lassilaiho.calculator.core.lexer.*;

/**
 * {@link Parser} parses a list of lexemes into an abstract syntax tree.
 *
 * Parsers are single-use objects. To parse another expression, construct a new {2link Parser}
 * object.
 */
public class Parser {
    private final List<Lexeme> lexemes;
    /**
     * Current index to {@link #lexemes}.
     */
    private int current = 0;

    /**
     * Constructs a new parser that parses lexemes.
     *
     * @param lexemes The lexems to parse. Modifying the list during parsing is undefined behavior.
     */
    public Parser(List<Lexeme> lexemes) {
        this.lexemes = lexemes;
    }

    /**
     * Parses a {@link Node}.
     * 
     * Calls after the first call will always return null.
     * 
     * @return the resulting node
     */
    public Node parseNode() {
        if (current > 0 || peek().type == LexemeType.EOF) {
            return null;
        }
        return parseExpression();
    }

    private Expression parseExpression() {
        return parseBinaryExpression(Operator.MIN_PRECEDENCE);
    }

    private Expression parseBinaryExpression(int precedence) {
        if (precedence > Operator.MAX_PRECEDENCE) {
            return parseUnaryExpression();
        }
        var left = parseBinaryExpression(precedence + 1);
        while (true) {
            var operator = parseBinaryOperator(precedence);
            if (operator == null) {
                return left;
            }
            var right = parseBinaryExpression(precedence + 1);
            left = new BinaryExpression(left, operator, right);
        }
    }

    private Operator parseBinaryOperator(int precedence) {
        var operator = matchBinaryOperator(peek().type);
        if (operator != null && operator.precedence() == precedence) {
            advance();
            return operator;
        }
        return null;
    }

    private Operator matchBinaryOperator(LexemeType type) {
        switch (type) {
            case PLUS:
                return Operator.ADD;
            case MINUS:
                return Operator.SUBTRACT;
            case ASTERISK:
                return Operator.MULTIPLY;
            case SLASH:
                return Operator.DIVIDE;
            default:
                return null;
        }
    }

    private Expression parseUnaryExpression() {
        var operator = parseUnaryOperator();
        if (operator == null) {
            return parseTerminal();
        }
        return new UnaryExpression(operator, parseUnaryExpression());
    }

    private Operator parseUnaryOperator() {
        if (peek().type == LexemeType.MINUS) {
            advance();
            return Operator.NEGATE;
        }
        return null;
    }

    private Expression parseTerminal() {
        var lexeme = peek();
        switch (lexeme.type) {
            case EOF:
                throw new ParserException("unexpected end of expression");
            case NUMBER:
                advance();
                return new NumberNode((double) lexeme.value);
            case IDENTIFIER:
                if (peek(1).type == LexemeType.LEFT_PAREN) {
                    return parseFunctionCall();
                }
                advance();
                return new VariableNode((String) lexeme.value);
            case LEFT_PAREN:
                return parseSubExpression();
            default:
                throw new ParserException("unexpected lexeme: " + lexeme);
        }
    }

    private Expression parseSubExpression() {
        parseToken(LexemeType.LEFT_PAREN);
        var subExpression = parseExpression();
        parseToken(LexemeType.RIGHT_PAREN);
        return subExpression;
    }

    private Expression parseFunctionCall() {
        var name = (String) parseToken(LexemeType.IDENTIFIER).value;
        parseToken(LexemeType.LEFT_PAREN);
        var arguments = new ArrayList<Expression>();
        if (peek().type == LexemeType.RIGHT_PAREN) {
            advance();
            return new FunctionCallNode(name, arguments);
        }
        for (var lexeme = peek();; advance()) {
            arguments.add(parseExpression());
            lexeme = peek();
            if (lexeme.type == LexemeType.RIGHT_PAREN) {
                advance();
                return new FunctionCallNode(name, arguments);
            } else if (lexeme.type != LexemeType.COMMA) {
                throwUnexpectedLexeme(LexemeType.RIGHT_PAREN, lexeme.type);
            }
        }
    }

    private Lexeme parseToken(LexemeType type) {
        var lexeme = peek();
        if (lexeme.type != type) {
            throwUnexpectedLexeme(type, lexeme.type);
        }
        advance();
        return lexeme;
    }

    private void throwUnexpectedLexeme(LexemeType expected, LexemeType found) {
        throw new ParserException(
            "expected lexeme of type " + expected + ", found " + found);
    }

    private Lexeme peek() {
        return peek(0);
    }

    private Lexeme peek(int count) {
        if (current + count >= lexemes.size()) {
            return new Lexeme(LexemeType.EOF);
        }
        return lexemes.get(current + count);
    }

    private void advance() {
        current++;
    }
}
