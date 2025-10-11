package org.kotlinnative.translator.llvm.types

class LLVMStringType(override val length: Int, var isLoaded: Boolean = true) : LLVMArray, LLVMType("kotlin") {
    override var size: Int = 1
    override val align = 8
    override val defaultValue = ""
    override fun mangle() = "String"
    override val typename = "i8*"

    override fun basicType() = LLVMByteType()
    override fun toString(): String = "i8*"
    override fun fullType() = "[${length + 1} x i8]"

    override fun equals(other: Any?): Boolean =
        when (other) {
            is LLVMStringType -> this.length == other.length
            else -> false
        }

    override fun hashCode(): Int {
        return length * 31 + if (isLoaded) 1 else 0 +
                mangle().hashCode()
    }
}