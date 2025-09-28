package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {
    override fun toString(): String = "void"
    override fun getAlign(): Int = 0
}