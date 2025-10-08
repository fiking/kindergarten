package org.kotlinnative.translator.llvm.types

class LLVMStringType(override val length: Int) : LLVMArray, LLVMType() {
    override val size: Byte = 1
    override val align = 8
    override val defaultValue = ""

    override fun basicType() = LLVMCharType()
    override fun toString(): String = "i8*"
    override fun fullType() = "[${length + 1} x i8]"
}