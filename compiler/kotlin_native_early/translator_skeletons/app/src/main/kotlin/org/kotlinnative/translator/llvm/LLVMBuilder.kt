package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMByteType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder(val arm: Boolean) {
    private val POINTER_SIZE = 4
    private var localCode : StringBuilder = StringBuilder()
    private var globalCode: StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0

    init {
        initBuilder()
    }
    private fun initBuilder() {
        val declares = arrayOf(
            "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)",
            if (arm) "declare i8* @malloc_static(i32)" else
                """
                    declare i8* @malloc(i32) #0
                    define i8* @malloc_static(i32 %size) #0 {
                      %1 = alloca i32, align 4
                      store i32 %size, i32* %1, align 4
                      %2 = load i32* %1, align 4
                      %3 = call i8* @malloc(i32 %2)
                      ret i8* %3
                    }
                """)
        declares.forEach { globalCode.appendLine(it) }
        if (arm) {
            val funcAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
            globalCode.appendLine(funcAttributes)
        }
    }

    fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null, scope: LLVMScope = LLVMRegisterScope()): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, kotlinName, scope, pointer)
    }

    fun getNewLabel(scope: LLVMScope = LLVMRegisterScope(), prefix: String) : LLVMLabel {
        labelCount++
        return LLVMLabel("%lablel.$prefix.$labelCount", scope)
    }

    fun addLLVMCode(code : String) {
        localCode.appendLine(code)
    }

    override fun toString(): String = globalCode.toString() + localCode.toString()

    fun clean() {
        localCode = StringBuilder()
        globalCode = StringBuilder()
        initBuilder()
    }

    fun addAssignment(lhs: LLVMVariable, rhs: LLVMNode) {
        localCode.appendLine("$lhs = $rhs")
    }

    fun addStartExpression() {
        localCode.appendLine("{")
    }

    fun addEndExpression() {
        localCode.appendLine("}")
    }

    fun receiveNativeValue(firstOp: LLVMSingleValue) : LLVMSingleValue = when (firstOp) {
        is LLVMConstant -> firstOp
        is LLVMVariable -> if (firstOp.pointer == 0) firstOp else loadAndGetVariable(firstOp)
        else -> throw UnsupportedOperationException()
    }

    fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
        assert(source.pointer > 0)
        val target = getNewVariable(source.type, source.pointer - 1, source.kotlinName)
        val code = "$target = load ${source.getType()} $source, align ${target.type.align}"
        localCode.appendln(code)
        return target
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        localCode.appendLine("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue) {
        localCode.appendLine("ret $type $value")
    }

    fun copyVariableValue(target: LLVMVariable, source: LLVMVariable) {
        var from = source
        if (source.pointer > 0) {
            from = getNewVariable(source.type, source.pointer)
            localCode.appendLine("$from = load ${source.getType()} $source, align ${from.type.align}")
        }
        localCode.appendLine("store ${target.type} $from, ${target.getType()} $target, align ${from.type.align}")
    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) : LLVMVariable {
        val allocVar = LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, LLVMRegisterScope(), pointer = llvmVariable.pointer + 1)
        addVariableByRef(allocVar, llvmVariable, store)
        return allocVar
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${source.getType()} $source, align ${target.type.align}"
        localCode.appendln(code)
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.getType()}* $source, i32, 0, i32 $offset"
        localCode.appendLine(code)
    }

    fun storeVariable(target: LLVMSingleValue, source: LLVMSingleValue) {
        val code = "store ${source.getType()} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        localCode.appendLine(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        localCode.appendLine("$targetVariable = alloca ${sourceVariable.type}${"*".repeat(sourceVariable.pointer)}, align ${sourceVariable.type.align}")
        if (store) {
            localCode.appendLine("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()}* $targetVariable, align ${targetVariable.type.align}")
        }
    }

    fun addConstant(allocVariable: LLVMVariable, constantValue: LLVMConstant) {
        localCode.appendLine("$allocVariable = alloca ${allocVariable.type}, align ${allocVariable.type.align}")
        localCode.appendLine("store ${allocVariable.type} $constantValue, ${allocVariable.getType()} $allocVariable, align ${allocVariable.type.align}")
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        localCode.appendLine(code)
    }

    fun allocStackVar(target: LLVMVariable) {
        localCode.appendLine("$target = alloca ${target.getType()}, align ${target.type.align}")
    }

    fun allocStaticVar(target: LLVMVariable) {
        val allocedVar = getNewVariable(LLVMByteType(), pointer = 1)
        val size = if (target.pointer > 0) POINTER_SIZE else target.type.size
        val alloc = "$allocedVar = call i8* @${if (arm) "malloc_static" else "malloc"}(i32 $size)"
        localCode.appendLine(alloc)
        val cast = "$target = bitcast ${allocedVar.getType()} $allocedVar to ${target.getType()}*"
        localCode.appendLine(cast)
    }

    fun bitcast(src: LLVMVariable, llvmType: LLVMVariable) : LLVMVariable {
        val empty = getNewVariable(llvmType.type, llvmType.pointer)
        val code = "$empty = bitcast ${src.getType()} $src to ${llvmType.getType()}"
        localCode.appendLine(code)
        return empty
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        localCode.appendLine(code)
    }

    fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
        localCode.appendLine("br ${condition.getType()} $condition, label $thenLabel, label $elseLabel")
    }

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            localCode.appendLine("${label.label}: ")
    }

    fun addNopInstruction() {
        localCode.appendLine(getNewVariable(LLVMIntType()).toString() + " = add i1 0, 0    ; nop instruction")
    }

    fun addUnconditionalJump(label: LLVMLabel) {
        localCode.appendLine("br label $label")
    }

    fun defineGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) {
        localCode.appendLine("$variable = global ${variable.type} $defaultValue, align ${variable.type.align}")
    }

    fun makeStructInitializer(args: List<LLVMVariable>, values: List<String>)
            = "{ ${args.mapIndexed { i: Int, variable: LLVMVariable -> "${variable.type} ${values[i]}" }.joinToString()} }"

    fun addStringConstant(variable: LLVMVariable, value: String) {
        val type = variable.type as LLVMStringType
        globalCode.appendLine("$variable = private unnamed_addr constant  ${type.fullType()} c\"$value\\00\", align 1")
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val stringType = source.type as LLVMStringType
        val code = "store ${target.type} getelementptr inbounds (${stringType.fullType()}, " +
                "${stringType.fullType()}* $source, i32 0, i32 $offset), ${target.getType()} $target, align ${stringType.align}"
        localCode.appendLine(code)
    }

    fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) {
        val code = "$target = getelementptr inbounds ${source.type} $source, ${index.type} ${index.value}"
        localCode.appendLine(code)
    }

    fun addGlobalIntialize(target: LLVMVariable, classType: LLVMType) {
        val code = "$target = internal global $classType zeroinitializer, align ${classType.align}"
        globalCode.appendLine(code)
    }

    fun storeNull(result: LLVMVariable) {
        val code = "store ${result.getType().dropLast(1)} null, ${result.getType()} $result, align $POINTER_SIZE"
        localCode.appendLine(code)
    }

    fun addComment(comment: String) {
        localCode.appendLine("; " + comment)
    }

    fun allocStackPointedVarAsValue(target: LLVMVariable) {
        localCode.appendLine("$target = alloca ${target.type}, align ${target.type.align}")
    }

    fun copyVariable(from: LLVMVariable, to: LLVMVariable) = when (from.type) {
        is LLVMStringType -> storeString(to, from, 0)
        else -> copyVariableValue(to, from)
    }
}