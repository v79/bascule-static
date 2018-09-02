package org.liamjd.bascule

import org.fusesource.jansi.AnsiConsole
import org.liamjd.bascule.generator.Generator
import org.liamjd.bascule.initializer.Destroyer
import org.liamjd.bascule.initializer.Initializer
import picocli.CommandLine
import println.info

@CommandLine.Command(name = "Bascule", version = ["0.0.1"],
		mixinStandardHelpOptions = true,
		description = ["Bascule static site generator"],
		subcommands = arrayOf(Generator::class))
class Bascule : Runnable {

	@CommandLine.Option(names = ["-n", "-new"], description = ["name of static site"])
	var siteName: String = ""

	@CommandLine.Option(names = ["-deleteSite"], description = ["destroy your entire website!"])
	var deleteAllName: String = ""

	init {
		AnsiConsole.systemInstall();
	}

	override fun run() {
		info(Constants.logos[(0..Constants.logos.size-1).random()])

		if(siteName.isNotBlank()) {
			Initializer(siteName)
		}

		if(deleteAllName.isNotBlank()) {
			Destroyer(deleteAllName)
		}
	}


}