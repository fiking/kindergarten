package org.kotlinnative.translator

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM.createContainer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.util.DummyLogger
import org.kotlinnative.translator.debug.printFile

class KotlinScriptParser {
    companion object {
        private val messageCollector = object : MessageCollector {
            override fun hasErrors(): Boolean {
                throw UnsupportedOperationException()
            }

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val path = location?.path
                val position = if (path == null) "" else "$path: (${location.line}, ${location.column})"
                val text = position + message

                if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
                    DummyLogger.log(text)
                }
            }

            override fun clear() {
                TODO("Not yet implemented")
            }
        }
        private val classPath : ArrayList<File> by lazy {
            val classpath = arrayListOf<File>()
            classpath += PathUtil.getResourcePathForClass(AnnotationTarget.CLASS.javaClass)
            classpath
        }
    }
    fun parse(vararg files: String) : TopDownAnalysisContext? {
        // The Kotlin compiler configuration
        val configuration = CompilerConfiguration()
        configuration.addJvmClasspathRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
        // The path to .kt files sources
        files.forEach { configuration.addKotlinSourceRoot(it) }
        // Configuring Kotlin class path
        configuration.addJvmClasspathRoots(classPath)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        val rootDisposable = Disposer.newDisposable()

        try {
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val project = environment.project
            val sourceFiles = environment.getSourceFiles()
            // for debug printing
            printFile(sourceFiles.first())
            val container = createContainer(
                project, sourceFiles, NoScopeRecordCliBindingTrace(), configuration, environment::createPackagePartProvider,
                ::FileBasedDeclarationProviderFactory, CompilerEnvironment)
            return container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.LocalDeclarations, sourceFiles)
        } finally {
            rootDisposable.dispose()
        }
    }
}