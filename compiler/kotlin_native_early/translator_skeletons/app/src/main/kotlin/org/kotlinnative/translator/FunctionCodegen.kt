package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.debug.debugPrintNode
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import org.kotlinnative.translator.llvm.types.parseLLVMType
import org.kotlinnative.translator.utils.FunctionArgument
import java.util.ArrayList
import kotlin.math.exp

class FunctionCodegen(val state: TranslationState, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {
    var name = function.fqName.toString()
    var returnType: LLVMType
    var args: List<LLVMVariable>?
    val variableManager = state.variableManager

    init {
        val descriptor = state.bindingContext?.get(BindingContext.FUNCTION,function)
        args = descriptor?.valueParameters?.map {
            LLVMVariable(it.name.toString(), LLVMMapStandardType(it.type.toString()))
        }
        returnType = LLVMMapStandardType(descriptor?.returnType.toString())
    }

    fun generate() {
        if (generateDeclaration()) {
            return
        }

        codeBuilder.addStartExpression()
        println("generate is start")
        debugPrintNode(function.bodyExpression)
        println("generate is end")
        generateLoadArguments()
        expressionWalker(function.bodyExpression)

        if (returnType is LLVMVoidType) {
            codeBuilder.addVoidReturn()
        }

        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments() {
        args?.forEach {
            val loadVariable = LLVMVariable("%{it.label}", it.type, it.label, false)
            codeBuilder.loadArgument(loadVariable)
            variableManager.addVariable(it.label, loadVariable, 2)
        }
    }

    private fun generateDeclaration() : Boolean {
        var external = false

        var keyword = function.firstChild
        while (keyword != null) {
            if (keyword.text == "external") {
                external = true
                break
            }

            keyword = keyword.getNextSiblingIgnoringWhitespaceAndComments()
        }

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(function.fqName.toString(), args, returnType, external))
        return external
    }

    private fun expressionWalker(expr : PsiElement?, scopeDepth: Int = 0) {
        when(expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, scopeDepth + 1)
            is KtProperty -> evaluateLeafPsiElement(expr.firstChild as LeafPsiElement, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }
        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth)
    }

    private fun evaluateExpression(expr: PsiElement?, scopeDepth: Int) : LLVMNode? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr)
            is KtReferenceExpression -> evaluateReferenceExpression(expr)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression): LLVMNode? {
        val variableName = expr.firstChild.text
        return variableManager.getLLVMValue(variableName)
    }

    private fun evaluateCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild.text
        if (state.functions.containsKey(function)) {
            return evaluateFunctionCallExpression(expr)
        }
        if (state.classes.containsKey(function)) {
            return evaluateConstructorCallExpression(expr)
        }
        return null
    }

    private fun evaluateConstructorCallExpression(expr: KtCallExpression): LLVMNode? {
        return null
    }

    private fun evaluateFunctionCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild
        val descriptor = state.functions[function.text] ?: return null
        val names = parseArgList(expr
            .firstChild
            .getNextSiblingIgnoringWhitespaceAndComments()
            ?.firstChild)

        return LLVMCall(descriptor.returnType, "@${function.text}", descriptor.args?.mapIndexed {
            i: Int, variable: LLVMVariable ->
            LLVMVariable(names[i], variable.type) } ?: listOf())
    }

    private fun parseArgList(argumentList: PsiElement?): List<String> {
        val args = ArrayList<String>()

        var currentArg = argumentList?.getNextSiblingIgnoringWhitespaceAndComments()

        while (currentArg?.text != ")" && currentArg != null) {
            args.add(currentArg.text)

            currentArg = currentArg
                .getNextSiblingIgnoringWhitespaceAndComments()
                ?.getNextSiblingIgnoringWhitespaceAndComments()
        }
        return args
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int) : LLVMNode {
//        debugPrintNode(expr)
        val left = evaluateExpression(expr.firstChild, scopeDepth) as LLVMSingleValue? ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) as LLVMSingleValue? ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken
        val newVar = codeBuilder.getNewVariable(LLVMIntType())
        return codeBuilder.addPrimitiveBinaryOperation(operator, newVar, left, right)
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int) : LLVMSingleValue? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(element)
            KtTokens.INTEGER_LITERAL -> null
            else -> null
        }
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression) : LLVMConstant {
        return LLVMConstant(expr.node.firstChildNode.text, LLVMIntType(), pointer = false)
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int) : LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.VAL_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.VAR_KEYWORD -> evaluateValExpression(element, scopeDepth)
            else -> null
        }
    }

    private fun evaluateValExpression(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val identifier = element.getNextSiblingIgnoringWhitespaceAndComments()
        val eq = identifier?.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val assignExpression = evaluateExpression(eq.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth) ?: return null
        when (assignExpression) {
            is LLVMVariable -> {
                assignExpression.kotlinName = identifier.text
                assignExpression.pointer = false
                variableManager.addVariable(identifier.text, assignExpression, scopeDepth)
                return null
            }
            is LLVMConstant -> {
                val newVar = LLVMVariable("%{identifier!!.text}.addr", type = LLVMIntType(), kotlinName = identifier.text, pointer = true)
                codeBuilder.addConstant(newVar, assignExpression)
                variableManager.addVariable(identifier.text, newVar, scopeDepth)
            }
            else -> {
                codeBuilder.addAssignment(LLVMVariable("%${identifier.text}", null, identifier.text), assignExpression)
            }
        }
        return null
    }

    fun evaluateReturnInstruction(element: LeafPsiElement, scopeDepth: Int) : LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next, scopeDepth) as LLVMSingleValue
        codeBuilder.addReturnOperator(retVar)
        return null
    }
}
