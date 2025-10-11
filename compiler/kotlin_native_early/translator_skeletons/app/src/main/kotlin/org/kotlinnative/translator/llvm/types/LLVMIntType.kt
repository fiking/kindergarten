package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

class LLVMIntType() : LLVMType() {
    override fun toString(): String = "i32"
    override val align: Int = 4
    override var size: Int = 4
    override val defaultValue = "0"
    override fun isPrimitive() = true
    override fun mangle() = "Int"
    override val typename = "i32"

    override fun operatorPlus(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun operatorTimes(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    override fun operatorMinus(
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMExpression = LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "sdiv i32 $firstOp, $secondOp")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp slt i32 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sgt i32 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sle i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "icmp sge i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp eq i32" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMBooleanType(), "icmp ne i32" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "or i32 $firstOp, $secondOp")

    override fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "and i32 $firstOp, $secondOp")

    override fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "xor i32 $firstOp, $secondOp")

    override fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "shl i32 $firstOp, $secondOp")

    override fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "ashr i32 $firstOp, $secondOp")

    override fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "lshr i32 $firstOp, $secondOp")

    override fun operatorInc(firstOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, 1")

    override fun operatorDec(firstOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, 1")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
        LLVMExpression(LLVMIntType(), "srem i32 $firstOp, $secondOp")

    override fun convertFrom(source: LLVMSingleValue): LLVMExpression = when (source.type!!) {
        is LLVMBooleanType,
        is LLVMByteType,
        is LLVMCharType,
        is LLVMShortType -> LLVMExpression(LLVMBooleanType(), "  sext ${source.type} $source to i32")
        else -> throw UnimplementedException()
    }

    override fun equals(other: Any?): Boolean {
        return other is LLVMIntType
    }

    override fun hashCode() =
        mangle().hashCode()
}