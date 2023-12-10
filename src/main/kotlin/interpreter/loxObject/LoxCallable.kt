package interpreter.loxObject

import interpreter.Interpreter

interface LoxCallable {
    val arity: Int

    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}
