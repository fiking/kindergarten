package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMReferenceType(val type: String) : LLVMType() {
    override fun toString(): String = type
    override val align: Int = 4
    override val size: Byte = 4
}