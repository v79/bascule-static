package org.liamjd.bascule.assets

import println.info
import java.io.File

class AssetsProcessor(val parent: File, val assetsDir: File, val outputDir: File) {

	fun copyStatics() {
		info("Copying image and other assets")
		val newAssetsDir = File(outputDir, assetsDir.name)
		newAssetsDir.mkdir()
		assetsDir.copyRecursively(newAssetsDir, false)
	}

	fun copyTheme() {
		info("Copying theme template files")
	}
}