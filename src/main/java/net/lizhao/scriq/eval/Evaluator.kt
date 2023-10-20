package net.lizhao.scriq.eval

import net.lizhao.scriq.eval.Value.Companion.VOID
import net.lizhao.scriq.py.Python3BaseVisitor
import net.lizhao.scriq.py.Python3Parser
import net.lizhao.scriq.py.Python3Parser.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.NotNull
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.math.pow

/**
 * The instances of classes that implement script language evaluator
 */
open class Evaluator : Python3BaseVisitor<Value?>() {
    // Array Ops
    override fun visitArray(ctx: ArrayContext): Value? {
        val sublist = visit(ctx.subscriptlist())
        return Value(sublist!!.value!!)
    }

    override fun visitSubscript_(ctx: Subscript_Context?): Value? {
        if (ctx != null) {
            if (ctx.COL() != null) {
                val first = if (ctx.getChild(0) is TestContext)
                    visit(ctx.test(0))?.asInt()
                else
                    0
                val sec = if (ctx.test().size == 2)
                    visit(ctx.test(1))?.asInt()
                else if (ctx.test().size == 1 && !(ctx.getChild(0) is TestContext))
                    visit(ctx.test(0))?.asInt()
                else
                    -1
                return Value(Pair<Int, Int>(first as Int, sec as Int))
            }
            return visitChildren(ctx)
        }
        return VOID
    }
    override fun visitSubscriptlist(ctx: SubscriptlistContext): Value? {
        val nArgs = if (ctx.subscript_() == null) 0 else ctx.subscript_().size
        if (nArgs > 0) {
            var first = visit(ctx.subscript_(0))!!
            when (first.value) {
                is Double, is Pair<*, *> -> {
                    var vArgs = listOf<Any>()
                    vArgs += first.value!!
                    if (nArgs>1) {
                        for (idx in 1 until nArgs) {
                            val sub = visit(ctx.subscript_(idx))!!
                            vArgs += sub.value!!
                        }
                    }
                    return Value(vArgs)
                }
                is List<*> -> {
                    var vArgs = listOf<Any>()
                    vArgs += first!!.value!!
                    if (nArgs>1) {
                        for (idx in 1 until nArgs) {
                            vArgs += visit(ctx.subscript_(idx))!!.value!!
                        }
                    }
                    return Value(vArgs)
                }
            }
        }
        return VOID
    }


    override fun visitAtomExpExpr(ctx: AtomExpExprContext): Value? {
        val name = ctx.atom_expr().NAME().text
        val trailer = visit(ctx.atom_expr().array())
        if ( memory[name]!!.isNDA)
            return memory[name]!!!!.asNDA()[trailer!!.asList()[0] as List<Any>]
        throw RuntimeException("not NDArray")
    }

    override fun visitAssignment_stmt(ctx: Assignment_stmtContext): Value? {
        val id = ctx.NAME().text // id is left-hand side of '='
        var ret = visit(ctx.expr()) ?: return VOID // compute value of expression on right

        memory[id] = when (ret.value) {
            is Value -> ret.value as Value?
            is List<*> -> Value(NDA(ret.asList()))
            else -> ret
        }
        return memory[id]
    }

    override fun visitNameAtom(ctx: NameAtomContext): Value? {
        val name = ctx.getText()
        val ret = memory[name]
        if (ret == null) {
            throw RuntimeException("no such variable: $ret")
        }
        return ret
    }

    override fun visitStringAtom(ctx: StringAtomContext): Value? {
        var str = ctx.getText()
        // strip quotes
        str = str.substring(1, str.length - 1).replace("\"\"", "\"")
        return Value(str)
    }

    override fun visitNumberAtom(ctx: NumberAtomContext): Value? {
        return Value(ctx.getText().toDouble())
    }

    override fun visitBooleanAtom(ctx: BooleanAtomContext): Value? {
        return Value(ctx.getText().toBoolean())
    }

    override fun visitNilAtom(ctx: NilAtomContext): Value? {
        return Value(null)
    }

    override fun visitParExpr(ctx: ParExprContext): Value? {
        return visit(ctx.expr())
    }

