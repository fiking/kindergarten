import com.intellij.openapi.util.Disposer
import org.kotlinnative.translator.ProjectTranslator
import org.kotlinnative.translator.parseAndAnalyze
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Enter filename")
        return
    }

    val analyzedFiles = ArrayList<String>();
    val kotlib = File("build/resources/main/kotlib/kotlin").listFiles()
    for (resource in kotlib) {
        analyzedFiles.add(resource.absolutePath)
    }
    analyzedFiles.addAll(args.toList())

    val disposer = Disposer.newDisposable()
    val state = parseAndAnalyze(analyzedFiles, disposer, true)
    val files = state.environment.getSourceFiles()
    if (files.isEmpty()) {
        println("Empty")
        return
    }

    println(ProjectTranslator(files, state).generateCode())
}