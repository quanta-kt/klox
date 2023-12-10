import lexer.Token
sealed interface Stmt {
fun <R> accept(visitor: Visitor<R>): R
interface Visitor<R> {
fun visitExpressionStmt(stmt: Expression): R
fun visitIfStmt(stmt: If): R
fun visitPrintStmt(stmt: Print): R
fun visitVarStmt(stmt: Var): R
fun visitBlockStmt(stmt: Block): R
fun visitWhileStmt(stmt: While): R
fun visitFunctionStmt(stmt: Function): R
fun visitReturnStmt(stmt: Return): R
fun visitClassStmt(stmt: Class): R
}
data class Expression(
val expression: Expr,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitExpressionStmt(this)
}
data class If(
val condition: Expr,
val thenBranch: Stmt,
val elseBranch: Stmt?,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitIfStmt(this)
}
data class Print(
val expression: Expr,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitPrintStmt(this)
}
data class Var(
val name: Token,
val initializer: Expr?,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitVarStmt(this)
}
data class Block(
val statements: List<Stmt>,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitBlockStmt(this)
}
data class While(
val condition: Expr,
val statement: Stmt,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitWhileStmt(this)
}
data class Function(
val name: Token,
val function: Expr.Function,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitFunctionStmt(this)
}
data class Return(
val keyword: Token,
val value: Expr?,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitReturnStmt(this)
}
data class Class(
val name: Token,
val superClass: Expr.Variable?,
val methods: List<Stmt.Function>,
): Stmt {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitClassStmt(this)
}
}
