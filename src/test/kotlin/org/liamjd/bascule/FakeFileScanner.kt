package org.liamjd.bascule

import java.io.File

/**
 * In-memory [FileScanner] test double. Model a directory tree by registering directories (with their
 * children) and files (with size + modification time), keyed by path. Unregistered paths behave as a
 * missing, zero-length, non-directory entry.
 */
class FakeFileScanner : FileScanner {

	private data class Entry(
		val isDirectory: Boolean,
		val children: List<File> = emptyList(),
		val length: Long = 0L,
		val lastModified: Long = 0L
	)

	private val entries = mutableMapOf<String, Entry>()

	fun addDirectory(path: File, children: List<File>): FakeFileScanner {
		entries[path.path] = Entry(isDirectory = true, children = children)
		return this
	}

	fun addFile(path: File, length: Long = 0L, lastModified: Long = 0L): FakeFileScanner {
		entries[path.path] = Entry(isDirectory = false, length = length, lastModified = lastModified)
		return this
	}

	override fun listFiles(folder: File): List<File> = entries[folder.path]?.children ?: emptyList()

	override fun isDirectory(file: File): Boolean = entries[file.path]?.isDirectory ?: false

	override fun length(file: File): Long = entries[file.path]?.length ?: 0L

	override fun lastModified(file: File): Long = entries[file.path]?.lastModified ?: 0L
}
