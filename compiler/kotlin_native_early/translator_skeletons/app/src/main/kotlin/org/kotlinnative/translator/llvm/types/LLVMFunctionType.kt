package org.kotlinnative.translator.llvm.types

import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.LLVMInstanceOfStandardType
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMFunctionType(type: KotlinType, state: TranslationState) : LLVMType() {
    override val align: Int = 4
    override var size: Int = 4
    override val defaultValue: String = ""
    override val typename = "FunctionType"

    val arguments : List<LLVMVariable>
    val returnType : LLVMVariable

    init {
        val types = type.arguments.map { LLVMInstanceOfStandardType("", it.type, state = state) }.toList()
        returnType = types.last()
        arguments = types.dropLast(1)
    }

    override fun mangle() =
        "F.${arguments.joinToString(separator = "_", transform = { it.type.mangle() })}.EF"

    fun mangleArgs(): String =
        if (arguments.isNotEmpty()) "_${arguments.joinToString(separator = "_", transform = { it.type.mangle() })}" else ""

    override fun toString(): String = "${returnType.type} (${arguments.joinToString { it.getType() }})"

    override fun hashCode() =
        mangle().hashCode()

    override fun equals(other: Any?): Boolean {
        return (other is LLVMFunctionType) && (mangle() == other.mangle())
    }
}