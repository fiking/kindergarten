package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMCall(val returnType: LLVMType, val name: String, val arguments: List<LLVMSingleValue>) : LLVMSingleValue(returnType) {
    override fun toString(): String = "call $returnType $name(${arguments.joinToString { "${it.getType()} ${it.toString()}" }})"
}