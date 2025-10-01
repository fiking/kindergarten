package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMDoubleType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType

fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false, arm: Boolean = false) =
    "${ if (declare) "declare" else "define"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable -> 
            "${s.getType()} ${s.label}"
        }?.joinToString() }) ${ if (arm) "#0 " else "" }"

fun LLVMMapStandardType(type: String) : LLVMType = when(type) {
    "Int" -> LLVMIntType()
    "Double" -> LLVMDoubleType()
    "Unit" -> LLVMVoidType()
    else -> LLVMReferenceType("%$type*")
}