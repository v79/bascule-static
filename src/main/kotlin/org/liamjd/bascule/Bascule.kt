package org.liamjd.bascule

import org.fusesource.jansi.AnsiConsole
import org.koin.standalone.StandAloneContext
import org.liamjd.bascule.generator.Generator
import org.liamjd.bascule.initializer.Destroyer
import org.liamjd.bascule.initializer.Initializer
import org.liamjd.bascule.initializer.Themes
import picocli.CommandLine
import println.info

@CommandLine.Command(name = "Bascule", version = ["0.0.1"],
		mixinStandardHelpOptions = true,
		description = ["Bascule static site generator"],
		subcommands = arrayOf(Generator::class, Themes::class))
class Bascule : Runnable {

	@CommandLine.Option(names = ["-n", "--new"], description = ["name of static site"])
	var siteName: String = ""

	@CommandLine.Option(names = ["--theme"], description = ["in-build theme to base your site on (run 'bascule themes' to see a list of options)"])
	var themeName: String? = null

	@CommandLine.Option(names = ["--deleteSite"], description = ["destroy your entire website!"])
	var deleteAllName: String = ""

	init {
		AnsiConsole.systemInstall()

		// start Koin DI
		StandAloneContext.startKoin(listOf(generationModule))
	}

	override fun run() {
		info(Constants.logos[(0 until Constants.logos.size).random()])

		if(siteName.isNotBlank()) {
			val initializer = Initializer(siteName, themeName, FileHandler())
			initializer.create()
		}

		if(deleteAllName.isNotBlank()) {
			Destroyer(deleteAllName)
		}
	}


}