package com.craftinginterpreteres.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  static boolean hadError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /**
   * Run the source code directly from the provided path.
   * @param path
   * @throws IOException
   */
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
  }

  /**
   * Prompt the user for the path, then run the source code from that path.
   * @throws IOException
   */
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.println("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
    }
  }

  /**
   * Load the source code and  run.
   * @param source
   */
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    for (Token token: tokens) {
      System.out.println(token);
    }
  }

  /**
   * Generate error
   * @param line
   * @param message
   */
  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where, String message) {
    System.err.println(
        String.format("[line %d]. Error %s: %s", line, where, message)
    );
    hadError = true;
  }
}
