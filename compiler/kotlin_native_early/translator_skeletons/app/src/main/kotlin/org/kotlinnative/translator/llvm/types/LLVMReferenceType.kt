package org.kotlinnative.translator.llvm.types

class LLVMReferenceType(val type: String, var prefix: String = "", override val align: Int = 4, var byRef: Boolean = false, val uncopyable: Boolean = false) : LLVMType() {
    override fun toString(): String = "%$prefix${if (prefix.length > 0) "." else ""}$type"

    override val size: Byte = 4
    override val defaultValue = ""

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}