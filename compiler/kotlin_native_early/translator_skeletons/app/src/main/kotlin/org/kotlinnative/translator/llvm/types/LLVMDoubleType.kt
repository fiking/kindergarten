package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMDoubleType() : LLVMType() {
    override fun toString(): String = "double"
    override val align: Int = 8
    override var size: Int = 8
    override val defaultValue = "0.0"
    override fun isPrimitive() = true
    override fun mangle() = "Double"

    override fun operatorPlus(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override fun operatorTimes(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")

    override fun operatorMinus(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")
}