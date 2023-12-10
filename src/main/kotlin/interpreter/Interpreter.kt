package interpreter

import Expr
import Stmt
import interpreter.loxObject.LoxCallable
import interpreter.loxObject.LoxClass
import interpreter.loxObject.LoxFunction
import interpreter.loxObject.LoxInstance
import lexer.Token
import lexer.TokenType
import error

object Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private val globals = Environment().apply {
        define("clock", object : LoxCallable {
            override val arity = 0
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                return System.currentTimeMillis().toDouble().div(1000)
            }
        })
    }

    private var environment = globals

    private val locals = mutableMapOf<Expr, Int>()

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (e: Return) {
            System.err.println("Error: used return outside function")
        } catch (e: RuntimeError) {
            error(e.token, e.message ?: "at token ${e.token}")
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    private fun lookupVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]

        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    private fun execute(statement: Stmt) = statement.accept(this)

    private fun evaluate(expr: Expr): Any? = expr.accept(this)

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = Interpreter.environment

        try {
            Interpreter.environment = environment

            for (statement in statements) {
                execute(statement)
            }
        } finally {
            Interpreter.environment = previous
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]

        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, left, right)
                left as Double - right as Double
            }

            TokenType.STAR -> {
                checkNumberOperand(expr.operator, left, right)
                left as Double * right as Double
            }

            TokenType.SLASH -> {
                checkNumberOperand(expr.operator, left, right)
                left as Double / right as Double
            }

            TokenType.LESS -> {
                checkNumberOperand(expr.operator, left, right)
                (left as Double) < right as Double
            }

            TokenType.GREATER -> {
                checkNumberOperand(expr.operator, left, right)
                (left as Double) > right as Double
            }

            TokenType.LESS_EQUAL -> {
                checkNumberOperand(expr.operator, left, right)
                (left as Double) <= right as Double
            }

            TokenType.GREATER_EQUAL -> {
                checkNumberOperand(expr.operator, left, right)
                (left as Double) >= right as Double
            }

            TokenType.EQUAL_EQUAL -> {
                left == right
            }

            TokenType.PLUS ->
                if (left is String) left + right.toString()
                else {
                    checkNumberOperand(expr.operator, left, right)
                    left as Double + right as Double
                }

            else -> null
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val function = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }

        if (function !is LoxCallable) {
            throw RuntimeError(expr.paren, "can only call functions and classes")
        }

        if (expr.arguments.size < function.arity) {
            throw RuntimeError(expr.paren, "too few arguments to the functions")
        }
        if (expr.arguments.size > function.arity) {
            throw RuntimeError(expr.paren, "too many arguments to the functions")
        }

        return function.call(this, arguments)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? = evaluate(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal): Any? = expr.value

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> -(right as Double)
            TokenType.BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookupVariable(expr.name, expr)
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        when (expr.operator.type) {
            TokenType.OR -> {
                if (!isTruthy(left)) {
                    return evaluate(expr.right)
                }
            }

            TokenType.AND -> {
                if (isTruthy(left)) {
                    return evaluate(expr.right)
                }
            }

            else -> {
                throw RuntimeError(expr.operator, "Internal compiler error: invalid logical operator ${expr.operator}")
            }
        }

        return left
    }

    override fun visitFunctionExpr(expr: Expr.Function): Any {
        val closure = environment
        return LoxFunction(null, expr, closure)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)

        if (obj is LoxInstance) {
            return obj.get(expr.identifier)
        }

        throw RuntimeError(expr.identifier, "Only class instances have properties.")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            val value = evaluate(expr.value)
            obj.set(expr.identifier, value)
            return value
        }

        throw RuntimeError(expr.identifier, "Only class instances have properties.")
    }

    override fun visitSuperExpr(expr: Expr.Super): Any {
        val distance = locals[expr]!!
        val superClass = environment.getAt(distance, "super") as LoxClass

        val obj = environment.getAt(distance - 1, "this") as LoxInstance

        val method = superClass.findMethod(expr.method.lexeme)
            ?: throw RuntimeError(expr.method, "Undefined property ${expr.method.lexeme}.")

        return method.bind(obj)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookupVariable(expr.keyword, expr)
    }

    private fun isTruthy(value: Any?) = value != null && value != false

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double) {
            throw RuntimeError(operator, "Operand must be a number")
        }
    }

    private fun checkNumberOperand(operator: Token, op1: Any?, op2: Any?) {
        checkNumberOperand(operator, op1)
        checkNumberOperand(operator, op2)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = if (stmt.initializer != null) {
            evaluate(stmt.initializer)
        } else {
            null
        }

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.statement)
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt.name.lexeme, stmt.function, environment)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val superClass = stmt.superClass?.let {
            val superClass = evaluate(it)

            if (superClass !is LoxClass) {
                throw RuntimeError(stmt.superClass.name, "Superclass must be a class.")
            }

            superClass
        }

        environment.define(stmt.name.lexeme, null)

        superClass?.let {
            environment = Environment(environment)
            environment.define("super", it)
        }

        val methods = stmt.methods.associate { method ->
            method.name.lexeme to LoxFunction(
                null,
                method.function,
                environment,
                isConstructor = method.name.lexeme == "init"
            )
        }

        val loxClass = LoxClass(stmt.name.lexeme, superClass, methods)

        if (superClass != null) environment = environment.enclosing!!

        environment.assign(stmt.name, loxClass)
    }

    private fun stringify(value: Any?): String {
        return value?.toString() ?: "nil"
    }
}