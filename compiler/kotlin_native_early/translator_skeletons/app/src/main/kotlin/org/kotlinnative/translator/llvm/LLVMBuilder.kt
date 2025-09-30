package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.ConstantValueKind
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
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        addAssignment(newVar, llvmExpression)
        return newVar
    }

    fun addReturnOperator(llvmVariable: LLVMVariable) {
        llvmCode.appendLine("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendLine("ret void")
    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) {
        addVariableByRef(LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, true), llvmVariable, store)
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type?.align!!}"
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.type}, ${source.type}* $source, i32, 0, i32 offset"
        llvmCode.appendLine(code)
    }

    fun storeVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        llvmCode.appendLine(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        llvmCode.appendLine("$targetVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.align}")
        if (store) {
            llvmCode.appendLine("store ${sourceVariable.type} $sourceVariable, ${targetVariable.type}* $targetVariable, align ${targetVariable.type?.align}")
        }
    }

    fun addVariableByValue(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        val tmp = getNewVariable(targetVariable.type, true)
        llvmCode.appendLine("$tmp = alloca ${tmp.type}, align ${tmp.type?.align}")
        llvmCode.appendLine("store ${tmp.type} $sourceVariable, ${tmp.type}* $tmp, align ${tmp.type?.align}")
        llvmCode.appendLine("$targetVariable = load ${targetVariable.type}, ${targetVariable.type}* tmp, align ${targetVariable.type?.align}")
    }

    fun addConstant(sourceVariable: LLVMVariable): LLVMVariable {
        val target = getNewVariable(sourceVariable.type, sourceVariable.pointer)
        addVariableByValue(target, sourceVariable)
        return target
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        llvmCode.appendLine(code)
    }

    fun bitcast(dst: LLVMVariable, llvmType: LLVMType) : LLVMVariable {
        val empty = getNewVariable(llvmType, true)
        val code = "$empty = bitcast ${dst.getType()} $dst to $llvmType"
        llvmCode.appendLine(code)
        return empty
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        llvmCode.appendLine(code)
    }

    fun getNewVariable(type: LLVMType?, pointer: Boolean = false): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, "", pointer = pointer)
    }

}