package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.types.ConstantValueKind
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

class LLVMBuilder(val arm: Boolean) {
    private var llvmCode : StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0

    init {
        initBuilder()
    }
    private fun initBuilder() {
        val memcpy = "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)"
        llvmCode.appendLine(memcpy)
        if (arm) {
            val funcAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
            llvmCode.appendLine(funcAttributes)
        }
    }

    fun getNewVariable(type: LLVMType, pointer: Boolean = false, kotlinName: String? = null): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, kotlinName, pointer)
    }

    fun getNewLabel(scope: LLVMScope = LLVMLocalScope(), prefix: String) : LLVMLabel {
        labelCount++
        return LLVMLabel("%lablel.$prefix.$labelCount", scope)
    }

    fun addLLVMCode(code : String) {
        llvmCode.appendLine(code)
    }

    override fun toString(): String = llvmCode.toString()

    fun clean() {
        llvmCode = StringBuilder()
        initBuilder()
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
        firstOp: LLVMSingleValue,
        secondOp: LLVMSingleValue
    ): LLVMVariable {
        val firstNativeOp = receiveNativeValue(firstOp)
        val secondNativeOp = receiveNativeValue(secondOp)
        val llvmExpression = when (operator) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
            KtTokens.LT -> firstOp.type!!.operatorLt(firstNativeOp, secondNativeOp)
            KtTokens.GT -> firstOp.type!!.operatorGt(firstNativeOp, secondNativeOp)
            KtTokens.LTEQ -> firstOp.type!!.operatorLeq(firstNativeOp, secondNativeOp)
            KtTokens.GTEQ -> firstOp.type!!.operatorGeq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQ -> firstOp.type!!.operatorEq(firstNativeOp, secondNativeOp)
            KtTokens.EXCLEQ -> firstOp.type!!.operatorNeq(firstNativeOp, secondNativeOp)
            KtTokens.EQ -> {
                val result = firstNativeOp as LLVMVariable
                storeVariable(result, secondNativeOp)
                return result
            }
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        val resultOp = getNewVariable(llvmExpression.variableType)
        addAssignment(resultOp, llvmExpression)
        return resultOp
    }

    fun receiveNativeValue(firstOp: LLVMSingleValue) : LLVMSingleValue = when (firstOp) {
        is LLVMConstant -> firstOp
        is LLVMVariable -> when (firstOp.pointer) {
            false -> firstOp
            else -> loadAndGetVariable(firstOp)
        }
        else -> throw UnsupportedOperationException()
    }

    fun loadAndGetVariable(source: LLVMVariable) : LLVMVariable {
        assert(!source.pointer)
        val target = getNewVariable(source.type,source.pointer, source.kotlinName)
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type.align}"
        llvmCode.appendLine(code)
        return target
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        llvmCode.appendLine("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendLine("ret void")
    }

    fun copyVariableValue(from: LLVMVariable, to: LLVMVariable) {
        val tmp = getNewVariable(from.type, from.pointer)
        llvmCode.appendln("$tmp = load ${tmp.type}, ${from.getType()} $from, align ${tmp.type.align}")
        llvmCode.appendln("store ${to.type} $tmp, ${to.getType()} $to, align ${tmp.type.align}")

    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) : LLVMVariable {
        val allocVar = LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, true)
        addVariableByRef(allocVar, llvmVariable, store)
        return allocVar
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type.align}"
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.type}, ${source.type}* $source, i32, 0, i32 $offset"
        llvmCode.appendLine(code)
    }

    fun storeVariable(target: LLVMVariable, source: LLVMSingleValue) {
        val code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        llvmCode.appendLine(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        llvmCode.appendLine("$targetVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type.align}")
        if (store) {
            llvmCode.appendLine("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()}* $targetVariable, align ${targetVariable.type.align}")
        }
    }

    fun addConstant(allocVariable: LLVMVariable, constantValue: LLVMConstant) {
        llvmCode.appendLine("$allocVariable   = alloca ${allocVariable.getType()}, align ${allocVariable.type.align}")
        llvmCode.appendLine("store ${allocVariable.getType()} $constantValue, ${allocVariable.getType()} $allocVariable, align ${allocVariable.type.align}")
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        llvmCode.appendLine(code)
    }

    fun allocaVar(target: LLVMVariable) {
        llvmCode.appendLine("$target = alloca ${target.type}, align ${target.type.align}")
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

    fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
        llvmCode.appendLine("br ${condition.getType()} $condition, label $thenLabel, label $elseLabel")
    }

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            llvmCode.appendLine("${label.label}: ")
    }

    fun addNopInstruction() {
        llvmCode.appendLine(getNewVariable(LLVMIntType()).toString() + " = add i1 0, 0    ; nop instruction")
    }

    fun addUnconditionJump(label: LLVMLabel) {
        llvmCode.appendLine("br label $label")
    }
}