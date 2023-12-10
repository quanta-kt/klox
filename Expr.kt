sealed interface Expr {
data class Binary(
left: left,
operator: operator,
right: right,
): Expr
data class Grouping(
expression: expression,
): Expr
data class Literal(
value: value,
): Expr
data class Unary(
operator: operator,
right: right,
): Expr
}
