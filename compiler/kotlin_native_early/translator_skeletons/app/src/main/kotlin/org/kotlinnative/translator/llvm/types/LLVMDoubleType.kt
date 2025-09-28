package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMDoubleType() : LLVMType() {
    override fun operatorPlus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        return LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")
    }

    override fun operatorTimes(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        return LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")
    }

    override fun operatorMinus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        return LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")
    }
}