package org.liamjd.bascule

import mu.KotlinLogging
import org.liamjd.bascule.cache.HandlebarsTemplateCacheItem
import org.liamjd.bascule.lib.FileHandler
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.FileSystems
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * File handling utility for creating files and directories, extracting files from resources, etc.
 */
class BasculeFileHandler : FileHandler {

	private val logger = KotlinLogging.logger {}
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
		logger.error {"Could not create directory $parentPath$pathSeparator$folderName"}
		throw Exception("Could not create directory $parentPath$pathSeparator$folderName")
	}

	override fun createDirectories(parentPath: String, folderName: String): File {
		val folder = File(parentPath.replace("/", pathSeparator), folderName)
		if (!folder.exists()) {
			if (folder.mkdirs()) {
				return folder
			}
		} else {
			return folder
		}
		logger.error {"Could not create directories $parentPath\$folderName"}
		throw Exception("Could not create directories $parentPath\$folderName")
	}

	@Throws(FileNotFoundException::class)
	override fun getFileStream(folder: File, fileName: String): InputStream {
		val file = File(folder, fileName)
		if(!file.exists() || !file.canRead()) throw FileNotFoundException("Could not find (or read) file ${folder.name}/$fileName")
		return file.inputStream()
	}

	override fun readFileAsString(folder: File, fileName: String): String {
		val fileStream = getFileStream(folder,fileName)
		return fileStream.bufferedReader().use(BufferedReader::readText)
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
		logger.debug {"Purging folder ${folder.name} of '$fileType' files" }
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
		logger.debug {"Purging folder ${folder.name}" }
		folder.deleteRecursively()
	}

	override fun copyFile(source: File, destination: File): File {
		logger.debug {"Copying '${source.path}' to '${destination.path}'" }
		return source.copyTo(destination, overwrite = true)
	}

	override fun getFile(folder: File, fileName: String) : File {
		return File(folder,fileName)
	}

	override fun readFileAsString(fileName: String) : String {
		val file = File(fileName)
		return readFileAsString(file.parentFile,file.name)
	}
	}
