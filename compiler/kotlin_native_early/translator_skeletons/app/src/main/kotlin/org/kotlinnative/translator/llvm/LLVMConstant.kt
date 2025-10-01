package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMConstant(value: String, override val type: LLVMType? = null, override var pointer: Boolean = false) : LLVMSingleValue(type, pointer) {
    val value: String = type?.parseArg(value) ?: value
    override fun getType(): String = type.toString() + if (pointer) "*" else " "
    override fun toString(): String = value
}