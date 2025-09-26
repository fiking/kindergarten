package org.kotlinnative.translator

class LLVMBuilder {
    private val llvmCode : StringBuilder = StringBuilder()

    constructor() {}

    fun addLLVMCode(code : String) {
        llvmCode.appendLine(code)
    }

    override fun toString(): String {
        return llvmCode.toString()
    }
}