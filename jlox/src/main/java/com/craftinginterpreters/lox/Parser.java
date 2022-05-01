package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;
import com.craftinginterpreters.lox.Stmt.Expression;
import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.Var;
import com.craftinginterpreters.lox.Stmt.While;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * is a top-down parser
 *
 * ---------------------------------
 *
 * program        → declaration* EOF ;
 *
 * declaration    → classDecl
 *                | funDecl
 *                | varDecl
 *                | statement ;
 *
 *                | varDecl
 *                | statement ;
 *
 * statement      → exprStmt
 *                | forStmt
 *                | ifStmt
 *                | printStmt
 *                | returnStmt
 *                | whileStmt
 *                | block ;
 *
 * classDecl      → "class" IDENTIFIER "{" function* "}" ;
 * funDecl        → "fun" function ;
 * function       → IDENTIFIER "(" parameters? ")" block ;
 * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
 *
 * block          → "{" declaration* "}" ;
 *
 * returnStmt     → "return" expression? ";" ;
 *
 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
 *                  expression? ";"
 *                  expression? ")" statement ;
 *
 * ifStmt         → "if" "(" expression ")" statement
 *                ( "else" statement )? ;
 *
 * whileStmt      → "while" "(" expression ")" statement ;
 *
 * ---------------------------------
 *
 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
 * arguments      → expression ( "," expression )* ;
 *
 * expression     → assignment ;
 * assignment     → IDENTIFIER "=" assignment
 *                | logic_or ;
 * logic_or       → logic_and ( "or" logic_and )* ;
 * logic_and      → equality ( "and" equality )* ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary | call ;
 * call           → primary ( "(" arguments? ")" )* ;
 * primary        → "true" | "false" | "nil"
 *                | NUMBER | STRING
 *                | "(" expression ")"
 *                | IDENTIFIER ;        // the name of the variable being accessed.
 *
 * ---------------------------------
 */
public class Parser {

  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }


//  Expr parse() {
//    try {
//      return expression();
//    } catch (ParseError error) {
//      return null;
//    }
//  }
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
//      statements.add(statement());
      statements.add(declaration());
    }

    return statements;
  }

  private Expr expression() {
    return assignment();
  }


  /**
   *  * assignment     → IDENTIFIER "=" assignment
   *  *                | logic_or ;
   * First parse the left-hand side, which could be equality() or any higher precedence
   * If we find an =, we parse the right-hand side and
   * then wrap it all up in an assignment expression tree node
   * @return
   */
  private Expr assignment() {
    Expr expr = or();

    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      // recurse to evaluate the right value,
      // this will eventually lead to higher precedences

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        // cast Object to Expr.Variable
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }


  /**
   * logic_or       → logic_and ( "or" logic_and )* ;
   * logic_and      → equality ( "and" equality )* ;
   * @return
   */
  private Expr or() {
    Expr expr = and();

    while (match(TokenType.OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Logical(expr, operator, right);
    }

    return expr;
  }


  private Expr and() {
    Expr expr = equality();

    while (match(TokenType.AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Logical(expr, operator, right);
    }

    return expr;
  }


  private Stmt declaration() {
    try {
      if (match(TokenType.CLASS)) return classDeclaration();
      if (match(TokenType.FUN)) return function("function");
      if (match(TokenType.VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
    consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
    return new Stmt.Class(name, methods);
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
   * statement      → exprStmt
   *                | ifStmt
   *                | printStmt
   *                | block ;
   * @return
   */
  private Stmt statement() {
    if (match(TokenType.IF)) return ifStatement();
    if (match(TokenType.PRINT)) return printStatement();
    if (match(TokenType.RETURN)) return returnStatement();
    if (match(TokenType.FOR)) return forStatement();
    if (match(TokenType.WHILE)) return whileStatement();
    if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
    return expressionStatement();
  }


  /**
   * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
   *                  expression? ";"
   *                  expression? ")" statement ;
   * @return
   */
  private Stmt forStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer = null;
    if (match(TokenType.SEMICOLON)) {
      initializer = null;
    } else if (match(TokenType.VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(TokenType.SEMICOLON)) {
      condition = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

    Expr increment = null;
    if (!check(TokenType.RIGHT_PAREN)) {
      increment = expression();
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();

    //  body with a little block
    //  that contains the original body
    //  followed by an expression statement that evaluates the increment.
    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(
              body,
              new Stmt.Expression(increment)
          )
      );
    }

    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }


  /**
   * ifStmt         → "if" "(" expression ")" statement
   *                ( "else" statement )? ;
   * @return
   */
  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(TokenType.ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }


  private Stmt returnStatement() {
   Token keyword = previous();
   Expr value = null;
   if (!check(TokenType.SEMICOLON)) {
     value = expression();
   }

   consume(TokenType.SEMICOLON, "Expect ';' after return value");
   return new Stmt.Return(keyword, value);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }


  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(TokenType.EQUAL)) {
      initializer = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");

    return new Var(name, initializer);
  }

  /**
   * whileStmt      → "while" "(" expression ")" statement ;
   * @return
   */
  private Stmt whileStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.");
    Stmt body = statement();

    return new While(condition, body);
  }

  /**
   * we parse an expression followed by a semicolon.
   * We wrap that Expr in a Stmt of the right type and return it.
   * @return
   */
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after expression.");
    return new Expression(expr);
  }


  private Stmt.Function function(String kind) {
    Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");

    consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }
        parameters.add(
            consume(TokenType.IDENTIFIER, "Expect parameter name.")
        );
      } while (match(TokenType.COMMA));
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters");

    consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + "body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
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

    return call();
  }


  /**
   * call           → primary ( "(" arguments? ")" )* ;
   * While loop:
   *  each time we see a (, we call finishCall() to parse the call expression
   *  using the previously parsed expression as the callee.
   *  The returned expression becomes the new expr and we loop to see if the result is itself called.
   * @return
   */
  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }


  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(TokenType.COMMA));
    }

    Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

    return new  Expr.Call(callee, paren, arguments);
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

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
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
