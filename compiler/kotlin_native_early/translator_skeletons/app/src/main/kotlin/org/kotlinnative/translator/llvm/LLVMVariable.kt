package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

data class LLVMVariable(val label: String, val type: LLVMType? = null) : LLVMNode() {
    override fun toString(): String = label
}