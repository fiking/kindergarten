package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMIntType() : LLVMType() {
    override fun toString(): String = "i32"
    override fun getAlign(): Int = 4

    override fun operatorPlus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun operatorTimes(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    override fun operatorMinus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")
}