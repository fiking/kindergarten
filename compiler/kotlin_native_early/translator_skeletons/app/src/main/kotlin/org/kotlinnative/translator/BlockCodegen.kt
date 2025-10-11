package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.getFunctionResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.util.getValueArgumentsInParentheses
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.*
import java.rmi.UnexpectedException
import java.util.*


abstract class BlockCodegen(val state: TranslationState, val variableManager: VariableManager, val codeBuilder: LLVMBuilder) {

    val topLevel = 2
    var returnType: LLVMVariable? = null
    var wasReturnOnTopLevel = false


    fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel? = null, nextIterationLabel: LLVMLabel? = null, breakLabel: LLVMLabel? = null, scopeDepth: Int = 0, isBlock: Boolean = true) {
        codeBuilder.markWithLabel(startLabel)
        if (isBlock) {
            expressionWalker(expr, breakLabel, scopeDepth)
        } else {

            var result = evaluateExpression(expr, scopeDepth)!!
            when (result) {
                is LLVMVariable -> {
                    if (result.pointer == 1 && result.type !is LLVMReferenceType) {
                        result = codeBuilder.loadAndGetVariable(result)
                    }

                    if (result.type is LLVMReferenceType) {
                        genReferenceReturn(result)
                    } else {
                        codeBuilder.addReturnOperator(result)
                    }
                }
                else -> codeBuilder.addAnyReturn(result.type!!, result.toString())
            }

            wasReturnOnTopLevel = true
        }
        codeBuilder.addUnconditionalJump(nextIterationLabel ?: return)
    }

    private fun expressionWalker(expr: PsiElement?, breakLabel: LLVMLabel?, scopeDepth: Int) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, breakLabel, scopeDepth + 1)
            is KtProperty -> evaluateValExpression(expr, scopeDepth)
            is KtPostfixExpression -> evaluatePostfixExpression(expr, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtDoWhileExpression -> evaluateDoWhileExpression(expr.firstChild, scopeDepth + 1)
            is KtBreakExpression -> evaluateBreakExpression(breakLabel!!)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtWhenExpression -> evaluateWhenExpression(expr, scopeDepth)
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }

        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), breakLabel, scopeDepth)
    }

    fun evaluateConstructorDelegationReferenceExpression(expr: KtConstructorDelegationReferenceExpression, structCodegen: StructCodegen, constructorArguments: List<LLVMVariable>, scopeDepth: Int): LLVMSingleValue? {
        val targetCall = state.bindingContext?.get(BindingContext.CALL, expr)

        val names = parseValueArguments(targetCall!!.valueArguments, scopeDepth)
        val args = loadArgsIfRequired(names, constructorArguments)
        return evaluateConstructorCallExpression(LLVMVariable(structCodegen.fullName + LLVMType.mangleFunctionArguments(names), structCodegen.type, scope = LLVMVariableScope()), args)
    }

    private fun evaluateBreakExpression(breakLabel: LLVMLabel) {
        codeBuilder.addUnconditionalJump(breakLabel)
    }

    private fun evaluateDoWhileExpression(element: PsiElement, scopeDepth: Int) {
        val expr = element.context as KtDoWhileExpression
        executeWhileBlock(expr.condition!!, expr.body!!, scopeDepth, checkConditionBeforeExecute = false)
    }

    fun evaluateExpression(expr: PsiElement?, scopeDepth: Int): LLVMSingleValue? {
        return when (expr) {
            is KtBlockExpression -> {
                expressionWalker(expr.firstChild, breakLabel = null, scopeDepth = scopeDepth + 1)
                return null
            }
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtPostfixExpression -> evaluatePostfixExpression(expr, scopeDepth)
            is KtPrefixExpression -> evaluatePrefixExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtWhenExpression -> evaluateWhenExpression(expr, scopeDepth)
            is KtCallableReferenceExpression -> evaluateCallableReferenceExpression(expr)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtReferenceExpression -> evaluateReferenceExpression(expr, scopeDepth)
            is KtIfExpression -> evaluateIfOperator(expr, scopeDepth + 1)
            is KtStringTemplateExpression -> evaluateStringTemplateExpression(expr)
            is KtReturnExpression -> evaluateReturnInstruction(expr.firstChild, scopeDepth)
            is KtThisExpression -> evaluateThisExpression()
            is KtSafeQualifiedExpression -> evaluateSafeAccessExpression(expr, scopeDepth)
            is KtIfExpression -> evaluateIfOperator(expr, scopeDepth + 1)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateSafeAccessExpression(expr: KtSafeQualifiedExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiver = expr.receiverExpression
        val selector = expr.selectorExpression

        val left = evaluateExpression(receiver, scopeDepth)!!
        val loadedLeft = codeBuilder.receiveNativeValue(left)
        val expectedType = LLVMMapStandardType(state.bindingContext?.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expr)!!, state) as LLVMReferenceType

        if (state.classes.containsKey(expectedType.type)) {
            expectedType.prefix = "class"
        }

        val result = codeBuilder.getNewVariable(expectedType, pointer = 1)
        codeBuilder.allocStaticVar(result)
        result.pointer++

        val condition = left.type!!.operatorEq(loadedLeft, LLVMVariable("", LLVMNullType()))
        val thenLabel = codeBuilder.getNewLabel(prefix = "safe.access")
        val elseLabel = codeBuilder.getNewLabel(prefix = "safe.access")
        val endLabel = codeBuilder.getNewLabel(prefix = "safe.access")

        val conditionResult = codeBuilder.getNewVariable(condition.variableType)
        codeBuilder.addAssignment(conditionResult, condition)

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        codeBuilder.storeNull(result)
        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(elseLabel)
        val right = evaluateDotBody(receiver, selector!!, scopeDepth)
        val rightLoaded = codeBuilder.loadAndGetVariable(right as LLVMVariable)
        codeBuilder.storeVariable(result, rightLoaded)
        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(endLabel)

        return result
    }

    private fun evaluateThisExpression(): LLVMSingleValue? {
        return variableManager["this"]
    }

    fun evaluateStringTemplateExpression(expr: KtStringTemplateExpression): LLVMSingleValue? {
        val receiveValue = state.bindingContext?.get(BindingContext.COMPILE_TIME_VALUE, expr)
        val type = (receiveValue as TypedCompileTimeConstant).type
        val value = receiveValue.getValue(type) ?: return null
        val variable = variableManager.receiveVariable(".str" + LLVMBuilder.UniqueGenerator.generateUniqueString(), LLVMStringType(value.toString().length, isLoaded = false), LLVMVariableScope(), pointer = 0)

        codeBuilder.addStringConstant(variable, value.toString())
        return variable
    }

    private fun evaluateCallableReferenceExpression(expr: KtCallableReferenceExpression): LLVMSingleValue? {
        val kotlinType = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val result = LLVMInstanceOfStandardType(expr.text.substring(2), kotlinType, LLVMVariableScope(), state)
        return LLVMVariable("${result.label}${(result.type as LLVMFunctionType).mangleArgs()}", result.type, result.kotlinName, result.scope, result.pointer)
    }

    private fun evaluateDotExpression(expr: KtDotQualifiedExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverExpr = expr.receiverExpression
        val selectorExpr = expr.selectorExpression!!

        return evaluateDotBody(receiverExpr, selectorExpr, scopeDepth)
    }

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    private fun evaluateDotBody(receiverExpr: KtExpression, selectorExpr: KtExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverName = receiverExpr.text

        var receiver = when (receiverExpr) {
            is KtCallExpression,
            is KtPrefixExpression,
            is KtPostfixExpression,
            is KtBinaryExpression,
            is KtDotQualifiedExpression -> evaluateExpression(receiverExpr, scopeDepth) as LLVMVariable
            is KtNameReferenceExpression ->{
                val referenceContext = state.bindingContext?.get(BindingContext.REFERENCE_TARGET, receiverExpr)
                when (referenceContext) {
                    is PropertyDescriptorImpl -> {
                        val receiverThis = variableManager["this"]!!
                        evaluateMemberMethodOrField(receiverThis, receiverName, topLevel, call = null)!! as LLVMVariable
                    }
                    else -> variableManager[receiverName]
                }
            }
            else -> variableManager[receiverName]
        }

        val isExtension = selectorExpr is KtCallExpression && selectorExpr.getFunctionResolvedCallWithAssert(state.bindingContext!!).extensionReceiver != null

        if (isExtension) {
            return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as KtCallExpression, scopeDepth)
        }

        if (receiver != null) {
            if (receiver.pointer == 2) {
                receiver = codeBuilder.loadAndGetVariable(receiver)
            }
            when (receiver.type) {
                is LLVMReferenceType -> return evaluateMemberMethodOrField(receiver, selectorExpr.text, scopeDepth, selectorExpr)
                else -> return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as KtCallExpression, scopeDepth)
            }
        }

        val clazz = resolveCodegen(receiverExpr) ?: return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as? KtCallExpression ?: throw UnexpectedException(selectorExpr.text), scopeDepth)
        return evaluateClassScopedDotExpression(clazz, selectorExpr, scopeDepth, receiver)
    }

    private fun evaluatePrefixExpression(expr: KtPrefixExpression, scopeDepth: Int): LLVMSingleValue? {
        val operator = expr.operationToken
        val left = evaluateExpression(expr.baseExpression, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        return executePrefixExpression(operator, expr.operationReference, left)
    }

    private fun executePrefixExpression(operator: IElementType?, operationReference: KtSimpleNameExpression, left: LLVMSingleValue): LLVMSingleValue?
            = addPrimitivePrefixOperation(operator, operationReference, left)

    private fun addPrimitivePrefixOperation(operator: IElementType?, operationReference: KtSimpleNameExpression, firstOp: LLVMSingleValue): LLVMSingleValue? {
        when (operator) {
            KtTokens.MINUS,
            KtTokens.PLUS -> {
                return addPrimitiveBinaryOperation(operator!!, operationReference, LLVMConstant("0", firstOp.type), firstOp)
            }
            KtTokens.EXCL -> {
                val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
                val llvmExpression = addPrimitiveReferenceOperationByName("xor", LLVMConstant("true", LLVMBooleanType()), firstNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                return resultOp
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateExtensionExpression(receiver: KtExpression, receiverExpressionArgument: LLVMVariable?, selector: KtCallExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverType = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, receiver)
        val standardType = LLVMMapStandardType(receiverType!!.type!!, state)
        val function = selector.firstChild.firstChild.text
        val names = parseArgList(selector, scopeDepth)
        val type = if (names.size > 0) LLVMType.mangleFunctionArguments(names) else ""
        val extensionCodegen = state.extensionFunctions[standardType.toString()]?.get("$function$type") ?: throw UnexpectedException(
            "$standardType:$function$type"
        )
        val receiverExpression = receiverExpressionArgument ?: evaluateExpression(receiver, scopeDepth + 1)!!

        val typeThisArgument = when (standardType) {
            is LLVMReferenceType -> LLVMVariable("type", standardType, pointer = 1)
            else -> LLVMVariable("type", standardType, pointer = 0)
        }

        val args = mutableListOf(codeBuilder.loadArgumentIfRequired(receiverExpression, typeThisArgument))

        args.addAll(loadArgsIfRequired(names, extensionCodegen.args))

        return evaluateFunctionCallExpression(LLVMVariable(extensionCodegen.fullName, extensionCodegen.returnType!!.type, scope = LLVMVariableScope()), args)
    }

    private fun evaluateClassScopedDotExpression(clazz: StructCodegen, selector: KtExpression, scopeDepth: Int, receiver: LLVMVariable? = null): LLVMSingleValue? = when (selector) {
        is KtCallExpression -> evaluateCallExpression(selector, scopeDepth, clazz, caller = receiver)
        is KtReferenceExpression -> evaluateReferenceExpression(selector, scopeDepth, clazz)
        else -> throw UnsupportedOperationException()
    }

    fun evaluateMemberMethodOrField(receiver: LLVMVariable, selectorName: String, scopeDepth: Int, call: PsiElement? = null): LLVMSingleValue? {
        val type = receiver.type as LLVMReferenceType
        val clazz = resolveClassOrObjectLocation(type) ?: throw UnexpectedException(type.toString())
        val field = clazz.fieldsIndex[selectorName]
        if (field != null) {
            val result = codeBuilder.getNewVariable(field.type, pointer = field.pointer + 1)
            codeBuilder.loadClassField(result, receiver, field.offset)
            return result
        }

        (call as? KtCallExpression) ?: throw UnexpectedException("$receiver:$selectorName")
        val names = parseArgList(call as KtCallExpression, scopeDepth)
        val typePath = type.location.joinToString(".")
        val types = if (names.size > 0) LLVMType.mangleFunctionArguments(names) else ""
        val methodName = "${if (typePath.length > 0) "$typePath." else ""}${clazz.structName}.${selectorName.substringBefore('(')}$types"

        val method = clazz.methods[methodName] ?: throw UnexpectedException(methodName)
        val returnType = clazz.methods[methodName]!!.returnType!!.type

        val loadedArgs = loadArgsIfRequired(names, method.args)
        val callArgs = mutableListOf<LLVMSingleValue>(receiver)
        callArgs.addAll(loadedArgs)
        return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMVariableScope()), callArgs)
    }

    private fun resolveClassOrObjectLocation(type: LLVMReferenceType): StructCodegen? {
        if (type.location.size == 0) {
            return state.classes[type.type] ?: state.objects[type.type]
        }

        var codegen = state.classes[type.location[0]]
        var i = 1
        while (i < type.location.size) {
            codegen = codegen?.nestedClasses?.get(type.location[i])
            i++
        }

        if (codegen?.companionObjectCodegen != null && type.type == codegen?.companionObjectCodegen?.structName) {
            return codegen?.companionObjectCodegen!!
        }

        return codegen?.nestedClasses?.get(type.type)
    }

    fun evaluateArrayAccessExpression(expr: KtArrayAccessExpression, scope: Int): LLVMSingleValue? {
        val arrayNameVariable = evaluateExpression(expr.arrayExpression, scope) as LLVMVariable
        return when (arrayNameVariable.type) {
            is LLVMReferenceType -> {
                val callMaker = state.bindingContext?.get(BindingContext.CALL, expr)
                when (callMaker!!.callType) {
                    Call.CallType.ARRAY_SET_METHOD,
                    Call.CallType.ARRAY_GET_METHOD -> {
                        val arrayActionType = if (callMaker.callType == Call.CallType.ARRAY_SET_METHOD) "set" else "get"
                        val explicitReceiver = callMaker.explicitReceiver as ExpressionReceiver
                        val receiver = evaluateExpression(explicitReceiver.expression, scope)!! as LLVMVariable
                        val pureReceiver = codeBuilder.downLoadArgument(receiver, 1)

                        val targetClassName = (receiver.type as LLVMReferenceType).type

                        val names = parseValueArguments(callMaker.valueArguments, scope)
                        val methodName = "$targetClassName.$arrayActionType${if (names.size > 0) LLVMType.mangleFunctionArguments(names) else ""}"
                        val type = receiver.type as LLVMReferenceType
                        val clazz = resolveClassOrObjectLocation(type) ?: throw UnexpectedException(type.toString())

                        val method = clazz.methods[methodName] ?: throw UnexpectedException(expr.text)
                        val returnType = clazz.methods[methodName]!!.returnType!!.type

                        val loadedArgs = loadArgsIfRequired(names, method.args)
                        val callArgs = mutableListOf(pureReceiver)
                        callArgs.addAll(loadedArgs)

                        return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMVariableScope()), callArgs)
                    }
                    else -> throw IllegalStateException()
                }
            }
            else -> {
                val arrayIndex = evaluateConstantExpression(expr.indexExpressions.first() as KtConstantExpression)
                val arrayReceivedVariable = codeBuilder.loadAndGetVariable(arrayNameVariable)
                val arrayElementType = (arrayNameVariable.type as LLVMArray).basicType()
                val indexVariable = codeBuilder.getNewVariable(arrayElementType, pointer = 1)
                codeBuilder.loadVariableOffset(indexVariable, arrayReceivedVariable, arrayIndex)
                indexVariable
            }
        }
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression, scopeDepth: Int, classScope: StructCodegen? = null): LLVMSingleValue? = when {
        expr is KtArrayAccessExpression -> evaluateArrayAccessExpression(expr, scopeDepth + 1)
        isEnumClassField(expr, classScope) -> resolveEnumClassField(expr, classScope)
        resolveContainingClass(expr) != null -> {
            val clazz = resolveContainingClass(expr)
            val receiver = if (clazz != null) variableManager[clazz.fullName] ?: variableManager["this"]  else variableManager["this"]
            evaluateMemberMethodOrField(receiver!!, expr.firstChild.text, topLevel)
        }
        (expr is KtNameReferenceExpression) && (classScope != null) -> evaluateNameReferenceExpression(expr, classScope.parentCodegen!!)
        else -> variableManager[expr.firstChild.text]
    }

    private fun evaluateNameReferenceExpression(expr: KtNameReferenceExpression, classScope: StructCodegen): LLVMSingleValue? {
        val fieldName = state.bindingContext?.get(BindingContext.REFERENCE_TARGET, expr)!!.name.toString()
        val companionObject = (classScope as ClassCodegen).companionObjectCodegen ?: throw UnexpectedException(expr.text)
        val field = companionObject.fieldsIndex[fieldName] ?: throw UnexpectedException(expr.text)
        val receiver = variableManager[companionObject.fullName]!!
        val result = codeBuilder.getNewVariable(field.type, pointer = 1)

        codeBuilder.loadClassField(result, receiver, field.offset)
        return result
    }

    private fun resolveEnumClassField(expr: KtReferenceExpression, classScope: StructCodegen?): LLVMSingleValue =
        (classScope ?: resolveCodegen(expr))!!.enumFields[expr.text]!!

    private fun isEnumClassField(expr: KtReferenceExpression, classScope: StructCodegen?): Boolean =
        (classScope ?: resolveCodegen(expr))?.enumFields?.containsKey(expr.text) ?: false

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    private fun resolveCodegen(expr: KtExpression): StructCodegen? {
        val type = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, expr)?.type
            ?: expr.getType(state.bindingContext!!)
            ?: expr.getQualifiedExpressionForReceiver()?.getType(state.bindingContext)

        val location = type?.getSubtypesPredicate()?.toString()?.split(".")
            ?: state.bindingContext.get(BindingContext.REFERENCE_TARGET, expr as KtReferenceExpression)?.fqNameSafe?.toString()?.split(".")
            ?: return null

        val name = location.last()

        val classType = LLVMReferenceType(name, prefix = "class")
        classType.location.addAll(location.dropLast(1))

        return resolveClassOrObjectLocation(classType)
    }

    private fun evaluateCallExpression(expr: KtCallExpression, scopeDepth: Int, classScope: StructCodegen? = null, caller: LLVMVariable? = null): LLVMSingleValue? {
        var names = parseArgList(expr, scopeDepth)
        var name = expr.firstChild.firstChild.text
        val targetFunction = state.bindingContext?.get(BindingContext.CALL, expr.calleeExpression)
        val resolvedCall = state.bindingContext?.get(BindingContext.RESOLVED_CALL, targetFunction)
        val functionDescriptor = expr.getFunctionResolvedCallWithAssert(state.bindingContext!!).candidateDescriptor
        val arguments = resolvedCall!!.valueArguments.toSortedMap(compareBy { it.index }).values

        val external = state.externalFunctions.containsKey(name)
        val functionArguments = functionDescriptor.valueParameters.map { it -> it.type }.map { LLVMMapStandardType(it, state) }
        val function = "$name${if (!external) LLVMType.mangleFunctionTypes(functionArguments) else ""}"

        if (state.functions.containsKey(function) || state.externalFunctions.containsKey(function)) {
            val descriptor = state.functions[function] ?: state.externalFunctions[function] ?: return null
            names = parseNamedValueArguments(arguments, descriptor.defaultValues, scopeDepth)
            val args = loadArgsIfRequired(names, descriptor.args)
            return evaluateFunctionCallExpression(LLVMVariable(function, descriptor.returnType!!.type, scope = LLVMVariableScope()), args)
        }


        if (state.classes.containsKey(name) || classScope?.structName == name) {
            val descriptor = state.classes[name] ?: classScope ?: return null
            val detectedConstructor = LLVMType.mangleFunctionTypes(functionArguments)
            val args = loadArgsIfRequired(names, descriptor.constructorFields[detectedConstructor]!!)
            return evaluateConstructorCallExpression(LLVMVariable(descriptor.fullName + detectedConstructor, descriptor.type, scope = LLVMVariableScope()), args)
        }

        val localFunction = variableManager[name]
        if (localFunction != null) {
            val type = localFunction.type as LLVMFunctionType
            val args = loadArgsIfRequired(names, type.arguments)
            return evaluateFunctionCallExpression(LLVMVariable(name, type.returnType.type, scope = LLVMRegisterScope()), args)
        }

        if (classScope != null) {
            val methodShortName = "${classScope.fullName}.$function"
            if (classScope.methods.containsKey(methodShortName)) {
                val descriptor = classScope.methods[methodShortName]!!
                val parentDescriptor = descriptor.parentCodegen!!
                val receiver = variableManager[parentDescriptor.fullName]!!
                val methodFullName = descriptor.name

                val returnType = descriptor.returnType!!.type

                val loadedArgs = loadArgsIfRequired(names, descriptor.args)
                val callArgs = mutableListOf<LLVMSingleValue>(receiver)
                callArgs.addAll(loadedArgs)

                return evaluateFunctionCallExpression(LLVMVariable(methodFullName, returnType, scope = LLVMVariableScope()), callArgs)
            }
        }

        val nestedConstructor = classScope?.nestedClasses?.get(expr.calleeExpression!!.text)
        if (nestedConstructor != null) {
            val args = loadArgsIfRequired(names, nestedConstructor.constructorFields[nestedConstructor.primaryConstructorIndex]!!)
            return evaluateConstructorCallExpression(LLVMVariable(nestedConstructor.fullName, nestedConstructor.type, scope = LLVMVariableScope()), args)
        }

        val containingClass = resolveContainingClass(expr) ?: return null
        name = "${containingClass.fullName}.$function"
        val method = containingClass.methods[name]!!
        val args = mutableListOf<LLVMSingleValue>()

        if (caller != null) {
            args.add(caller)
        } else if (variableManager[containingClass.fullName] != null) {
            args.add(variableManager[containingClass.fullName]!!)
        } else {
            args.add(variableManager["this"]!!)
        }

        args.addAll(loadArgsIfRequired(names, method.args))

        return evaluateFunctionCallExpression(LLVMVariable(method.fullName, method.returnType?.type ?: LLVMVoidType(), scope = LLVMVariableScope()), args)
    }

    private fun resolveContainingClass(expr: KtElement): StructCodegen? {
        val name = expr.getResolvedCallWithAssert(state.bindingContext!!).dispatchReceiver?.type?.toString() ?: return null
        val location = expr.getResolvedCallWithAssert(state.bindingContext).dispatchReceiver!!.type.getSubtypesPredicate().toString().split(".").dropLast(1)

        if (location.size > 0) {
            val type = LLVMReferenceType(name, prefix = "class")
            type.location.addAll(location)

            return resolveClassOrObjectLocation(type)
        }

        return state.classes[name] ?: state.objects[name]
    }

    private fun evaluateFunctionCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val returnType = function.type
        when (returnType) {
            is LLVMVoidType -> {
                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), names).toString())
            }
            is LLVMReferenceType -> {
                val returnVar = codeBuilder.getNewVariable(returnType, pointer = 1)
                codeBuilder.allocStaticVar(returnVar)
                returnVar.pointer++

                val args = ArrayList<LLVMSingleValue>()
                args.add(returnVar)
                args.addAll(names)

                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), args).toString())
                if (returnVar.pointer == 2) {
                    return returnVar
                } else {
                    return codeBuilder.loadAndGetVariable(returnVar)
                }
            }
            else -> {
                val result = codeBuilder.getNewVariable(returnType)
                codeBuilder.addAssignment(result, LLVMCall(returnType, function.toString(), names))

                val resultPtr = codeBuilder.getNewVariable(returnType)
                codeBuilder.allocStackVar(resultPtr)
                resultPtr.pointer = 1
                codeBuilder.storeVariable(resultPtr, result)
                return resultPtr
            }
        }

        return null
    }

    private fun loadArgsIfRequired(names: List<LLVMSingleValue>, args: List<LLVMVariable>) =
        names.mapIndexed(fun(i: Int, value: LLVMSingleValue): LLVMSingleValue {
            return codeBuilder.loadArgumentIfRequired(value, args[i])
        }).toList()

    private fun evaluateConstructorCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val store = codeBuilder.getNewVariable(function.type)
        codeBuilder.allocStaticVar(store)
        store.pointer++

        val result = codeBuilder.getNewVariable(function.type, pointer = 1)
        codeBuilder.allocStackVar(result)
        result.pointer++

        codeBuilder.storeVariable(result, store)

        val args = ArrayList<LLVMSingleValue>()
        args.add(store)
        args.addAll(names)

        codeBuilder.addLLVMCode(LLVMCall(
            LLVMVoidType(),
            function.toString(),
            args
        ).toString())

        return result
    }

    private fun parseArgList(expr: KtCallExpression, scopeDepth: Int): List<LLVMSingleValue> {
        val args = expr.getValueArgumentsInParentheses()
        return parseValueArguments(args, scopeDepth)
    }

    private fun parseValueArguments(args: List<ValueArgument>, scopeDepth: Int): List<LLVMSingleValue> {
        val result = ArrayList<LLVMSingleValue>()

        for (arg in args) {
            result.add(parseOneValueArgument(arg, scopeDepth))
        }

        return result
    }

    fun parseOneValueArgument(arg: ValueArgument, scopeDepth: Int): LLVMSingleValue {
        return evaluateExpression(arg.getArgumentExpression(), scopeDepth) as LLVMSingleValue
    }

    private fun parseNamedValueArguments(args: MutableCollection<ResolvedValueArgument>, defaultValues: List<KtExpression?>, scopeDepth: Int): List<LLVMSingleValue> =
        args.mapIndexed(fun(i: Int, value: ResolvedValueArgument): LLVMSingleValue {
            return when (value) {
                is DefaultValueArgument -> evaluateExpression(defaultValues[i], scopeDepth)!!
                else -> {
                    val expr = ((value as ExpressionValueArgument).valueArgument as KtValueArgument).getArgumentExpression()!!
                    evaluateExpression(expr, scopeDepth)!!
                }
            }
        }).toList()

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable? {
        val operator = expr.operationToken
        if (operator == KtTokens.ELVIS) {
            return evaluateElvisOperator(expr, scopeDepth)
        }

        val left = evaluateExpression(expr.left, scopeDepth)
        if (expr.left is KtArrayAccessExpression) {
            val callMaker = state.bindingContext?.get(BindingContext.CALL, expr.left)
            if (callMaker!!.callType == Call.CallType.ARRAY_SET_METHOD) {
                return left as LLVMVariable?
            }
        }

        left ?: throw UnsupportedOperationException("Wrong binary exception: ${expr.text}")


        val right = evaluateExpression(expr.right, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception: ${expr.text}")
        return executeBinaryExpression(operator, expr.operationReference, left, right)
    }

    fun executeBinaryExpression(operator: IElementType, referenceName: KtSimpleNameExpression?, left: LLVMSingleValue, right: LLVMSingleValue)
            = addPrimitiveBinaryOperation(operator, referenceName, left, right)

    private fun evaluateElvisOperator(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable {
        val left = evaluateExpression(expr.firstChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val lptr = codeBuilder.loadAndGetVariable(left as LLVMVariable)

        val condition = lptr.type.operatorEq(lptr, LLVMVariable("", LLVMNullType()))

        val conditionResult = codeBuilder.getNewVariable(condition.variableType)
        codeBuilder.addAssignment(conditionResult, condition)

        val thenLabel = codeBuilder.getNewLabel(prefix = "elvis")
        val elseLabel = codeBuilder.getNewLabel(prefix = "elvis")
        val endLabel = codeBuilder.getNewLabel(prefix = "elvis")

        codeBuilder.addCondition(conditionResult, elseLabel, thenLabel)

        codeBuilder.markWithLabel(thenLabel)

        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(elseLabel)
        var right = evaluateExpression(expr.lastChild, scopeDepth + 1)
        if (right != null) {
            right = codeBuilder.loadAndGetVariable(right as LLVMVariable)
            codeBuilder.storeVariable(left, right)
        }

        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return left
    }

    private fun addPrimitiveBinaryOperation(operator: IElementType, referenceName: KtSimpleNameExpression?, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMVariable {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        val secondNativeOp = codeBuilder.receiveNativeValue(secondOp)
        val llvmExpression = when (operator) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
            KtTokens.LT -> firstOp.type!!.operatorLt(firstNativeOp, secondNativeOp)
            KtTokens.GT -> firstOp.type!!.operatorGt(firstNativeOp, secondNativeOp)
            KtTokens.LTEQ -> firstOp.type!!.operatorLeq(firstNativeOp, secondNativeOp)
            KtTokens.GTEQ -> firstOp.type!!.operatorGeq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQ ->
                if (firstOp.type is LLVMReferred)
                    if (secondOp.type is LLVMReferred) {
                        val firstPointedArgument = receivePointedArgument(firstOp, 1)
                        val secondPointedArgument = receivePointedArgument(secondOp, 1)
                        firstOp.type!!.operatorEq(firstPointedArgument, secondPointedArgument)
                    } else
                        firstOp.type!!.operatorEq(firstNativeOp, secondOp)
                else
                    firstOp.type!!.operatorEq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQEQ -> {
                val firstPointedArgument = receivePointedArgument(firstOp, 1)
                val secondPointedArgument = receivePointedArgument(secondOp, 1)
                firstOp.type!!.operatorEq(firstPointedArgument, secondPointedArgument)
            }
            KtTokens.EXCLEQ -> {
                if (firstOp.type is LLVMReferred)
                    if (secondOp.type is LLVMReferred) {
                        val firstPointedArgument = receivePointedArgument(firstOp, 1)
                        val secondPointedArgument = receivePointedArgument(secondOp, 1)
                        firstOp.type!!.operatorNeq(firstPointedArgument, secondPointedArgument)
                    } else
                        firstOp.type!!.operatorNeq(firstNativeOp, secondOp)
                else
                    firstOp.type!!.operatorNeq(firstNativeOp, secondNativeOp)
            }
            KtTokens.EXCLEQEQEQ -> {
                val firstPointedArgument = receivePointedArgument(firstOp, 1)
                val secondPointedArgument = receivePointedArgument(secondOp, 1)
                firstOp.type!!.operatorNeq(firstPointedArgument, secondPointedArgument)
            }
            KtTokens.EQ -> {
                codeBuilder.addComment("start variable assignment")
                if (secondOp.type is LLVMNullType) {
                    codeBuilder.storeNull(firstOp as LLVMVariable)
                    codeBuilder.addComment("end null variable assignment")
                    return firstOp
                }

                val result = firstOp as LLVMVariable
                val sourceArgument: LLVMSingleValue
                if (secondOp.type!!.isPrimitive() && (secondOp.pointer == 0) && (firstOp.pointer == 2)) {
                    sourceArgument = codeBuilder.getNewVariable(secondOp.type!!, 1)
                    codeBuilder.allocStaticVar(sourceArgument, true)
                    codeBuilder.storeVariable(sourceArgument, secondOp)
                } else {
                    sourceArgument = if (result.pointer == secondOp.pointer + 1) secondOp else secondNativeOp
                }
                codeBuilder.storeVariable(result, sourceArgument)
                codeBuilder.addComment("end variable assignment")
                return result
            }
            else -> addPrimitiveReferenceOperation(referenceName!!, firstNativeOp, secondNativeOp)
        }
        val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType, pointer = llvmExpression.pointer)
        codeBuilder.addAssignment(resultOp, llvmExpression)
        return resultOp
    }

    private fun receivePointedArgument(variable: LLVMSingleValue, pointer: Int): LLVMSingleValue {
        var currentVariable = variable
        while (currentVariable.pointer > pointer) {
            currentVariable = codeBuilder.receiveNativeValue(variable)
        }
        return currentVariable
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMConstant {
        val expressionKotlinType = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val expressionValue = state.bindingContext?.get(BindingContext.COMPILE_TIME_VALUE, expr)?.getValue(expressionKotlinType)

        val type = LLVMMapStandardType(expressionKotlinType, state)

        return LLVMConstant(expressionValue?.toString() ?: "", type, pointer = 0)
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int): LLVMSingleValue? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(element)
            KtTokens.INTEGER_LITERAL -> null
            else -> null
        }
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfOperator(element.context as KtIfExpression, scopeDepth, isExpression = false)
            KtTokens.WHILE_KEYWORD -> evaluateWhileOperator(element.context as KtWhileExpression, scopeDepth)
            KtTokens.FOR_KEYWORD -> evaluateForOperator(element.context as KtForExpression, scopeDepth)
            else -> null
        }
    }

    private fun evaluateWhileOperator(expr: KtWhileExpression, scopeDepth: Int): LLVMVariable? =
        executeWhileBlock(expr.condition!!, expr.body!!, scopeDepth, checkConditionBeforeExecute = true)

    private fun executeWhileBlock(condition: KtExpression, bodyExpression: PsiElement, scopeDepth: Int, checkConditionBeforeExecute: Boolean): LLVMVariable? {
        val conditionLabel = codeBuilder.getNewLabel(prefix = "while")
        val bodyLabel = codeBuilder.getNewLabel(prefix = "while")
        val exitLabel = codeBuilder.getNewLabel(prefix = "while")

        codeBuilder.addUnconditionalJump(if (checkConditionBeforeExecute) conditionLabel else bodyLabel)
        codeBuilder.markWithLabel(conditionLabel)
        var conditionResult = evaluateExpression(condition, scopeDepth + 1)!!
        while (conditionResult.pointer > 0) {
            conditionResult = codeBuilder.loadAndGetVariable(conditionResult as LLVMVariable)
        }

        codeBuilder.addCondition(conditionResult, bodyLabel, exitLabel)
        evaluateCodeBlock(bodyExpression, bodyLabel, conditionLabel, exitLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLabel)

        return null
    }

    private fun evaluateIfOperator(element: KtIfExpression, scopeDepth: Int, isExpression: Boolean = true): LLVMVariable? {
        codeBuilder.addComment("begin evaluate if")
        val conditionResult = evaluateExpression(element.condition, scopeDepth)!!
        val conditionNativeResult = codeBuilder.downLoadArgument(conditionResult, 0)

        return if (isExpression)
            executeIfExpression(conditionNativeResult, element.then!!, element.`else`, element, scopeDepth + 1)
        else
            executeIfBlock(conditionNativeResult, element.then!!, element.`else`, scopeDepth + 1)
    }

    private fun executeIfExpression(conditionResult: LLVMSingleValue, thenExpression: KtExpression, elseExpression: PsiElement?, ifExpression: KtIfExpression, scopeDepth: Int): LLVMVariable? {
        val kotlinType = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, ifExpression)!!.type!!
        val expressionType = LLVMInstanceOfStandardType("type", kotlinType, LLVMVariableScope(), state).type
        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)
        codeBuilder.addComment("start if expression")
        when (resultVariable.type) {
            is LLVMReferenceType -> codeBuilder.allocStaticVar(resultVariable, asValue = true)
            else -> codeBuilder.allocStackVar(resultVariable, asValue = true)
        }
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        val thenResultExpression = evaluateExpression(thenExpression, scopeDepth + 1) ?: return null
        val thenResultNativeExpression = codeBuilder.receiveNativeValue(thenResultExpression)
        codeBuilder.storeVariable(resultVariable, thenResultNativeExpression)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(elseLabel)

        val elseResultExpression = evaluateExpression(elseExpression, scopeDepth + 1) ?: return null
        val elseResultNativeExpression = codeBuilder.receiveNativeValue(elseResultExpression)
        codeBuilder.storeVariable(resultVariable, elseResultNativeExpression)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        codeBuilder.addComment("end if expression")
        return resultVariable
    }

    private fun executeIfBlock(conditionResult: LLVMSingleValue, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addComment("start if operator")
        codeBuilder.addCondition(conditionResult, thenLabel, if (elseExpression != null) elseLabel else endLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, endLabel, scopeDepth + 1)
        if (elseExpression != null) {
            evaluateCodeBlock(elseExpression, elseLabel, endLabel, endLabel, scopeDepth + 1)
        }
        codeBuilder.markWithLabel(endLabel)
        codeBuilder.addComment("end if operator")
        return null
    }

    private fun copyVariable(from: LLVMVariable, to: LLVMVariable) = when (from.type) {
        is LLVMStringType -> codeBuilder.storeString(to, from, 0)
        else -> codeBuilder.copyVariableValue(to, from)
    }

    private fun evaluateValExpression(element: KtProperty, scopeDepth: Int): LLVMVariable? {
        val variable = state.bindingContext?.get(BindingContext.VARIABLE, element)!!
        val identifier = variable.name.toString()

        val assignExpression = evaluateExpression(element.delegateExpressionOrInitializer, scopeDepth)
        val expectedExpressionType = LLVMInstanceOfStandardType("", variable.type, state = state)

        val primitivePointer = LLVMMapStandardType(variable.type, state) !is LLVMReferred

        val allocVar = variableManager.receiveVariable(identifier, expectedExpressionType.type, LLVMRegisterScope(), pointer =
            expectedExpressionType.pointer)
        codeBuilder.allocStackVar(allocVar)
        allocVar.pointer++
        allocVar.kotlinName = identifier

        variableManager.addVariable(identifier, allocVar, scopeDepth)
        if (assignExpression != null) {
            if ((primitivePointer) && (assignExpression.type is LLVMReferenceType)) {
                throw UnexpectedException(element.text)
            }
            addPrimitiveBinaryOperation(KtTokens.EQ, null, allocVar, assignExpression)
        }
        return null
    }

    private fun evaluateReturnInstruction(element: PsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        var retVar = evaluateExpression(next, scopeDepth)
        val type = retVar?.type ?: LLVMVoidType()

        when (type) {
            is LLVMReferenceType -> genReferenceReturn(retVar!!)
            is LLVMNullType -> {
                retVar = when (retVar) {
                    is LLVMConstant -> LLVMConstant(retVar.value, LLVMNullType(returnType!!.type), returnType!!.pointer - 1)
                    is LLVMVariable -> LLVMVariable(retVar.label, LLVMNullType(returnType!!.type), retVar.kotlinName, retVar.scope, returnType!!.pointer - 1)
                    else -> throw IllegalStateException()
                }
                genReferenceReturn(retVar)
            }
            is LLVMVoidType -> {
                codeBuilder.addAnyReturn(LLVMVoidType())
            }
            else -> {
                val retNativeValue = codeBuilder.receiveNativeValue(retVar!!)
                codeBuilder.addReturnOperator(retNativeValue)
            }
        }
        if (scopeDepth == topLevel + 2) {
            wasReturnOnTopLevel = true
        }
        return null
    }

    private fun evaluateWhenItem(item: KtWhenEntry, target: LLVMSingleValue, resultVariable: LLVMVariable, elseLabel: LLVMLabel, endLabel: LLVMLabel, isElse: Boolean, scopeDepth: Int) {
        val successConditionsLabel = codeBuilder.getNewLabel(prefix = "when_condition_success")
        var nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")
        codeBuilder.addUnconditionalJump(nextLabel)
        for (condition in item.conditions) {
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")

            val currentConditionExpression = evaluateExpression(condition.firstChild, scopeDepth + 1)!!
            val conditionResult = executeBinaryExpression(KtTokens.EQEQ, null, target, currentConditionExpression)

            codeBuilder.addCondition(conditionResult, successConditionsLabel, nextLabel)
        }
        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addComment("last condition item")
        codeBuilder.addUnconditionalJump(if (isElse) successConditionsLabel else elseLabel)
        codeBuilder.markWithLabel(successConditionsLabel)

        var successExpression = evaluateExpression(item.expression, scopeDepth + 1)
        while (successExpression is LLVMVariable && successExpression.pointer > 0) {
            successExpression = codeBuilder.loadAndGetVariable(successExpression)
        }
        if (successExpression != null && resultVariable.type !is LLVMVoidType && resultVariable.type !is LLVMNullType) {
            codeBuilder.storeVariable(resultVariable, successExpression)
        }
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.addComment("end last condition item")
    }

    private fun evaluateWhenExpression(expr: KtWhenExpression, scopeDepth: Int): LLVMVariable? {
        codeBuilder.addComment("start when expression")
        val whenExpression = expr.subjectExpression
        val kotlinType = state.bindingContext?.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val expressionType = LLVMInstanceOfStandardType("type", kotlinType, LLVMVariableScope(), state).type

        val targetExpression = evaluateExpression(whenExpression, scopeDepth + 1)!!

        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)
        when (expressionType) {
            is LLVMVoidType,
            is LLVMNullType -> {}
            is LLVMReferenceType -> codeBuilder.allocStaticVar(resultVariable, asValue = true)
            else -> codeBuilder.allocStackVar(resultVariable, asValue = true)
        }

        var nextLabel = codeBuilder.getNewLabel(prefix = "when_start")
        val endLabel = codeBuilder.getNewLabel(prefix = "when_end")
        codeBuilder.addUnconditionalJump(nextLabel)
        for (item in expr.entries) {
            codeBuilder.addComment("start new when item")
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_item")
            evaluateWhenItem(item, targetExpression, resultVariable, nextLabel, endLabel, item.isElse, scopeDepth + 1)
            codeBuilder.addComment("end new when item")
        }
        codeBuilder.addComment("else branch of when expression")
        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        codeBuilder.addComment("end when expression")
        return resultVariable
    }

    private fun genReferenceReturn(retVar: LLVMSingleValue) {
        var result = retVar
        if (result.pointer == 2) {
            result = codeBuilder.loadAndGetVariable(retVar as LLVMVariable)
        }

        codeBuilder.storeVariable(returnType!!, result)
        codeBuilder.addAnyReturn(LLVMVoidType())
    }

    private fun evaluatePostfixExpression(expr: KtPostfixExpression, scopeDepth: Int): LLVMSingleValue? {
        val operator = expr.operationToken

        val left = evaluateExpression(expr.baseExpression, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception: ${expr.text}")

        return executePostfixExpression(operator, expr.operationReference, left as LLVMVariable)
    }

    private fun executePostfixExpression(operator: IElementType?, operationReference: KtSimpleNameExpression, left: LLVMVariable): LLVMSingleValue?
            = addPrimitivePostfixOperation(operator, operationReference, left)

    private fun addPrimitivePostfixOperation(operator: IElementType?, operationReference: KtSimpleNameExpression, firstOp: LLVMVariable): LLVMSingleValue? {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        when (operator) {
            KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> {
                val oldValue = codeBuilder.getNewVariable(firstOp.type, firstOp.pointer)
                codeBuilder.allocStackVar(oldValue, asValue = true)
                codeBuilder.copyVariable(firstOp, oldValue)

                val llvmExpression = when (operator) {
                    KtTokens.PLUSPLUS -> firstOp.type.operatorInc(firstNativeOp)
                    KtTokens.MINUSMINUS -> firstOp.type.operatorDec(firstNativeOp)
                    else -> throw IllegalAccessError()
                }

                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)

                codeBuilder.storeVariable(firstOp, resultOp)
                return oldValue
            }
            KtTokens.EXCLEXCL -> {
                var result = firstOp
                codeBuilder.addComment("start EXCLEXCL")
                val nullLabel = codeBuilder.getNewLabel(prefix = "nullCheck")
                val notNullLabel = codeBuilder.getNewLabel(prefix = "nullCheck")
                val nullCheck = codeBuilder.nullCheck(firstOp)
                codeBuilder.addCondition(nullCheck, nullLabel, notNullLabel)
                codeBuilder.markWithLabel(nullLabel)
                codeBuilder.addExceptionCall("KotlinNullPointerException")
                codeBuilder.addUnconditionalJump(notNullLabel)
                codeBuilder.markWithLabel(notNullLabel)
                if (firstOp.type.isPrimitive()) {
                    result = codeBuilder.downLoadArgument(firstOp, 0) as LLVMVariable
                }
                codeBuilder.addComment("end EXCLEXCL")
                return result
            }
            else -> throw UnsupportedOperationException()
        }
    }

    fun addPrimitiveReferenceOperation(referenceName: KtSimpleNameExpression, firstOp: LLVMSingleValue, secondNativeOp: LLVMSingleValue): LLVMExpression
            = addPrimitiveReferenceOperationByName(referenceName.getReferencedName(), firstOp, secondNativeOp)

    fun addPrimitiveReferenceOperationByName(operator: String, firstOp: LLVMSingleValue, secondNativeOp: LLVMSingleValue): LLVMExpression {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        return when (operator) {
            "||",
            "or" -> firstNativeOp.type!!.operatorOr(firstNativeOp, secondNativeOp)
            "xor" -> firstNativeOp.type!!.operatorXor(firstNativeOp, secondNativeOp)
            "&&",
            "and" -> firstNativeOp.type!!.operatorAnd(firstNativeOp, secondNativeOp)
            "%" -> firstNativeOp.type!!.operatorMod(firstNativeOp, secondNativeOp)
            "shl" -> {
                var secondNativeOpWithRequiredType = secondNativeOp
                if (firstNativeOp.type != secondNativeOp.type) {
                    val convertedExpression = firstNativeOp.type!!.convertFrom(secondNativeOp)
                    secondNativeOpWithRequiredType = codeBuilder.getNewVariable(convertedExpression.variableType)
                    codeBuilder.addAssignment(secondNativeOpWithRequiredType, convertedExpression)
                }
                firstNativeOp.type!!.operatorShl(firstNativeOp, secondNativeOpWithRequiredType)
            }
            "shr" -> {
                var secondNativeOpWithRequiredType = secondNativeOp
                if (firstNativeOp.type != secondNativeOp.type) {
                    val convertedExpression = firstNativeOp.type!!.convertFrom(secondNativeOp)
                    secondNativeOpWithRequiredType = codeBuilder.getNewVariable(convertedExpression.variableType)
                    codeBuilder.addAssignment(secondNativeOpWithRequiredType, convertedExpression)
                }
                firstNativeOp.type!!.operatorShr(firstNativeOp, secondNativeOpWithRequiredType)
            }
            "ushr" -> {
                var secondNativeOpWithRequiredType = secondNativeOp
                if (firstNativeOp.type != secondNativeOp.type) {
                    val convertedExpression = firstNativeOp.type!!.convertFrom(secondNativeOp)
                    secondNativeOpWithRequiredType = codeBuilder.getNewVariable(convertedExpression.variableType)
                    codeBuilder.addAssignment(secondNativeOpWithRequiredType, convertedExpression)
                }

                firstNativeOp.type!!.operatorUshr(firstNativeOp, secondNativeOpWithRequiredType)
            }
            "+=" -> {
                val llvmExpression = firstNativeOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                codeBuilder.storeVariable(firstOp, resultOp)
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "-=" -> {
                val llvmExpression = firstNativeOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                codeBuilder.storeVariable(firstOp, resultOp)
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "*=" -> {
                val llvmExpression = firstNativeOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                codeBuilder.storeVariable(firstOp, resultOp)
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "%=" -> {
                val llvmExpression = firstNativeOp.type!!.operatorMod(firstNativeOp, secondNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                codeBuilder.storeVariable(firstOp, resultOp)
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            ".." -> {
                val descriptor = state.classes["IntRange"]
                val arguments = listOf(firstOp, secondNativeOp)
                val detectedConstructor = LLVMType.mangleFunctionTypes(arguments.map { it.type!! })
                val result = evaluateConstructorCallExpression(LLVMVariable(descriptor!!.fullName + detectedConstructor, descriptor.type, scope = LLVMVariableScope()), arguments)
                return LLVMExpression(result!!.type!!, "load ${descriptor.type}** $result, align ${descriptor.type.align}", pointer = 1)
            }
            else -> throw UnsupportedOperationException("Unknown binary operator: $operator(${firstNativeOp.type}, ${secondNativeOp.type})")
        }
    }

    private fun evaluateForOperator(expr: KtForExpression, scopeDepth: Int): LLVMVariable? {
        val range = evaluateExpression(expr.loopRange, scopeDepth + 1)!!
        val conditionLabel = codeBuilder.getNewLabel(prefix = "for_condition")
        val bodyLabel = codeBuilder.getNewLabel(prefix = "for_body")
        val exitLabel = codeBuilder.getNewLabel(prefix = "for_exit")
        val rangeTypeName = (range.type as LLVMReferenceType).type

        val descriptor = state.classes[rangeTypeName]
        val method = descriptor!!.methods["$rangeTypeName.iterator"] ?: throw UnexpectedException("$rangeTypeName.iterator")
        val returnType = method.returnType!!.type
        val returnTypeName = (returnType as LLVMReferenceType).type
        val iteratorDescriptor = state.classes[returnTypeName]
        val nextDescriptor = iteratorDescriptor!!.methods["$returnTypeName.nextInt"] ?: throw UnexpectedException("$returnTypeName.hasNext")

        val conditionIterator = evaluateFunctionCallExpression(LLVMVariable("$rangeTypeName.iterator", returnType, scope = LLVMVariableScope()), listOf(range))!!
        val iteratorThisArgument = codeBuilder.loadArgumentIfRequired(conditionIterator, LLVMVariable("type", descriptor.type, pointer = 1))
        codeBuilder.addUnconditionalJump(conditionLabel)
        codeBuilder.markWithLabel(conditionLabel)
        var conditionResult = evaluateFunctionCallExpression(LLVMVariable("$returnTypeName.hasNext", LLVMBooleanType(), scope = LLVMVariableScope()), listOf(iteratorThisArgument))!!

        while (conditionResult.pointer > 0) {
            conditionResult = codeBuilder.loadAndGetVariable(conditionResult as LLVMVariable)
        }

        codeBuilder.addCondition(conditionResult, bodyLabel, exitLabel)

        codeBuilder.addUnconditionalJump(bodyLabel)
        codeBuilder.markWithLabel(bodyLabel)
        val currentParameter = evaluateFunctionCallExpression(LLVMVariable("$returnTypeName.nextInt", LLVMIntType(), scope = LLVMVariableScope()), listOf(iteratorThisArgument))!!

        val allocVar = variableManager.receiveVariable(expr.loopParameter!!.name!!, nextDescriptor.returnType!!.type, LLVMRegisterScope(), pointer =
            nextDescriptor.returnType!!.pointer)
        variableManager.addVariable(expr.loopParameter!!.name!!, allocVar, scopeDepth + 1)
        codeBuilder.allocStackVar(allocVar)
        allocVar.pointer++
        allocVar.kotlinName = expr.loopParameter!!.name!!

        addPrimitiveBinaryOperation(KtTokens.EQ, null, allocVar, currentParameter)

        evaluateCodeBlock(expr.body, null, conditionLabel, exitLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLabel)

        return null
    }
}