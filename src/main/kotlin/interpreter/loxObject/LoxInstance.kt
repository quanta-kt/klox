package interpreter.loxObject

import interpreter.RuntimeError
import lexer.Token

class LoxInstance(private val klass: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method = klass.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw RuntimeError(
            name,
            "Undefined property '${name.lexeme}' on object of class '${klass.name}'"
        )
    }

    override fun toString(): String {
        return "<${klass.name} instance>"
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
