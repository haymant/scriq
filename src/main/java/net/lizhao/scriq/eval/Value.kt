package net.lizhao.scriq.eval

import net.lizhao.scriq.py.Python3Parser
import org.jetbrains.kotlinx.multik.ndarray.data.*
import java.util.concurrent.CompletableFuture

class Value(val value: Any?) {
    fun asInt(): Int? {
        return (value as Double).toInt()
    }
    fun asBoolean(): Boolean? {
        return value as Boolean?
    }

    fun asDouble(): Double? {
        return value as Double?
    }

    fun asString(): String {
        return value.toString()
    }

    fun asList(): List<Any> {
        return listOf(value) as List<Any>
    }

    fun asFuture(): CompletableFuture<Any>? {
        return value as CompletableFuture<Any>?
    }

    fun asNDA(): NDA {
        return value as NDA
    }

    val isDouble: Boolean
        get() = value is Double
    val isString: Boolean
        get() = value is String
    val isFuture: Boolean
        get() = value is CompletableFuture<*>
    val isList: Boolean
        get() = value is List<*>
    val isNDA: Boolean
        get() =  value is NDA

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun equals(o: Any?): Boolean {
        if (value === o) {
            return true
        }
        if (isDouble) {
            return if (o is Number)
                value == o.toDouble()
            else if (o is String)
                value == o.toDouble()
            else
                value == (o as Value?)!!.value
        }
        if (isString) {
            return value == o
        }
        return if (value == null || o == null || o.javaClass != this.javaClass)
            false
        else
            value == (o as Value).value
    }

    operator fun rem(right: Value): Value {
        return Value(right.asDouble()?.let { value as Double % it })
    }
    operator fun plus(right: Value): Value {
        return when (value) {
            is Double -> if (right.value is Double) Value (value + right.value) else Value((value as Double).toString() + right.value.toString())
            is String -> Value(value as String + right)
            is NDA -> Value(value as NDA + right)
            else -> throw RuntimeException("'+' doesn't support the type'")
        }
    }

    operator fun minus(right: Value): Value {
        return when (value) {
            is Double -> Value(right.asDouble()?.let { value as Double - it })
            is NDA -> Value(value as NDA - right)
            else -> throw RuntimeException("'-' doesn't support the type'")
        }
    }

    operator fun div(right: Value): Value {
        return when (value) {
            is Double -> Value(right.asDouble()?.let { value as Double / it })
            is NDA -> Value(value as NDA / right)
            else -> throw RuntimeException("'/' doesn't support the type'")
        }
    }

    operator fun times(right: Value): Value {
        return when (value) {
            is Double -> Value(right.asDouble()?.let { value as Double * it })
            is NDA -> Value(value as NDA * right)
            else -> throw RuntimeException("'*' doesn't support the type'")
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        var VOID = Value(Any())
    }
}
