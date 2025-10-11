package org.kotlinnative.translator

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable

class TranslationState
private constructor
    (
    val environment: KotlinCoreEnvironment,
    val bindingContext: BindingContext,
    val mainFunction: String, arm: Boolean
) {
    var externalFunctions = HashMap<String, FunctionCodegen>()
    var functions = HashMap<String, FunctionCodegen>()
    val globalVariableCollection = HashMap<String, LLVMVariable>()
    var classes = HashMap<String, ClassCodegen>()
    var objects = HashMap<String, ObjectCodegen>()
    var properties = HashMap<String, PropertyCodegen>()
    val codeBuilder = LLVMBuilder(arm)
    val extensionFunctions = HashMap<String, HashMap<String, FunctionCodegen>>()

    init {
        POINTER_ALIGN = if (arm) 4 else 8
        POINTER_SIZE = if (arm) 4 else 8
    }

    companion object {
        var POINTER_ALIGN = 4
        var POINTER_SIZE = 4

        fun createTranslationState(
            sources: List<String>,
            disposer: Disposable,
            mainFunction: String,
            arm: Boolean = false
        ): TranslationState {
            val configuration = CompilerConfiguration()
            val messageCollector = object : MessageCollector {
                private var hasError = false
                override fun clear() {
                    TODO("Not yet implemented")
                }

                override fun hasErrors(): Boolean = hasError
                override fun report(
                    severity: CompilerMessageSeverity,
                    message: String,
                    location: CompilerMessageSourceLocation?
                ) {
                    if (severity.isError) {
                        System.err.println("[${severity.toString()}]${location?.path} ${location?.line}:${location?.column} $message")
                        hasError = true
                    }
                }
            }
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
            configuration.addKotlinSourceRoots(sources)

            val environment = KotlinCoreEnvironment.createForProduction(
                disposer, configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
            val bindingContext = analyze(environment)?.bindingContext ?: throw TranslationException(
                "Can't initialize binding context for project"
            )
            return TranslationState(environment, bindingContext, mainFunction, arm)
        }

        private fun analyze(environment: KotlinCoreEnvironment): AnalysisResult? {
            val collector =
                environment.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            val resolvedKlibs =
                environment.configuration.get(JVMConfigurationKeys.KLIB_PATHS)?.let { klibPaths ->
                    jvmResolveLibraries(klibPaths, collector.toLogger())
                }?.getFullList() ?: emptyList()

            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(environment.configuration)
            analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
                val project = environment.project
                val sourceFiles = environment.getSourceFiles()
                val sourcesOnly =
                    TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
                val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)
                    ?.mapNotNullTo(hashSetOf()) { module ->
                        environment.findLocalFile(module.getOutputDirectory())
                    }.orEmpty()
                val scope = if (moduleOutputs.isEmpty()) sourcesOnly else sourcesOnly.uniteWith(
                    KotlinToJVMBytecodeCompiler.DirectoriesScope(project, moduleOutputs)
                )
//            printFile(sourceFiles.first())
                TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    project,
                    sourceFiles,
                    NoScopeRecordCliBindingTrace(),
                    environment.configuration,
                    environment::createPackagePartProvider,
                    sourceModuleSearchScope = scope,
                    klibList = resolvedKlibs
                )
            }
            val analysisResult = analyzerWithCompilerReport.analysisResult
            return if (!analyzerWithCompilerReport.hasErrors())
                analysisResult
            else
                null
        }
    }
}

