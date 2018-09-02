package org.liamjd.bascule

import org.liamjd.bascule.generator.Generator
import org.liamjd.bascule.initializer.Destroyer
import org.liamjd.bascule.initializer.Initializer
import picocli.CommandLine

@CommandLine.Command(name = "Bascule", version = ["0.0.1"],
		mixinStandardHelpOptions = true,
		description = ["Bascule static site generator"],
		subcommands = arrayOf(Generator::class))
class Bascule : Runnable {

	@CommandLine.Option(names = ["-n", "-new"], description = ["name of static site"])
	var siteName: String = ""

	@CommandLine.Option(names = ["-deleteSite"], description = ["destroy your entire website!"])
	var deleteAllName: String = ""

	override fun run() {
		println("Welcome to Bascule")

		if(siteName.isNotBlank()) {
			Initializer(siteName)
		}

		if(deleteAllName.isNotBlank()) {
			Destroyer(deleteAllName)
		}
	}
}