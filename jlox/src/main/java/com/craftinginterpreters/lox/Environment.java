package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    this.enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(
        name,
        "Undefined variable '" + name.lexeme + "'."
    );
  }


  /**
   * Assign the value to the already defined variable
   * @param name
   * @param value
   */
  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(
            name,
            "Undefined variable '" + name.lexeme + "'."
    );
  }

  void assignAt(Integer distance, Token name, Object value) {
    ancestor(distance).values. put(name.lexeme, value);
  }


  /**
   * Define a new variable, or reassign it.
   * @param name
   * @param value
   */
  void define(String name, Object value) {
    values.put(name, value);
  }

  /**
   * Walk up the environment chain and return the value.
   * @param distance
   * @param name
   * @return
   */
  Object getAt(Integer distance, String name) {
    return ancestor(distance).values.get(name);
  }

  /**
   * Walk up the environment chain with the given distance.
   * @param distance
   * @return
   */
  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }
}
