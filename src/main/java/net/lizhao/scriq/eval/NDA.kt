package net.lizhao.scriq.eval

import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times


class NDA {
    var data: NDArray<*,*>? = null
    constructor(inNDarray: NDArray<*,*>) {
        data = inNDarray
    }
    constructor(inList: List<Any>) { //build ndarray from nested list
        var flattenedList: MutableList<Double> = ArrayList<Double>() // keep flatten list
        var dimList: MutableList<Int> = ArrayList<Int>() // keep shape of the list
        var mid = true;
        var root = true;

        fun List<*>.flatten(){
            if (mid && !root) {
                dimList.add(this.size)
            }
            for(i in this){
                root = false
                when(i){
                    is Double -> {
                        mid = false
                        flattenedList.add(i)
                    }
                    is List<*> -> {
                        i.flatten()
                    }
                }
            }
        }
        inList.flatten()
        data= mk.ndarray<Double, DN>(flattenedList.toList<Double>(), dimList.toIntArray())
    }

    operator fun get (trailer: List<Any>): Value {
        // if all index are single Int, reduce to Double
        if (trailer.isEmpty()) return Value(this)
        var mData: NDArray<*,*> = data!!
        for (idx in trailer.indices) {
            when (mData.shape.size) {
                1 -> {
                    when (trailer[idx]) {
                        // reduce to Double
                        is Double -> {
                            val ret = (mData as D1Array<Double>)[(trailer[idx] as Double).toInt()]
                            return Value(ret)
                        }

                        is Pair<*, *> -> {
                            val sub1 = (trailer[idx] as Pair<*, *>).first as Int
                            val sub2 = (trailer[idx] as Pair<*, *>).second as Int
                            val t: D1Array<Double> = (mData as D1Array<Double>).slice(Slice(sub1, if (sub2 == -1) data!!.shape[idx] -1 else sub2, 1))
                            mData = t
                        }
                        else -> throw RuntimeException("unknown type for multik: ")
                    }
                }
                2 -> {
                    when (trailer[idx]) {
                        is Double -> {
                            // reduce current dimension
                            when (mData.shape.size) {
                                2 -> {
                                    val t: MultiArray<Double, D1> = (mData as D2Array<Double>).view((trailer[idx] as Double).toInt(), idx - data!!.shape.size + 2)
                                    mData = t as D1Array<Double>
                                } //reduce 2nd dim
                                1 -> {
                                    val ret = (mData as D1Array<Double>)[(trailer[idx] as Double).toInt()]
                                    return Value(ret)
                                }
                            }
                        }
                        is Pair<*, *> -> {
                            val sub1 = (trailer[idx] as Pair<*, *>).first as Int
                            val sub2 = (trailer[idx] as Pair<*, *>).second as Int
                            val t: D2Array<Double> = (mData as D2Array<Double>).slice(Slice(sub1, if (sub2 == -1) data!!.shape[idx] -1 else sub2, 1), idx - data!!.shape.size + 2)
                            mData = t
                        }
                        else -> throw RuntimeException("unknown type for multik: ")
                    }
                }
                3 -> {
                    when (trailer[idx]) {
                        is Double -> {
                            when (mData.shape.size) {
                                3 -> {
                                    val t: MultiArray<Double, D2> = (mData as D3Array<Double>).view((trailer[idx] as Double).toInt(),  idx )
                                    mData = t as D2Array<Double>
                                }
                                2 -> {
                                    val t: MultiArray<Double, D1> = (mData as D2Array<Double>).view((trailer[idx] as Double).toInt(), idx )
                                    mData = t as D1Array<Double>
                                }
                                1 -> {
                                    val ret = (mData as D1Array<Double>)[(trailer[idx] as Double).toInt()]
                                    return Value(ret)
                                }
                            }
                        }
                        is Pair<*, *> -> {
                            val sub1 = (trailer[idx] as Pair<*, *>).first as Int
                            val sub2 = (trailer[idx] as Pair<*, *>).second as Int
                            val t: D3Array<Double> = (mData as D3Array<Double>).slice(Slice(sub1, if (sub2 == -1) data!!.shape[idx] -1 else sub2, 1), idx)
                            mData = t
                        }
                        else -> throw RuntimeException("unknown type for multik: ")
                    }
                }
                else -> throw RuntimeException("higher dimension not support yet")
            }
        }
        return Value(mData?.let { NDA(it) })
    }

    fun dot(right: Value): Value {
        return when (data?.shape?.size) {
            1 -> Value(data as D1Array<Double> dot right.asNDA().data as D1Array<Double>)
            else -> Value(Double.MIN_VALUE)
        }
    }

    operator fun plus(right: Value): NDA {
        return when (data?.shape?.size) {
            1 -> (NDA((data!!.asD1Array() as D1Array<Double>) + (right.asNDA().data as D1Array<Double>)))
            2 -> (NDA((data!!.asD2Array() as D2Array<Double>) + (right.asNDA().data as D2Array<Double>)))
            3 -> (NDA((data!!.asD3Array() as D3Array<Double>) + (right.asNDA().data as D3Array<Double>)))
            4 -> (NDA((data!!.asD4Array() as D4Array<Double>) + (right.asNDA().data as D4Array<Double>)))
            else -> throw RuntimeException("higher dimension to be supported")
        }
    }

    operator fun times(right: Value): NDA {
        return when (data?.shape?.size) {
            1 -> (NDA((data!!.asD1Array() as D1Array<Double>) * (right.asNDA().data as D1Array<Double>)))
            2 -> (NDA((data!!.asD2Array() as D2Array<Double>) * (right.asNDA().data as D2Array<Double>)))
            3 -> (NDA((data!!.asD3Array() as D3Array<Double>) * (right.asNDA().data as D3Array<Double>)))
            4 -> (NDA((data!!.asD4Array() as D4Array<Double>) * (right.asNDA().data as D4Array<Double>)))
            else -> throw RuntimeException("higher dimension to be supported")
        }
    }

    operator fun div(right: Value): NDA {
        return when (data?.shape?.size) {
            1 -> (NDA((data!!.asD1Array() as D1Array<Double>) / (right.asNDA().data as D1Array<Double>)))
            2 -> (NDA((data!!.asD2Array() as D2Array<Double>) / (right.asNDA().data as D2Array<Double>)))
            3 -> (NDA((data!!.asD3Array() as D3Array<Double>) / (right.asNDA().data as D3Array<Double>)))
            4 -> (NDA((data!!.asD4Array() as D4Array<Double>) / (right.asNDA().data as D4Array<Double>)))
            else -> throw RuntimeException("higher dimension to be supported")
        }
    }

    operator fun minus(right: Value): NDA {
        return when (data?.shape?.size) {
            1 -> (NDA((data!!.asD1Array() as D1Array<Double>) - (right.asNDA().data as D1Array<Double>)))
            2 -> (NDA((data!!.asD2Array() as D2Array<Double>) - (right.asNDA().data as D2Array<Double>)))
            3 -> (NDA((data!!.asD3Array() as D3Array<Double>) - (right.asNDA().data as D3Array<Double>)))
            4 -> (NDA((data!!.asD4Array() as D4Array<Double>) - (right.asNDA().data as D4Array<Double>)))
            else -> throw RuntimeException("higher dimension to be supported")
        }
    }
}