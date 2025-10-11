package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMShortType() : LLVMType() {
    override fun toString(): String = "i16"
    override val align: Int = 2
    override var size: Int = 2
    override val defaultValue: String = "0"
    override val typename = "i16"
    override fun isPrimitive() = true
    override fun mangle() = "Short"

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp slt i16 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sgt i16 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sle i16 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sge i16 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp eq i16" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp ne i16" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMShortType(), "srem i16 $firstOp, $secondOp")

    override fun equals(other: Any?): Boolean {
        return other is LLVMShortType
    }
    override fun hashCode() =
        mangle().hashCode()
}