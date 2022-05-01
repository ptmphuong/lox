package com.craftinginterpreters.lox;

/**
 * the runtime representation of an instance of a Lox class.
 */
public class LoxInstance {
    private LoxClass klass;

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
