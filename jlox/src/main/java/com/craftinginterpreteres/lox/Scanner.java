package com.craftinginterpreteres.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan for the lexeme, consumes it and any following characters that are part of it.
 * When the end of the lexeme is reached, it emits a token.
 */
public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();

  // offset of the index to the string
  private int start = 0; // the first character in the lexeme being scanned
  private int current = 0; // the character currently being considered
  private int line = 1;

  public Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanTokens();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  /**
   * Check if we’ve consumed all the characters.
   * @return
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }
}
