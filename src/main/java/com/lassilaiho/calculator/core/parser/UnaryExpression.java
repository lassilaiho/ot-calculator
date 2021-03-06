package com.lassilaiho.calculator.core.parser;

/**
 * {@link UnaryExpression} is a binary expression node.
 */
public final class UnaryExpression implements Expression {
    public final UnaryOperator operator;
    public final Expression operand;

    /**
     * Constructs a unary expression node.
     * 
     * @param operator operator
     * @param operand  operand
     */
    public UnaryExpression(UnaryOperator operator, Expression operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return operator.toString() + operand.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof UnaryExpression)) {
            return false;
        }
        var o = (UnaryExpression) other;
        return operator.equals(o.operator) && operand.equals(o.operand);
    }
}
