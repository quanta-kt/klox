package parser

import lexer.Token

class ParseError(
    val token: Token,
    message: String
) : RuntimeException(message)
