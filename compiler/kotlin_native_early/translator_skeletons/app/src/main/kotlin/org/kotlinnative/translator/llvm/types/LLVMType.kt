package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

abstract class LLVMType() : Cloneable {
    open fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) : LLVMExpression = throw UnimplementedException()
    open fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) : LLVMExpression = throw UnimplementedException()
    open fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) : LLVMExpression = throw UnimplementedException()
    open fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun parseArg(inputArg: String) = inputArg

    fun makeClone() = clone()

    abstract val align: Int
    abstract var size: Int
    abstract val defaultValue: String
}

fun parseLLVMType(type: String): LLVMType = when(type) {
    "i32" -> LLVMIntType()
    "i16" -> LLVMShortType()
    "i8" -> LLVMByteType()
    "i1" -> LLVMBooleanType()
    "double" -> LLVMDoubleType()
    "Unit" -> LLVMVoidType()
    else -> LLVMReferenceType(type)
}