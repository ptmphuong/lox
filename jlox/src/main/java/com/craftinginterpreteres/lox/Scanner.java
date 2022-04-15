package com.craftinginterpreteres.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan for the lexeme, consumes it and any following characters that are part of it.
 * When the end of the lexeme is reached, it emits a token.
 *
 * Since hadError (in Main) gets set, we’ll never try to execute any of the code,
 * even though we keep going and scan the rest of it.
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
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      // matching single character
      case '(': addToken(TokenType.LEFT_PAREN); break;
      case ')': addToken(TokenType.RIGHT_PAREN); break;
      case '{': addToken(TokenType.LEFT_BRACE); break;
      case '}': addToken(TokenType.RIGHT_BRACE); break;
      case ',': addToken(TokenType.COMMA); break;
      case '.': addToken(TokenType.DOT); break;
      case '-': addToken(TokenType.MINUS); break;
      case '+': addToken(TokenType.PLUS); break;
      case ';': addToken(TokenType.SEMICOLON); break;
      case '*': addToken(TokenType.STAR); break;

      // matching single character which can be followed but another character
      case '!':
        addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
        break;
      case '=':
        addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
        break;
      case '<':
        addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        break;
      case '>':
        addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.EQUAL);
        break;
      case '/':
        if (match('/')) {
          // ignore if // (comment)
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(TokenType.SLASH);
        }
        break;

      case ' ': case '\r': case 't': break;

      case '\n': line++; break;

      default:
        Lox.error(line, "Unexpected character,");
        break;
    }
  }


  /**
   * Matching the current character with the expected character.
   * Consume the current character if matched.
   * @param expected
   * @return
   */
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;
    current++;
    return true;
  }


  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }


  private char advance() {
    return source.charAt(current++);
  }


  private void addToken(TokenType type) {
    addToken(type, null);
  }


  private void addToken(TokenType type, Object liternal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, liternal, line));
  }


  /**
   * Check if we’ve consumed all the characters.
   * @return
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }

}