    override fun visitPowExpr(ctx: PowExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null && right != null) {
                return if (left.isFuture) {
                    // right as Future is not supported
                    Value(left.asFuture()!!.thenApply { l: Any -> (l as BigDecimal).pow(right.asDouble()!!.toInt()) })
                } else Value(left.asDouble()!!.pow(right.asDouble()!!.toInt()))
        }
        return VOID
    }

    override fun visitUnaryMinusExpr(ctx: UnaryMinusExprContext): Value? {
        val value = visit(ctx.expr())
        if (value != null) {
            return Value(-value.asDouble()!!)
        }
        return VOID
    }

    override fun visitNotExpr(ctx: NotExprContext): Value? {
        val value = visit(ctx.expr())
        if (value != null) {
            return Value(!value.asBoolean()!!)
        }
        return VOID
    }

    private fun arithmeticOps(op: Token, left: Value, right: Value): Value? {
        return when (op.type) {
            Python3Parser.MUL -> left * right
            Python3Parser.DIV -> left / right
            Python3Parser.MOD -> left % right
            Python3Parser.ADD -> left + right
            Python3Parser.SUB -> left - right
            Python3Parser.DOT -> left.asNDA().dot(right)
            else -> throw RuntimeException("unknown operator: " + Python3Parser.tokenNames[op.type])
        }
    }

    override fun visitMultiplicationExpr(@NotNull ctx: MultiplicationExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null && right != null) {
                return if (left.isFuture) {
                    if (right.isFuture) {
                        val combinedFuture = CompletableFuture.allOf(left.asFuture(), right.asFuture())
                        try {
                            combinedFuture.get()
                            arithmeticOps(ctx.op, left.asFuture()!!.get() as Value, right.asFuture()!!.get() as Value)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        } catch (e: ExecutionException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        Value(left.asFuture()!!.thenApply { l: Any -> arithmeticOps(ctx.op, l as Value, right) })
                    }
                } else {
                    if (right.isFuture) {
                        Value(right.asFuture()!!.thenApply { r: Any -> arithmeticOps(ctx.op, left, r as Value) })
                    } else {
                        arithmeticOps(ctx.op, left, right)
                    }
                }
        }
        return VOID
    }

    override fun visitAdditiveExpr(@NotNull ctx: AdditiveExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null) {
            if (right != null) {
                return if (left.isFuture) {
                    if (right.isFuture) {
                        val combinedFuture = CompletableFuture.allOf(left.asFuture(), right.asFuture())
                        Value(combinedFuture
                                .thenApply { i: Void? ->
                                    arithmeticOps(ctx.op,
                                            left.asFuture()!!.join() as Value, right.asFuture()!!.join() as Value)
                                })
                    } else {
                        Value(left.asFuture()!!.thenApply { l: Any -> arithmeticOps(ctx.op, l as Value, right) })
                    }
                } else {
                    if (right.isFuture) {
                        Value(right.asFuture()!!.thenApply { r: Any -> arithmeticOps(ctx.op, left, r as Value) })
                    } else {
                        arithmeticOps(ctx.op, left, right)
                    }
                }
            }
        }
        return VOID
    }

    override fun visitRelationalExpr(@NotNull ctx: RelationalExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        val comp = right?.asDouble()?.let { left?.asDouble()!!.compareTo(it) }
        if (comp != null) {
            return when (ctx.op.type) {
                Python3Parser.LT -> Value(comp < 0)
                Python3Parser.LTEQ -> Value(comp <= 0)
                Python3Parser.GT -> Value(comp > 0)
                Python3Parser.GTEQ -> Value(comp >= 0)
                else -> throw RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.type])
            }
        }
        return VOID
    }

    val epsilon = BigDecimal(1)
    override fun visitEqualityExpr(@NotNull ctx: EqualityExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null && right != null) {
                return when (ctx.op.type) {
                    Python3Parser.EQ -> if (left.isDouble && right.isDouble) Value(right.asDouble()?.let { Math.abs(left.asDouble()!!.minus(it)).compareTo(epsilon) < 0 }) else Value(left.equals(right))
                    Python3Parser.NEQ -> if (left.isDouble && right.isDouble) Value(right.asDouble()?.let { Math.abs(left.asDouble()!!.minus(it)).compareTo(epsilon) >=0 }) else Value(!left.equals(right))
                    else -> throw RuntimeException("unknown operator: " + Python3Parser.tokenNames[ctx.op.type])
                }
        }
        return VOID
    }

    override fun visitAndExpr(ctx: AndExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null && right != null) {
                return Value(left.asBoolean()!! && right.asBoolean()!!)
        }
        return VOID
    }

    override fun visitOrExpr(ctx: OrExprContext): Value? {
        val left = visit(ctx.expr(0))
        val right = visit(ctx.expr(1))
        if (left != null && right != null) {
            return Value(left.asBoolean()!! || right.asBoolean()!!)
        }
        return VOID
    }

    // log override
    override fun visitPrint_stmt(ctx: Print_stmtContext): Value? {
        val value = visit(ctx.expr())
        println(value)
        return value
    }

    override fun visitIf_stmt(ctx: If_stmtContext): Value? {
        val tests = ctx.test_block()
        var evaluatedBlock = false
        for (test in tests) {
            val evaluated = visit(test.expr())
            if (evaluated != null) {
                if (evaluated.asBoolean()!!) {
                    evaluatedBlock = true
                    // evaluate this block whose expr==true
                    visit(test.block())
                    break
                }
            }
        }
        if (!evaluatedBlock && ctx.block() != null) {
            // evaluate the else-stat_block (if present == not null)
            visit(ctx.block())
        }
        return VOID
    }

    override fun visitWhile_stmt(ctx: While_stmtContext): Value? {
        var value = visit(ctx.expr())
        if (value != null) {
            while (value!!.asBoolean()!!) {

                // evaluate the code block
                visit(ctx.block())
                if (memory["break"] != null) {
                    memory.remove("break")
                    break
                }

                // evaluate the expression
                value = visit(ctx.expr())
            }
        }
        return VOID
    }

    override fun visitBreak_stmt(ctx: Break_stmtContext): Value? {
        memory["break"] = VOID
        return VOID
    }

    override fun visitContinue_stmt(ctx: Continue_stmtContext): Value? {
        memory["continue"] = VOID
        return VOID
    }

    override fun visitReturn_stmt(ctx: Return_stmtContext): Value? {
        val ret = visit(ctx.expr())
        memory["return"] = ret
        return ret
    }

    override fun visitFuncCall(ctx: FuncCallContext): Value? {
        val nArgs = if (ctx.arguments() == null) 0 else ctx.arguments().expr().size
        val cArgs: Array<Class<*>?> = arrayOfNulls(nArgs)
        Arrays.fill(cArgs, Value::class.java)
        var mtd: Method? = null
        mtd = try {
            this.javaClass.getMethod(ctx.NAME().text, *cArgs)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
        var vArgs: Array<Value?> = arrayOfNulls(nArgs)
        if (nArgs > 0) {
            val sPos = ctx.getStart().startIndex
            if (funcPos != null && funcPos!![sPos] != null) {
                vArgs = funcPos!![sPos] as Array<Value?>
            } else {
                for (idx in 0 until nArgs) {
                    vArgs!![idx] = visit(ctx.arguments().expr(idx))
                }
                if (funcPos != null) {
                    funcPos!![sPos] = vArgs
                }
            }
        }
        return try {
            val ret = mtd?.invoke(this, *vArgs)
            if (ret is Value) {
                ret
            } else if (ret is CompletableFuture<*>) {
                Value(ret)
            } else {
                throw RuntimeException("unknown return type: ")
            }
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }

    override fun visitFuncExpr(ctx: FuncExprContext): Value? {
        return visitChildren(ctx)!!
    }

    override fun visit(tree: ParseTree): Value? {
        val v = super.visit(tree)
        val ret = memory["return"]
        return ret ?: v
    }

    /**
     * Memory used in evaluate.
     */
    private var memory: MutableMap<String, Value?> = HashMap()
    @Throws(ExecutionException::class, InterruptedException::class)
    fun evalAsync(tree: ParseTree, mem: MutableMap<String, Value?>): Value? {
        return eval(tree, mem, null, true)
    }

    /**
     * Map from start position of a function call in the script string to the arguments used to call the function.
     */
    private var funcPos: MutableMap<Int, Any?>? = null
    /**
     * Execute a script.
     * @param mem a predefined memory for execution.
     * @param posMap a predefined funcPos for execution.
     * @param async return a future or not.
     * @return the value returned from `return` statement in script.
     */
    /**
     * Execute a script.
     * @param mem a predefined memory for execution.
     * @return the value returned from `return` statement in script.
     */
    @JvmOverloads
    @Throws(ExecutionException::class, InterruptedException::class)
    fun eval(tree: ParseTree, mem: MutableMap<String, Value?>, posMap: MutableMap<Int, Any?>? = null, async: Boolean = false): Value? {
        memory = mem
        funcPos = posMap
        val ret = visit(tree)
        if (ret != null) {
            if (!ret.isFuture) {
                return ret
            }
            return if (async) {
                ret
            } else {
                ret.asFuture()!!.get() as Value
            }
        }
        return VOID
    }

    companion object {
        /**
         * Generate Tree in map data structure.
         * @param tree a Antlr ParseTree.
         * @param treeMap a map to keep generated tree. It can be serialised to json string, e.g., using Jackson
         * objectMapper.writeValueAsString(treeMap);
         */
        @JvmStatic
        fun genTree(tree: ParseTree, treeMap: MutableMap<String?, Any?>) {
            if (tree is TerminalNodeImpl) {
                val token = tree.getSymbol()
                treeMap["type"] = token.type
                treeMap["type"] = token.text
            } else {
                val children: MutableList<Map<String?, Any?>> = ArrayList()
                val name = tree.javaClass.simpleName.replace("context$".toRegex(), "")
                treeMap[name[0].lowercaseChar().toString() + name.substring(1)] = children
                if (tree is ParserRuleContext) {
                    treeMap["sLine"] = tree.getStart().line
                    treeMap["sPos"] = tree.getStart().charPositionInLine
                    treeMap["eLine"] = tree.getStop().line
                    treeMap["ePos"] = tree.getStop().charPositionInLine
                }
                for (i in 0 until tree.childCount) {
                    val nested: MutableMap<String?, Any?> = LinkedHashMap()
                    children.add(nested)
                    genTree(tree.getChild(i), nested)
                }
            }
        }
    }
}
