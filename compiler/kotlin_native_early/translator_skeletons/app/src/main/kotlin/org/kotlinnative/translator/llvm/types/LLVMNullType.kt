package org.kotlinnative.translator.llvm.types

class LLVMNullType(var basetype: LLVMType? = null) : LLVMReferred, LLVMType() {
    override val align: Int = 1
    override var size: Int = 0
    override val defaultValue: String = "null"

    override fun mangle() = ""

    override val typename = basetype?.typename ?: ""

    override fun equals(other: Any?): Boolean {
        return other is LLVMNullType
    }

    override fun parseArg(inputArg: String) = "null"
    override fun toString() = basetype?.toString() ?: ""
    override fun hashCode() =
        "null".hashCode()

}