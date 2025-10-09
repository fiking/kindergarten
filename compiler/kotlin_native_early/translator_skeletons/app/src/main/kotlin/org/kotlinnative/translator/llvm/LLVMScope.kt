package org.kotlinnative.translator.llvm

open class LLVMScope

class LLVMVariableScope : LLVMScope() {
    override fun toString(): String = "@"
}

class LLVMRegisterScope : LLVMScope() {
    override fun toString(): String = "%"
}