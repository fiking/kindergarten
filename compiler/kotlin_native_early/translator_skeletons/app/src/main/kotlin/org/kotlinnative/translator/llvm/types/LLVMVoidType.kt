package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {
    override fun toString(): String = "void"
    override val align: Int = 0
    override var size: Int = 0
    override fun mangle() = ""
    override val typename = "void"
    override val defaultValue: String = ""

    override fun equals(other: Any?): Boolean {
        return other is LLVMVoidType
    }

    override fun hashCode() =
        typename.hashCode()
}