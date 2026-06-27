package org.liamjd.bascule.assets

import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import println.clearProgress
import println.progress
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

	val pathSeparator = FileSystems.getDefault().separator!!

	fun copyStatics() {

		val destinationDir = project.dirs.output.path + pathSeparator + "assets" + pathSeparator
		copyDirectory(project.dirs.assets, destinationDir)
	}

	private fun copyFile(file: File, destinationDir: String) {
		fileHandler.copyFile(file, File(destinationDir + pathSeparator + file.name))
	}

	private fun copyDirectory(dir: File, destination: String) {
		val files = dir.listFiles() ?: return
		files.forEachIndexed { idx, file ->
			if (file.isDirectory) {
				fileHandler.createDirectories(destination, file.name)
				progress("Copying", idx, "Directory ${file.name}")
				copyDirectory(file, destination + pathSeparator + file.name)
			} else {
				progress("Copying", idx, file.name)
				copyFile(file, destination)
			}
		}
		clearProgress()
	}
}
