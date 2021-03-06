package com.lassilaiho.calculator.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.lassilaiho.calculator.core.lexer.*;

/**
 * {@link Parser} parses a list of lexemes into an abstract syntax tree.
 *
 * Parsers are single-use objects. To parse another expression, construct a new {@link Parser}
 * object.
 */
public final class Parser {
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
        var statement = tryParseStatement();
        if (statement != null) {
            return statement;
        }
        return parseExpression();
    }

    private Node tryParseStatement() {
        if (peek().type == LexemeType.DELETE) {
            return parseDeleteStatement();
        }
        if (!lookAheadForLexeme(LexemeType.ASSIGN)) {
            return null;
        }
        var name = parseIdentifier();
        if (peek().type == LexemeType.ASSIGN) {
            parseLexeme(LexemeType.ASSIGN);
            var value = parseExpression();
            return new AssignmentNode(name, value);
        }
        var parameters = parseList(this::parseIdentifier);
        parseLexeme(LexemeType.ASSIGN);
        var body = parseExpression();
        return new FunctionDefinitionNode(name, parameters, body);
    }

    private Statement parseDeleteStatement() {
        parseLexeme(LexemeType.DELETE);
        var names = new ArrayList<String>();
        names.add(parseIdentifier());
        while (peek().type == LexemeType.COMMA) {
            advance();
            names.add(parseIdentifier());
        }
        return new DeleteNode(names);
    }

    private Expression parseExpression() {
        return parseBinaryExpression(BinaryOperator.MIN_PRECEDENCE);
    }

    private Expression parseBinaryExpression(int precedence) {
        if (precedence > BinaryOperator.MAX_PRECEDENCE) {
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

    private BinaryOperator parseBinaryOperator(int precedence) {
        var operator = matchBinaryOperator(peek().type);
        if (operator != null && operator.precedence() == precedence) {
            advance();
            return operator;
        }
        return null;
    }

    private BinaryOperator matchBinaryOperator(LexemeType type) {
        switch (type) {
            case PLUS:
                return BinaryOperator.ADD;
            case MINUS:
                return BinaryOperator.SUBTRACT;
            case ASTERISK:
                return BinaryOperator.MULTIPLY;
            case SLASH:
                return BinaryOperator.DIVIDE;
            default:
                return null;
        }
    }

    private Expression parseUnaryExpression() {
        var operator = matchUnaryOperator();
        if (operator == null) {
            return parseTerminal();
        }
        advance();
        return new UnaryExpression(operator, parseUnaryExpression());
    }

    private UnaryOperator matchUnaryOperator() {
        switch (peek().type) {
            case PLUS:
                return UnaryOperator.PLUS;
            case MINUS:
                return UnaryOperator.NEGATE;
            default:
                return null;
        }
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
        parseLexeme(LexemeType.LEFT_PAREN);
        var subExpression = parseExpression();
        parseLexeme(LexemeType.RIGHT_PAREN);
        return subExpression;
    }

    private Expression parseFunctionCall() {
        var name = parseIdentifier();
        return new FunctionCallNode(name, parseList(this::parseExpression));
    }

    private <T> List<T> parseList(Supplier<T> elementParser) {
        parseLexeme(LexemeType.LEFT_PAREN);
        var list = new ArrayList<T>();
        if (peek().type == LexemeType.RIGHT_PAREN) {
            advance();
            return list;
        }
        for (var lexeme = peek();; advance()) {
            list.add(elementParser.get());
            lexeme = peek();
            if (lexeme.type == LexemeType.RIGHT_PAREN) {
                advance();
                return list;
            } else if (lexeme.type != LexemeType.COMMA) {
                throwUnexpectedLexeme(LexemeType.RIGHT_PAREN, lexeme.type);
            }
        }
    }

    private String parseIdentifier() {
        return (String) parseLexeme(LexemeType.IDENTIFIER).value;
    }

    private Lexeme parseLexeme(LexemeType type) {
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

    private boolean lookAheadForLexeme(LexemeType type) {
        for (var i = current; i < lexemes.size(); i++) {
            if (lexemes.get(i).type == type) {
                return true;
            }
        }
        return false;
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
