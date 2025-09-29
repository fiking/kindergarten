package org.kotlinnative.translator.llvm.types

class LLVMShortType() : LLVMType() {
    override fun toString(): String = "i16"
    override val align: Int = 1
}