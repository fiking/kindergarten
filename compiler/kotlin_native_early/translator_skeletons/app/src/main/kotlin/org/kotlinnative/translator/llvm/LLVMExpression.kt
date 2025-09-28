package org.kotlinnative.translator.llvm
import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

class LLVMExpression(val variableType: LLVMType, val llvmCode: String) : LLVMNode() {
    override fun toString(): String {
        return llvmCode
    }
}