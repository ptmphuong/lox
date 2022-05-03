package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Call;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitAssignExpr(Assign expr) {
    return null;
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return parenthesize(
        expr.operator.lexeme,
        expr.left, expr.right
    );
  }

  @Override
  public String visitCallExpr(Call expr) {
    return null;
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return null;
  }

  @Override
  public String visitGroupingExpr(Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitLogicalExpr(Logical expr) {
    return null;
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return null;
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return null;
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Variable expr) {
    return null;
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr: exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)
        ),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(new Expr.Literal(45.67))
    );

    System.out.println(new AstPrinter().print(expression));
  }
}
