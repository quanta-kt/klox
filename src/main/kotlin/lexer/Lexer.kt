package lexer

import error

class Lexer(private val code: String) {
    companion object {
        private var keywords = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE
        )
    }


    private val tokens = ArrayList<Token>()
    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", null, line))

        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when {
            c == '(' -> addToken(TokenType.LEFT_PAREN)
            c == ')' -> addToken(TokenType.RIGHT_PAREN)
            c == '{' -> addToken(TokenType.LEFT_BRACE)
            c == '}' -> addToken(TokenType.RIGHT_BRACE)
            c == ',' -> addToken(TokenType.COMMA)
            c == '.' -> addToken(TokenType.DOT)
            c == '-' -> addToken(TokenType.MINUS)
            c == '+' -> addToken(TokenType.PLUS)
            c == ';' -> addToken(TokenType.SEMICOLON)
            c == '*' -> addToken(TokenType.STAR)
            c == '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            c == '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            c == '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            c == '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            c == '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            c == '"' -> string()
            c == '\n' -> line++
            c in listOf(' ', '\r', '\t') -> {}

            c.isDigit() -> {
                number()
            }

            c.isLetter() || c == '_' -> {
                identifier()
            }

            else -> {
                error(line, "Unexpected character.")
                return
            }
        }
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            error(line, "Unterminated string.")
            return
        }

        // "
        advance()

        val value = code.substring(start + 1, current - 1)

        addToken(TokenType.STRING, value)
    }

    private fun number() {
        while (peek().isDigit()) advance()

        if (peek() == '.' && peekNext().isDigit()) {
            advance()

            while (peek().isDigit()) advance()
        }

        addToken(TokenType.NUMBER, code.substring(start, current).toDouble())
    }

    private fun identifier() {
        while (peek().isLetter() || peek() == '_' || peek().isDigit()) {
            advance()
        }

        val text = code.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER

        addToken(type)
    }

    private fun isAtEnd() = current >= code.length

    private fun advance() = code[current++]

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = code.substring(start, current)
        tokens.add(
            Token(
                type = type, lexeme = text, literal = literal, line = line
            )
        )
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (code[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return code[current]
    }

    private fun peekNext() = if (current + 1 >= code.length) '\u0000' else code[current + 1]
}