package org.kotlinnative.translator.llvm
import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

class LLVMExpression(val variableType: LLVMType, val llvmCode: String, val pointer: Int = 0) : LLVMNode() {
    override fun toString() = llvmCode
}