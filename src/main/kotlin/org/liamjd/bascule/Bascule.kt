package org.liamjd.bascule

import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.EmptyLogger
import org.liamjd.bascule.generator.Generator
import org.liamjd.bascule.initializer.BasculeInitializer
import org.liamjd.bascule.initializer.Destroyer
import org.liamjd.bascule.initializer.Themes
import picocli.CommandLine
import println.info

/**
 * Command line parser for Bascule. Declares the different ways of running Bascule
 */
@CommandLine.Command(
    name = "Bascule", version = [Constants.VERSION_STRING],
    mixinStandardHelpOptions = true,
    description = ["Bascule static site generator"],
    subcommands = [Generator::class, Themes::class]
)
class Bascule : Runnable, KoinComponent {

    @CommandLine.Option(names = ["-n", "--new"], description = ["generate a new website with the given name"])
    var siteName: String = ""

    @CommandLine.Option(
        names = ["--theme"],
        description = ["in-built theme to base your site on (run 'bascule themes' to see a list of options)"]
    )
    var themeName: String? = null

    @CommandLine.Option(names = ["--deleteSite"], description = ["destroy your entire website!"])
    var deleteAllName: String = ""

    init {
        // start Koin DI, change logger to PrintLogger() for DI logs or EmptyLogger for no logs
        // EmptyLogger suppresses Koin's internal DI logs from the console.
        // Koin starts before Picocli parses arguments, so we can't gate this on --verbose.
        startKoin {
            modules(generationModule, fileModule)
            logger(EmptyLogger())
        }
    }

    override fun run() {
        info(Constants.logos[(0 until Constants.logos.size).random()])

        if (siteName.isNotBlank()) {
            val initializer = BasculeInitializer(siteName, themeName, getKoin().get())
            initializer.create()
        }

        if (deleteAllName.isNotBlank()) {
            Destroyer(deleteAllName)
        }
    }


}
