package interpreter

class Return(val value: Any?) : RuntimeException(null, null, false, false)
