package org.example

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import kotlin.script.dependencies.Environment

class KotlinScriptParser {
    companion object {
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
       // configuration.put(JVMConfigurationKey, JvmAbi)

        val rootDisposable = Disposer.newDisposable()
        try {
//            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration,
//                EnvironmentConfigFiles.JVM_CONFIG_FILES)
//            val ktFiles = environment.getSourceFiles()
////            val sharedTrace = CliLightClassGenerationSupport
//            val container = TopDownAnalyzerFacadeForJVM.createContainer()
//            val additionalProviders = ArrayList<PackageFragmentProvider>()
            return null
        } finally {
            rootDisposable.dispose()
        }
    }
}