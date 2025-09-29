package org.kotlinnative.translator.llvm.types

class LLVMCharType() : LLVMType() {
    override fun toString(): String = "i8"

    override val align = 1
    override val size: Byte = 1
}