package org.kotlinnative.translator.llvm.types

class LLVMReferenceType(val type: String, var prefix: String = "", override val align: Int = 4, var byRef: Boolean = true) : LLVMType() {
    override fun toString() = "%$prefix${if (prefix.length > 0) "." else ""}${
        if (location.size > 0) "${location.joinToString(".")}." else ""
    }$type"

    override var size: Int = 4
    override val defaultValue = ""

    private val params = ArrayList<String>()
    val location = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}