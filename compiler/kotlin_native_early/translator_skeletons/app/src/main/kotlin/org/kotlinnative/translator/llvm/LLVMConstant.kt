package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMConstant(value: String, type: LLVMType, pointer: Int = 0) : LLVMSingleValue(type, pointer) {
    val value: String = type.parseArg(value)

    override fun toString(): String = value
}