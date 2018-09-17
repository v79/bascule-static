package org.liamjd.bascule

import org.fusesource.jansi.AnsiConsole
import org.koin.log.EmptyLogger
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext
import org.koin.standalone.get
import org.liamjd.bascule.generator.Generator
import org.liamjd.bascule.initializer.BasculeInitializer
import org.liamjd.bascule.initializer.Destroyer
import org.liamjd.bascule.initializer.Themes
import picocli.CommandLine
import println.info

/**
 * Command line parser for Bascule. Declares the different ways of running Bascule
 */
@CommandLine.Command(name = "Bascule", version = ["0.0.1"],
		mixinStandardHelpOptions = true,
		description = ["Bascule static site generator"],
		subcommands = [Generator::class, Themes::class])
class Bascule : Runnable, KoinComponent {

	@CommandLine.Option(names = ["-n", "--new"], description = ["name of static site"])
	var siteName: String = ""

	@CommandLine.Option(names = ["--theme"], description = ["in-built theme to base your site on (run 'bascule themes' to see a list of options)"])
	var themeName: String? = null

	@CommandLine.Option(names = ["--deleteSite"], description = ["destroy your entire website!"])
	var deleteAllName: String = ""

	init {
		AnsiConsole.systemInstall()
		// start Koin DI, change logger to PrintLogger() for DI logs or EmptyLogger for no logs
		StandAloneContext.startKoin(listOf(generationModule, fileModule), logger = EmptyLogger())
	}

	override fun run() {
		info(Constants.logos[(0 until Constants.logos.size).random()])

		if(siteName.isNotBlank()) {
			val initializer = BasculeInitializer(siteName, themeName, get())
			initializer.create()
		}

		if(deleteAllName.isNotBlank()) {
			Destroyer(deleteAllName)
		}
	}


}
