package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {
    override fun toString(): String = "void"
    override val align: Int = 0
    override val size: Byte = 0
}