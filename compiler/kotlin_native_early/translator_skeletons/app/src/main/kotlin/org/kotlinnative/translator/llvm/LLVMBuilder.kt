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
        val newVar = getNewVariable(::LLVMIntType)
        val llvmOperator = when (operator) {
            KtTokens.PLUS -> "add nsw i32"
            KtTokens.MINUS -> "sub nsw i32"
            KtTokens.MUL -> "mul nsw i32"
            else -> throw UnsupportedOperationException("Unkbown binary operator")
        }

        llvmCode.appendLine("$newVar = $llvmOperator $left, $right")
        return newVar
    }

    fun getNewVariable(type: KFunction0<LLVMType>?): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type?.invoke())
    }
}