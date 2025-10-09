package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.utils.FunctionDescriptor

class FileTranslator(val state: TranslationState, val file: KtFile) {
    private var codeBuilder = LLVMBuilder(state.arm)
    fun generateCode(): String {
        codeBuilder.clean()
        generateFileBody()
        return codeBuilder.toString()
    }

    private fun generateFileBody() {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.functions.put(function.name, function)
                }
                is KtClass -> {
                    val classCodeGen = ClassCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.classes.put(declaration.name!!, classCodeGen)
                }
                is KtProperty -> {
                    val property = PropertyCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.properties.put(declaration.name!!, property)
                }
                is KtObjectDeclaration -> {
                    val property = ObjectCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.objects.put(declaration.name!!, property)
                }
            }
        }

        for (clazz in state.classes.values) {
            clazz.generate()
        }

        for (objectDeclaration in state.objects.values) {
            objectDeclaration.generate()
        }

        for (property in state.properties.values) {
            property.generate()
        }

        for (function in state.functions.values) {
            function.generate()
        }
    }
}