package interpreter

import lexer.Token

class Environment(val enclosing: Environment? = null) {

    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Undefined variable: '${name.lexeme}'")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) return enclosing.assign(name, value)

        throw RuntimeError(name, "Undefined variable: '${name.lexeme}'")
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any?): Any? {
        return ancestor(distance).values.put(name.lexeme, value)
    }

    private fun ancestor(distance: Int): Environment {
        var env = this

        for (i in 0..<distance) {
            env = env.enclosing!!
        }

        return env
    }

    override fun toString(): String {
        return buildString {
            append("{")
            append(values.map { "${it.key}: ${it.value}" }.joinToString())
            append("} -> ")
            append(enclosing.toString())
        }
    }
}