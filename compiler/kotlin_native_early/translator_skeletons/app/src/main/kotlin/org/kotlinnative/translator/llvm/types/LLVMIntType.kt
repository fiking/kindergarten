package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMIntType() : LLVMType() {
    override fun toString(): String {
        return "i32"
    }

    override fun operatorPlus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        // TODO switch by types: int + double = int
        return LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")
    }

    override fun operatorTimes(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        return LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")
    }

    override fun operatorMinus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression {
        return LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")
    }
}