package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

class LLVMBuilder {
    private var llvmCode : StringBuilder = StringBuilder()
    private var variableCount = 0

    constructor() {}

    fun addLLVMCode(code : String) {
        llvmCode.appendLine(code)
    }

    override fun toString(): String {
        return llvmCode.toString()
    }

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addStartExpression() {
        llvmCode.appendLine("{")
    }

    fun addEndExpression() {
        llvmCode.appendLine("}")
    }

    fun addPrimitiveBinaryOperation(
        operator: IElementType,
        left: LLVMVariable,
        right: LLVMVariable
    ): LLVMVariable {
        val newVar = getNewVariable(LLVMIntType())
        val llvmExpression = when (operator) {
            KtTokens.PLUS -> left.type!!.operatorPlus(newVar, left, right)
            KtTokens.MINUS -> left.type!!.operatorMinus(newVar, left, right)
            KtTokens.MUL -> left.type!!.operatorTimes(newVar, left, right)
            else -> throw UnsupportedOperationException("Unkbown binary operator")
        }

        addAssignment(newVar, llvmExpression)
        return newVar
    }

    fun addAssignment(llvmVariable: LLVMNode, assignExpression: LLVMNode) {
        llvmCode.appendLine("$llvmVariable = $assignExpression")
    }

    fun getNewVariable(type: LLVMType?): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type)
    }
}