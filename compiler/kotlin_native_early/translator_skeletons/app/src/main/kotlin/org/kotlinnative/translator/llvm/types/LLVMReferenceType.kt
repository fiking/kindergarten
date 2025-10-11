package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

class LLVMReferenceType(val type: String,
                        var prefix: String = "",
                        override var align: Int = TranslationState.pointerAlign,
                        override var size: Int = TranslationState.pointerSize,
                        packageName: String = "",
                        var byRef: Boolean = true) : LLVMReferred, LLVMType(packageName) {
    override val typename: String
        get() = "$prefix${if (prefix.length > 0) "." else ""}" +
                "$type"

    override fun toString() = "%$typename"

    override val defaultValue: String = "null"
    override fun mangle() = "Ref_$type"

    val location = ArrayList<String>()

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp eq ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp ne ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun equals(other: Any?): Boolean {
        return (other is LLVMReferenceType) and (typename.equals((other as LLVMReferenceType).typename))
    }

    override fun hashCode() =
        typename.hashCode()
}