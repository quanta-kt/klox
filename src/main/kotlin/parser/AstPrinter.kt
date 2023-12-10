package parser

import Expr
import Stmt

object AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String> {
    fun print(expr: Expr) = expr.accept(this)

    fun print(stmt: Stmt) = stmt.accept(this)

    private fun paren(name: String, vararg expr: Expr) = buildString {
        append("($name")

        expr.forEach {
            append(" ")
            append(it.accept(this@AstPrinter))
        }

        append(")")
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        return "(${expr.name} = ${expr.value})"
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String = paren(expr.operator.lexeme, expr.left, expr.right)
    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String = paren("group", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal): String = expr.value?.toString() ?: "nil"

    override fun visitUnaryExpr(expr: Expr.Unary): String = paren(expr.operator.lexeme, expr.right)

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return "(${expr.name})"
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO("Not yet implemented")
    }

    override fun visitFunctionExpr(expr: Expr.Function): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(expr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(expr: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expr.Super): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): String {
        return "exprStmt ${print(stmt.expression)}"
    }

    override fun visitIfStmt(stmt: Stmt.If): String {
        return "(if ${print(stmt.condition)} (${print(stmt.thenBranch)}) ${stmt.elseBranch?.let { print(it) }})"
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String {
        return "print ${print(stmt.expression)}"
    }

    override fun visitVarStmt(stmt: Stmt.Var): String {
        return "(${stmt.name} = ${print(stmt.initializer)})"
    }

    override fun visitBlockStmt(stmt: Stmt.Block): String {
        return stmt.statements.joinToString(separator = "\n") {
            print(it)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While): String {
        return "(while (${print(stmt.condition)}) (${print(stmt.statement)}))"
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): String {
        TODO("Not yet implemented")
    }

    override fun visitReturnStmt(stmt: Stmt.Return): String {
        TODO("Not yet implemented")
    }

    override fun visitClassStmt(stmt: Stmt.Class): String {
        TODO("Not yet implemented")
    }
}
