package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMReferenceType(val type: String) : LLVMType() {
    override fun toString(): String = type
    override val align: Int = -1

    override fun operatorPlus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }

    override fun operatorMinus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }

    override fun operatorTimes(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }
}