import lexer.Token
sealed interface Expr {
fun <R> accept(visitor: Visitor<R>): R
interface Visitor<R> {
fun visitAssignExpr(expr: Assign): R
fun visitBinaryExpr(expr: Binary): R
fun visitCallExpr(expr: Call): R
fun visitGroupingExpr(expr: Grouping): R
fun visitLiteralExpr(expr: Literal): R
fun visitUnaryExpr(expr: Unary): R
fun visitVariableExpr(expr: Variable): R
fun visitLogicalExpr(expr: Logical): R
fun visitFunctionExpr(expr: Function): R
fun visitGetExpr(expr: Get): R
fun visitSetExpr(expr: Set): R
fun visitSuperExpr(expr: Super): R
fun visitThisExpr(expr: This): R
}
data class Assign(
val name: Token,
val value: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitAssignExpr(this)
}
data class Binary(
val left: Expr,
val operator: Token,
val right: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitBinaryExpr(this)
}
data class Call(
val callee: Expr,
val paren: Token,
val arguments: List<Expr>,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitCallExpr(this)
}
data class Grouping(
val expression: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitGroupingExpr(this)
}
data class Literal(
val value: Any?,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLiteralExpr(this)
}
data class Unary(
val operator: Token,
val right: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitUnaryExpr(this)
}
data class Variable(
val name: Token,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitVariableExpr(this)
}
data class Logical(
val left: Expr,
val operator: Token,
val right: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitLogicalExpr(this)
}
data class Function(
val params: List<Token>,
val body: List<Stmt>,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitFunctionExpr(this)
}
data class Get(
val obj: Expr,
val identifier: Token,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitGetExpr(this)
}
data class Set(
val obj: Expr,
val identifier: Token,
val value: Expr,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitSetExpr(this)
}
data class Super(
val keyword: Token,
val method: Token,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitSuperExpr(this)
}
data class This(
val keyword: Token,
): Expr {
override fun <R> accept(visitor: Visitor<R>): R = visitor.visitThisExpr(this)
}
}
