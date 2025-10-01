package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getValueArgumentsInParentheses
import org.kotlinnative.translator.debug.debugPrintNode
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMBooleanType
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMDoubleType
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
        if (descriptor == null) {
            args = null
//                {
//                LLVMVariable("", LLVMVoidType())
//            }.

            returnType = LLVMVoidType()
        } else {
            args = descriptor.valueParameters.map {
                LLVMMapStandardType(it.name.toString(), it.type)
            }
            returnType = LLVMMapStandardType("", descriptor.returnType).type
        }
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
        evaluateCodeBlock(function.bodyExpression)

        if (returnType is LLVMVoidType) {
            codeBuilder.addVoidReturn()
        }

        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments() {
        args?.forEach {
            val loadVariable = LLVMVariable("%{it.label}", it.type, it.label, false)
            val allocVar = codeBuilder.loadArgument(loadVariable)
            variableManager.addVariable(it.label, allocVar, 2)
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

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(function.fqName.toString(), args, returnType, external, state.arm))
        return external
    }

    private fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel? = null, finishLabel: LLVMLabel? = null, scopeDepth: Int = 0) {
        codeBuilder.markWithLabel(startLabel)
        expressionWalker(expr, scopeDepth)
        codeBuilder.addUnconditionJump(finishLabel ?: return)
    }

    private fun expressionWalker(expr : PsiElement?, scopeDepth: Int = 0) {
        when(expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, scopeDepth + 1)
            is KtProperty -> evaluateLeafPsiElement(expr.firstChild as LeafPsiElement, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtCallExpression -> {
                val expression = evaluateCallExpression(expr) as LLVMCall
                codeBuilder.addLLVMCode(expression.toString())
            }
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }
        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth)
    }

    private fun evaluateExpression(expr: PsiElement?, scopeDepth: Int) : LLVMSingleValue? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr)
            is KtReferenceExpression -> evaluateReferenceExpression(expr)
            is KtIfExpression -> evaluateIfOperator(expr.firstChild as LeafPsiElement, scopeDepth + 1, true)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression): LLVMSingleValue? {
        val variableName = expr.firstChild.text
        return variableManager.getLLVMValue(variableName)
    }

    private fun evaluateCallExpression(expr: KtCallExpression): LLVMSingleValue? {
        val function = expr.firstChild.firstChild.text
        if (state.functions.containsKey(function)) {
            return evaluateFunctionCallExpression(expr)
        }
        if (state.classes.containsKey(function)) {
            return evaluateConstructorCallExpression(expr)
        }
        return null
    }

    private fun evaluateConstructorCallExpression(expr: KtCallExpression): LLVMSingleValue? {
        val function = expr.firstChild.firstChild
        val descriptor = state.classes[function.text] ?: return null
        val names = parseArgList(expr).mapIndexed { i: Int, s: String ->
            LLVMVariable(s, descriptor.fields[i].type, pointer = descriptor.fields[i].pointer)
        }.toList()
        return LLVMConstructorCall(
            descriptor.type, fun(thisVar): LLVMCall {
                val args = ArrayList<LLVMVariable>()
                args.add(thisVar)
                args.addAll(names)
                return LLVMCall(LLVMVoidType(), descriptor.constructorName, args)
            }
        )
    }

    private fun evaluateFunctionCallExpression(expr: KtCallExpression): LLVMSingleValue? {
        val function = expr.firstChild.firstChild
        val descriptor = state.functions[function.text] ?: return null
        val names = parseArgList(expr)

        return LLVMCall(descriptor.returnType, "@${descriptor.name}", descriptor.args?.mapIndexed {
            i: Int, variable: LLVMVariable ->
            LLVMVariable(names[i], variable.type, pointer = variable.pointer) } ?: listOf())
    }

    private fun parseArgList(expr: KtCallExpression): List<String> {
        val args = expr.getValueArgumentsInParentheses()
        val result = ArrayList<String>()

        for (arg in args) {
            var text = (arg as KtValueArgument).text
            if (text.startsWith("::")) {
                text = "@${text.substring(2)}"
            }
            result.add(text)
        }
        return result
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int) : LLVMVariable {
//        debugPrintNode(expr)
        val left = evaluateExpression(expr.firstChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken
        return codeBuilder.addPrimitiveBinaryOperation(operator,left, right)
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
        val type = when (expr.node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> LLVMBooleanType()
            KtNodeTypes.INTEGER_CONSTANT -> LLVMIntType()
            KtNodeTypes.FLOAT_CONSTANT -> LLVMDoubleType()
            KtNodeTypes.CHARACTER_CONSTANT -> LLVMCharType()
            else -> throw IllegalArgumentException("Unknown type")
        }
        return LLVMConstant(expr.node.firstChildNode.text, type, pointer = false)
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int) : LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.VAL_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.VAR_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfOperator(element, scopeDepth,false)
            KtTokens.WHILE_KEYWORD -> evaluateWhileOperator(element, scopeDepth)
            else -> null
        }
    }

    private fun evaluateValExpression(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val identifier = element.getNextSiblingIgnoringWhitespaceAndComments()
        val eq = identifier?.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val assignExpression = evaluateExpression(eq.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth) ?: return null
        when (assignExpression) {
            is LLVMVariable -> {
                val allocVar = variableManager.getVariable(identifier.text, LLVMIntType(), pointer = true)
                codeBuilder.allocaVar(allocVar)
                variableManager.addVariable(identifier.text, allocVar, scopeDepth)
                codeBuilder.copyVariableValue(assignExpression, allocVar)
            }
            is LLVMConstant -> {
                val newVar = variableManager.getVariable(identifier.text, LLVMIntType(), pointer = true)
                codeBuilder.addConstant(newVar, assignExpression)
                variableManager.addVariable(identifier.text, newVar, scopeDepth)
            }
            is LLVMConstructorCall -> {
                val result = LLVMVariable("%${identifier.text}", assignExpression.type)
                codeBuilder.allocaVar(result)
                result.pointer = true
                codeBuilder.addLLVMCode(assignExpression.call(result).toString())
            }
            else -> {
                codeBuilder.addAssignment(LLVMVariable("%${identifier.text}", LLVMIntType(), identifier.text), assignExpression)
            }
        }
        return null
    }

    fun evaluateReturnInstruction(element: LeafPsiElement, scopeDepth: Int) : LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next, scopeDepth) as LLVMSingleValue
        val retNativeValue = codeBuilder.receiveNativeValue(retVar)
        codeBuilder.addReturnOperator(retNativeValue)
        return null
    }

    private fun evaluateIfOperator(element: LeafPsiElement, scopeDepth: Int, containReture: Boolean) : LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val thenExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseKeyword = thenExpression.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseExpresssion = elseKeyword.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        return when (containReture) {
            false -> evaluateIBlock(
                condition.firstChild as KtBinaryExpression,
                thenExpression.firstChild,
                elseExpresssion.firstChild,
                scopeDepth + 1
            )
            true -> executeIfExpression(
                condition.firstChild as KtBinaryExpression,
                thenExpression.firstChild,
                elseExpresssion.firstChild,
                scopeDepth + 1
            )
        }
    }

    private fun evaluateIBlock(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpresssion: PsiElement?, scopeDepth: Int) : LLVMVariable? {
        val conditionResult = evaluateBinaryExpression(condition, scopeDepth + 1)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")
        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, scopeDepth + 1)
        evaluateCodeBlock(elseExpresssion, elseLabel, endLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(endLabel)
        return null
    }

    private fun executeIfExpression(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val conditionResult: LLVMVariable = evaluateBinaryExpression(condition, scopeDepth + 1)
        val variable = codeBuilder.getNewVariable(LLVMIntType(), true)
        codeBuilder.allocaVar(variable)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        val thenResultExpression = evaluateExpression(thenExpression, scopeDepth + 1)
        codeBuilder.storeVariable(variable, thenResultExpression ?: return null)
        codeBuilder.addUnconditionJump(endLabel)
        codeBuilder.markWithLabel(elseLabel)
        val elseResultExpression = evaluateExpression(elseExpression, scopeDepth + 1)
        codeBuilder.storeVariable(variable, elseResultExpression ?: return null)
        codeBuilder.addUnconditionJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return variable
    }

    private fun evaluateWhileOperator(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val bodyExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null

        return executeWhileBlock(condition.firstChild as KtBinaryExpression, bodyExpression.firstChild, scopeDepth)
    }

    private fun executeWhileBlock(condition: KtBinaryExpression, bodyExpression: PsiElement, scopeDepth: Int): LLVMVariable? {
        val conditionLable = codeBuilder.getNewLabel(prefix = "while")
        val bodyLable = codeBuilder.getNewLabel(prefix = "while")
        val exitLable = codeBuilder.getNewLabel(prefix = "while")

        codeBuilder.addUnconditionJump(conditionLable)
        codeBuilder.markWithLabel(conditionLable)
        val conditionResult = evaluateBinaryExpression(condition, scopeDepth + 1)

        codeBuilder.addCondition(conditionResult, bodyLable, exitLable)
        evaluateCodeBlock(bodyExpression, bodyLable, conditionLable, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLable)

        return null
    }
}
