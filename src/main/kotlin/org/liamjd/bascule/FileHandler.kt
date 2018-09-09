package org.liamjd.bascule

import java.io.File

class FileHandler {

	fun createDirectories(path: String): Boolean {
		val folders = File(path)
		return folders.mkdirs()
	}

	fun createDirectories(path: File): Boolean {
		return path.mkdirs()
	}

	fun createDirectory(parentPath: String, folderName: String): File {
		val folder = File(parentPath, folderName)
		if (folder.mkdir() != false) {
			return folder
		}
		throw Exception("Could not create directory $parentPath/$folderName")
	}

	/**
	 * Reads a file from /src/main/resources/_sourceDir_ and writes it to _destination_.
	 * You can override the filename by supplying a value for _destFileName_
	 * @param[fileName] Name of the file to copy
	 * @param[destination] Folder the file is to be copied to
	 * @param[destFileName] The final name of the file once copied. If left out, the destination file will have the same name as the source
	 * @param[sourceDir] the folder within /src/main/resources to copy from. Optional.
	 */
	fun copyFileFromResources(fileName: String, destination: File, destFileName: String? = null, sourceDir: String = "") {
		val data = readFileFromResources(sourceDir, fileName)
		val finalFileName = if (destFileName == null) fileName else destFileName

		writeFile(destination, finalFileName, data)
	}

	/**
	 * Write the specified data to the file finalFileName in the directory destination
	 * @param[data] The string to write
	 * @param[finalFileName] The file name
	 * @param[destination] The destination directory
	 */
	fun writeFile(destination: File, finalFileName: String, data: String) {
		File(destination, finalFileName).bufferedWriter().use { out ->
			out.write(data)
		}
	}

	fun readFileFromResources(sourceDir: String, fileName: String): String {
		val data = this.javaClass.getResource(sourceDir + fileName).readText()
		return data
	}
}
