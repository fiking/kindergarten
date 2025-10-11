package org.kotlinnative.translator.llvm
import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

class LLVMExpression(type: LLVMType, val llvmCode: String, pointer: Int = 0) : LLVMSingleValue(type, pointer) {
    override fun toString() = llvmCode
}