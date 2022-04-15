package com.craftinginterpreters.lox;

/**
 * Literal value: converted textual representation of a value to the living runtime object
 *                that will be used by the interpreter later
 */
public class Token {
  final TokenType type;
  final String lexeme;
  final Object literal; // the value of that lexeme (if it's a literal TokenType)
  final int line;

  public Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  @Override
  public String toString() {
    return "Token{" +
        "type=" + type +
        ", lexeme='" + lexeme + '\'' +
        ", literal=" + literal +
        ", line=" + line +
        '}';
  }
}
