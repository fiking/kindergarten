package org.kotlinnative.translator.llvm

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.types.LLVMBooleanType
import org.kotlinnative.translator.llvm.types.LLVMByteType
import org.kotlinnative.translator.llvm.types.LLVMCharType

import org.kotlinnative.translator.llvm.types.LLVMDoubleType
import org.kotlinnative.translator.llvm.types.LLVMFloatType
import org.kotlinnative.translator.llvm.types.LLVMFunctionType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMLongType
import org.kotlinnative.translator.llvm.types.LLVMNullType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMShortType
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType

fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false) =
    "${if (declare) "declare" else "define weak"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.getType()} ${if (s.type is LLVMReferenceType && !(s.type as LLVMReferenceType).byRef) "byval" else ""} %${s.label}"
        }?.joinToString()}) #0"

fun LLVMInstanceOfStandardType(name: String, type: KotlinType, scope: LLVMScope = LLVMRegisterScope(), state: TranslationState): LLVMVariable {
    val typeName = type.toString().dropLastWhile { it == '?' }
    val pointerMark = if (type.toString().last() == '?') 1 else 0
    return when {
        type.isFunctionTypeOrSubtype -> LLVMVariable(name, LLVMFunctionType(type, state), name, scope, pointer = 1)
        typeName == "Boolean" -> LLVMVariable(name, LLVMBooleanType(), name, scope, pointerMark)
        typeName == "Byte" -> LLVMVariable(name, LLVMByteType(), name, scope, pointerMark)
        typeName == "Char" -> LLVMVariable(name, LLVMCharType(), name, scope, pointerMark)
        typeName == "Short" -> LLVMVariable(name, LLVMShortType(), name, scope, pointerMark)
        typeName == "Int" -> LLVMVariable(name, LLVMIntType(), name, scope, pointerMark)
        typeName == "Long" -> LLVMVariable(name, LLVMLongType(), name, scope, pointerMark)
        typeName == "Float" -> LLVMVariable(name, LLVMFloatType(), name, scope, pointerMark)
        typeName == "Double" -> LLVMVariable(name, LLVMDoubleType(), name, scope, pointerMark)
        typeName == "String" -> LLVMVariable(name, LLVMStringType(0), name, scope, pointerMark)
        type.nameIfStandardType.toString() == "Nothing" -> LLVMVariable(name, LLVMNullType(), name, scope)
        type.isUnit() -> LLVMVariable("", LLVMVoidType(), name, scope)
        type.isMarkedNullable -> LLVMVariable(name, LLVMReferenceType(typeName, prefix = "class"), name, scope, pointer = pointerMark)
        else -> {
            val declarationDescriptor = type.constructor.declarationDescriptor!!
            val refName = declarationDescriptor.fqNameSafe.asString()
            val targetPackage = declarationDescriptor.fqNameSafe.parent().asString()
            val refType = state.classes[type.toString()]?.type ?: LLVMReferenceType(refName, packageName = targetPackage, align = TranslationState.pointerAlign, prefix = "class")

            val result = LLVMVariable(name, refType, name, scope, pointer = 1)
            var currentPrefix = ""
            for (currentLocation in type.getSubtypesPredicate().toString().split(".").dropLast(1)) {
                currentPrefix += (if (currentPrefix.length > 0) "." else "") + currentLocation
                refType.location.add(currentPrefix)
            }
            result
        }
    }
}

fun LLVMMapStandardType(type: KotlinType, state: TranslationState): LLVMType =
    LLVMInstanceOfStandardType("type", type, LLVMRegisterScope(), state).type

fun String.addBeforeIfNotEmpty(add: String): String {
    return if (this.length > 0) add + this else this
}

fun String.addAfterIfNotEmpty(add: String): String {
    return if (this.length > 0) this + add else this
}