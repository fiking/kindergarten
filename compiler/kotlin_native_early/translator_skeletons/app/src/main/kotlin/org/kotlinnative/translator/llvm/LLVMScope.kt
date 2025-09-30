package org.kotlinnative.translator.llvm

open class LLVMScope

class LLVMGlobalScope : LLVMScope() {
    override fun toString(): String = "@"
}

class LLVMLocalScope : LLVMScope() {
    override fun toString(): String = "%"
}