package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMReferenceType(val type: String, var prefix: String = "", var isReturn: Boolean = false) : LLVMType() {
    override fun toString(): String = "%$prefix${if (prefix.length > 0) "." else ""}$type"
    override val align: Int = 4
    override val size: Byte = 4
    override val defaultValue = ""

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}