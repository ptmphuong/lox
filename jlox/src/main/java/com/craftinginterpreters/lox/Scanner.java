package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and",    TokenType.AND);
    keywords.put("class",  TokenType.CLASS);
    keywords.put("else",   TokenType.ELSE);
    keywords.put("false",  TokenType.FALSE);
    keywords.put("for",    TokenType.FOR);
    keywords.put("fun",    TokenType.FUN);
    keywords.put("if",     TokenType.IF);
    keywords.put("nil",    TokenType.NIL);
    keywords.put("or",     TokenType.OR);
    keywords.put("print",  TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super",  TokenType.SUPER);
    keywords.put("this",   TokenType.THIS);
    keywords.put("true",   TokenType.TRUE);
    keywords.put("var",    TokenType.VAR);
    keywords.put("while",  TokenType.WHILE);
  }

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
        addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        break;
      case '/':
        if (match('/')) {
          // ignore if // (comment)
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(TokenType.SLASH);
        }
        break;
      case ' ': case '\r': case '\t': break;
      case '\n': line++; break;

      // LITERALS
      case '"': string(); break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) { // RESERVED WORDS & IDENTIFIERS - maximal munch principle
          identifier();
        } else {
          Lox.error(line, "Unexpected character,");
        }
        break;
    }
  }


  private void identifier() {
    while (isAlphanumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
    addToken(type);
  }


  private void number() {
    while (isDigit(peek())) advance();

    if (peek() == '.' && isDigit(peekNext())) {
      advance(); // consume the "."
      while (isDigit(peek())) advance();
    }

    String value = source.substring(start, current);
    addToken(TokenType.NUMBER, Double.parseDouble(value));
  }


  /**
   * scan & consume a string literal
   * add it to the tokens list if it's a valid string literal.
   */
  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    advance(); // consume the closing ".


    String value = source.substring(start+1, current-1);
    addToken(TokenType.STRING, value);
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


  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }


  private boolean isAlpha(char c) {
    return (
        (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || c == '_'
        );
  }


  private boolean isAlphanumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }


  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }


  private char advance() {
    return source.charAt(current++);
  }


  private void addToken(TokenType type) {
    addToken(type, null);
  }


  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }


  /**
   * Check if we’ve consumed all the characters.
   * @return
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }

}
