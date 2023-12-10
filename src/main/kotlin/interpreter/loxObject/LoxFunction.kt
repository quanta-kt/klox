package interpreter.loxObject

import Expr
import interpreter.Environment
import interpreter.Interpreter
import interpreter.Return

class LoxFunction(
    private val name: String?,
    private val function: Expr.Function,
    private val closure: Environment,
    private val isConstructor: Boolean = false,
) : LoxCallable {

    override val arity: Int = function.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)

        for ((param, arg) in function.params.zip(arguments)) {
            environment.define(param.lexeme, arg)
        }

        try {
            interpreter.executeBlock(function.body, environment)
        } catch (returnValue: Return) {
            if (isConstructor) return closure.getAt(0, "this")

            return returnValue.value
        }

        return if (isConstructor) closure.getAt(0, "this") else null
    }

    override fun toString(): String {
        return "<fun ${name ?: ""}(${function.params.joinToString { it.lexeme }})>"
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(name, function, environment, isConstructor)
    }
}