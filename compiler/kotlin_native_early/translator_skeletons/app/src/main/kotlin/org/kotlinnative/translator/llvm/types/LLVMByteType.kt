package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

class LLVMByteType() : LLVMType() {
    override fun toString(): String = "i8"

    override val align = 1
    override val size: Byte = 1
    override val defaultValue = "0"

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp slt i8 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sgt i8 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sle i8 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sge i8 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp eq i8 $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp ne i8 $firstOp, $secondOp")
}