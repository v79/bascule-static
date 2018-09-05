package org.liamjd.bascule

import java.util.*

fun ClosedRange<Int>.random() =
		Random().nextInt((endInclusive + 1) - start) + start

object Constants {
	val VERSION_STRING = "v0.0.1"


	// TODO: these will all be parameterised
	val SOURCE_DIR = "sources"
	val OUTPUT_DIR = "site"
	val ASSETS_DIR = "assets"
	val PAGES_DIR = "pages"
	val TEMPLATES_DIR = "templates"
	val DEFAULT_THEME = "unthemed"

	val CONFIG_YAML  = "/initailizer/config.yaml"
	val THEME_FOLDER = "/initailizer/themes/"


	val basculeLogo = """
8 888888888o          .8.            d888888o.       ,o888888o.    8 8888      88 8 8888         8 8888888888
8 8888    `88.       .888.         .`8888:' `88.    8888     `88.  8 8888      88 8 8888         8 8888
8 8888     `88      :88888.        8.`8888.   Y8 ,8 8888       `8. 8 8888      88 8 8888         8 8888
8 8888     ,88     . `88888.       `8.`8888.     88 8888           8 8888      88 8 8888         8 8888
8 8888.   ,88'    .8. `88888.       `8.`8888.    88 8888           8 8888      88 8 8888         8 888888888888
8 8888888888     .8`8. `88888.       `8.`8888.   88 8888           8 8888      88 8 8888         8 8888
8 8888    `88.  .8' `8. `88888.       `8.`8888.  88 8888           8 8888      88 8 8888         8 8888
8 8888      88 .8'   `8. `88888.  8b   `8.`8888. `8 8888       .8' ` 8888     ,8P 8 8888         8 8888
8 8888    ,88'.888888888. `88888. `8b.  ;8.`8888    8888     ,88'    8888   ,d8P  8 8888         8 8888
8 888888888P .8'       `8. `88888. `Y8888P ,88P'     `8888888P'       `Y88888P'   8 888888888888 8 888888888888 ${VERSION_STRING}

	""".trimIndent()

	val logo2 = """
  ____                       _
 | __ )  __ _ ___  ___ _   _| | ___
 |  _ \ / _` / __|/ __| | | | |/ _ \
 | |_) | (_| \__ \ (__| |_| | |  __/
 |____/ \__,_|___/\___|\__,_|_|\___| ${VERSION_STRING}

	""".trimIndent()


	val logo5 = """
    ____                        __
   / __ )____ _____________  __/ /__
  / __  / __ `/ ___/ ___/ / / / / _ \
 / /_/ / /_/ (__  ) /__/ /_/ / /  __/
/_____/\__,_/____/\___/\__,_/_/\___/  ${VERSION_STRING}

	""".trimIndent()

	val logos = arrayOf(basculeLogo, logo2, logo5)
}