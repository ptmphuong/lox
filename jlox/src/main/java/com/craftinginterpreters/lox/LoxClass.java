package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    final private Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int arity() {
        return 0;
    }

    /**
     * When you “call” a class, it instantiates a new LoxInstance
     * for the called class and returns it.
     * @param interpreter
     * @param arguments
     * @return
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        return instance;
    }
}
