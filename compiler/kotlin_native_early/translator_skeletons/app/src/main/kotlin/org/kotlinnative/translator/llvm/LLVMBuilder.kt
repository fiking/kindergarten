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

    override fun toString(): String = llvmCode.toString()

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmCode.appendLine("$llvmVariable = $rhs")
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
            KtTokens.EQ -> throw UnsupportedOperationException()
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        addAssignment(newVar, llvmExpression)
        return newVar
    }

    fun addReturnOperator(llvmVariable: LLVMVariable) {
        llvmCode.appendLine("ret i32 $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendLine("ret void")
    }

    fun loadVariable(llvmVariable: LLVMVariable) {
        addVariableByRef(llvmVariable, LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName))
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        llvmCode.appendLine("$sourceVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.getAlign()}")
        llvmCode.appendLine("store ${targetVariable.type} $targetVariable, ${targetVariable.type}* $sourceVariable, align ${targetVariable.type?.getAlign()}")
    }

    fun addVariableByValue(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        val tmp = getNewVariable(targetVariable.type)
        llvmCode.appendLine("$tmp = alloca ${tmp.type}, align ${tmp.type?.getAlign()}")
        llvmCode.appendLine("store ${tmp.type} $sourceVariable, ${tmp.type}* $tmp, align ${tmp.type?.getAlign()}")
        llvmCode.appendLine("$targetVariable = load ${targetVariable.type}, ${targetVariable.type}* tmp, align ${targetVariable.type?.getAlign()}")
    }

    fun addConstant(sourceVariable: LLVMVariable): LLVMVariable {
        val target = getNewVariable(sourceVariable.type)
        addVariableByValue(target, sourceVariable)
        return target
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        llvmCode.appendLine(code)
    }

    fun getNewVariable(type: LLVMType?): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type)
    }
}