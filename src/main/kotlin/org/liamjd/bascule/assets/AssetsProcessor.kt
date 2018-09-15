package org.liamjd.bascule.assets

import println.info
import java.io.File

/**
 * Copies and generates fixed assets, such images, CSS, etc
 */
class AssetsProcessor(val parent: File, val assetsDir: File, val outputDir: File) {

	// TODO: Need to make this cleverer, perhaps including executable actions like running sass for CSS.
	// AS it is, it's not respecting theme customisation
	fun copyStatics() {
	/*	info("Copying image and other assets")
		val newAssetsDir = File(outputDir, assetsDir.name)
		newAssetsDir.mkdir()
		assetsDir.copyRecursively(newAssetsDir, false)*/
	}

	fun copyTheme() {
		info("Copying theme template files")
	}
}