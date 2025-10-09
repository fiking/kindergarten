package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMClassVariable
import org.kotlinnative.translator.llvm.LLVMFunctionDescriptor
import org.kotlinnative.translator.llvm.LLVMMapStandardType
import org.kotlinnative.translator.llvm.LLVMRegisterScope
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.types.LLVMByteType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMVoidType

abstract class StructCodegen(open val state: TranslationState,
                             open val variableManager: VariableManager,
                             open val classOrObject: KtClassOrObject,
                             val classDescriptor: ClassDescriptor,
                             open val codeBuilder: LLVMBuilder,
                             val prefix: String = "") {
    val fields = ArrayList<LLVMVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    val nestedClasses = HashMap<String, ClassCodegen>()
    val constructorFields = ArrayList<LLVMVariable>()
    abstract val type: LLVMReferenceType
    abstract var size: Int
    var methods = HashMap<String, FunctionCodegen>()
    abstract var structName: String
    val fullName: String
        get() = "${if (prefix.isNotEmpty()) "${prefix}_" else ""}$structName"


    fun generate(declarations: List<KtDeclaration>) {
        generateStruct()
        generatePrimaryConstructor()

        for (declaration in declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, variableManager, declaration, codeBuilder)
                    methods.put(function.name, function)
                }
            }
        }
        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)
        for (function in methods.values) {
            function.generate(classVal)
        }
    }


    fun generateInnerFields(declarations: List<KtDeclaration>) {
        var offset = fields.size

        for (declaration in declarations) {
            when (declaration) {
                is KtProperty -> {
                    val ktType = state.bindingContext?.get(BindingContext.TYPE, declaration.typeReference)!!
                    val field = resolveType(declaration, ktType)
                    field.offset = offset
                    offset++

                    fields.add(field)
                    fieldsIndex[field.label] = field
                    size += field.type.size
                }
                is KtClass -> {
                    nestedClasses.put(declaration.name!!,
                        ClassCodegen(state,
                            VariableManager(state.globalVariableCollection),
                            declaration, codeBuilder,
                            fullName))
                }
            }
        }
    }

    private fun generateStruct() {
        codeBuilder.createClass(fullName, fields)
    }

    private fun generatePrimaryConstructor() {
        val argFields = ArrayList<LLVMVariable>()
        val refType = type.makeClone() as LLVMReferenceType
        refType.addParam("sret")
        refType.byRef = true

        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)

        argFields.add(classVal)
        argFields.addAll(constructorFields)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(fullName, argFields,
            LLVMVoidType(), arm = state.arm))

        codeBuilder.addStartExpression()
        generateLoadArguments(classVal)
        generateAssignments()
        generateReturn()
        genClassInitializers()
        codeBuilder.addAnyReturn(LLVMVoidType())
        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments(thisField: LLVMVariable) {

        val thisVariable = LLVMVariable(thisField.label, thisField.type, thisField.label,
            LLVMRegisterScope(), pointer = 0)
        codeBuilder.loadArgument(thisVariable, false)

        constructorFields.forEach {
            if (it.type !is LLVMReferenceType) {
                val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope())
                codeBuilder.loadArgument(loadVariable)
            }
        }
    }

    private fun generateAssignments() {
        constructorFields.forEach {
            when (it.type) {
                is LLVMReferenceType -> {
                    val classField = codeBuilder.getNewVariable(it.type, pointer = it.pointer + 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, it)
                }
                else -> {
                    val argument = codeBuilder.getNewVariable(it.type, it.pointer)
                    codeBuilder.loadVariable(argument, LLVMVariable("${it.label}.addr", it.type, scope = LLVMRegisterScope(), pointer = it.pointer + 1))
                    val classField = codeBuilder.getNewVariable(it.type, pointer = 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, argument)
                }
            }

        }
    }

    private fun generateReturn() {
        val dst = LLVMVariable("classvariable.this", type, scope = LLVMRegisterScope(), pointer = 1)
        val src = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)

        val castedDst = codeBuilder.bitcast(dst, LLVMVariable("", LLVMByteType(), pointer = 1))
        val castedSrc = codeBuilder.bitcast(src, LLVMVariable("", LLVMByteType(), pointer = 1))

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    protected fun resolveType(field: KtNamedDeclaration, ktType: KotlinType): LLVMClassVariable {
        val annotations = parseFieldAnnotations(field)

        val result = LLVMMapStandardType(field.name!!, ktType, LLVMRegisterScope())

        if (result.type is LLVMReferenceType) {
            val type = result.type as LLVMReferenceType
            type.prefix = "class"
            type.byRef = true
        }

        if (annotations.contains("Plain")) {
            result.pointer = 0
        }

        return LLVMClassVariable(result.label, result.type, result.pointer)
    }

    private fun parseFieldAnnotations(field: KtNamedDeclaration): Set<String> {
        val result = HashSet<String>()

        for (annotation in field.annotationEntries) {
            val annotationDescriptor = state.bindingContext?.get(BindingContext.ANNOTATION, annotation)
            val type = annotationDescriptor?.type.toString()

            result.add(type)
        }

        return result
    }

    protected fun genClassInitializers() {
        for (init in classOrObject.getAnonymousInitializers()) {
            val blockCodegen = object : BlockCodegen(state, variableManager, codeBuilder) {
                fun generate(expr: PsiElement?) {
                    evaluateCodeBlock(expr, scopeDepth = topLevel)
                }
            }
            blockCodegen.generate(init.body)
        }

    }
}
