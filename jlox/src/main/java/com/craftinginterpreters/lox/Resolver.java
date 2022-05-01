package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * A static analysis to resolve variable bindings
 *      Capture closures
 *      Provide distance from the current scope to the enclosing scope where the value is declared
 *
 * Make sure return statement is declared within a function
 *
 * ------------------------------
 * Resolve bindings:
 *      A block statement introduces a new scope for the statements it contains.
 *
 *      A function declaration introduces a new scope for its body
 *      and binds its parameters in that scope.
 *
 *      A variable declaration adds a new variable to the current scope.
 *
 *      Variable and assignment expressions need to have their variables resolved.
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    // keeps track of the stack of scopes currently, uh, in scope.
    // Each element in the stack is a Map representing a single block scope.
    // Keys, as in Environment, are variable names.
    // The value associated with a key in the scope map represents whether
    // we have finished resolving that variable’s initializer
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    // make sure a return stmt is always declared inside a function.
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for(Expr argument: expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    /**
     * Variable and assignment expressions need to have their variables resolved.
     * @param expr
     * @return
     */
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {

        // If the variable exists in the current scope but its value is false,
        // that means we have declared it but not yet defined it.
        // We report that error.
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name,
                    "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    /**
     * A block statement introduces a new scope for the statements it contains.
     * @param stmt
     * @return
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    /**
     *
     * A function declaration introduces a new scope for its body
     * and binds its parameters in that scope.
     * @param stmt
     * @return
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // we define the name eagerly, before resolving the function’s body.
        // This lets a function recursively refer to itself inside its own body.
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(
                    stmt.keyword,
                    "Can't return from top-level code."
            );
        }
        if (stmt.value != null) resolve(stmt.value);
        return null;
    }

    /**
     * A variable declaration adds a new variable to the current scope.
     * @param stmt
     * @return
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement: statements) {
            resolve(statement);
        }
    }

    void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    /**
     * Declaration adds the variable to the innermost scope so that
     * it shadows any outer one and so that we know the variable exists.
     * We mark it as “not ready yet” by binding its name to false in the scope map.
     * @param name
     */
    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme)) {
            Lox.error(
                    name,
                    "Already a variable with this name in this scope."
            );
        }

        scope.put(name.lexeme, false);
    }

    /**
     * We set the variable’s value in the scope map to true
     * to mark it as fully initialized and available for use.
     * @param name
     */
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    /**
     * Each time it visits a variable, it tells the interpreter how many scopes there are
     * between the current scope and the scope where the variable is defined.
     *
     * At runtime, this corresponds exactly to the number of environments between
     * the current one and the enclosing one where the interpreter can find the variable’s value.
     * @param expr
     * @param name
     */
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function function, FunctionType functionType) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = functionType;

        beginScope();
        for (Token param: function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }
}
