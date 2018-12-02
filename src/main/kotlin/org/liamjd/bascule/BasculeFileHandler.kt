package org.liamjd.bascule

import org.liamjd.bascule.lib.FileHandler
import println.info
import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems

/**
 * File handling utility for creating files and directories, extracting files from resources, etc.
 */
class BasculeFileHandler: FileHandler {

	override val pathSeparator = FileSystems.getDefault().separator!!

	override fun createDirectories(path: String): Boolean {
		val folders = File(path.replace("/", pathSeparator))
		return folders.mkdirs()
	}

	/**
	 * Create a directory tree matching the given file path
	 * @param[path] path of directories to create
	 */
	override fun createDirectories(path: File): Boolean {
		return path.mkdirs()
	}

	/**
	 * Create a directory with the given folderName in the file path
	 * If the folder already exists, it is returned. Otherwise, the new folder is created and returned
	 */
	override fun createDirectory(parentPath: String, folderName: String): File {
		val folder = File(parentPath.replace("/", pathSeparator), folderName)
		if (!folder.exists()) {
			if (folder.mkdir()) {
				return folder
			}
		} else {
			return folder
		}
		throw Exception("Could not create directory $parentPath$pathSeparator$folderName")
	}

	override fun createDirectories(parentPath: String, folderName: String): File {
		val folder = File(parentPath.replace("/", pathSeparator), folderName)
		if (folder.mkdirs()) {
			return folder
		}
		throw Exception("Could not create directories $parentPath/$folderName")
	}

	override fun getFileStream(folder: File, fileName: String): InputStream {
		val file = File(folder, fileName)
		return file.inputStream()
	}

	/**
	 * Reads a file from /src/main/resources/_sourceDir_ and writes it to _destination_.
	 * You can override the filename by supplying a value for _destFileName_
	 * @param[fileName] Name of the file to copy
	 * @param[destination] Folder the file is to be copied to
	 * @param[destFileName] The final name of the file once copied. If left out, the destination file will have the same name as the source
	 * @param[sourceDir] the folder within /src/main/resources to copy from. Optional.
	 */
	override fun copyFileFromResources(fileName: String, destination: File, destFileName: String?, sourceDir: String) {
		val data = readFileFromResources(sourceDir, fileName)
		val finalFileName = destFileName ?: fileName

		writeFile(destination, finalFileName, data)
	}

	/**
	 * Write the specified data to the file finalFileName in the directory destination
	 * @param[data] The string to write
	 * @param[finalFileName] The file name
	 * @param[destination] The destination directory
	 */
	override fun writeFile(destination: File, finalFileName: String, data: String) {
		File(destination, finalFileName).bufferedWriter().use { out ->
			out.write(data)
		}
	}

	/**
	 * Read a text file from the project resource package
	 */
	override fun readFileFromResources(sourceDir: String, fileName: String): String {
		return javaClass.getResource(sourceDir + fileName).readText()
	}

	/**
	 * Delete all files of the specified type from the given directory
	 * @param[folder] the folder to empty
	 * @param[fileType] the type of file to delete, e.g. ".html"
	 */
	override fun emptyFolder(folder: File, fileType: String) {
		info("Clearing out old generated files")
		folder.walk().forEach {
			if (it != folder && it.name.endsWith(fileType)) {
				it.delete()
			}
		}
	}

	/**
	 * Delete everything in the given folder. Destructive!
	 * @param[folder] the folder to empty recursively
	 */
	override fun emptyFolder(folder: File) {
		info("Purging folder ${folder.name}")
		folder.deleteRecursively()
	}
}
