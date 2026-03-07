package backend.native.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.toLogger
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.ir.backend.jvm.jvmResolveLibraries
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.utils.KotlinPaths
import kotlin.collections.orEmpty

private val JvmAbi.DEFAULT_MODULE_NAME: String
    get() = "benchmark"

class K2Native() : CLICompiler<K2NativeCompilerArguments>() {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            doMain(K2Native(), args)
        }
    }

    override val defaultPerformanceManager: CommonCompilerPerformanceManager by lazy {
        K2NativeCompilerPerformanceManager()
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        TODO("Not yet implemented")
    }

    override fun doExecute(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)
        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable, configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
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

        val translator = Psi2IrTranslator(, Psi2IrConfiguration(false))
        val module = translator.generateModule(analyzerWithCompilerReport.analysisResult.moduleDescriptor,
            environment.getSourceFiles(),
            analyzerWithCompilerReport.analysisResult.bindingContext)

        module.accept(DumpIrTreeVisitor(out), "")
        return ExitCode.OK
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
        services: Services
    ) {
        configuration.setupFromArguments(arguments)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {
        TODO("Not yet implemented")
    }

    override fun createArguments(): K2NativeCompilerArguments = K2NativeCompilerArguments()

    override fun executableScriptFileName(): String {
        TODO("Not yet implemented")
    }
}

private fun CompilerConfiguration.setupFromArguments(arguments: K2NativeCompilerArguments) {

}

fun main(args: Array<String>) = K2Native.main(args)
