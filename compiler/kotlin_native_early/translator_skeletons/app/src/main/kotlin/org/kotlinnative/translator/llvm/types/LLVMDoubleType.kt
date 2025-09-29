package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMDoubleType() : LLVMType() {
    override fun toString(): String = "double"
    override val align: Int = 8
    override val size: Byte = 8

    override fun operatorPlus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override fun operatorTimes(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")

    override fun operatorMinus(
        result: LLVMVariable,
        firstOp: LLVMVariable,
        secondOp: LLVMVariable
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")
}