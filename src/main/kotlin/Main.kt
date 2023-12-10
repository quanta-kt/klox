import interpreter.Interpreter
import lexer.Lexer
import lexer.Token
import parser.Parser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths


private var hadError = false
private var hadRuntimeError = false

fun error(line: Int, message: String) {
    report(line, "", message)
}

fun error(token: Token, message: String) {
    report(token.line, " at '${token.lexeme}'", message)
}

private fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

private fun parse(code: String): List<Stmt> {
    val lexer = Lexer(code)
    val tokens = lexer.scanTokens()
    return Parser(tokens).parse()
}

private fun run(code: String) {
    val statements = parse(code)
    if (hadError) return

    Resolver(Interpreter).resolve(statements)
    if (hadError) return

    Interpreter.interpret(statements)
}

fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes, Charsets.UTF_8))
}

fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        run(reader.readLine() ?: return)

        hadError = false
        hadRuntimeError = false
    }
}

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: lox [script]")
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}
