package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Token;

import java.util.List;

abstract class Stmt {

  abstract <R> R accept(Visitor<R> visitor);
  interface Visitor<R> {
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
  }

  static class Expression extends Stmt {
    final Expr expression;
    Expression (Expr expression) {
      this.expression = expression;
      }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
    }

  static class Print extends Stmt {
    final Expr Expression;
    Print (Expr Expression) {
      this.Expression = Expression;
      }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
    }

}