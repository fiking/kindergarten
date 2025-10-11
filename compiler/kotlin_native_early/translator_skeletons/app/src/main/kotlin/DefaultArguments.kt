import com.jshmrsn.karg.Arguments
import com.jshmrsn.karg.RawArguments

class DefaultArguments(raw: RawArguments) : Arguments(raw, name = "default") {

    val includeDir = optionalParameter(
        name = "include directory",
        aliasNames = listOf("include"),
        shortNames = listOf('I')
    )

    val arm = optionalFlag(
        name = "arm",
        description = "enable arm build",
        aliasNames = listOf("arm")
    )

    val output = optionalParameter(
        name = "output",
        description = "output file",
        shortNames = listOf('o')
    )

    val mainClass = optionalParameter(
        name = "main class",
        aliasNames = listOf("main"),
        default = "main",
        shortNames = listOf('M')
    )

    val sources = positionalArguments(
        name = "sources",
        description = "source files",
        minCount = 1
    )
}