package org.liamjd.bascule.assets

import mu.KotlinLogging
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import println.ProgressBar
import java.io.File
import java.nio.file.FileSystems


/**
 * Copies fixed assets — images, CSS, JavaScript, etc — from the project's assets directory into an
 * `assets/` subfolder of the output directory, preserving the nested folder structure.
 *
 * The [fileHandler] is supplied via the constructor (previously injected through Koin) so the
 * processor can be exercised with a test double.
 */
class AssetsProcessor(val project: Project, private val fileHandler: BasculeFileHandler) {

	private val logger = KotlinLogging.logger {}

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
}
