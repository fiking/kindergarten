package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope
import org.kotlinnative.translator.llvm.types.LLVMReferenceType

class ObjectCodegen(state: TranslationState,
                    variableManager: VariableManager,
                    val objectDeclaration: KtObjectDeclaration,
                    codeBuilder: LLVMBuilder,
                    parentCodegen: StructCodegen? = null) :
    StructCodegen(state, variableManager, objectDeclaration, state.bindingContext?.get(BindingContext.CLASS, objectDeclaration) ?: throw TranslationException(), codeBuilder, parentCodegen = parentCodegen) {
    override var size: Int = 0
    override var structName: String = objectDeclaration.name!!
    override val type: LLVMReferenceType = LLVMReferenceType(structName, "class", byRef = true)

    init {
        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }
        generateInnerFields(objectDeclaration.declarations)
    }

    override fun prepareForGenerate() {
        super.prepareForGenerate()
    }

    override fun generate() {
        super.generate()
        val classInstance = LLVMVariable("object.instance.$fullName", type, objectDeclaration.name, LLVMVariableScope(), pointer = 1)
        codeBuilder.addGlobalInitialize(classInstance, type)
        variableManager.addGlobalVariable(fullName, classInstance)
    }
}
