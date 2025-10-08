package org.kotlinnative.translator.llvm

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMDoubleType
import org.kotlinnative.translator.llvm.types.LLVMFunctionType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType

fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false, arm: Boolean = false) =
    "${ if (declare) "declare" else "define"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.getType()} ${if (s.type is LLVMReferenceType && !(s.type as LLVMReferenceType).isReturn) "byval" else ""} %${s.label}"
        }?.joinToString()}) ${ if (arm) "#0" else "" }"

fun LLVMMapStandardType(name: String, type: KotlinType?): LLVMVariable {
    if (type == null) return LLVMVariable("", LLVMVoidType())
    return when {
        type.isFunctionType -> LLVMVariable(
            name,
            LLVMFunctionType(type),
            type.toString(),
            pointer = true
        )

        type.toString() == "Int" -> LLVMVariable(name, LLVMIntType(), type.toString())
        type.toString() == "Double" -> LLVMVariable(name, LLVMDoubleType(), type.toString())
        type.toString() == "Char" -> LLVMVariable(name, LLVMCharType(), type.toString())
        type.isUnit() -> LLVMVariable("", LLVMVoidType())
        else -> LLVMVariable(name, LLVMReferenceType("$type"), name, pointer = true)
    }
}