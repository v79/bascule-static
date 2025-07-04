package org.liamjd.bascule.assets

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import println.ProgressBar
import println.info
import java.io.File
import java.nio.file.FileSystems


/**
 * Copies and generates fixed assets, such images, CSS, etc
 */
class AssetsProcessor(val project: Project) : KoinComponent {

	private val logger = KotlinLogging.logger {}

	private val fileHandler: BasculeFileHandler by inject { parametersOf() }
	val pathSeparator = FileSystems.getDefault().separator!!

	fun copyStatics() {

		logger.info {"Copying asset files through processor" }
		val destinationDir = project.dirs.output.path + pathSeparator + "assets" + pathSeparator
		copyDirectory(project.dirs.assets, destinationDir)
	}

	private fun copyFile(file: File, destinationDir: String) {
		fileHandler.copyFile(file, File(destinationDir + pathSeparator + file.name))
	}

	private fun copyDirectory(dir: File, destination: String) {
		val files = dir.listFiles()
		val copyDirProgress = ProgressBar("Copying",messageLine = null,bar = true,max = files.size)
		files.forEachIndexed { idx, file ->
			if (file.isDirectory) {
				fileHandler.createDirectories(destination, file.name)
				copyDirProgress.progress(idx,"Directory ${file.name} to ${destination + pathSeparator + file.name}")
				copyDirectory(file, destination + pathSeparator + file.name)
			} else {
				copyDirProgress.progress(idx,"Copying ${file.name} to $destination")
				copyFile(file, destination)
			}
		}
		copyDirProgress.clear()
	}

	fun copyTheme() {
		info("Copying theme template files")
	}
}
