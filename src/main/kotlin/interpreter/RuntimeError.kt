package interpreter

import lexer.Token

class RuntimeError(
    val token: Token,
    message: String
) : RuntimeException(message)
