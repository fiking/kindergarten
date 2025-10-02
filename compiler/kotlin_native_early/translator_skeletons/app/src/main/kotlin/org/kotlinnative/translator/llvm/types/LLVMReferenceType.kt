package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMReferenceType(val type: String, val prefix: String = "") : LLVMType() {
    override fun toString(): String = "$prefix.$type"
    override val align: Int = 4
    override val size: Byte = 4

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}