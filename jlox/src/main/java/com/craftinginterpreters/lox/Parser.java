package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Unary;
import java.util.List;
import java.util.logging.Level;

/**
 * is a top-down parser
 *
 * expression     → equality ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                | primary ;
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")" ;
 */
public class Parser {

  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }


  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return equality();
  }

  /**
   * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
   *
   * the ( ... )* loop in the rule maps to a while loop.
   *  if the parser never encounters an equality operator, then it never enters the loop.
   * @return
   */
  private Expr equality() {
    Expr expr = comparison();

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Binary(expr, operator, right);
    }

    return expr;
  }


  /**
   * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * @return
   */
  private Expr comparison() {
    Expr expr = term();

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }


  /**
   * term           → factor ( ( "-" | "+" ) factor )* ;
   * @return
   */
  private Expr term() {
    Expr expr = factor();

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Binary(expr, operator, right);
    }

    return expr;
  }


  /**
   * factor         → unary ( ( "/" | "*" ) unary )* ;
   * @return
   */
  private Expr factor() {
    Expr expr = unary();

    while (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Binary(expr, operator, right);
    }

    return expr;
  }


  /**
   * unary          → ( "!" | "-" ) unary
   *                | primary ;
   * @return
   */
  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Unary(operator, right);
    }

    return primary();
  }


  /**
   * primary        → NUMBER | STRING | "true" | "false" | "nil"
   *                | "(" expression ")" ;
   * @return
   */
  private Expr primary() {
    if (match(TokenType.FALSE)) return new Literal(false);
    if (match(TokenType.TRUE)) return new Literal(true);
    if (match(TokenType.NIL)) return new Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      // the match method consumes
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Grouping(expr);
    }

    throw error(peek(), "Expect expression");
  }

  /**
   * This checks to see if the current token has any of the given types.
   * If so, it consumes the token and returns true.
   * Otherwise, it returns false and leaves the current token alone.
   * @param types
   * @return
   */
  private boolean match(TokenType... types) {
    for (TokenType type: types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }


  /**
   * atch() in that it checks to see if the next token is of the expected type.
   * If so, it consumes the token and everything is groovy.
   * If some other token is there, then we’ve hit an error.
   * @param type
   * @param message
   * @return
   */
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }


  /**
   * check if the current token has this TokenType
   * @param type
   * @return
   */
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }


  /**
   * consumes the current token and returns it
   * @return
   */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }


  /**
   * check if we’ve run out of tokens to parse
   * @return
   */
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }


  /**
   * return the current token we have yet to consume
   * @return
   */
  private Token peek() {
    return tokens.get(current);
  }


  /**
   * return the most recently consumed token
   * @return
   */
  private Token previous() {
    return tokens.get(current - 1);
  }


  static ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while  (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}