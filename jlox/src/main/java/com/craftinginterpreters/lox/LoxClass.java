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
        LoxFunction initializer = findMethod("init");
        if (initializer != null) return initializer.arity();
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

        LoxFunction initializer = findMethod("init");
        // When a class is called, after the LoxInstance is created, we look for an “init” method.
        // If we find one, we immediately bind and invoke it just like a normal method call.
        // The argument list is forwarded along.
        // init() methods always return this
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }
}
