package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMConstant(value: String, type: LLVMType? = null, pointer: Int = 0) : LLVMSingleValue(type, pointer) {
    val value: String = type?.parseArg(value) ?: value
    override fun getType(): String = type.toString() + "*".repeat(pointer)
    override fun toString(): String = value
}