package interpreter.loxObject

import interpreter.Interpreter

class LoxClass(
    val name: String,
    private val superClass: LoxClass?,
    private val methods: Map<String, LoxFunction>
) : LoxCallable {
    override val arity: Int
        get() = findMethod("init")?.arity ?: 0

    fun findMethod(name: String): LoxFunction? {
        return methods[name] ?: superClass?.findMethod(name)
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        val constructor = findMethod("init")
        constructor?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString(): String {
        return "<class $name>"
    }
}
