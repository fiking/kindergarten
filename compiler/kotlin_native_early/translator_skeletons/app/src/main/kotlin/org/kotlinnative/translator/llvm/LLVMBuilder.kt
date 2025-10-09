package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder(val arm: Boolean) {
    private var llvmLocalCode : StringBuilder = StringBuilder()
    private var llvmGlobalCode: StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0

    init {
        initBuilder()
    }
    private fun initBuilder() {
        val memcpy = "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)"
        llvmLocalCode.appendLine(memcpy)
        if (arm) {
            val funcAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
            llvmLocalCode.appendLine(funcAttributes)
        }
    }

    fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, kotlinName, LLVMLocalScope(), pointer)
    }

    fun getNewLabel(scope: LLVMScope = LLVMLocalScope(), prefix: String) : LLVMLabel {
        labelCount++
        return LLVMLabel("%lablel.$prefix.$labelCount", scope)
    }

    fun addLLVMCode(code : String) {
        llvmLocalCode.appendLine(code)
    }

    override fun toString(): String = llvmGlobalCode.toString() + llvmLocalCode.toString()

    fun clean() {
        llvmLocalCode = StringBuilder()
        initBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmLocalCode.appendLine("$llvmVariable = $rhs")
    }

    fun addStartExpression() {
        llvmLocalCode.appendLine("{")
    }

    fun addEndExpression() {
        llvmLocalCode.appendLine("}")
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
        is LLVMVariable -> if (firstOp.pointer == 0) firstOp else loadAndGetVariable(firstOp)
        else -> throw UnsupportedOperationException()
    }

    fun loadAndGetVariable(source: LLVMVariable) : LLVMVariable {
        assert(source.pointer > 0)
        val target = getNewVariable(source.type,source.pointer, source.kotlinName)
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type.align}"
        llvmLocalCode.appendLine(code)
        return target
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        llvmLocalCode.appendLine("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmLocalCode.appendLine("ret void")
    }

    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue) {
        llvmLocalCode.appendLine("ret $type $value")
    }

    fun copyVariableValue(from: LLVMVariable, to: LLVMVariable) {
        val tmp = getNewVariable(from.type, from.pointer)
        llvmLocalCode.appendLine("$tmp = load ${tmp.type}, ${from.getType()} $from, align ${tmp.type.align}")
        llvmLocalCode.appendLine("store ${to.type} $tmp, ${to.getType()} $to, align ${tmp.type.align}")

    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) : LLVMVariable {
        val allocVar = LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, LLVMLocalScope(), 1)
        addVariableByRef(allocVar, llvmVariable, store)
        return allocVar
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type.align}"
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.type}, ${source.type}* $source, i32, 0, i32 $offset"
        llvmLocalCode.appendLine(code)
    }

    fun storeVariable(target: LLVMVariable, source: LLVMSingleValue) {
        val code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        llvmLocalCode.appendLine(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        llvmLocalCode.appendLine("$targetVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type.align}")
        if (store) {
            llvmLocalCode.appendLine("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()}* $targetVariable, align ${targetVariable.type.align}")
        }
    }

    fun addConstant(allocVariable: LLVMVariable, constantValue: LLVMConstant) {
        llvmLocalCode.appendLine("$allocVariable   = alloca ${allocVariable.getType()}, align ${allocVariable.type.align}")
        llvmLocalCode.appendLine("store ${allocVariable.getType()} $constantValue, ${allocVariable.getType()} $allocVariable, align ${allocVariable.type.align}")
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        llvmLocalCode.appendLine(code)
    }

    fun allocaVar(target: LLVMVariable) {
        llvmLocalCode.appendLine("$target = alloca ${target.type}, align ${target.type.align}")
    }

    fun bitcast(src: LLVMVariable, llvmType: LLVMType) : LLVMVariable {
        val empty = getNewVariable(llvmType, 1)
        val code = "$empty = bitcast ${src.getType()} $src to $llvmType"
        llvmLocalCode.appendLine(code)
        return empty
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        llvmLocalCode.appendLine(code)
    }

    fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
        llvmLocalCode.appendLine("br ${condition.getType()} $condition, label $thenLabel, label $elseLabel")
    }

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            llvmLocalCode.appendLine("${label.label}: ")
    }

    fun addNopInstruction() {
        llvmLocalCode.appendLine(getNewVariable(LLVMIntType()).toString() + " = add i1 0, 0    ; nop instruction")
    }

    fun addUnconditionalJump(label: LLVMLabel) {
        llvmLocalCode.appendLine("br label $label")
    }

    fun declareGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) {
        llvmLocalCode.appendLine("$variable = global ${variable.type} $defaultValue, align ${variable.type.align}")
    }

    fun addStringConstant(variable: LLVMVariable, value: String) {
        val type = variable.type as LLVMStringType
        llvmGlobalCode.appendLine("$variable = private unnamed_addr constant  ${type.fullType()} c\"$value\\00\", align 1")
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val stringType = source.type as LLVMStringType
        val code = "store ${target.type} getelementptr inbounds (${stringType.fullType()}, " +
                "${stringType.fullType()}* $source, i32 0, i32 $offset), ${target.getType()} $target, align ${stringType.align}"
        llvmLocalCode.appendLine(code)
    }

    fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) {
        val code = "$target = getelementptr inbounds ${target.type}, ${source.type} $source, ${index.type} ${index.value}"
        llvmLocalCode.appendLine(code)
    }
}