package parser

import Expr
import Stmt
import error
import lexer.Token
import lexer.TokenType

class Parser(
    private val tokens: List<Token>
) {

    private var current: Int = 0

    fun parse(): List<Stmt> =
        buildList {
            while (!isAtEnd()) {
                declaration()?.let {
                    add(it)
                }
            }
        }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.VAR)) return varDeclaration()
            if (check(TokenType.FUN) && checkNext(TokenType.IDENTIFIER)) {
                consume(TokenType.FUN, "internal compiler error: expected fun to be the next token")
                return function("function")
            }
            if (match(TokenType.CLASS)) return loxClass()

            return statement()
        } catch (e: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected an identifier")

        val initializer: Expr? =
            if (match(TokenType.EQUAL)) {
                expression()
            } else {
                null
            }

        consume(TokenType.SEMICOLON, "Expected a ';' after variable declaration")

        return Stmt.Var(name, initializer)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "expected $kind name")
        val function = functionBody()

        return Stmt.Function(name, function)
    }

    private fun functionBody(): Expr.Function {
        consume(TokenType.LEFT_PAREN, "expected '('")

        val params = mutableListOf<Token>()

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "expected identifier in parameter"))
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RIGHT_PAREN, "expected ')' after parameters")
        consume(TokenType.LEFT_BRACE, "expected '{' after parameters")

        val body = block()

        return Expr.Function(params, body)
    }

    private fun loxClass(): Stmt.Class {
        val name = consume(TokenType.IDENTIFIER, "expected an identifier after 'class'")

        val superClass = if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expected superclass name")
            Expr.Variable(previous())
        } else {
            null
        }

        consume(TokenType.LEFT_BRACE, "expected '}'")

        val methods = mutableListOf<Stmt.Function>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "expected '}' after class declaration")

        return Stmt.Class(name, superClass, methods)
    }

    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.RETURN)) return returnStatement()

        return expressionStatement()
    }

    private fun ifStatement(): Stmt.If {
        consume(TokenType.LEFT_PAREN, "expected '(' before 'if'")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "expected ')' after condition")

        val thenBranch = statement()

        val elseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun whileStatement(): Stmt.While {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'")

        val init = if (match(TokenType.VAR)) {
            varDeclaration()
        } else if (!match(TokenType.SEMICOLON)) {
            expressionStatement()
        } else {
            null
        }

        val condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true)
        }

        consume(TokenType.SEMICOLON, "Expected semicolon after condition")

        val increment = if (!check(TokenType.SEMICOLON)) expression() else null

        consume(TokenType.RIGHT_PAREN, "expected ')'")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        body = Stmt.While(condition, body)

        if (init != null) {
            body = Stmt.Block(listOf(init, body))
        }

        return body
    }

    private fun returnStatement(): Stmt.Return {
        val keyword = previous()
        val expr = if (match(TokenType.SEMICOLON)) {
            null
        } else {
            val expr = expression()
            consume(TokenType.SEMICOLON, "expected semicolon")
            expr
        }

        return Stmt.Return(keyword, expr)
    }

    private fun printStatement(): Stmt.Print {
        val value = expression()
        consume(TokenType.SEMICOLON, "expected a ';' after statement.")
        return Stmt.Print(value)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }

        consume(TokenType.RIGHT_BRACE, "unclosed block")

        return statements
    }

    private fun expressionStatement(): Stmt.Expression {
        val expr = expression()
        consume(TokenType.SEMICOLON, "expected a ';' after statement.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val op = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.identifier, value)
            }

            error(op, "Expected r-value")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator: Token = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.STAR, TokenType.SLASH)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            return Expr.Unary(previous(), unary())
        }

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            when {
                match(TokenType.LEFT_PAREN) -> {
                    val arguments = mutableListOf<Expr>()

                    if (!check(TokenType.RIGHT_PAREN)) {
                        do {
                            if (arguments.size >= 255) {
                                error(peek(), "Can't have more than 255 arguments.")
                            }

                            val arg = expression()
                            arguments.add(arg)
                        } while (match(TokenType.COMMA))
                    }

                    val paren = consume(TokenType.RIGHT_PAREN, "expected ')' after arguments")

                    expr = Expr.Call(expr, paren, arguments)
                }

                match(TokenType.DOT) -> {
                    val name = consume(
                        TokenType.IDENTIFIER,
                        "expected identifier after '.'"
                    )

                    expr = Expr.Get(expr, name)
                }

                else -> return expr
            }
        }
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)
        if (match(TokenType.FUN)) return functionBody()
        if (match(TokenType.THIS)) return Expr.This(previous())

        if (match(TokenType.SUPER)) {
            val keyword = previous()
            consume(TokenType.DOT, "Expected '.' after 'super'")
            val method = consume(TokenType.IDENTIFIER, "Expected superclass method name")
            return Expr.Super(keyword, method)
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
            return expr
        }

        throw error(peek(), "Expected expression")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun match(vararg types: TokenType) =
        types.any(::check).also { if (it) advance() }

    private fun check(type: TokenType) = !isAtEnd() && peek().type === type

    private fun checkNext(type: TokenType) = when {
        isAtEnd() -> false
        else -> tokens[current + 1].type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun isAtEnd(): Boolean {
        return tokens[current].type == TokenType.EOF
    }

    private fun error(token: Token, message: String): ParseError {
        if (isAtEnd()) {
            error(token.line, "at end $message")
        } else {
            error(token.line, " at '${token.lexeme}' $message")
        }

        return ParseError(token, message)
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type === TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.PRINT,
                TokenType.RETURN -> return

                else -> {}
            }

            advance()
        }
    }
}