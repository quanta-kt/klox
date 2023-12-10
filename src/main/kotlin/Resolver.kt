import interpreter.Interpreter
import lexer.Token
import java.util.*

class Resolver(private val interpreter: Interpreter) : Stmt.Visitor<Unit>, Expr.Visitor<Unit> {

    private enum class FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        CONSTRUCTOR,
    }

    private enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS,
    }

    private val scopes = Stack<HashMap<String, Boolean>>()

    private var currentFunction: FunctionType = FunctionType.NONE
    private var currentClass: ClassType = ClassType.NONE

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
    }

    private fun resolve(statement: Stmt) {
        statement.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for ((inx, scope) in scopes.reversed().withIndex()) {
            if (scope.containsKey(name.lexeme)) {
                interpreter.resolve(expr, inx)
            }
        }
    }

    private fun resolveFunction(function: Expr.Function, declaration: FunctionType = FunctionType.FUNCTION) {
        val outerFun = currentFunction

        try {
            currentFunction = declaration

            withScope {
                for (param in function.params) {
                    declare(param)
                    define(param)
                }

                resolve(function.body)
            }
        } finally {
            currentFunction = outerFun
        }
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun <T> withScope(predicate: () -> T): T {
        beginScope()

        return try {
            predicate()
        } finally {
            endScope()
        }
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val curr = scopes.peek()

        if (curr.containsKey(name.lexeme)) {
            error(name, "a variable with this name already exists in this scope")
        }

        curr[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return

        val curr = scopes.peek()
        curr[name.lexeme] = true
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach(::resolve)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
        // Nothing to resolve
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            error(expr.name.line, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitFunctionExpr(expr: Expr.Function) {
        resolveFunction(expr)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.obj)
        resolve(expr.value)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        if (currentClass == ClassType.NONE) {
            error(expr.keyword, "Can't use 'super' outside of a class.")
        } else if (currentClass != ClassType.SUBCLASS) {
            error(expr.keyword, "Can't use 'super' in a class with no superclass.")
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitThisExpr(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            error(expr.keyword, "Can't use 'this' outside of a class")
            return
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        withScope { resolve(stmt.statements) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.statement)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt.function)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) error(stmt.keyword, "can't return outside a function")

        if (currentFunction == FunctionType.CONSTRUCTOR && stmt.value !=null) error(
            stmt.keyword,
            "Can't return a value from constructors"
        )

        if (stmt.value != null) resolve(stmt.value)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superClass != null) {
            if (stmt.superClass.name.lexeme == stmt.name.lexeme) {
                error(stmt.superClass.name, "A class can't inherit from itself.")
            }

            currentClass = ClassType.SUBCLASS

            resolve(stmt.superClass)

            beginScope()
            scopes.peek()["super"] = true
        }

        withScope {
            scopes.peek()["this"] = true
            stmt.methods.forEach { method ->
                val declaration = if (method.name.lexeme == "init") {
                    FunctionType.CONSTRUCTOR
                } else {
                    FunctionType.METHOD
                }

                resolveFunction(method.function, declaration)
            }
        }

        if (stmt.superClass != null) {
            endScope()
        }

        currentClass = enclosingClass
    }
}
