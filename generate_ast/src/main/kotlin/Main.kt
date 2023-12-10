import java.io.PrintWriter
import kotlin.system.exitProcess

fun defineAst(outputDir: String, baseName: String, types: List<String>) {

    val path = "$outputDir/$baseName.kt"


    PrintWriter(path, "UTF-8").use { writer ->
        writer.println("import lexer.Token")

        writer.println("sealed interface $baseName {")

        writer.println("fun <R> accept(visitor: Visitor<R>): R")

        defineVisitor(writer, baseName, types)

        for (type in types) {
            val className = type.split(":")[0].trim()
            val fields = type.split(":")[1].trim()

            defineType(writer, baseName, className, fields)
        }

        writer.println("}")
    }
}

fun defineVisitor(
    writer: PrintWriter,
    baseName: String,
    types: List<String>
) {
    writer.println("interface Visitor<R> {")

    for (type in types) {
        val typeName = type.split(":")[0].trim()
        writer.println("fun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
    }

    writer.println("}")
}

fun defineType(
    writer: PrintWriter,
    baseName: String,
    className: String,
    fields: String
) {
    writer.println("data class $className(")

    for (field in fields.split(", ")) {
        val name = field.split(" ")[1]
        val type = field.split(" ")[0]
        writer.println("val $name: $type,")
    }

    writer.println("): $baseName {")

    writer.println("override fun <R> accept(visitor: Visitor<R>): R = visitor.visit$className$baseName(this)")

    writer.println("}")
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }

    val outputDir = args[0]

    defineAst(
        outputDir, "Expr", listOf(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Call     : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Unary    : Token operator, Expr right",
            "Variable : Token name",
            "Logical  : Expr left, Token operator, Expr right",
            "Function : List<Token> params, List<Stmt> body",
            "Get      : Expr obj, Token identifier",
            "Set      : Expr obj, Token identifier, Expr value",
            "Super    : Token keyword, Token method",
            "This     : Token keyword",
        )
    )

    defineAst(
        outputDir, "Stmt", listOf(
            "Expression : Expr expression",
            "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
            "Print      : Expr expression",
            "Var        : Token name, Expr? initializer",
            "Block      : List<Stmt> statements",
            "While      : Expr condition, Stmt statement",
            "Function   : Token name, Expr.Function function",
            "Return     : Token keyword, Expr? value",
            "Class      : Token name, Expr.Variable? superClass, List<Stmt.Function> methods",
        )
    )
}
