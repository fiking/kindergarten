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
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.codegens.ClassCodegen
import org.kotlinnative.translator.codegens.FunctionCodegen
import org.kotlinnative.translator.codegens.ObjectCodegen
import org.kotlinnative.translator.codegens.PropertyCodegen
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import java.io.File

class TranslationState
private constructor
    (
    val environment: KotlinCoreEnvironment,
    val bindingContext: BindingContext,
    val mainFunction: String, arm: Boolean
) {
    var externalFunctions = HashMap<String, FunctionCodegen>()
    var functions = HashMap<String, FunctionCodegen>()
    var classes = HashMap<String, ClassCodegen>()
    var objects = HashMap<String, ObjectCodegen>()
    var properties = HashMap<String, PropertyCodegen>()
    val codeBuilder = LLVMBuilder(arm)
    val extensionFunctions = HashMap<String, HashMap<String, FunctionCodegen>>()
    val globalVariableCollection = HashMap<String, LLVMVariable>()

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
            val RUNTIME_JAR = File(
                System.getProperty("kotlin.runtime.path") ?: "/Users/bytedance/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler/1.9.0/1b80b7d3dc77f12a52893f25c502f380705ad55d/kotlin-compiler-1.9.0.jar"
            )
            val JDK_PATH = File("/Users/bytedance/.gradle/jdks/azul_systems__inc_-8-aarch64-os_x.2/zulu-8.jdk/Contents/Home/jre/lib/rt.jar")
            configuration.addJvmClasspathRoot(JDK_PATH)
            configuration.addJvmClasspathRoot(RUNTIME_JAR)
            configuration.configureJdkClasspathRoots()
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

