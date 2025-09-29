package org.kotlinnative.translator

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.optimization.common.analyze
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.js.dce.Analyzer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.PathUtil
import org.kotlinnative.translator.debug.printFile
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.utils.FunctionDescriptor
import java.io.File
import kotlin.script.dependencies.Environment

class TranslationState(sources: List<String>, disposer: Disposable) {
    val environment: KotlinCoreEnvironment
    val bindingContext: BindingContext?
    var functions = HashMap<String, FunctionDescriptor>()
    val variableManager = VariableManager()

    private val classPath : ArrayList<File> by lazy {
        val classpath = arrayListOf<File>()
        classpath += PathUtil.getResourcePathForClass(AnnotationTarget.CLASS.javaClass)
        classpath
    }
    init {
        val configuration = CompilerConfiguration()
        val messageCollector = GroupingMessageCollector(object : MessageCollector {
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
                println("[report] $message")
                hasError = severity.isError || hasError
            }
        }, false)
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
        configuration.addKotlinSourceRoots(sources)

        environment = KotlinCoreEnvironment.createForProduction(disposer, configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES)
        bindingContext = analyze(environment)?.bindingContext //?: throw TranslationException()
    }

    fun analyze(environment: KotlinCoreEnvironment) : AnalysisResult? {
        val collector = environment.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val resolvedKlibs = environment.configuration.get(JVMConfigurationKeys.KLIB_PATHS)?.let { klibPaths ->
            jvmResolveLibraries(klibPaths, collector.toLogger())
        }?.getFullList() ?: emptyList()

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(environment.configuration)
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
            val project = environment.project
            val sourceFiles = environment.getSourceFiles()
            val sourcesOnly = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, sourceFiles)
            val moduleOutputs = environment.configuration.get(JVMConfigurationKeys.MODULES)?.mapNotNullTo(hashSetOf()) { module ->
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