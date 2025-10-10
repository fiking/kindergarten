package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty

class ProjectTranslator(val files: List<KtFile>, val state: TranslationState) {
    private var codeBuilder = state.codeBuilder

    fun generateCode(): String {
        codeBuilder.clean()
        files.map { addDeclarations(it) }
        generateProjectBody()
        return codeBuilder.toString()
    }

    fun addDeclarations(file: KtFile) {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    if (function.external) {
                        state.externalFunctions.put(function.name, function)
                    } else {
                        state.functions.put(function.name, function)
                    }
                }
                is KtClass -> {
                    val codegen = ClassCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.classes.put(declaration.name!!, codegen)
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
    }

    private fun generateProjectBody() {
        with (state) {
            properties.values.map { it.generate() }
            objects.values.map { it.generate() }
            classes.values.map { it.generate() }
            functions.values.filter { it.isExtensionDeclaration }.map { it.generate() }
            functions.values.filter { !it.isExtensionDeclaration }.map { it.generate() }
            externalFunctions.values.map { it.generate() }
        }
    }

}